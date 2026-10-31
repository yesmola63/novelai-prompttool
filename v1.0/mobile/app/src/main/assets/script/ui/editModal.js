import { state } from '../state.js';
import { getImageUrl, ensureArray } from '../utils.js';
import { updatePrompt, uploadImages, deleteImageFiles } from '../api.js'; 
import { populateGroupSelect } from './common/groupSelect.js';
import { renderImageList } from './common/imageList.js';
import { showAlert, showConfirm } from './dialog.js';

// ---------- DOM 引用 ----------
let modal, backBtn, cancelBtn, submitBtn;
let typeSelect, typeCustom, roleInput, commonInput, versionInput, descInput;
let imageList, fileInput;

// ---------- 编辑状态 ----------
let originalGroup = '';
let originalRole = '';
let originalCommon = '';
let existingImages = [];      // 已存在的文件名
let pendingFiles = [];        // 待上传的 File 对象
let originalId = '';
let initialSnapshot = '';

// ---------- 初始化 ----------
export function initEditModal() {
    modal = document.getElementById('editPromptModal');
    backBtn = document.getElementById('editModalBackBtn');
    cancelBtn = document.getElementById('editModalCancelBtn');
    submitBtn = document.getElementById('editModalSubmitBtn');
    typeSelect = document.getElementById('editTypeSelect');
    typeCustom = document.getElementById('editTypeCustom');
    roleInput = document.getElementById('editRole');
    commonInput = document.getElementById('editCommon');
    versionInput = document.getElementById('editVersion');
    descInput = document.getElementById('editDesc');
    imageList = document.getElementById('editImageList');
    fileInput = document.getElementById('editImageFileInput');

    // 事件绑定
    backBtn.addEventListener('click', () => closeEditModal());
    cancelBtn.addEventListener('click', () => closeEditModal());
    modal.addEventListener('click', (e) => { if (e.target === modal) closeEditModal(); });
    submitBtn.addEventListener('click', handleSubmit);
    fileInput.addEventListener('change', handleFileSelect);

    // 监听打开编辑事件（由详情弹窗触发）
    document.addEventListener('openEditModal', (e) => {
        const index = e.detail.index;
        openEditModal(index);
    });
}

// ---------- 打开编辑弹窗 ----------
export function openEditModal(index) {
    const entry = state.flatItems[index];
    if (!entry) return;
    const { group, item } = entry;

    // 保存原始信息
    originalGroup = group;
    originalRole = item.role;
    originalCommon = item.common;
    existingImages = ensureArray(item.cover);
    pendingFiles = [];

    // 填充表单
    populateGroupSelect(typeSelect, typeCustom, group);
    roleInput.value = item.role || '';
    commonInput.value = item.common || '';
    versionInput.value = item.version || '';
    descInput.value = item.desc || '';
    originalId = item.id || crypto.randomUUID();

    renderImageList(imageList, existingImages, deleteExistingImage, false);
    // 清除文件输入
    fileInput.value = '';
    initialSnapshot = getEditSnapshot();

    modal.classList.add('open');
    modal.style.zIndex = '1001';
    document.body.classList.add('modal-open');
}

// ---------- 快照 / 修改检测 ----------
function getEditSnapshot() {
    const group = typeSelect.value === '__new__' ? typeCustom.value : typeSelect.value;
    return JSON.stringify({
        group,
        role: roleInput.value,
        common: commonInput.value,
        version: versionInput.value,
        desc: descInput.value,
        existingImages,
        pendingFiles: pendingFiles.map(f => f.name + ':' + f.size)
    });
}

function isEditDirty() {
    return getEditSnapshot() !== initialSnapshot;
}

// ---------- 关闭编辑弹窗 ----------
export async function closeEditModal(force = false) {
    if (!force && isEditDirty()) {
        const ok = await showConfirm('您已对提示词内容进行了更改，确定要取消吗？取消后修改的内容将消失');
        if (!ok) return;
    }
    modal.classList.remove('open');
    modal.style.zIndex = '';
    document.body.classList.remove('modal-open');
    // 清理状态
    originalGroup = '';
    originalRole = '';
    originalCommon = '';
    existingImages = [];
    pendingFiles = [];
}

