// script/ui/packModal.js
// 打包提示词界面（带密码/webp/压缩模式）
import { packPrompts, getPackProgress, choosePackDir, getPackDirStatus, checkPromptsEmpty, checkPackFilename } from '../api.js';
import { showAlert, showConfirm, showConfirmHtml } from './dialog.js';
import { openSelectModal } from './selectPrompts.js';
import { syncBodyModalLock } from '../utils.js';

let modal, backBtn, cancelBtn, confirmBtn;
let filenameInput, savePathInput, browseBtn, needPasswordChk, passwordInput, passwordGroup;
let scopeRadios, selectBtn, selectedCountEl;
let imageModeRadios, webpOptions, webpQuality, webpQualityValue, webpQualityHint, zipOptions, zipModeSelect, customSlider, sliderThumb, sliderFill, qualityRow;

let selectedIds = [];
let initialSnapshot = '';
let selectedPackPath = '';

const QUALITY_HINTS = [
    { min: 96, text: '相对原图的视觉质量：几乎无损，肉眼看不出区别；体积减少范围：10%~20%' },
    { min: 86, text: '相对原图的视觉质量：非常高质量，放大看也几乎没区别；体积减少范围：30%~50%' },
    { min: 76, text: '相对原图的视觉质量：高质量，细微纹理有轻微损失，但基本看不出来；体积减少范围：50%~70%' },
    { min: 66, text: '相对原图的视觉质量：良好质量，仔细看能发现一些压缩痕迹；体积减少范围：70%~80%' },
    { min: 56, text: '相对原图的视觉质量：一般质量，能明显看到压缩痕迹，但还能接受；体积减少范围：80%~85%' },
    { min: 0, text: '相对原图的视觉质量：较差质量，有明显压缩块和细节丢失；体积减少范围：85%~90%' }
];

export function initPackModal() {
    modal = document.getElementById('packModal');
    backBtn = document.getElementById('packModalBackBtn');
    cancelBtn = document.getElementById('packCancelBtn');
    confirmBtn = document.getElementById('packConfirmBtn');
    filenameInput = document.getElementById('packFilename');
    savePathInput = document.getElementById('packSavePath');
    browseBtn = document.getElementById('packBrowseBtn');
    needPasswordChk = document.getElementById('packNeedPassword');
    passwordInput = document.getElementById('packPassword');
    passwordGroup = document.getElementById('packPasswordGroup');
    scopeRadios = document.querySelectorAll('input[name="packScope"]');
    selectBtn = document.getElementById('packSelectBtn');
    selectedCountEl = document.getElementById('packSelectedCount');
    imageModeRadios = document.querySelectorAll('input[name="packImageMode"]');
    webpOptions = document.getElementById('webpOptions');
    webpQuality = document.getElementById('webpQuality');
    webpQualityValue = document.getElementById('webpQualityValue');
    webpQualityHint = document.getElementById('webpQualityHint');
    zipOptions = document.getElementById('zipOptions');
    zipModeSelect = document.getElementById('packZipMode');
    customSlider = document.getElementById('customSlider');
    sliderThumb = customSlider.querySelector('.slider-thumb');
    sliderFill = customSlider.querySelector('.slider-fill');
    qualityRow = webpOptions.querySelector('.quality-row');

    backBtn.addEventListener('click', handleCancel);
    cancelBtn.addEventListener('click', handleCancel);
    modal.addEventListener('click', (e) => { if (e.target === modal) handleCancel(); });
    confirmBtn.addEventListener('click', handleConfirm);

    needPasswordChk.addEventListener('change', updatePasswordUI);

    scopeRadios.forEach(r => r.addEventListener('change', updateScopeUI));
    imageModeRadios.forEach(r => r.addEventListener('change', updateImageModeUI));
    webpQuality.addEventListener('input', updateQualityUI);
    document.querySelectorAll('input[name="webpMode"]').forEach(r => r.addEventListener('change', updateQualityUI));

    browseBtn.addEventListener('click', handleChooseDir);
    selectBtn.addEventListener('click', openSelectModal);

    initPasswordToggle('packPassword');

    initCustomSlider();
    updatePasswordUI();
    updateScopeUI();
    updateImageModeUI();
    updateQualityUI();
    updatePathDisplay();
}

