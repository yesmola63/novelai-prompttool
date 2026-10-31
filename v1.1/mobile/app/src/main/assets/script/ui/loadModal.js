// script/ui/loadModal.js
// 加载提示词界面
import { state } from '../state.js';
import { selectLoadFile, unlockLoadFile, applyLoad, getLoadFileStatus, getLoadProgress } from '../api.js';
import { showAlert, showConfirmHtml } from './dialog.js';

let modal, backBtn, cancelBtn, applyBtn, selectFileBtn;
let fileNameEl, passwordGroup, passwordInput, errorEl, okEl;

let unlockTimer = null;

export function initLoadModal() {
    modal = document.getElementById('loadPromptModal');
    backBtn = document.getElementById('loadModalBackBtn');
    cancelBtn = document.getElementById('loadCancelBtn');
    applyBtn = document.getElementById('loadApplyBtn');
    selectFileBtn = document.getElementById('loadSelectFileBtn');
    fileNameEl = document.getElementById('loadFileName');
    passwordGroup = document.getElementById('loadPasswordGroup');
    passwordInput = document.getElementById('loadPassword');
    errorEl = document.getElementById('loadError');
    okEl = document.getElementById('loadOk');

    backBtn.addEventListener('click', closeLoadModal);
    cancelBtn.addEventListener('click', closeLoadModal);
    modal.addEventListener('click', (e) => { if (e.target === modal) closeLoadModal(); });
    selectFileBtn.addEventListener('click', handleSelectFile);
    applyBtn.addEventListener('click', handleApply);

    passwordInput.addEventListener('input', () => {
        clearTimeout(unlockTimer);
        unlockTimer = setTimeout(tryUnlock, 600);
    });
    passwordInput.addEventListener('keydown', (e) => {
        if (e.key === 'Enter') {
            clearTimeout(unlockTimer);
            tryUnlock();
        }
    });

    initPasswordToggle('loadPassword');
}

/**
 * 为密码输入框初始化眼睛切换按钮（明文/密文）
 * @param {string} inputId
 */
function initPasswordToggle(inputId) {
    const input = document.getElementById(inputId);
    const wrapper = input && input.closest('.password-field-wrapper');
    const btn = wrapper && wrapper.querySelector('.password-toggle-btn');
    if (!btn || !input) return;
    btn.addEventListener('click', () => {
        const eyeOff = btn.querySelector('.eye-off');
        const eyeOn = btn.querySelector('.eye-on');
        if (input.type === 'password') {
            input.type = 'text';
            if (eyeOff) eyeOff.style.display = 'none';
            if (eyeOn) eyeOn.style.display = 'block';
        } else {
            input.type = 'password';
            if (eyeOn) eyeOn.style.display = 'none';
            if (eyeOff) eyeOff.style.display = 'block';
        }
    });
}

export function openLoadModal() {
    modal.classList.add('open');
    document.body.classList.add('modal-open');
    resetUI();
}

function closeLoadModal() {
    modal.classList.remove('open');
    document.body.classList.remove('modal-open');
    resetUI();
}

function resetUI() {
    fileNameEl.textContent = '';
    fileNameEl.classList.remove('show');
    passwordGroup.style.display = 'none';
    passwordInput.value = '';
    errorEl.textContent = '';
    errorEl.classList.remove('show');
    okEl.style.display = 'none';
    okEl.textContent = '✅ 文件校验通过，可以加载';
    applyBtn.disabled = true;
}

const VALIDATION_OK_TEXT = '✅ 文件校验通过，可以加载';

function showError(msg) {
    errorEl.textContent = msg;
    errorEl.classList.add('show');
    okEl.style.display = 'none';
    applyBtn.disabled = true;
}

async function handleSelectFile() {
    console.log('[loadModal] handleSelectFile: launching picker');
    try {
        await selectLoadFile();
        console.log('[loadModal] selectLoadFile() returned; starting poll loop');
        let tries = 0;
        while (tries < 40) {
            await new Promise(r => setTimeout(r, 500));
            tries++;
            const res = await getLoadFileStatus();
            console.log('[loadModal] select-file poll #' + tries + ' status=' + (res.status||'') + ' valid=' + res.valid + ' needPassword=' + res.needPassword + ' error=' + (res.error||''));
            if (res.cancelled) {
                fileNameEl.textContent = '';
                return;
            }
            if (res.status === 'pending') continue;

            if (res.error) {
                showError(res.error);
                return;
            }
            if (res.valid) {
                fileNameEl.textContent = '✅ 已选择：' + res.filename;
                fileNameEl.classList.add('show');
                okEl.textContent = VALIDATION_OK_TEXT;
                okEl.style.display = 'block';
                applyBtn.disabled = false;
                passwordGroup.style.display = 'none';
                console.log('[loadModal] file accepted, applying enabled');
                return;
            }
            if (res.needPassword) {
                fileNameEl.textContent = '🔒 已选择加密文件';
                fileNameEl.classList.add('show');
                passwordGroup.style.display = 'block';
                passwordInput.focus();
                showError('该文件已加密，请输入密码');
                return;
            }
        }
        showError('等待文件选择超时');
    } catch (e) {
        console.warn('[loadModal] selectFile error:', e);
        showError(e.message);
    }
}

