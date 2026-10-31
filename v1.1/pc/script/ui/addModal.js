import { state } from '../state.js';
import { uploadImages, addPrompt, recoverPrompt } from '../api.js';   // 导入 uploadImages
import { initGroupInput } from './common/groupInput.js';
import { renderImageList } from './common/imageList.js';
import { showAlert, showConfirm } from './dialog.js';

// ---------- DOM 引用 ----------
let modal, backBtn, cancelBtn, submitBtn;
let groupInputEl, groupSuggestEl, groupHintEl, groupInput;
let roleInput, commonInput, versionInput, descInput;
let imageList, fileInput;
let initialSnapshot = '';

// ---------- 初始化 ----------
export function initAddModal() {
    modal = document.getElementById('addPromptModal');
    backBtn = document.getElementById('addModalBackBtn');
    cancelBtn = document.getElementById('addModalCancelBtn');
    submitBtn = document.getElementById('addModalSubmitBtn');
    groupInputEl = document.getElementById('addGroup');
    groupSuggestEl = document.getElementById('addGroupSuggest');
    groupHintEl = document.getElementById('addGroupHint');
    roleInput = document.getElementById('addRole');
    commonInput = document.getElementById('addCommon');
    versionInput = document.getElementById('addVersion');
    descInput = document.getElementById('addDesc');
    imageList = document.getElementById('addImageList');
    fileInput = document.getElementById('addImageFileInput');

    groupInput = initGroupInput(groupInputEl, groupSuggestEl, groupHintEl, 'add');

    // 事件绑定
    backBtn.addEventListener('click', () => closeAddModal());
    cancelBtn.addEventListener('click', () => closeAddModal());
    modal.addEventListener('click', (e) => { if (e.target === modal) closeAddModal(); });
    submitBtn.addEventListener('click', handleSubmit);
    fileInput.addEventListener('change', handleFileSelect);
}

// ---------- 打开模态框 ----------
export function openAddModal() {
    modal.classList.add('open');
    document.body.classList.add('modal-open');

    // 清空表单
    roleInput.value = '';
    commonInput.value = '';
    versionInput.value = '';
    descInput.value = '';
    state.addPendingFiles = [];
    renderAddImageList();

    // 分组输入框重置（并提示可选的已有分组）
    groupInput.reset('');
    initialSnapshot = getAddSnapshot();
}

// ---------- 快照 / 修改检测 ----------
function getAddSnapshot() {
    return JSON.stringify({
        group: groupInput.getValue(),
        role: roleInput.value,
        common: commonInput.value,
        version: versionInput.value,
        desc: descInput.value,
        images: state.addPendingFiles.map(f => f.name + ':' + f.size)
    });
}

function isAddDirty() {
    return getAddSnapshot() !== initialSnapshot;
}

// ---------- 关闭模态框 ----------
export async function closeAddModal(force = false) {
    if (!force && isAddDirty()) {
        const ok = await showConfirm('您已对提示词内容进行了更改，确定要取消吗？取消后填写的内容将消失');
        if (!ok) return;
    }
    modal.classList.remove('open');
    document.body.classList.remove('modal-open');
    fileInput.value = '';
}

// ---------- 渲染图片列表 ----------
function renderAddImageList() {
    const items = state.addPendingFiles.map(f => f.name + ' (待上传)');
    renderImageList(imageList, items, (idx) => {
        state.addPendingFiles.splice(idx, 1);
        renderAddImageList();
    });
}

// ---------- 文件选择事件 ----------
function handleFileSelect() {
    const files = Array.from(this.files);
    if (files.length === 0) return;
    state.addPendingFiles.push(...files);
    this.value = '';
    renderAddImageList();
}

// ---------- 提交表单 ----------
async function handleSubmit() {
    const group = groupInput.getValue();
    if (!group) {
        await showAlert('请输入分组名称');
        groupInputEl.focus();
        return;
    }

    const role = roleInput.value.trim();
    const common = commonInput.value.trim();
    const version = versionInput.value.trim();
    const desc = descInput.value.trim();

    if (!common) {
        await showAlert('请填写指令内容');
        commonInput.focus();
        return;
    }
    if (state.addPendingFiles.length > 0) {
        uploadImages(state.addPendingFiles)   // ✅ 改为 uploadImages
            .then(filenames => {
                return submitAddPrompt(group, role, common, version, desc, filenames);
            })
            .catch(async err => await showAlert('图片上传失败: ' + err.message));
    } else {
        submitAddPrompt(group, role, common, version, desc, []);
    }
}

// ---------- 提交提示词数据 ----------
function submitAddPrompt(group, role, common, version, desc, coverFilenames) {
    const newItem = {
        id: crypto.randomUUID(),   // 生成唯一 ID
        common: common,
        version: version || '',
        role: role,
        desc: desc || '',
        cover: coverFilenames
    };

    // 恢复模式：用户修改后用数据覆盖原本错误的地方
    if (state.recoveryPending) {
        const recoveryData = state.recoveryPending;
        const groups = Object.keys(recoveryData);
        const origGroup = groups[0] || group;
        const items = recoveryData[origGroup] || [];
        if (items.length > 0 && items[0].id) newItem.id = items[0].id;
        if (origGroup === group) {
            if (items.length > 0) {
                items[0] = newItem;
            } else {
                recoveryData[origGroup] = [newItem];
            }
        } else {
            // 用户改了分组：原分组条目挪到新分组
            delete recoveryData[origGroup];
            recoveryData[group] = [newItem];
        }
        state.recoveryPending = null;
        return recoverPrompt(recoveryData)
            .then(async() => {
                await showAlert('✅ prompt.json 已修复！');
                closeAddModal(true);
                document.dispatchEvent(new CustomEvent('refreshDisplay'));
            })
            .catch(async err => await showAlert('❌ ' + err.message));
    }

    addPrompt(group, newItem)
        .then(async() => {
            await showAlert('✅ 提示词添加成功！');
            closeAddModal(true);
            document.dispatchEvent(new CustomEvent('refreshDisplay'));
        })
        .catch(async err => await showAlert('❌ ' + err.message));
}