// script/ui/selectPrompts.js
// 选择提示词界面（打包部分提示词时使用）
import { state } from '../state.js';
import { getImageUrl, ensureArray, syncBodyModalLock } from '../utils.js';
import { openDetail, setDetailReadonly, closeDetail } from './detailModal.js';
import { enableDragScroll } from './categoryNav.js';

let modal, categoryNav, cardsWrapper, countEl, confirmBtn, cancelBtn, backBtn;
let selectedIds = new Set();
let currentCategory = '全部';

export function initSelectModal() {
    modal = document.getElementById('selectPromptModal');
    categoryNav = document.getElementById('selectCategoryNav');
    cardsWrapper = document.getElementById('selectCardsWrapper');
    countEl = document.getElementById('selectCount');
    confirmBtn = document.getElementById('selectConfirmBtn');
    cancelBtn = document.getElementById('selectCancelBtn');
    backBtn = document.getElementById('selectModalBackBtn');

    cancelBtn.addEventListener('click', closeSelectModal);
    backBtn.addEventListener('click', closeSelectModal);
    modal.addEventListener('click', (e) => { if (e.target === modal) closeSelectModal(); });
    confirmBtn.addEventListener('click', handleConfirm);
}

export function openSelectModal() {
    selectedIds = new Set();
    currentCategory = '全部';
    setDetailReadonly(true);
    modal.classList.add('open');
    document.body.classList.add('modal-open');
    renderCategoryNav();
    renderCards();
    updateCount();
}

// 若详情页仍处于打开状态则关闭它：避免关闭选择界面后详情页残留在最上层，
// 导致"返回键先关详情再关打包界面"的顺序错乱，以及后续点击被详情页拦截
function closeDetailIfOpen() {
    const detail = document.getElementById('detailModal');
    if (detail && detail.classList.contains('open')) closeDetail();
}

export function closeSelectModal() {
    closeDetailIfOpen();
    modal.classList.remove('open');
    syncBodyModalLock();   // 打包界面此时仍开着，不能直接解锁背景滚动
    setDetailReadonly(false);
    // 通知打包界面当前已选数量（用于取消后仍保留选择）
    document.dispatchEvent(new CustomEvent('packSelectionChanged', {
        detail: { ids: Array.from(selectedIds) }
    }));
    // 恢复主界面
    document.dispatchEvent(new CustomEvent('refreshDisplay'));
}

function handleConfirm() {
    closeDetailIfOpen();
    modal.classList.remove('open');
    syncBodyModalLock();
    setDetailReadonly(false);
    document.dispatchEvent(new CustomEvent('packSelectionConfirmed', {
        detail: { ids: Array.from(selectedIds) }
    }));
    document.dispatchEvent(new CustomEvent('refreshDisplay'));
}

// 点击面板外部收起（只绑定一次）
let outsideBound = false;
function bindOutsideClose() {
    if (outsideBound) return;
    outsideBound = true;
    document.addEventListener('click', (e) => {
        if (!categoryNav || !categoryNav.classList.contains('nav-open')) return;
        if (!categoryNav.contains(e.target)) categoryNav.classList.remove('nav-open');
    }, true);
}

function renderCategoryNav() {
    const groups = Object.keys(state.allData).filter(g => state.allData[g]?.length > 0);
    const totalAll = groups.reduce((s, g) => s + state.allData[g].length, 0);
    const wasOpen = categoryNav.classList.contains('nav-open');
    categoryNav.innerHTML = '';
    if (groups.length === 0) {
        categoryNav.classList.remove('nav-open');
        return;
    }
    bindOutsideClose();

    const select = (name) => {
        currentCategory = name;
        categoryNav.classList.remove('nav-open');
        renderCategoryNav();
        renderCards();
    };

    // 横向可拖动滚动的标签条（与主界面结构一致）
    const scroller = document.createElement('div');
    scroller.className = 'category-tags';

    const all = createTag('全部', totalAll);
    all.addEventListener('click', () => select('全部'));
    scroller.appendChild(all);

    groups.forEach(g => {
        const tag = createTag(g, state.allData[g].length);
        tag.addEventListener('click', () => select(g));
        scroller.appendChild(tag);
    });

    categoryNav.appendChild(scroller);
    enableDragScroll(scroller);

    // 右侧箭头：展开 / 收起全部分类（与主界面一致）
    const arrow = document.createElement('button');
    arrow.className = 'nav-arrow cat-arrow';
    arrow.type = 'button';
    arrow.setAttribute('aria-label', '展开全部分类');
    arrow.innerHTML =
        '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.6" ' +
        'stroke-linecap="round" stroke-linejoin="round"><polyline points="9 6 15 12 9 18"></polyline></svg>';
    arrow.addEventListener('click', (e) => {
        e.stopPropagation();
        categoryNav.classList.toggle('nav-open');
    });
    categoryNav.appendChild(arrow);

    // 展开面板：一次性列出所有分类
    const panel = document.createElement('div');
    panel.className = 'category-panel';

    const panelAll = createTag('全部', totalAll);
    panelAll.addEventListener('click', () => select('全部'));
    panel.appendChild(panelAll);

    groups.forEach(g => {
        const tag = createTag(g, state.allData[g].length);
        tag.addEventListener('click', () => select(g));
        panel.appendChild(tag);
    });
    categoryNav.appendChild(panel);

    if (wasOpen) categoryNav.classList.add('nav-open');

    const active = scroller.querySelector('.category-tag.active');
    if (active) {
        try { active.scrollIntoView({ block: 'nearest', inline: 'center' }); } catch (e) { /* ignore */ }
    }
}

