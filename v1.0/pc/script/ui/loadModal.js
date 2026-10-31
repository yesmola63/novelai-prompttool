// script/ui/loadModal.js
// 加载提示词界面
import { state } from '../state.js';
import { selectLoadFile, unlockLoadFile, applyLoad } from '../api.js';
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
}

export function openLoadModal() {
    modal.classList.add('open');
    document.body.style.overflow = 'hidden';
    resetUI();
}

function closeLoadModal() {
    modal.classList.remove('open');
    document.body.style.overflow = '';
    resetUI();
}

function resetUI() {
    fileNameEl.textContent = '';
    passwordGroup.style.display = 'none';
    passwordInput.value = '';
    errorEl.textContent = '';
    okEl.style.display = 'none';
    applyBtn.disabled = true;
}

function showError(msg) {
    errorEl.textContent = msg;
    okEl.style.display = 'none';
    applyBtn.disabled = true;
}

async function handleSelectFile() {
    try {
        const res = await selectLoadFile();
        if (res.cancelled) return;
        if (res.error) { showError(res.error); return; }
        fileNameEl.textContent = '✅ 已选择提示词文件';
        if (res.needPassword) {
            passwordGroup.style.display = 'block';
            passwordInput.focus();
            showError('该文件已加密，请输入密码');
        } else if (res.valid) {
            passwordGroup.style.display = 'none';
            errorEl.textContent = '';
            okEl.style.display = 'block';
            applyBtn.disabled = false;
        } else {
            passwordGroup.style.display = 'none';
            showError(res.error || '该文件可能不是提示词文件或者提示词文件损坏');
        }
    } catch (e) {
        showError(e.message);
    }
}

async function tryUnlock() {
    const pwd = passwordInput.value;
    if (!pwd) return;
    try {
        const res = await unlockLoadFile(pwd);
        if (res.valid) {
            errorEl.textContent = '';
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
    try {
        await applyLoad(mode);
        await showAlert('✅ 提示词加载成功！');
        closeLoadModal();
        document.dispatchEvent(new CustomEvent('refreshDisplay'));
    } catch (e) {
        await showAlert('❌ 加载失败：' + e.message);
    }
}
