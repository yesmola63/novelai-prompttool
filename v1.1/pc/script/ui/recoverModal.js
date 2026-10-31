// script/ui/recoverModal.js
// prompt.json 语法错误恢复弹窗模块
import { state } from '../state.js';
import { checkPromptRecovery, recoverPrompt } from '../api.js';
import { showAlert } from './dialog.js';
import { openAddModal } from './addModal.js';

// ---------- 初始化：启动后检查一次 ----------
export function initRecoverModal() {
    checkPromptRecovery()
        .then(res => {
            if (res.needRecovery && res.params && Object.keys(res.params).length > 0) {
                showRecoveryDialog(res.params);
            }
        })
        .catch(() => { /* 后端不支持也不影响正常使用 */ });
}

// ---------- 工具 ----------
function escapeHtml(str) {
    return String(str ?? '')
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;');
}

function buildParamsHtml(params) {
    let html = '<div style="max-height:55vh;overflow-y:auto;text-align:left;font-size:13px;">';
    for (const [group, items] of Object.entries(params)) {
        html += `<p style="font-weight:bold;color:#d9534f;margin:10px 0 4px;">📁 分组：${escapeHtml(group)}</p>`;
        if (!items || items.length === 0) {
            html += '<p style="margin:2px 0;color:#999;">(未提取到提示词条目)</p>';
            continue;
        }
        items.forEach((item, i) => {
            html += `<div style="background:#f7fafc;border:1px solid #e3eef5;border-radius:8px;padding:8px 10px;margin:6px 0;">`;
            html += `<p style="margin:2px 0;"><b>角色：</b>${escapeHtml(item.role || '(空)')}</p>`;
            if (item.version) html += `<p style="margin:2px 0;"><b>版本：</b>${escapeHtml(item.version)}</p>`;
            if (item.desc) html += `<p style="margin:2px 0;"><b>描述：</b>${escapeHtml(item.desc)}</p>`;
            if (item.common) html += `<p style="margin:2px 0;word-break:break-all;"><b>指令：</b>${escapeHtml(item.common)}</p>`;
            if (item.cover && item.cover.length) {
                html += `<p style="margin:2px 0;color:#7a9bb5;">🖼️ 图片：${escapeHtml(item.cover.join(', '))}</p>`;
            }
            html += `</div>`;
        });
    }
    html += '</div>';
    return html;
}

// ---------- 显示恢复弹窗 ----------
function showRecoveryDialog(params) {
    const overlay = document.createElement('div');
    overlay.className = 'modal-overlay form-modal';
    overlay.id = 'recoveryModal';
    overlay.innerHTML = `
        <div class="modal-box" style="max-width:720px;">
            <div class="modal-body" style="padding:24px;">
                <h3 style="margin:0 0 12px;color:#d9534f;">⚠️ prompt.json 存在语法错误</h3>
                <p style="margin:0 0 10px;font-size:14px;color:#555;line-height:1.6;">
                    程序检测到 prompt.json 被外部修改且语法不正确喵~<br>
                    已尽力提取出以下参数，请确认这些信息是否正确：
                </p>
                <div id="recoveryParamsBox">${buildParamsHtml(params)}</div>
                <div class="form-actions" style="margin-top:16px;gap:10px;">
                    <button class="btn-submit" id="recoveryAcceptBtn" style="flex:1;">✅ 信息正确，直接修复</button>
                    <button class="btn-cancel" id="recoveryEditBtn" style="flex:1;">❌ 信息不正确，我去修改</button>
                </div>
            </div>
        </div>`;
    document.body.appendChild(overlay);
    requestAnimationFrame(() => overlay.classList.add('open'));
    document.body.classList.add('modal-open');

    // 信息正确 → 直接修复
    overlay.querySelector('#recoveryAcceptBtn').addEventListener('click', async () => {
        overlay.querySelector('#recoveryAcceptBtn').disabled = true;
        try {
            await recoverPrompt(params);
            closeRecoveryModal(overlay);
            await showAlert('✅ prompt.json 已修复！');
            document.dispatchEvent(new CustomEvent('refreshDisplay'));
        } catch (err) {
            await showAlert('❌ 修复失败：' + err.message);
            overlay.querySelector('#recoveryAcceptBtn').disabled = false;
        }
    });

    // 信息不正确 → 打开添加提示词窗口自动填入
    overlay.querySelector('#recoveryEditBtn').addEventListener('click', () => {
        closeRecoveryModal(overlay);
        openAddModalWithParams(params);
    });
}

function closeRecoveryModal(overlay) {
    overlay.classList.remove('open');
    document.body.classList.remove('modal-open');
    setTimeout(() => overlay.remove(), 300);
}

// ---------- 打开添加提示词窗口并自动填入提取的信息 ----------
function openAddModalWithParams(params) {
    const groups = Object.keys(params);
    const firstGroup = groups[0] || '未分类';
    const firstItem = (params[firstGroup] || [])[0] || {};

    openAddModal();

    const groupInputEl = document.getElementById('addGroup');
    const roleInput = document.getElementById('addRole');
    const commonInput = document.getElementById('addCommon');
    const versionInput = document.getElementById('addVersion');
    const descInput = document.getElementById('addDesc');

    // 分组输入框直接填入提取到的分组名（已有/新分组都由它处理）
    if (groupInputEl) {
        groupInputEl.value = firstGroup;
        groupInputEl.dispatchEvent(new Event('input', { bubbles: true }));
    }

    roleInput.value = firstItem.role || '';
    commonInput.value = firstItem.common || '';
    versionInput.value = firstItem.version || '';
    descInput.value = firstItem.desc || '';

    // 标记恢复模式：保存时覆盖原本错误的地方
    state.recoveryPending = params;
}
