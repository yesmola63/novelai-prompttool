export function renderImageList(container, items, onDelete, showPending = false) {
    if (items.length === 0) {
        container.innerHTML = '<div style="color:#6a8fa8; font-size:13px;">暂无图片</div>';
        return;
    }
    let html = '';
    items.forEach((item, idx) => {
        const label = typeof item === 'string' ? item : (item.name + (showPending ? ' (待上传)' : ''));
        html += `
            <div class="image-item">
                <span>${label}</span>
                <button class="del-img-btn" data-index="${idx}">✕</button>
            </div>
        `;
    });
    container.innerHTML = html;
    container.querySelectorAll('.del-img-btn').forEach(btn => {
        btn.addEventListener('click', function() {
            const idx = parseInt(this.dataset.index, 10);
            if (!isNaN(idx)) onDelete(idx);
        });
    });
}