function createTag(name, count) {
    const div = document.createElement('div');
    div.className = 'category-tag' + (currentCategory === name ? ' active' : '');
    div.dataset.category = name;
    div.textContent = name;
    const badge = document.createElement('span');
    badge.className = 'badge';
    badge.textContent = count;
    div.appendChild(badge);
    return div;
}

function renderCards() {
    const data = state.allData;
    const filtered = currentCategory === '全部'
        ? data
        : (data[currentCategory] ? { [currentCategory]: data[currentCategory] } : {});
    const groups = Object.keys(filtered).filter(g => filtered[g]?.length > 0);

    if (groups.length === 0) {
        cardsWrapper.innerHTML = `<div class="empty-state">该分类下暂无提示词</div>`;
        state.flatItems = [];
        return;
    }

    let html = '';
    state.flatItems = [];
    let idx = 0;

    groups.forEach(g => {
        const items = filtered[g];
        html += `<div class="group-section">`;
        if (currentCategory === '全部') {
            html += `<div class="group-title">${g} <span class="badge">${items.length}</span></div>`;
        }
        html += `<div class="card-grid">`;
        items.forEach(item => {
            const covers = ensureArray(item.cover);
            // 与主界面保持一致：用 400px 缩略图 + <img class="card-img">（绝对定位铺满）
            // 不能再用 background-image，否则图片会按原始尺寸显示导致预览图异常
            const coverSrc = covers.length > 0 ? getImageUrl(covers[0], 400) : '';
            const role = item.role || '无角色';
            const safeRole = role.length > 20 ? role.slice(0, 18) + '…' : role;
            const isSel = selectedIds.has(item.id) ? ' checked' : '';
            const i = idx++;
            state.flatItems.push({ group: g, item, index: i });
            html += `
                <div class="prompt-card select-card" data-index="${i}">
                    ${coverSrc ? `<img class="card-img" loading="lazy" decoding="async" src="${coverSrc}" alt="">` : ''}
                    <div class="select-circle${isSel}" data-id="${item.id}"></div>
                    <div class="card-role-tag">${safeRole}</div>
                    <div class="card-overlay"><button class="card-detail-btn" type="button">查看详细信息</button></div>
                </div>
            `;
        });
        html += `</div></div>`;
    });

    cardsWrapper.innerHTML = html;

    // 切换选中状态（圆圈与封面共用）
    function toggleSelect(circle) {
        const id = circle.dataset.id;
        if (selectedIds.has(id)) {
            selectedIds.delete(id);
            circle.classList.remove('checked');
        } else {
            selectedIds.add(id);
            circle.classList.add('checked');
        }
        updateCount();
    }

    // 右上角圆圈：切换选中
    cardsWrapper.querySelectorAll('.select-circle').forEach(circle => {
        circle.addEventListener('click', (e) => {
            e.stopPropagation();
            toggleSelect(circle);
        });
    });

    // 点击封面（卡片主体）：选择该提示词（切换勾选）
    cardsWrapper.querySelectorAll('.prompt-card.select-card').forEach(card => {
        card.addEventListener('click', function() {
            const circle = this.querySelector('.select-circle');
            if (circle) toggleSelect(circle);
        });
    });

    // 点击"查看详细信息"按钮：打开详情（只读），不触发选中
    cardsWrapper.querySelectorAll('.card-detail-btn').forEach(btn => {
        btn.addEventListener('click', function(e) {
            e.stopPropagation();
            const card = this.closest('.prompt-card');
            const i = parseInt(card.dataset.index, 10);
            if (!isNaN(i) && i >= 0 && i < state.flatItems.length) openDetail(i);
        });
    });
}

function updateCount() {
    countEl.textContent = `已选 ${selectedIds.size} 个`;
    confirmBtn.textContent = `✅ 确定选择（${selectedIds.size}）`;
}
