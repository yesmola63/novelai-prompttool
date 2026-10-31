import { state } from './state.js';
import { getPrompts, updateAllPrompts } from './api.js';
import { renderCategoryNav } from './ui/categoryNav.js';
import { renderCards } from './ui/cards.js';
import { initDetailModal, closeDetail, setDetailReadonly } from './ui/detailModal.js';
import { initAddModal, openAddModal, closeAddModal } from './ui/addModal.js';
import { initEditModal, closeEditModal } from './ui/editModal.js';
import { initDialog } from './ui/dialog.js';
import { initPackModal, openPackModal } from './ui/packModal.js';
import { initSelectModal, closeSelectModal } from './ui/selectPrompts.js';
import { initLoadModal, openLoadModal } from './ui/loadModal.js';
import { initRecoverModal } from './ui/recoverModal.js';
import { initUpdateModal, checkForUpdate } from './ui/updateModal.js';

(function init() {
    try {
        // 滚动/触摸时禁用 hover 效果：Android WebView 触屏滚动划过卡片会触发 :hover，
        // 若 CSS 仍匹配 hover 状态会导致卡片缩放动画，造成"往上跳/抖动"的错觉。
        // 通过 body class 在滚动期间强制禁用所有 hover 样式。
        let hoverTimer = null;
        const disableHover = () => {
            document.body.classList.add('scrolling');
            clearTimeout(hoverTimer);
            hoverTimer = setTimeout(() => document.body.classList.remove('scrolling'), 150);
        };
        document.addEventListener('touchstart', disableHover, { passive: true });
        document.addEventListener('touchmove', disableHover, { passive: true });

        // 输入框获得焦点后滚动到可视区中间：
        // 弹窗内部是嵌套滚动容器，软键盘弹出时浏览器不一定能把输入框带进视野
        document.addEventListener('focusin', (e) => {
            const el = e.target;
            if (!el || typeof el.matches !== 'function') return;
            if (!el.matches('input, textarea, select')) return;
            setTimeout(() => {
                try {
                    el.scrollIntoView({ block: 'center', inline: 'nearest' });
                } catch (err) { /* 老 WebView 忽略 */ }
            }, 320); // 等软键盘动画结束再滚动
        }, true);

        // ===== 滚动诊断（排查"往下翻时往上飘"）：滚动容器为 #app =====
        (function scrollDiag() {
            const appEl = document.getElementById('app');
            if (!appEl) return;
            let lastY = appEl.scrollTop;
            let lastDir = 0;
            let quietUntil = 0;
            appEl.addEventListener('scroll', () => {
                const y = appEl.scrollTop;
                const now = Date.now();
                const dir = y > lastY ? 1 : (y < lastY ? -1 : 0);
                // 记录滚动方向突然反向（非用户手势导致的回跳）
                if (dir !== 0 && lastDir !== 0 && dir !== lastDir && now > quietUntil) {
                    const jump = lastY - y;
                    if (Math.abs(jump) > 5) {
                        console.log(`[scrollDiag] direction REVERSED y=${y} lastY=${lastY} jump=${jump} ts=${now}`);
                    }
                    quietUntil = now + 500;
                }
                lastDir = dir;
                lastY = y;
            }, { passive: true });
        })();

        // 确保 DOM 已就绪
        function boot() {
            // DOM 引用
            const categoryNav = document.getElementById('categoryNav');
            const cardsWrapper = document.getElementById('promptCardsWrapper');
            const loadingOverlay = document.getElementById('promptLoading');
            const toolbar = document.getElementById('promptToolbar');
            const addPromptBtn = document.getElementById('addPromptBtn');
            const packPromptBtn = document.getElementById('packPromptBtn');
            const loadPromptBtn = document.getElementById('loadPromptBtn');

            // 任何关键元素缺失时写入诊断信息
            if (!categoryNav || !cardsWrapper || !loadingOverlay) {
                document.body.innerHTML = '<div style="padding:40px;text-align:center;color:#c0392b;">' +
                    '<p>❌ 页面初始化失败</p><p>关键 DOM 元素缺失，请检查 index.html</p></div>';
                return;
            }

            // 10 秒兜底：确保 loading overlay 最终被隐藏
            setTimeout(() => {
                if (loadingOverlay && loadingOverlay.style.display !== 'none') {
                    loadingOverlay.style.display = 'none';
                    const current = cardsWrapper.innerHTML.trim();
                    if (!current || current.length < 20) {
                        cardsWrapper.innerHTML =
                            '<div style="padding:40px 20px;text-align:center;color:#c0392b;font-size:14px;">' +
                            '<p>❌ 加载超时（10 秒）</p>' +
                            '<p style="font-size:12px;margin-top:8px;color:#666;">fetch /api/prompts 无响应</p>' +
                            '<p style="font-size:12px;margin-top:4px;color:#666;">请检查 logcat 中 NanoHTTPD 是否启动</p></div>';
                    }
                }
            }, 10000);

            // 绑定按钮事件
            if (addPromptBtn) addPromptBtn.addEventListener('click', openAddModal);
            if (packPromptBtn) packPromptBtn.addEventListener('click', openPackModal);
            if (loadPromptBtn) loadPromptBtn.addEventListener('click', openLoadModal);

            document.addEventListener('refreshDisplay', loadData);
            document.addEventListener('closeDetail', closeDetail);
            document.addEventListener('closeTopModal', closeTopModal);

            // 初始化模态框
            initDetailModal();
            initAddModal();
            initEditModal();
            initDialog();
            initPackModal();
            initSelectModal();
            initLoadModal();
            initRecoverModal();
            initUpdateModal();
            setDetailReadonly(false);

            // 启动加载
            loadData();

            // 立即检查更新：与首屏加载并行，不再额外叠加延迟
            checkForUpdate();
        }

        if (document.readyState === 'loading') {
            document.addEventListener('DOMContentLoaded', boot);
        } else {
            boot();
        }
    } catch (e) {
        console.error('main.js boot error:', e);
        try {
            document.body.innerHTML =
                '<div style="padding:40px;text-align:center;color:#c0392b;">' +
                '<p>❌ 应用启动错误</p>' +
                '<p style="font-size:12px;margin-top:8px;">' + (e.message || e) + '</p></div>';
        } catch (_) { /* ignore */ }
    }
})();

