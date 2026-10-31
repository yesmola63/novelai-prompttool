// script/ui/selectPrompts.js
// 选择提示词界面（打包部分提示词时使用）
import { state } from '../state.js';
import { getImageUrl, ensureArray } from '../utils.js';
import { openDetail, setDetailReadonly } from './detailModal.js';

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

function closeSelectModal() {
    modal.classList.remove('open');
    document.body.classList.remove('modal-open');
    setDetailReadonly(false);
    // 通知打包界面当前已选数量（用于取消后仍保留选择）
    document.dispatchEvent(new CustomEvent('packSelectionChanged', {
        detail: { ids: Array.from(selectedIds) }
    }));
    // 恢复主界面
    document.dispatchEvent(new CustomEvent('refreshDisplay'));
}

function handleConfirm() {
    document.dispatchEvent(new CustomEvent('packSelectionConfirmed', {
        detail: { ids: Array.from(selectedIds) }
    }));
    modal.classList.remove('open');
    document.body.classList.remove('modal-open');
    setDetailReadonly(false);
    document.dispatchEvent(new CustomEvent('refreshDisplay'));
}

function renderCategoryNav() {
    const groups = Object.keys(state.allData).filter(g => state.allData[g]?.length > 0);
    categoryNav.innerHTML = '';

    const all = createTag('全部', groups.reduce((s, g) => s + state.allData[g].length, 0));
    all.addEventListener('click', () => { currentCategory = '全部'; renderCategoryNav(); renderCards(); });
    categoryNav.appendChild(all);

    groups.forEach(g => {
        const tag = createTag(g, state.allData[g].length);
        tag.addEventListener('click', () => { currentCategory = g; renderCategoryNav(); renderCards(); });
        categoryNav.appendChild(tag);
    });
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
            const coverSrc = covers.length > 0 ? getImageUrl(covers[0]) : '';
            const role = item.role || '无角色';
            const safeRole = role.length > 20 ? role.slice(0, 18) + '…' : role;
            const isSel = selectedIds.has(item.id) ? ' checked' : '';
            const i = idx++;
            state.flatItems.push({ group: g, item, index: i });
            html += `
                <div class="prompt-card select-card" data-index="${i}" style="background-image: url('${coverSrc}');">
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
