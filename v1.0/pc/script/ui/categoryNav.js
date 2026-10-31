import { state } from '../state.js';
import { renderCards } from './cards.js';

const MIN_PAGE_SIZE = 2;
const MAX_PAGE_SIZE = 15;

// ---------- 辅助函数 ----------

function createTag(name, count) {
    const div = document.createElement('div');
    div.className = 'category-tag';
    div.dataset.category = name;
    div.textContent = name;
    const badge = document.createElement('span');
    badge.className = 'badge';
    badge.textContent = count;
    div.appendChild(badge);
    return div;
}

function createArrow(symbol) {
    const btn = document.createElement('button');
    btn.className = 'nav-arrow';
    btn.textContent = symbol;
    return btn;
}

/**
 * 二分查找：找到第 0 页（含「全部」标签的最宽情况）能放下的最大分组数
 * 所有页共用同一 pageSize，保证 start = navPage * pageSize 一致正确
 */
function calcPageSize(container, groups) {
    if (groups.length === 0) return MAX_PAGE_SIZE;

    const containerWidth = container.clientWidth;
    if (containerWidth < 100) return MIN_PAGE_SIZE;

    // 临时容器，绝对定位到屏幕外，宽度对齐真实容器
    const temp = document.createElement('div');
    temp.className = 'category-nav';
    temp.style.cssText =
        'position:absolute;left:-9999px;top:-9999px;' +
        'width:' + containerWidth + 'px;' +
        'visibility:hidden;overflow:hidden;';
    document.body.appendChild(temp);

    // 二分查找最大 page size
    let lo = MIN_PAGE_SIZE;
    let hi = Math.min(groups.length, MAX_PAGE_SIZE);
    while (lo < hi) {
        const mid = Math.ceil((lo + hi) / 2);
        temp.innerHTML = '';

        // 第 0 页模拟：全部 + 前 mid 个分组 + 右箭头（无左箭头）
        const totalAll = groups.reduce((s, g) => s + state.allData[g].length, 0);
        temp.appendChild(createTag('全部', totalAll));
        for (let i = 0; i < mid; i++) {
            temp.appendChild(createTag(groups[i], state.allData[groups[i]].length));
        }
        if (mid < groups.length) {
            temp.appendChild(createArrow('›'));
        }

        if (temp.scrollWidth <= containerWidth + 1) {
            lo = mid;      // 放得下，尝试更多
        } else {
            hi = mid - 1;  // 溢出，减少
        }
    }

    document.body.removeChild(temp);
    return lo;
}

// ---------- 主函数 ----------

export function renderCategoryNav(container, cardsContainer) {
    const groups = Object.keys(state.allData).filter(g => state.allData[g]?.length > 0);
    if (groups.length === 0) {
        container.innerHTML = '';
        return;
    }

    // 动态计算每页分组数（保证任何页都不会溢出）
    const pageSize = calcPageSize(container, groups);
    const totalPages = Math.ceil(groups.length / pageSize) || 1;

    // 页码越界纠正（分组数 / 窗口尺寸变化后）
    if (state.navPage >= totalPages && totalPages > 0) state.navPage = totalPages - 1;
    if (state.navPage < 0) state.navPage = 0;

    container.innerHTML = '';

    // 左箭头（首页时不显示）
    if (state.navPage > 0) {
        const prev = createArrow('‹');
        prev.addEventListener('click', () => {
            state.navPage--;
            renderCategoryNav(container, cardsContainer);
        });
        container.appendChild(prev);
    }

    // 「全部」标签（仅第 0 页显示）
    if (state.navPage === 0) {
        const totalAll = groups.reduce((s, g) => s + state.allData[g].length, 0);
        const all = createTag('全部', totalAll);
        all.addEventListener('click', () => {
            state.currentCategory = '全部';
            renderCategoryNav(container, cardsContainer);
            renderCards(state.allData, '全部', cardsContainer);
        });
        container.appendChild(all);
    }

    // 当前页分组
    const start = state.navPage * pageSize;
    const end = Math.min(start + pageSize, groups.length);
    for (let i = start; i < end; i++) {
        const g = groups[i];
        const tag = createTag(g, state.allData[g].length);
        tag.addEventListener('click', () => {
            state.currentCategory = g;
            state.navPage = Math.floor(i / pageSize);
            renderCategoryNav(container, cardsContainer);
            renderCards(state.allData, g, cardsContainer);
        });
        container.appendChild(tag);
    }

    // 右箭头（末页时不显示）
    if (end < groups.length) {
        const next = createArrow('›');
        next.addEventListener('click', () => {
            state.navPage++;
            renderCategoryNav(container, cardsContainer);
        });
        container.appendChild(next);
    }

    // 高亮当前分类
    container.querySelectorAll('.category-tag').forEach(el => {
        if (el.dataset.category === state.currentCategory) el.classList.add('active');
    });
}
