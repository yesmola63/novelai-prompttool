// script/ui/updateModal.js
// 检查更新 → 提示 → 下载 → 安装
// 说明：更新缓存由后端写入文件（PC: temp/update_cache.json；安卓: 应用私有目录），
// 前端不再依赖 localStorage（浏览器隐私模式会清空它）。
import { getAppInfo, getUpdatePending, snoozeUpdate, checkUpdateRemote, startUpdateDownload, getUpdateProgress, installUpdate } from '../api.js';
import { showAlert } from './dialog.js';
import { syncBodyModalLock } from '../utils.js';

let updateModal, updateVersionEl, updateChangelogEl, updateNowBtn, updateLaterBtn, updateSnoozeEl;
let progressModal, progressFill, progressPercentEl, speedEl;

let remoteInfo = null;
let downloading = false;
let pollTimer = null;

export function initUpdateModal() {
    updateModal = document.getElementById('updateModal');
    updateVersionEl = document.getElementById('updateVersion');
    updateChangelogEl = document.getElementById('updateChangelog');
    updateNowBtn = document.getElementById('updateNowBtn');
    updateLaterBtn = document.getElementById('updateLaterBtn');
    updateSnoozeEl = document.getElementById('updateSnooze');

    progressModal = document.getElementById('updateProgressModal');
    progressFill = document.getElementById('updateProgressFill');
    progressPercentEl = document.getElementById('updateProgressPercent');
    speedEl = document.getElementById('updateSpeed');

    if (updateLaterBtn) updateLaterBtn.addEventListener('click', closeUpdateModal);
    if (updateNowBtn) updateNowBtn.addEventListener('click', handleUpdateNow);
    if (updateModal) {
        // 点击遮罩不关闭：更新提示需要用户明确选择
        updateModal.addEventListener('click', (e) => e.stopPropagation());
    }
}

/** 版本号比较：a > b 返回 1，相等 0，小于 -1 */
function compareVersions(a, b) {
    const pa = String(a || '0').split(/[.+\-]/).map(n => parseInt(n, 10) || 0);
    const pb = String(b || '0').split(/[.+\-]/).map(n => parseInt(n, 10) || 0);
    const len = Math.max(pa.length, pb.length);
    for (let i = 0; i < len; i++) {
        const x = pa[i] || 0;
        const y = pb[i] || 0;
        if (x > y) return 1;
        if (x < y) return -1;
    }
    return 0;
}

function escapeHtml(s) {
    return String(s)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;');
}

/** 应用启动后调用：有新版本则弹窗，否则静默 */
export async function checkForUpdate() {
    // 远程检查与本地版本查询并行，避免相互等待
    const remotePromise = checkUpdateRemote().catch(e => {
        // 离线 / 接口异常：静默忽略（稍后若已有缓存结果则保持不动）
        console.log('[update] check skipped:', e && e.message);
        return null;
    });

    // 本地版本信息（走本地服务，很快）；拿不到就不继续
    let local = null;
    try {
        local = await getAppInfo();
    } catch (e) { /* ignore */ }
    if (!local || !local.version) return;

    // 1) 后端缓存里若有"上次发现但没提示过"的更新 → 立刻弹窗（秒显示）
    try {
        const pending = await getUpdatePending();
        if (pending && pending.show && pending.info && pending.info.version
            && compareVersions(pending.info.version, local.version) > 0) {
            remoteInfo = pending.info;
            showUpdateModal(local.version, pending.info);
        }
    } catch (e) { /* 忽略 */ }

    // 2) 等远程结果：已弹窗则刷新内容；没有更新则关窗
    const remote = await remotePromise;
    if (!remote || !remote.version) return;

    if (compareVersions(remote.version, local.version) <= 0) {
        remoteInfo = null;
        if (updateModal.classList.contains('open')) hideUpdateModal();
        return;
    }

    remoteInfo = remote;
    if (updateModal.classList.contains('open')) {
        // 已由缓存弹出：只刷新内容，避免窗口闪烁
        renderUpdateContent(local.version, remote);
    }
    // 本次新发现的版本只由后端写入缓存，不在此处弹窗；下次启动由 /update/pending 提示
}

