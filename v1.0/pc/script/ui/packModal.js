// script/ui/packModal.js
// 打包提示词界面
import { packPrompts, getPackProgress, choosePackDir, checkPromptsEmpty } from '../api.js';
import { showAlert, showConfirm } from './dialog.js';
import { openSelectModal } from './selectPrompts.js';

let modal, backBtn, cancelBtn, confirmBtn;
let filenameInput, savePathInput, browseBtn, needPasswordChk, passwordInput;
let scopeRadios, selectBtn, selectedCountEl;
let imageModeRadios, webpOptions, webpQuality, webpQualityValue, webpQualityHint, qualityRow, zipOptions, zipModeSelect, customSlider, sliderThumb, sliderFill;

let selectedIds = [];
let initialSnapshot = '';

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
    scopeRadios = document.querySelectorAll('input[name="packScope"]');
    selectBtn = document.getElementById('packSelectBtn');
    selectedCountEl = document.getElementById('packSelectedCount');
    imageModeRadios = document.querySelectorAll('input[name="packImageMode"]');
    webpOptions = document.getElementById('webpOptions');
    webpQuality = document.getElementById('webpQuality');
    webpQualityValue = document.getElementById('webpQualityValue');
    webpQualityHint = document.getElementById('webpQualityHint');
    qualityRow = webpOptions.querySelector('.quality-row');
    zipOptions = document.getElementById('zipOptions');
    zipModeSelect = document.getElementById('packZipMode');
    customSlider = document.getElementById('customSlider');
    sliderThumb = customSlider.querySelector('.slider-thumb');
    sliderFill = customSlider.querySelector('.slider-fill');

    backBtn.addEventListener('click', handleCancel);
    cancelBtn.addEventListener('click', handleCancel);
    modal.addEventListener('click', (e) => { if (e.target === modal) handleCancel(); });
    confirmBtn.addEventListener('click', handleConfirm);

    needPasswordChk.addEventListener('change', () => {
        passwordInput.disabled = !needPasswordChk.checked;
        if (needPasswordChk.checked) passwordInput.focus();
    });

    scopeRadios.forEach(r => r.addEventListener('change', updateScopeUI));
    imageModeRadios.forEach(r => r.addEventListener('change', updateImageModeUI));
    webpQuality.addEventListener('input', updateQualityUI);
    document.querySelectorAll('input[name="webpMode"]').forEach(r => r.addEventListener('change', updateQualityUI));

    // 初始化自定义滑块
    initCustomSlider();

    browseBtn.addEventListener('click', async () => {
        try {
            const res = await choosePackDir();
            if (res.cancelled) return;
            if (res.error) { await showAlert('无法打开文件夹选择框: ' + res.error); return; }
            savePathInput.value = res.path;
        } catch (e) {
            await showAlert('选择文件夹失败: ' + e.message);
        }
    });

    selectBtn.addEventListener('click', openSelectModal);

    // 监听选择提示词结果
    document.addEventListener('packSelectionConfirmed', (e) => {
        selectedIds = e.detail.ids || [];
        updateScopeUI();
    });
    document.addEventListener('packSelectionChanged', (e) => {
        selectedIds = e.detail.ids || [];
        updateScopeUI();
    });

    updateScopeUI();
    updateImageModeUI();
    updateQualityUI();
}

export async function openPackModal() {
    // 空提示词拦截：没有任何提示词时不打开打包界面
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
    filenameInput.value = '';
    savePathInput.value = '';
    needPasswordChk.checked = false;
    passwordInput.value = '';
    passwordInput.disabled = true;
    document.querySelector('input[name="packScope"][value="all"]').checked = true;
    document.querySelector('input[name="packImageMode"][value="none"]').checked = true;
    document.querySelector('input[name="webpMode"][value="quality"]').checked = true;
    webpQuality.value = 80;
    zipModeSelect.value = 'standard';
    updateScopeUI();
    updateImageModeUI();
    updateQualityUI();
    initialSnapshot = getSnapshot();
    modal.classList.add('open');
    document.body.style.overflow = 'hidden';
    // modal 显示后再计算滑块位置（offsetWidth 此时才准确）
    updateCustomSlider(80);
}

function closePackModal() {
    modal.classList.remove('open');
    document.body.style.overflow = '';
}

