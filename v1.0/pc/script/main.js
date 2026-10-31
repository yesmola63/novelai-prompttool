import { state } from './state.js';
import { getPrompts, updateAllPrompts } from './api.js';
import { renderCategoryNav } from './ui/categoryNav.js';
import { renderCards } from './ui/cards.js';
import { initDetailModal, closeDetail, setDetailReadonly } from './ui/detailModal.js';
import { initAddModal, openAddModal } from './ui/addModal.js';
import { initEditModal } from './ui/editModal.js';
import { initDialog } from './ui/dialog.js';
import { initPackModal, openPackModal } from './ui/packModal.js';
import { initSelectModal } from './ui/selectPrompts.js';
import { initLoadModal, openLoadModal } from './ui/loadModal.js';
import { initRecoverModal } from './ui/recoverModal.js';

// 监听事件
document.getElementById('addPromptBtn').addEventListener('click', openAddModal);
document.getElementById('packPromptBtn').addEventListener('click', openPackModal);
document.getElementById('loadPromptBtn').addEventListener('click', openLoadModal);
document.addEventListener('refreshDisplay', loadData);
document.addEventListener('closeDetail', closeDetail);

// DOM 引用
const categoryNav = document.getElementById('categoryNav');
const cardsWrapper = document.getElementById('promptCardsWrapper');
const loadingOverlay = document.getElementById('promptLoading');
const toolbar = document.getElementById('promptToolbar');

// 窗口尺寸变化时重新计算导航分页（窗口/全屏切换时动态调整）
let resizeTimer;
window.addEventListener('resize', () => {
    clearTimeout(resizeTimer);
    resizeTimer = setTimeout(() => {
        renderCategoryNav(categoryNav, cardsWrapper);
    }, 200);
});

// 初始化模态框
initDetailModal();
initAddModal();
initEditModal();
initDialog();
initPackModal();
initSelectModal();
initLoadModal();
initRecoverModal();
setDetailReadonly(false);

// 进度 overlay 元素
let progressOverlay, progressText, progressSpinner;

function createProgressOverlay() {
    if (progressOverlay) return;
    progressOverlay = document.createElement('div');
    progressOverlay.style.cssText = `
        position: absolute; inset: 0; background: rgba(232, 244, 248, 0.85);
        backdrop-filter: blur(4px); border-radius: 20px; display: flex;
        flex-direction: column; align-items: center; justify-content: center;
        z-index: 20; gap: 16px;
    `;
    progressSpinner = document.createElement('div');
    progressSpinner.style.cssText = `
        width: 48px; height: 48px;
        border: 5px solid #d0e2ed; border-top-color: #4a90d9;
        border-radius: 50%; animation: spin 0.9s linear infinite;
    `;
    progressText = document.createElement('p');
    progressText.style.cssText = 'font-size: 16px; color: #2c5a7a; margin: 0;';
    progressOverlay.appendChild(progressSpinner);
    progressOverlay.appendChild(progressText);
    document.getElementById('promptContainer').appendChild(progressOverlay);
    progressOverlay.style.display = 'none';
}

function showProgressOverlay(title, current, total) {
    createProgressOverlay();
    progressOverlay.style.display = 'flex';
    progressText.textContent = `${title} (${current}/${total})`;
}

function updateProgress(current, total) {
    if (progressText) {
        const title = progressText.textContent.split('(')[0].trim();
        progressText.textContent = `${title} (${current}/${total})`;
    }
}

function hideProgressOverlay() {
    if (progressOverlay) {
        progressOverlay.style.display = 'none';
    }
}

// 加载数据
async function loadData() {
    loadingOverlay.classList.remove('hidden');
    toolbar.style.display = 'none';
    try {
        const data = await getPrompts();
        let needGenerate = false;
        for (const group of Object.keys(data)) {
            if (data[group].some(item => !item.id)) {
                needGenerate = true;
                break;
            }
        }
        let finalData = data;
        if (needGenerate) {
            // 统计总数
            let total = 0;
            for (const group of Object.keys(data)) {
                total += data[group].length;
            }
            showProgressOverlay('正在为提示词生成 UUID', 0, total);
            let generated = 0;
            for (const group of Object.keys(data)) {
                for (const item of data[group]) {
                    if (!item.id) {
                        item.id = crypto.randomUUID();
                    }
                    generated++;
                    updateProgress(generated, total);
                    await new Promise(r => setTimeout(r, 10));
                }
            }
            // 保存到后端
            await updateAllPrompts(data);
            finalData = data;
            hideProgressOverlay();
        }
        state.allData = finalData;
        state.navPage = 0;
        renderCategoryNav(categoryNav, cardsWrapper);
        renderCards(state.allData, state.currentCategory, cardsWrapper);
        toolbar.style.display = 'flex';
    } catch (err) {
        console.error(err);
        cardsWrapper.innerHTML = `<div class="empty-state">加载失败: ${err.message}</div>`;
    } finally {
        loadingOverlay.classList.add('hidden');
    }
}

// 暴露刷新函数
window.refreshDisplay = loadData;

loadData();