/** 密码框：未勾选时隐藏，勾选后显示并聚焦 + 保证在可视区内 */
function updatePasswordUI() {
    const on = needPasswordChk.checked;
    if (passwordGroup) passwordGroup.classList.toggle('expanded', on);
    passwordInput.disabled = !on;
    if (on) {
        // 等显示动画开始后再聚焦
        setTimeout(() => {
            try { passwordInput.focus(); } catch (e) { /* ignore */ }
            try { passwordInput.scrollIntoView({ block: 'center', inline: 'nearest' }); } catch (e) { /* ignore */ }
        }, 160);
        // 软键盘动画结束后再校正一次位置，避免被键盘遮住
        setTimeout(() => {
            try { passwordInput.scrollIntoView({ block: 'center', inline: 'nearest' }); } catch (e) { /* ignore */ }
        }, 560);
    } else {
        passwordInput.value = '';
    }
    console.log('[pack] password ui on=' + on + ' cls=' + (passwordGroup ? passwordGroup.className : 'null'));
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
        if (input.disabled) return;
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

async function handleChooseDir() {
    try {
        const filename = filenameInput.value.trim() || 'prompt';
        await choosePackDir(filename);

        let tries = 0;
        while (tries < 20) {
            await new Promise(r => setTimeout(r, 500));
            tries++;
            const res = await getPackDirStatus();
            if (res.cancelled) {
                selectedPackPath = '';
                updatePathDisplay();
                return;
            }
            if (res.status === 'pending') continue;
            if (res.path) {
                selectedPackPath = res.path;
                updatePathDisplay();
            }
            return;
        }
    } catch (e) {
        await showAlert('选择路径失败：' + e.message);
    }
}

function updatePathDisplay() {
    if (savePathInput) {
        if (selectedPackPath) {
            savePathInput.value = selectedPackPath;
        } else {
            savePathInput.placeholder = '默认：Download/prompt_pack/';
            savePathInput.value = '';
        }
    }
}

export async function openPackModal() {
    try {
        const res = await checkPromptsEmpty();
        if (res.empty) {
            await showAlert('你当前未保存任何提示词\n请在保存了提示词后再使用打包提示词功能');
            return;
        }
    } catch (e) {
        await showAlert('检查提示词状态失败：' + e.message);
        return;
    }

    selectedIds = [];
    selectedPackPath = '';
    savePathInput.value = '';
    needPasswordChk.checked = false;
    passwordInput.value = '';
    passwordInput.disabled = true;
    filenameInput.value = '';
    document.querySelector('input[name="packScope"][value="all"]').checked = true;
    document.querySelector('input[name="packImageMode"][value="none"]').checked = true;
    document.querySelector('input[name="webpMode"][value="quality"]').checked = true;
    webpQuality.value = 80;
    updatePasswordUI();
    updateScopeUI();
    updateImageModeUI();
    updateQualityUI();
    updatePathDisplay();
    initialSnapshot = getSnapshot();
    modal.classList.add('open');
    document.body.classList.add('modal-open');
}

function closePackModal() {
    modal.classList.remove('open');
    syncBodyModalLock();
}

function getSnapshot() {
    return JSON.stringify({
        filename: filenameInput.value,
        needPassword: needPasswordChk.checked,
        password: passwordInput.value,
        scope: document.querySelector('input[name="packScope"]:checked')?.value,
        imageMode: document.querySelector('input[name="packImageMode"]:checked')?.value,
        webpMode: document.querySelector('input[name="webpMode"]:checked')?.value,
        webpQuality: webpQuality.value,
        selectedIds,
        selectedPackPath
    });
}

function isDirty() {
    return getSnapshot() !== initialSnapshot;
}

async function handleCancel() {
    if (isDirty()) {
        const ok = await showConfirm('您已对打包设置进行了更改，确定要取消吗？取消后配置的内容将消失');
        if (!ok) return;
    }
    closePackModal();
}

function updateScopeUI() {
    const scope = document.querySelector('input[name="packScope"]:checked')?.value;
    const partial = scope === 'partial';
    // 选择"打包所有提示词"时不需要选择按钮
    selectBtn.style.display = partial ? '' : 'none';
    selectBtn.disabled = !partial;
    if (partial) {
        selectedCountEl.style.display = 'block';
        selectedCountEl.textContent = `已选 ${selectedIds.length} 个提示词`;
    } else {
        selectedCountEl.style.display = 'none';
    }
}

function updateImageModeUI() {
    const mode = document.querySelector('input[name="packImageMode"]:checked')?.value;
    webpOptions.style.display = mode === 'webp' ? 'block' : 'none';
    zipOptions.style.display = mode === 'zip' ? 'block' : 'none';
}

function updateQualityUI() {
    const webpMode = document.querySelector('input[name="webpMode"]:checked')?.value;
    const isLossless = webpMode === 'lossless';
    webpQualityHint.style.display = isLossless ? 'none' : 'block';
    qualityRow.style.display = isLossless ? 'none' : 'flex';

    const value = parseInt(webpQuality.value, 10) || 80;
    webpQualityValue.textContent = value;
    updateCustomSlider(value);

    const hint = QUALITY_HINTS.find(h => value >= h.min);
    webpQualityHint.textContent = hint ? hint.text : '';
}

function updateCustomSlider(value) {
    const percent = (value - 50) / (100 - 50) * 100;
    customSlider.style.setProperty('--percent', percent + '%');
    sliderThumb.style.left = `calc(${percent}% - 7px)`;
    sliderFill.style.width = `${percent}%`;
}

function initCustomSlider() {
    // 使用 hidden 的 range input 处理拖动（兼容触摸）
    webpQuality.addEventListener('input', () => {
        updateQualityUI();
    });

    customSlider.addEventListener('mousedown', startDrag);
    customSlider.addEventListener('touchstart', startDrag, { passive: false });

    function startDrag(e) {
        if (e.type === 'touchstart') e.preventDefault();
        const rect = customSlider.getBoundingClientRect();
        const move = (ev) => {
            const clientX = ev.touches ? ev.touches[0].clientX : ev.clientX;
            const p = ((clientX - rect.left) / rect.width) * 100;
            const clamped = Math.max(0, Math.min(100, p));
            const val = Math.round(50 + clamped / 100 * 50);
            webpQuality.value = val;
            updateQualityUI();
        };
        const up = () => {
            document.removeEventListener('mousemove', move);
            document.removeEventListener('mouseup', up);
            document.removeEventListener('touchmove', move);
            document.removeEventListener('touchend', up);
        };
        document.addEventListener('mousemove', move);
        document.addEventListener('mouseup', up);
        document.addEventListener('touchmove', move);
        document.addEventListener('touchend', up);
        move(e);
    }

    updateCustomSlider(80);
}

async function handleConfirm() {
    const filename = filenameInput.value.trim() || 'prompt';
    const scope = document.querySelector('input[name="packScope"]:checked')?.value;
    const imageMode = document.querySelector('input[name="packImageMode"]:checked')?.value;

    if (scope === 'partial' && selectedIds.length === 0) { await showAlert('请先选择要打包的提示词'); return; }
    if (needPasswordChk.checked && !passwordInput.value) { await showAlert('请设置密码'); return; }

    // 检查目标文件名是否已存在（仅检查默认 Download 路径）
    if (!savePathInput.value) {
        try {
            const { exists } = await checkPackFilename(filename);
            if (exists) {
                const ok = await showConfirmHtml(
                    `文件名 "<b>${filename}.bundle</b>" 已存在于保存目录。<br><br>` +
                    `选择 <b>是</b>：直接替换该文件<br>` +
                    `选择 <b style="color:#c0392b; font-weight:bold;">否</b>：请修改文件名后重试`,
                    '文件名冲突',
                    '是',
                    '否'
                );
                if (ok) {
                    // 用户选择替换，打包时会先删除已有文件
                } else {
                    // 用户选择修改文件名，聚焦输入框并选中文本
                    filenameInput.focus();
                    filenameInput.select();
                    filenameInput.scrollIntoView({ behavior: 'smooth', block: 'center' });
                    return;
                }
            }
        } catch (e) {
            // 检查失败不阻塞打包
            console.warn('[packModal] filename check failed:', e.message);
        }
    }

    const payload = {
        filename,
        needPassword: needPasswordChk.checked,
        password: passwordInput.value || '',
        selectedIds: scope === 'partial' ? selectedIds : null,
        imageMode: imageMode || 'none',
        webpLossless: document.querySelector('input[name="webpMode"]:checked')?.value === 'lossless',
        webpQuality: parseInt(webpQuality.value, 10) || 80,
        zipMode: zipModeSelect?.value || 'standard',
        savePath: savePathInput.value || null
    };

    const progressModal = document.getElementById('packProgressModal');
    const progressText = document.getElementById('packProgressText');
    progressText.textContent = '正在初始化...';
    progressModal.classList.add('open');
    document.body.classList.add('modal-open');

    try {
        const { task_id } = await packPrompts(payload);
        const timer = setInterval(async () => {
            try {
                const info = await getPackProgress(task_id);
                progressText.textContent = info.text || '';
                if (info.done) {
                    clearInterval(timer);
                    progressModal.classList.remove('open');
                    syncBodyModalLock();
                    if (info.success) {
                        await showAlert('✅ 已打包完毕：' + info.message);
                        closePackModal();
                        document.dispatchEvent(new CustomEvent('refreshDisplay'));
                    } else {
                        await showAlert('❌ 打包失败：' + info.error);
                    }
                }
            } catch (e) {
                clearInterval(timer);
                progressModal.classList.remove('open');
                syncBodyModalLock();
                await showAlert('获取打包进度失败：' + e.message);
            }
        }, 500);
    } catch (e) {
        progressModal.classList.remove('open');
        syncBodyModalLock();
        await showAlert('启动打包失败：' + e.message);
    }
}

document.addEventListener('packSelectionConfirmed', (e) => {
    selectedIds = e.detail.ids || [];
    updateScopeUI();
});
document.addEventListener('packSelectionChanged', (e) => {
    selectedIds = e.detail.ids || [];
    updateScopeUI();
});