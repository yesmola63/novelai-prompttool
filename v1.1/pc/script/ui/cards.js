import { state } from '../state.js';
import { getImageUrl, ensureArray } from '../utils.js';
import { openDetail } from './detailModal.js';

export function renderCards(data, category, container) {
    const filtered = category === '全部' ? data : (data[category] ? { [category]: data[category] } : {});
    const groups = Object.keys(filtered).filter(g => filtered[g]?.length > 0);

    if (groups.length === 0) {
        container.innerHTML = `<div class="empty-state">该分类下暂无提示词，请添加</div>`;
        state.flatItems = [];
        return;
    }

    let html = '';
    state.flatItems = [];
    let globalIdx = 0;

    groups.forEach(groupName => {
        const items = filtered[groupName];
        html += `<div class="group-section">`;
        if (category === '全部') {
            html += `<div class="group-title">${groupName} <span class="badge">${items.length}</span></div>`;
        }
        html += `<div class="card-grid">`;
        items.forEach(item => {
            const covers = ensureArray(item.cover);
            // 卡片用 400px 缩略图（小图加载快、解码快）+ 原生 lazy：滚动时仅视口附近加载
            const coverSrc = covers.length > 0 ? getImageUrl(covers[0], 400) : '';
            const role = item.role || '无角色';
            const safeRole = role.length > 20 ? role.slice(0, 18) + '…' : role;
            const idx = globalIdx++;
            state.flatItems.push({ group: groupName, item, index: idx });
            html += `
                <div class="prompt-card" data-index="${idx}">
                    ${coverSrc ? `<img class="card-img" loading="lazy" decoding="async" src="${coverSrc}" alt="">` : ''}
                    <div class="card-role-tag">${safeRole}</div>
                    <div class="card-overlay"><span class="card-label">查看提示词详细信息</span></div>
                </div>
            `;
        });
        html += `</div></div>`;
    });

    container.innerHTML = html;
    container.querySelectorAll('.prompt-card').forEach(card => {
        card.addEventListener('click', function() {
            const idx = parseInt(this.dataset.index, 10);
            if (!isNaN(idx) && idx >= 0 && idx < state.flatItems.length) openDetail(idx);
        });
    });
}