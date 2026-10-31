// script/ui/detailModal.js
import { state } from '../state.js';
import { getImageUrl, ensureArray, syncBodyModalLock } from '../utils.js';
import { deletePrompt, deleteImageFiles } from '../api.js';
import { showAlert, showConfirm } from './dialog.js';

// ---------- DOM 引用 ----------
let modal, img, loading, common, version, role, desc, descItem;
let counter, prevBtn, nextBtn, backBtn, copyBtn, downloadBtn, editBtn, deleteBtn;
let infoArea;   // 信息区域（用于重置滚动位置）
let readonlyMode = false;   // 只读模式（选择提示词时查看详情，隐藏编辑/删除）

// ---------- 初始化 ----------
export function initDetailModal() {
    modal = document.getElementById('detailModal');
    img = document.getElementById('modalImg');
    loading = document.getElementById('modalLoading');
    common = document.getElementById('modalCommon');
    version = document.getElementById('modalVersion');
    role = document.getElementById('modalRole');
    desc = document.getElementById('modalDesc');
    descItem = document.getElementById('modalDescItem');
    counter = document.getElementById('modalCounter');
    prevBtn = document.getElementById('modalPrevBtn');
    nextBtn = document.getElementById('modalNextBtn');
    backBtn = document.getElementById('modalBackBtn');
    copyBtn = document.getElementById('modalCopyBtn');
    downloadBtn = document.getElementById('modalDownloadBtn');
    editBtn = document.getElementById('modalEditBtn');
    deleteBtn = document.getElementById('modalDeleteBtn');
    infoArea = modal.querySelector('.modal-info-area');

    // 事件绑定
    backBtn.addEventListener('click', closeDetail);
    modal.addEventListener('click', (e) => { if (e.target === modal) closeDetail(); });
    document.addEventListener('keydown', handleKeydown);
    prevBtn.addEventListener('click', () => changeImage(-1));
    nextBtn.addEventListener('click', () => changeImage(1));
    copyBtn.addEventListener('click', copyCommonText);
    deleteBtn.addEventListener('click', handleDelete);
    editBtn.addEventListener('click', () => {
        const event = new CustomEvent('openEditModal', {
            detail: { index: state.currentModalIndex }
        });
        document.dispatchEvent(event);
    });

    // 重新设置下载按钮文本和事件
    downloadBtn.textContent = '保存图片';
    downloadBtn.innerHTML = '<svg viewBox="0 0 24 24"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg> 保存图片';
    downloadBtn.addEventListener('click', saveCurrentImage);
}

// ---------- 设置只读模式 ----------
export function setDetailReadonly(v) {
    readonlyMode = !!v;
    if (editBtn && deleteBtn) {
        editBtn.style.display = readonlyMode ? 'none' : 'inline-flex';
        deleteBtn.style.display = readonlyMode ? 'none' : 'inline-flex';
    }
}

