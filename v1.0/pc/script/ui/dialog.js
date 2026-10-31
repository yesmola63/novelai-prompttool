// script/ui/dialog.js
// 自定义弹窗模块

let dialogModal, dialogTitle, dialogMessage, dialogConfirmBtn, dialogCancelBtn;
let currentResolve = null;      // 当前弹窗的 resolve 函数
let isConfirm = false;          // 当前是否为确认框（true 表示有取消按钮）

export function initDialog() {
    dialogModal = document.getElementById('customDialog');
    dialogTitle = document.getElementById('dialogTitle');
    dialogMessage = document.getElementById('dialogMessage');
    dialogConfirmBtn = document.getElementById('dialogConfirmBtn');
    dialogCancelBtn = document.getElementById('dialogCancelBtn');

    // 点击遮罩（弹窗外部）关闭
    dialogModal.addEventListener('click', (e) => {
        if (e.target === dialogModal) {
            if (currentResolve) {
                // 如果是 alert（无取消按钮），视为确定；如果是 confirm，视为取消
                currentResolve(isConfirm ? false : true);
                currentResolve = null;
            }
            closeDialog();
        }
    });

    // 确定按钮
    dialogConfirmBtn.addEventListener('click', () => {
        if (currentResolve) {
            currentResolve(true);
            currentResolve = null;
        }
        closeDialog();
    });

    // 取消按钮
    dialogCancelBtn.addEventListener('click', () => {
        if (currentResolve) {
            currentResolve(false);
            currentResolve = null;
        }
        closeDialog();
    });

    // ESC 键
    document.addEventListener('keydown', (e) => {
        if (e.key === 'Escape' && dialogModal.classList.contains('open')) {
            if (currentResolve) {
                currentResolve(isConfirm ? false : true);
                currentResolve = null;
            }
            closeDialog();
        }
    });
}

function closeDialog() {
    dialogModal.classList.remove('open');
    dialogModal.style.zIndex = '';
    document.body.style.overflow = '';
    // 如果还存在 currentResolve（未处理），则视为取消（一般不会）
    if (currentResolve) {
        currentResolve(false);
        currentResolve = null;
    }
}

/**
 * 显示提示框（仅确定按钮）
 * @param {string} message 提示内容
 * @param {string} title 标题（可选）
 * @returns {Promise<void>}
 */
export function showAlert(message, title = '提示') {
    return new Promise((resolve) => {
        dialogTitle.textContent = title;
        dialogMessage.textContent = message;
        dialogConfirmBtn.textContent = '确定';
        dialogCancelBtn.style.display = 'none';
        isConfirm = false;
        currentResolve = resolve;
        dialogModal.classList.add('open');
        dialogModal.style.zIndex = '1001';
        document.body.style.overflow = 'hidden';
    });
}

/**
 * 显示确认框（支持 HTML 内容，可加粗/红字，自定义按钮文本）
 * @param {string} html HTML 内容
 * @param {string} title 标题（可选）
 * @param {string} confirmText 确定按钮文本（可选，默认"确定"）
 * @param {string} cancelText 取消按钮文本（可选，默认"取消"）
 * @returns {Promise<boolean>} true=确定，false=取消
 */
export function showConfirmHtml(html, title = '确认', confirmText = '确定', cancelText = '取消') {
    return new Promise((resolve) => {
        dialogTitle.textContent = title;
        dialogMessage.innerHTML = html;
        dialogConfirmBtn.textContent = confirmText;
        dialogCancelBtn.textContent = cancelText;
        dialogCancelBtn.style.display = 'inline-flex';
        isConfirm = true;
        currentResolve = resolve;
        dialogModal.classList.add('open');
        dialogModal.style.zIndex = '1001';
        document.body.style.overflow = 'hidden';
    });
}

/**
 * 显示确认框（确定/取消）
 * @param {string} message 确认内容
 * @param {string} title 标题（可选）
 * @returns {Promise<boolean>} true=确定，false=取消
 */
export function showConfirm(message, title = '确认') {
    return new Promise((resolve) => {
        dialogTitle.textContent = title;
        dialogMessage.textContent = message;
        dialogConfirmBtn.textContent = '确定';
        dialogCancelBtn.style.display = 'inline-flex';
        isConfirm = true;
        currentResolve = resolve;
        dialogModal.classList.add('open');
        dialogModal.style.zIndex = '1001';
        document.body.style.overflow = 'hidden';
    });
}