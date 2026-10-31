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