// ---------- 打开详情 ----------
export function openDetail(index) {
    const entry = state.flatItems[index];
    if (!entry) return;
    state.currentModalIndex = index;
    const item = entry.item;

    // 处理图片
    const covers = ensureArray(item.cover);
    state.currentImages = covers.length > 0 ? covers.map(f => getImageUrl(f)) : [];
    if (state.currentImages.length === 0) {
        state.currentImages = ['data:image/svg+xml,%3Csvg xmlns="http://www.w3.org/2000/svg" width="400" height="400" viewBox="0 0 400 400"%3E%3Crect width="400" height="400" fill="%23d8e6ef"/%3E%3Ctext x="200" y="200" font-family="sans-serif" font-size="24" fill="%236a8fa8" text-anchor="middle" dy=".3em"%3E无图片%3C/text%3E%3C/svg%3E'];
    }
    state.currentImageIndex = 0;
    updateModalImage();

    // 填充信息
    common.textContent = item.common || '';
    version.textContent = item.version || '—';
    role.textContent = item.role || '—';
    const descText = item.desc?.trim() || '';
    if (descText) {
        desc.textContent = descText;
        descItem.classList.add('show');
    } else {
        descItem.classList.remove('show');
    }

    copyBtn.textContent = '复制';
    copyBtn.classList.remove('copied');

    // 编辑删除按钮（只读模式隐藏）
    editBtn.style.display = readonlyMode ? 'none' : 'inline-flex';
    deleteBtn.style.display = readonlyMode ? 'none' : 'inline-flex';

    updateNavButtons();

    // 每次打开都让信息区域回到顶部（避免残留上次的滚动位置）
    // 注意：必须先让模态框显示出来再重置滚动位置，
    // 否则在 display:none 状态下设置 scrollTop 不生效，详情还是会停留在最底部
    modal.classList.add('open');
    document.body.style.overflow = 'hidden';
    document.body.classList.add('modal-open');
    // 提升层级，确保在"选择提示词"等其它模态框之上显示（但低于后续打开的编辑/确认弹窗）
    modal.style.zIndex = '1000';
    // 多重保险：立即重置 + 双帧 + 延时，确保信息区稳稳显示在最顶部
    const resetScroll = () => {
        if (infoArea) infoArea.scrollTop = 0;
        modal.scrollTop = 0;
        const box = modal.querySelector('.modal-box');
        if (box) box.scrollTop = 0;
        const body = modal.querySelector('.modal-body');
        if (body) body.scrollTop = 0;
    };
    resetScroll();
    requestAnimationFrame(() => requestAnimationFrame(resetScroll));
    setTimeout(resetScroll, 60);
}

// ---------- 关闭详情 ----------
export function closeDetail() {
    modal.classList.remove('open');
    modal.style.zIndex = '';
    document.body.style.overflow = '';
    syncBodyModalLock();
    state.currentModalIndex = -1;
    state.currentImages = [];
    if (state.loadingImage) {
        state.loadingImage.onload = null;
        state.loadingImage.onerror = null;
        state.loadingImage.src = '';
        state.loadingImage = null;
    }
}

// ---------- 内部辅助函数 ----------
function updateModalImage() {
    const url = state.currentImages[state.currentImageIndex] || '';
    // 取消之前的加载
    if (state.loadingImage) {
        state.loadingImage.onload = null;
        state.loadingImage.onerror = null;
        state.loadingImage.src = '';
        state.loadingImage = null;
    }
    loading.style.display = 'flex';
    img.style.display = 'none';
    img.src = '';

    if (!url || url.startsWith('data:image/svg+xml')) {
        img.src = url || 'data:image/svg+xml,%3Csvg xmlns="http://www.w3.org/2000/svg" width="400" height="400" viewBox="0 0 400 400"%3E%3Crect width="400" height="400" fill="%23d8e6ef"/%3E%3Ctext x="200" y="200" font-family="sans-serif" font-size="24" fill="%236a8fa8" text-anchor="middle" dy=".3em"%3E无图片%3C/text%3E%3C/svg%3E';
        img.style.display = 'block';
        loading.style.display = 'none';
        counter.textContent = `${state.currentImageIndex + 1} / ${state.currentImages.length}`;
        updateNavButtons();
        return;
    }

    const newImg = new Image();
    state.loadingImage = newImg;
    newImg.onload = function() {
        if (state.loadingImage === newImg) {
            img.src = url;
            img.style.display = 'block';
            loading.style.display = 'none';
            state.loadingImage = null;
        }
    };
    newImg.onerror = function() {
        if (state.loadingImage === newImg) {
            img.src = 'data:image/svg+xml,%3Csvg xmlns="http://www.w3.org/2000/svg" width="400" height="400" viewBox="0 0 400 400"%3E%3Crect width="400" height="400" fill="%23d8e6ef"/%3E%3Ctext x="200" y="200" font-family="sans-serif" font-size="22" fill="%236a8fa8" text-anchor="middle" dy=".3em"%3E图片加载失败%3C/text%3E%3C/svg%3E';
            img.style.display = 'block';
            loading.style.display = 'none';
            state.loadingImage = null;
        }
    };
    newImg.src = url;
    counter.textContent = `${state.currentImageIndex + 1} / ${state.currentImages.length}`;
    updateNavButtons();
}

