import { API_BASE } from './config.js';

/**
 * 通用请求封装
 */
async function request(endpoint, options = {}) {
    const response = await fetch(`${API_BASE}${endpoint}`, options);
    const data = await response.json();
    if (!response.ok) {
        throw new Error(data.error || `HTTP ${response.status}`);
    }
    return data;
}

export function getPrompts() {
    return request('/prompts');
}

export function checkPromptsEmpty() {
    return request('/prompts/check-empty');
}

export function addPrompt(group, item) {
    return request('/prompts', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ group, item })
    });
}

export function updatePrompt(originalGroup, originalRole, originalCommon, newGroup, newItem) {
    return request('/prompts', {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ originalGroup, originalRole, originalCommon, newGroup, newItem })
    });
}

export function deletePrompt(id) {
    const params = new URLSearchParams({ id });
    return request(`/prompts?${params.toString()}`, { method: 'DELETE' });
}

export function uploadImages(files) {
    const formData = new FormData();
    for (const f of files) formData.append('files', f);
    return request('/upload-images', {
        method: 'POST',
        body: formData
    }).then(data => data.filenames || []);
}

export function deleteImageFiles(filenames) {
    if (!filenames || filenames.length === 0) return Promise.resolve();
    return request('/images', {
        method: 'DELETE',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ filenames })
    }).then(data => data.deleted || []);
}
export function updateAllPrompts(data) {
    return request('/prompts/replace', {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(data)
    });
}

// ---------- 打包提示词 ----------
export function packPrompts(payload) {
    return request('/pack', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
    });
}

export function getPackProgress(taskId) {
    return request(`/pack/progress?task_id=${encodeURIComponent(taskId)}`);
}

export function choosePackDir(filename = 'prompt') {
    return request('/pack/choose-dir', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ filename: filename.trim() || 'prompt' })
    });
}

export function getPackDirStatus() {
    return request('/pack/choose-dir/poll');
}

export function checkPackFilename(filename) {
    return request('/pack/check-filename', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ filename })
    });
}

export function saveImage(filename) {
    return request('/save-image', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ filename })
    });
}

export function getLoadFileStatus() {
    return request('/load/select-file/poll');
}

export function selectLoadFile() {
    return request('/load/select-file', { method: 'POST' });
}

// ---------- 加载提示词 ----------

export function unlockLoadFile(password) {
    return request('/load/unlock', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ password })
    });
}

export function applyLoad(mode) {
    return request('/load/apply', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ mode })
    });
}

export function getLoadProgress(taskId) {
    return request(`/load/progress?task_id=${encodeURIComponent(taskId)}`);
}

// ---------- prompt.json 恢复 ----------
export function checkPromptRecovery() {
    return request('/prompt/recovery-check');
}

export function recoverPrompt(data) {
    return request('/prompt/recover', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ data })
    });
}

// ---------- 应用信息 / 更新 ----------
export function getAppInfo() {
    return request('/app-info');
}

/** 由本地服务端代理请求远程更新接口（避免 WebView 跨域限制） */
export function checkUpdateRemote() {
    return request('/update/check');
}

/** 读取后端文件里缓存的更新信息（有则弹窗） */
export function getUpdatePending() {
    return request('/update/pending');
}

/** 记录"当天不再显示"（只影响今天） */
export function snoozeUpdate() {
    return request('/update/snooze', { method: 'POST' });
}

export function startUpdateDownload(url) {
    return request('/update/download', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ url })
    });
}

export function getUpdateProgress() {
    return request('/update/progress');
}

export function installUpdate() {
    return request('/update/install', { method: 'POST' });
}