async function tryUnlock() {
    const pwd = passwordInput.value;
    if (!pwd) return;
    try {
        const res = await unlockLoadFile(pwd);
        if (res.valid) {
            errorEl.classList.remove('show');
            okEl.textContent = VALIDATION_OK_TEXT;
            okEl.style.display = 'block';
            applyBtn.disabled = false;
        } else if (res.needPassword) {
            showError('密码不正确，请重试');
        } else {
            showError(res.error || '该文件可能不是提示词文件或者提示词文件损坏');
        }
    } catch (e) {
        showError(e.message);
    }
}

async function handleApply() {
    const hasPrompts = Object.keys(state.allData).some(g => (state.allData[g] || []).length > 0);
    let mode = 'merge';
    if (hasPrompts) {
        const choice = await showConfirmHtml(
            '当前您已保存有提示词，是否需要合并提示词？<br><br>' +
            '选择 <b>是</b>：合并提示词<br>' +
            '选择 <b style="color:#c0392b; font-weight:bold;">否</b>：会删除所有当前已保存的提示词和对应的图片，全部替换为提示词文件里的提示词',
            '合并提示词',
            '是',
            '否'
        );
        mode = choice ? 'merge' : 'replace';
    }
    console.log('[loadModal] handleApply mode=' + mode);
    try {
        const resp = await applyLoad(mode);
        const task_id = resp && resp.task_id;
        if (!task_id) {
            applyBtn.disabled = false;
            if (okEl) okEl.style.display = 'none';
            await showAlert('❌ 服务器未返回 task_id');
            return;
        }
        console.log('[loadModal] apply accepted task_id=' + task_id);
        applyBtn.disabled = true;
        const okEl = document.getElementById('loadOk');
        if (okEl) okEl.style.display = 'block';
        if (okEl) okEl.textContent = '正在加载，请稍候…';

        const IDLE_TIMEOUT_MS = 15000;
        const TOTAL_TIMEOUT_MS = 120000;
        const MAX_POLLS = 600;
        let idleTimer = setTimeout(() => {
            clearInterval(timer);
            applyBtn.disabled = false;
            if (okEl) okEl.textContent = '⚠️ 加载过程无响应，请重试';
        }, IDLE_TIMEOUT_MS);
        const totalTimer = setTimeout(() => {
            clearTimeout(idleTimer);
            clearInterval(timer);
            applyBtn.disabled = false;
            if (okEl) {
                okEl.textContent = '⚠️ 加载超时，请重试';
                okEl.style.display = 'none';
            }
        }, TOTAL_TIMEOUT_MS);

        let polls = 0;
        let timer = setInterval(async () => {
            polls++;
            if (polls > MAX_POLLS) {
                clearTimeout(idleTimer);
                clearTimeout(totalTimer);
                clearInterval(timer);
                applyBtn.disabled = false;
                await showAlert('获取加载进度失败：轮询次数超过上限');
                if (okEl) okEl.style.display = 'none';
                return;
            }
            try {
                const info = await getLoadProgress(task_id);
                console.log('[loadModal] poll #' + polls + ' status=' + (info.status||'') + ' done=' + info.done + ' text=' + (info.text||'') + ' success=' + info.success + ' type=' + typeof info.success);
                console.log('[loadModal] full response: ' + JSON.stringify(info));
                if (info.text && okEl && info.text !== okEl.textContent) {
                    okEl.textContent = info.text;
                    clearTimeout(idleTimer);
                    idleTimer = setTimeout(() => {
                        clearInterval(timer);
                        clearTimeout(totalTimer);
                        applyBtn.disabled = false;
                        if (okEl) okEl.textContent = '⚠️ 加载过程无响应，请重试';
                    }, IDLE_TIMEOUT_MS);
                }
                if (info.done) {
                    console.log('[loadModal] done=true, entering terminal branch, success=' + info.success);
                    clearTimeout(idleTimer);
                    clearTimeout(totalTimer);
                    clearInterval(timer);
                    if (info.success) {
                        console.log('[loadModal] success=true, showing success alert');
                        await showAlert('✅ 提示词加载成功！');
                        console.log('[loadModal] success alert dismissed, closing modal');
                        closeLoadModal();
                        document.dispatchEvent(new CustomEvent('refreshDisplay'));
                    } else {
                        console.log('[loadModal] success=false/error, showing error alert: ' + (info.error || 'no_error_field'));
                        await showAlert('❌ 加载失败：' + (info.error || '未知错误'));
                        applyBtn.disabled = false;
                        if (okEl) okEl.style.display = 'none';
                    }
                }
            } catch (e) {
                clearTimeout(idleTimer);
                clearTimeout(totalTimer);
                clearInterval(timer);
                await showAlert('获取加载进度失败：' + e.message);
                applyBtn.disabled = false;
                if (okEl) okEl.style.display = 'none';
            }
        }, 500);
    } catch (e) {
        applyBtn.disabled = false;
        await showAlert('❌ 加载失败：' + e.message);
    }
}