function updateNavButtons() {
    const total = state.currentImages.length;
    prevBtn.disabled = (total <= 1 || state.currentImageIndex === 0);
    nextBtn.disabled = (total <= 1 || state.currentImageIndex === total - 1);
}

function changeImage(delta) {
    const newIndex = state.currentImageIndex + delta;
    if (newIndex < 0 || newIndex >= state.currentImages.length) return;
    state.currentImageIndex = newIndex;
    updateModalImage();
}

function handleKeydown(e) {
    if (!modal.classList.contains('open')) return;
    if (e.key === 'Escape') {
        closeDetail();
        e.preventDefault();
    }
    if (e.key === 'ArrowLeft') {
        changeImage(-1);
        e.preventDefault();
    }
    if (e.key === 'ArrowRight') {
        changeImage(1);
        e.preventDefault();
    }
}

async function saveCurrentImage() {
    const imgUrl = img.src;
    if (!imgUrl || imgUrl.startsWith('data:image/svg+xml')) {
        await showAlert('当前图片无法保存（占位图或无图片）');
        return;
    }
    // 从 url 中提取文件名
    const parts = imgUrl.split('/');
    const filename = parts[parts.length - 1];
    if (!filename) {
        await showAlert('无法获取图片文件名');
        return;
    }
    try {
        const response = await fetch('/api/download-image', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ filename })
        });
        const result = await response.json();
        if (result.cancelled) {
            // 用户取消，不做任何提示
            return;
        }
        if (result.error) {
            await showAlert('保存失败: ' + result.error);
            return;
        }
        await showAlert('✅ 图片保存成功！');
    } catch (err) {
        await showAlert('保存失败: ' + err.message);
    }
}

function copyCommonText() {
    const text = common.textContent;
    if (!text) return;
    const copy = () => {
        if (navigator.clipboard?.writeText) {
            navigator.clipboard.writeText(text).then(() => feedback(true)).catch(() => fallback());
        } else {
            fallback();
        }
    };
    const fallback = () => {
        const ta = document.createElement('textarea');
        ta.value = text;
        ta.style.position = 'fixed';
        ta.style.left = '-9999px';
        document.body.appendChild(ta);
        ta.select();
        try {
            const ok = document.execCommand('copy');
            feedback(ok);
        } catch (_) {
            feedback(false);
        }
        document.body.removeChild(ta);
    };
    const feedback = (ok) => {
        if (ok) {
            copyBtn.textContent = '✅ 已复制';
            copyBtn.classList.add('copied');
        } else {
            copyBtn.textContent = '❌ 失败';
        }
        setTimeout(() => {
            copyBtn.textContent = '复制';
            copyBtn.classList.remove('copied');
        }, 2000);
    };
    copy();
}

// ---------- 删除提示词 ----------
async function handleDelete() {
    if (state.currentModalIndex < 0 || state.currentModalIndex >= state.flatItems.length) return;
    const entry = state.flatItems[state.currentModalIndex];
    if (!entry) return;
    const { group, item } = entry;
    
    const confirmed = await showConfirm(`确定要删除提示词 “${item.role}” 吗？此操作不可恢复。`);
    if (!confirmed) return;

    const covers = ensureArray(item.cover);
    // 先删图片，再删提示词
    deleteImageFiles(covers)
        .then(() => deletePrompt(group, item.role, item.common))
        .then(async () => {
            await showAlert('✅ 提示词已删除');
            closeDetail();
            // 触发刷新
            document.dispatchEvent(new CustomEvent('refreshDisplay'));
        })
        .catch(async (err) => await showAlert('❌ 删除失败: ' + err.message));
}