// 进度 overlay 元素
let progressOverlay, progressText, progressSpinner;

function createProgressOverlay() {
    if (progressOverlay) return;
    const promptContainer = document.getElementById('promptContainer');
    if (!promptContainer) return;
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
    promptContainer.appendChild(progressOverlay);
    progressOverlay.style.display = 'none';
}

function showProgressOverlay(title, current, total) {
    createProgressOverlay();
    progressOverlay.style.display = 'flex';
    progressText.textContent = `${title} (${current}/${total})`;
}

function updateProgress(current, total) {
    if (progressText) {
        const t = progressText.textContent.split('(')[0].trim();
        progressText.textContent = `${t} (${current}/${total})`;
    }
}

function hideProgressOverlay() {
    if (progressOverlay) progressOverlay.style.display = 'none';
}

// 关闭最顶层的模态框或详情页（Android 返回键 / 键盘 ESC 调用）
function closeTopModal() {
    const detailView = document.getElementById('detailModal');
    if (detailView && detailView.classList.contains('open')) {
        closeDetail();
        return true;
    }
    const all = Array.from(document.querySelectorAll('.modal-overlay.open'));
    if (all.length === 0) return false;
    // 所有 .modal-overlay 的 z-index 相同，视觉叠放顺序 = DOM 顺序，
    // 因此"最上层"就是 DOM 中最后打开/最后出现的那个（不要用 z-index 排序，会不稳定）
    const top = all[all.length - 1];
    const id = top.id;
    if (id === 'addPromptModal') { closeAddModal(true); return true; }
    if (id === 'editPromptModal') { closeEditModal(true); return true; }
    if (id === 'packModal') { top.classList.remove('open'); document.body.classList.remove('modal-open'); return true; }
    if (id === 'recoveryModal') { top.classList.remove('open'); document.body.classList.remove('modal-open'); top.remove(); return true; }
    if (id === 'loadPromptModal') { top.classList.remove('open'); document.body.classList.remove('modal-open'); return true; }
    // 选择提示词界面：走它自己的关闭流程（同时关闭详情页、通知打包界面选中状态）
    if (id === 'selectPromptModal') { closeSelectModal(); return true; }
    top.classList.remove('open');
    document.body.classList.remove('modal-open');
    return true;
}

window.closeTopModal = closeTopModal;

// 加载数据
async function loadData() {
    const loadingOverlay = document.getElementById('promptLoading');
    const toolbar = document.getElementById('promptToolbar');
    const categoryNav = document.getElementById('categoryNav');
    const cardsWrapper = document.getElementById('promptCardsWrapper');

    if (!loadingOverlay || !cardsWrapper) {
        console.error('loadData: critical DOM missing');
        return;
    }
    loadingOverlay.style.display = 'flex';
    if (toolbar) toolbar.style.display = 'none';
    let rendered = false;
    let hideTimer = null;
    try {
        const data = await Promise.race([
            getPrompts(),
            new Promise((_, rej) => {
                hideTimer = setTimeout(() => rej(new Error('加载超时（5秒）')), 5000);
            })
        ]);
        let needGenerate = false;
        if (data && typeof data === 'object') {
            for (const group of Object.keys(data)) {
                if (Array.isArray(data[group]) && data[group].some(item => !item.id)) {
                    needGenerate = true;
                    break;
                }
            }
        }
        let finalData = data || {};
        if (needGenerate) {
            let total = 0;
            for (const group of Object.keys(finalData)) {
                total += finalData[group].length;
            }
            showProgressOverlay('正在为提示词生成 UUID', 0, total);
            let generated = 0;
            for (const group of Object.keys(finalData)) {
                for (const item of finalData[group]) {
                    if (!item.id) item.id = crypto.randomUUID();
                    generated++;
                    updateProgress(generated, total);
                    await new Promise(r => setTimeout(r, 10));
                }
            }
            await updateAllPrompts(finalData);
            finalData = data;
            hideProgressOverlay();
        }
        state.allData = finalData;
        state.navPage = 0;
        renderCategoryNav(categoryNav, cardsWrapper);
        renderCards(state.allData, state.currentCategory, cardsWrapper);
        rendered = true;
    } catch (err) {
        console.error('loadData error:', err);
        cardsWrapper.innerHTML =
            '<div style="padding:40px 20px;text-align:center;color:#c0392b;font-size:14px;">' +
            '<p>❌ 加载失败</p>' +
            '<p style="color:#666;font-size:12px;margin-top:8px;">' + (err.message || String(err)) + '</p>' +
            '<p style="color:#666;font-size:12px;margin-top:4px;">请检查网络或重启应用</p></div>';
        rendered = true;
    } finally {
        if (hideTimer) clearTimeout(hideTimer);
        loadingOverlay.style.display = 'none';
        if (toolbar) toolbar.style.display = 'flex';
    }
}

window.refreshDisplay = loadData;
