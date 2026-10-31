/**
 * 生成图片 URL（兼容旧链接）
 */
export function getImageUrl(src) {
    if (!src) return '';
    if (src.startsWith('http://') || src.startsWith('https://') || src.startsWith('data:')) {
        return src;
    }
    return `/picture/${src}`;
}

/**
 * 安全获取数组
 */
export function ensureArray(val) {
    return Array.isArray(val) ? val : (val ? [val] : []);
}