// script/ui/categoryNav.js
// 分类导航：横向拖动滚动 + 右侧箭头展开全部分类
import { state } from '../state.js';
import { renderCards } from './cards.js';

function createTag(name, count, currentCategory) {
    const div = document.createElement('div');
    div.className = 'category-tag' + (name === currentCategory ? ' active' : '');
    div.dataset.category = name;
    div.textContent = name;
    const badge = document.createElement('span');
    badge.className = 'badge';
    badge.textContent = count;
    div.appendChild(badge);
    return div;
}

// 点击面板外部收起（只绑定一次）
let outsideBound = false;
function bindOutsideClose() {
    if (outsideBound) return;
    outsideBound = true;
    document.addEventListener('click', (e) => {
        const nav = document.getElementById('categoryNav');
        if (!nav || !nav.classList.contains('nav-open')) return;
        if (!nav.contains(e.target)) nav.classList.remove('nav-open');
    }, true);
}

/** 让标签条支持鼠标拖动（触屏由原生横向滚动处理）+ 鼠标滚轮横向滚动 */
export function enableDragScroll(scroller) {
    let startX = 0;
    let startScroll = 0;
    let dragging = false;
    let moved = false;

    scroller.addEventListener('pointerdown', (e) => {
        if (e.pointerType === 'touch') return; // 触屏用原生滚动
        dragging = true;
        moved = false;
        startX = e.clientX;
        startScroll = scroller.scrollLeft;
        scroller.classList.add('dragging');
    });
    scroller.addEventListener('pointermove', (e) => {
        if (!dragging) return;
        const dx = e.clientX - startX;
        if (Math.abs(dx) > 6) moved = true;
        if (moved) {
            e.preventDefault();
            scroller.scrollLeft = startScroll - dx;
        }
    });
    const end = () => {
        dragging = false;
        scroller.classList.remove('dragging');
    };
    scroller.addEventListener('pointerup', end);
    scroller.addEventListener('pointercancel', end);
    scroller.addEventListener('pointerleave', end);
    // 拖动结束时抑制一次点击，避免误切换分类
    scroller.addEventListener('click', (e) => {
        if (moved) {
            e.stopPropagation();
            e.preventDefault();
            moved = false;
        }
    }, true);

    // 鼠标滚轮：纵向滚轮转成横向滚动（触控板横向滑动则原样处理）
    scroller.addEventListener('wheel', (e) => {
        if (scroller.scrollWidth <= scroller.clientWidth + 1) return; // 没有可滚动的量
        const delta = Math.abs(e.deltaX) > Math.abs(e.deltaY) ? e.deltaX : e.deltaY;
        if (!delta) return;
        e.preventDefault();
        scroller.scrollLeft += delta;
    }, { passive: false });
}

export function renderCategoryNav(container, cardsContainer) {
    const groups = Object.keys(state.allData).filter(g => state.allData[g]?.length > 0);
    const wasOpen = container.classList.contains('nav-open');

    container.innerHTML = '';
    if (groups.length === 0) {
        container.classList.remove('nav-open');
        return;
    }
    bindOutsideClose();

    const totalAll = groups.reduce((s, g) => s + state.allData[g].length, 0);

    const select = (name) => {
        state.currentCategory = name;
        container.classList.remove('nav-open');
        renderCategoryNav(container, cardsContainer);
        renderCards(state.allData, name, cardsContainer);
    };

    // ---- 横向可拖动的标签条 ----
    const scroller = document.createElement('div');
    scroller.className = 'category-tags';

    const allTag = createTag('全部', totalAll, state.currentCategory);
    allTag.addEventListener('click', () => select('全部'));
    scroller.appendChild(allTag);

    groups.forEach(g => {
        const tag = createTag(g, state.allData[g].length, state.currentCategory);
        tag.addEventListener('click', () => select(g));
        scroller.appendChild(tag);
    });

    container.appendChild(scroller);
    enableDragScroll(scroller);

    // ---- 右侧箭头：展开 / 收起全部分类 ----
    const arrow = document.createElement('button');
    arrow.className = 'nav-arrow cat-arrow';
    arrow.type = 'button';
    arrow.setAttribute('aria-label', '展开全部分类');
    arrow.innerHTML =
        '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.6" ' +
        'stroke-linecap="round" stroke-linejoin="round"><polyline points="9 6 15 12 9 18"></polyline></svg>';
    arrow.addEventListener('click', (e) => {
        e.stopPropagation();
        container.classList.toggle('nav-open');
    });
    container.appendChild(arrow);

    // ---- 展开面板：一次性列出所有分类 ----
    const panel = document.createElement('div');
    panel.className = 'category-panel';

    const panelAll = createTag('全部', totalAll, state.currentCategory);
    panelAll.addEventListener('click', () => select('全部'));
    panel.appendChild(panelAll);

    groups.forEach(g => {
        const tag = createTag(g, state.allData[g].length, state.currentCategory);
        tag.addEventListener('click', () => select(g));
        panel.appendChild(tag);
    });
    container.appendChild(panel);

    if (wasOpen) container.classList.add('nav-open');

    // 当前分类滚入可视区域
    const active = scroller.querySelector('.category-tag.active');
    if (active) {
        try {
            active.scrollIntoView({ block: 'nearest', inline: 'center' });
        } catch (e) { /* 老 WebView 忽略 */ }
    }
}