// ---------- 删除已有图片 ----------
// ---------- 删除已有图片 ----------
async function deleteExistingImage(index) {
    const filename = existingImages[index];
    if (!filename) return;
    const confirmed = await showConfirm(`确定要删除图片 ${filename} 吗？`);
    if (!confirmed) return;
    deleteImageFiles([filename])
        .then(() => {
            existingImages.splice(index, 1);
            renderImageList(imageList, existingImages, deleteExistingImage, false);
        })
        .catch(async err => await showAlert('删除图片失败: ' + err.message));
}

// ---------- 选择文件事件 ----------
function handleFileSelect(e) {
    const files = Array.from(e.target.files);
    if (files.length === 0) return;
    pendingFiles.push(...files);
    // 显示所有图片（已有 + 待上传）
    renderCombinedImages();
    fileInput.value = ''; // 清空以便重复选择
}

// ---------- 渲染合并图片列表 ----------
function renderCombinedImages() {
    // 先显示已有的，再显示待上传的（带标记）
    const allItems = [
        ...existingImages.map(f => ({ type: 'existing', label: f, data: f })),
        ...pendingFiles.map(f => ({ type: 'pending', label: f.name + ' (待上传)', data: f }))
    ];
    if (allItems.length === 0) {
        imageList.innerHTML = '<div style="color:#6a8fa8; font-size:13px;">暂无图片</div>';
        return;
    }
    let html = '';
    allItems.forEach((item, idx) => {
        html += `
            <div class="image-item">
                <span>${item.label}</span>
                <button class="del-img-btn" data-index="${idx}" data-type="${item.type}">✕</button>
            </div>
        `;
    });
    imageList.innerHTML = html;
    imageList.querySelectorAll('.del-img-btn').forEach(btn => {
        btn.addEventListener('click', function() {
            const idx = parseInt(this.dataset.index, 10);
            const type = this.dataset.type;
            if (type === 'existing') {
                deleteExistingImage(idx);
            } else if (type === 'pending') {
                // 从待上传列表中移除
                const pendingIdx = idx - existingImages.length;
                if (pendingIdx >= 0 && pendingIdx < pendingFiles.length) {
                    pendingFiles.splice(pendingIdx, 1);
                    renderCombinedImages();
                }
            }
        });
    });
}

// ---------- 提交编辑 ----------
async function handleSubmit() {
    let newGroup = typeSelect.value;
    if (newGroup === '__new__') {
        newGroup = typeCustom.value.trim();
        if (!newGroup) {
            await showAlert('请输入新分组名称');
            return;
        }
    }
    const newRole = roleInput.value.trim();
    const newCommon = commonInput.value.trim();
    const newVersion = versionInput.value.trim();
    const newDesc = descInput.value.trim();

    if (!newCommon) { await showAlert('请填写指令内容'); return; }

    // 如果有待上传图片，先上传
    if (pendingFiles.length > 0) {
        uploadImages(pendingFiles)
            .then(filenames => {
                const allFilenames = [...existingImages, ...filenames];
                return doUpdate(newGroup, newRole, newCommon, newVersion, newDesc, allFilenames);
            })
            .catch(async err => await showAlert('图片上传失败: ' + err.message));
    } else {
        doUpdate(newGroup, newRole, newCommon, newVersion, newDesc, existingImages);
    }
}

async function doUpdate(newGroup, newRole, newCommon, newVersion, newDesc, coverFilenames) {
    const newItem = {
        common: newCommon,
        version: newVersion || '',
        role: newRole,
        desc: newDesc || '',
        cover: coverFilenames.length > 0 ? coverFilenames : [],
        id: originalId
    };

    updatePrompt(originalGroup, originalRole, originalCommon, newGroup, newItem)
        .then(async() => {
            await showAlert('✅ 提示词已更新');
            closeEditModal(true);
            // 触发刷新
            document.dispatchEvent(new CustomEvent('refreshDisplay'));
            // 关闭详情（如果打开）
            if (state.currentModalIndex !== -1) {
                document.dispatchEvent(new CustomEvent('closeDetail'));
            }
        })
        .catch(async err => await showAlert('❌ 更新失败: ' + err.message));
}