function getSnapshot() {
    return JSON.stringify({
        filename: filenameInput.value,
        savePath: savePathInput.value,
        needPassword: needPasswordChk.checked,
        password: passwordInput.value,
        scope: document.querySelector('input[name="packScope"]:checked')?.value,
        imageMode: document.querySelector('input[name="packImageMode"]:checked')?.value,
        webpMode: document.querySelector('input[name="webpMode"]:checked')?.value,
        webpQuality: webpQuality.value,
        zipMode: zipModeSelect.value,
        selectedIds
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
    // 无损模式隐藏拖动条和提示，指定压缩率模式显示
    qualityRow.style.display = isLossless ? 'none' : 'flex';
    webpQualityHint.style.display = isLossless ? 'none' : 'block';

    if (isLossless) return;

    webpQualityValue.textContent = webpQuality.value;
    const v = parseInt(webpQuality.value, 10);
    const hint = QUALITY_HINTS.find(h => v >= h.min);
    webpQualityHint.textContent = hint ? hint.text : '';
}

// ---------- 自定义滑块 ----------
function updateCustomSlider(value) {
    value = Math.max(50, Math.min(100, parseInt(value) || 50));
    webpQuality.value = value;
    webpQualityValue.textContent = value;
    const percent = (value - 50) / 50;
    customSlider.style.setProperty('--percent', percent);
    const hint = QUALITY_HINTS.find(h => value >= h.min);
    webpQualityHint.textContent = hint ? hint.text : '';
}

function initCustomSlider() {
    updateCustomSlider(webpQuality.value);

    let dragging = false;

    function setValueFromX(clientX) {
        const rect = customSlider.getBoundingClientRect();
        const x = clientX - rect.left;
        const percent = Math.max(0, Math.min(1, x / rect.width));
        const value = Math.round(50 + percent * 50);
        updateCustomSlider(value);
    }

    customSlider.addEventListener('mousedown', (e) => {
        dragging = true;
        setValueFromX(e.clientX);
    });
    document.addEventListener('mousemove', (e) => {
        if (dragging) setValueFromX(e.clientX);
    });
    document.addEventListener('mouseup', () => { dragging = false; });
}

async function handleConfirm() {
    const filename = filenameInput.value.trim();
    if (!filename) { await showAlert('请填写打包后的文件名'); return; }
    const scope = document.querySelector('input[name="packScope"]:checked')?.value;
    const imageMode = document.querySelector('input[name="packImageMode"]:checked')?.value;
    const needPassword = needPasswordChk.checked;
    if (needPassword && !passwordInput.value) { await showAlert('请输入密码'); return; }
    if (scope === 'partial' && selectedIds.length === 0) { await showAlert('请先选择要打包的提示词'); return; }

    const payload = {
        filename,
        savePath: savePathInput.value.trim(),
        needPassword,
        password: needPassword ? passwordInput.value : '',
        imageMode,
        webpLossless: document.querySelector('input[name="webpMode"]:checked')?.value === 'lossless',
        webpQuality: parseInt(webpQuality.value, 10),
        zipMode: zipModeSelect.value,
        selectedIds: scope === 'partial' ? selectedIds : null
    };

    // 显示进度弹窗
    const progressModal = document.getElementById('packProgressModal');
    const progressText = document.getElementById('packProgressText');
    progressText.textContent = '正在初始化...';
    progressModal.classList.add('open');
    document.body.style.overflow = 'hidden';

    try {
        const { task_id } = await packPrompts(payload);
        const timer = setInterval(async () => {
            try {
                const info = await getPackProgress(task_id);
                progressText.textContent = info.text || '';
                if (info.done) {
                    clearInterval(timer);
                    progressModal.classList.remove('open');
                    document.body.style.overflow = '';
                    if (info.success) {
                        await showAlert('✅ 已打包完毕，文件保存在：\n' + info.message);
                        closePackModal();
                        document.dispatchEvent(new CustomEvent('refreshDisplay'));
                    } else {
                        await showAlert('❌ 打包失败：' + info.error);
                    }
                }
            } catch (e) {
                clearInterval(timer);
                progressModal.classList.remove('open');
                document.body.style.overflow = '';
                await showAlert('获取打包进度失败：' + e.message);
            }
        }, 500);
    } catch (e) {
        progressModal.classList.remove('open');
        document.body.style.overflow = '';
        await showAlert('启动打包失败：' + e.message);
    }
}