function renderUpdateContent(currentVersion, remote) {
    updateVersionEl.textContent = `当前版本 ${currentVersion} → 新版本 ${remote.version}`;
    const changelog = String(remote.changelog || '').trim();
    updateChangelogEl.innerHTML = changelog
        ? escapeHtml(changelog).replace(/\r\n|\r|\n/g, '<br>')
        : '本次更新未提供更新说明';
}

function showUpdateModal(currentVersion, remote) {
    renderUpdateContent(currentVersion, remote);
    if (updateSnoozeEl) updateSnoozeEl.checked = false;   // 每次弹出都重置勾选
    updateModal.classList.add('open');
    document.body.classList.add('modal-open');
}

function hideUpdateModal() {
    if (updateModal) updateModal.classList.remove('open');
    syncBodyModalLock();
}

/** 用户点「暂不更新」：勾了"当天不再显示"就记录，然后关闭 */
async function closeUpdateModal() {
    if (updateSnoozeEl && updateSnoozeEl.checked) {
        try { await snoozeUpdate(); } catch (e) { /* 记录失败不影响关闭 */ }
    }
    hideUpdateModal();
}

async function handleUpdateNow() {
    if (downloading) return;
    // 电脑版优先使用 downloads.pc，缺失时回退到 mobile
    const dl = (remoteInfo && remoteInfo.downloads) || {};
    const url = dl.mobile || dl.pc;
    if (!url) {
        await showAlert('未获取到下载地址，请稍后再试');
        return;
    }
    downloading = true;
    // 点"立即更新"时忽略"当天不再显示"勾选
    hideUpdateModal();
    showProgressModal();
    try {
        await startUpdateDownload(url);
        pollProgress();
    } catch (e) {
        downloading = false;
        closeProgressModal();
        await showAlert('启动下载失败：' + (e.message || e));
    }
}

function showProgressModal() {
    progressFill.style.width = '0%';
    progressPercentEl.textContent = '0%';
    speedEl.textContent = '';
    progressModal.classList.add('open');
    document.body.classList.add('modal-open');
}

function closeProgressModal() {
    if (progressModal) progressModal.classList.remove('open');
    syncBodyModalLock();
}

function formatSpeed(bps) {
    if (!bps || bps <= 0) return '';
    if (bps < 1024) return Math.round(bps) + ' B/s';
    if (bps < 1024 * 1024) return (bps / 1024).toFixed(1) + ' KB/s';
    return (bps / (1024 * 1024)).toFixed(2) + ' MB/s';
}

function pollProgress() {
    clearInterval(pollTimer);
    pollTimer = setInterval(async () => {
        let info;
        try {
            info = await getUpdateProgress();
        } catch (e) {
            return; // 单次失败忽略，继续轮询
        }
        const total = Number(info.total) || 0;
        const downloaded = Number(info.downloaded) || 0;
        let percent = 0;
        if (total > 0) {
            percent = Math.min(100, Math.floor(downloaded * 100 / total));
            progressPercentEl.textContent = percent + '%';
        } else {
            progressPercentEl.textContent = (downloaded / (1024 * 1024)).toFixed(1) + ' MB';
        }
        progressFill.style.width = percent + '%';
        speedEl.textContent = info.downloading ? formatSpeed(Number(info.speed) || 0) : '';

        if (info.done) {
            clearInterval(pollTimer);
            pollTimer = null;
            downloading = false;
            if (info.success) {
                progressFill.style.width = '100%';
                progressPercentEl.textContent = '100%';
                speedEl.textContent = '正在安装…';
                try {
                    await installUpdate();
                } catch (e) {
                    await showAlert('安装包下载完成，但启动安装失败：' + (e.message || e));
                }
                closeProgressModal();
            } else {
                closeProgressModal();
                await showAlert('下载失败：' + (info.error || '未知错误'));
            }
        }
    }, 400);
}
