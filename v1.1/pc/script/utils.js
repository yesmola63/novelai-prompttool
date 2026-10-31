/**
 * 生成图片 URL（兼容旧链接）
 * @param {string} src 图片文件名
 * @param {number} [thumbWidth] 缩略图宽度（可选，服务端生成小图，卡片列表用）
 */
export function getImageUrl(src, thumbWidth) {
    if (!src) return '';
    if (src.startsWith('http://') || src.startsWith('https://') || src.startsWith('data:')) {
        return src;
    }
    if (thumbWidth) {
        return `/picture/${encodeURIComponent(src)}?w=${thumbWidth}`;
    }
    return `/picture/${src}`;
}

/**
 * 安全获取数组
 */
export function ensureArray(val) {
    return Array.isArray(val) ? val : (val ? [val] : []);
}

/**
 * 重新计算 body 的滚动锁定状态。
 * 关闭某个弹窗时不能无条件解锁：上层还可能开着别的弹窗/详情页，
 * 否则背景会跟着滚动。统一调用此函数即可。
 */
export function syncBodyModalLock() {
    const anyOverlayOpen = document.querySelector('.modal-overlay.open');
    const detailOpen = document.getElementById('detailModal')?.classList.contains('open');
    if (anyOverlayOpen || detailOpen) {
        document.body.classList.add('modal-open');
    } else {
        document.body.classList.remove('modal-open');
    }
}