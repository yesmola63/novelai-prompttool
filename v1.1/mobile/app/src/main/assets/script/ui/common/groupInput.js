// script/ui/common/groupInput.js
// 分组输入框：输入时匹配已有分组并可一键点选；未匹配时提示将新建分组
import { state } from '../../state.js';

function escapeHtml(s) {
    return String(s)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;');
}

/**
 * @param {HTMLInputElement} inputEl 分组输入框
 * @param {HTMLElement} suggestEl    建议标签容器
 * @param {HTMLElement} hintEl       提示文案
 * @param {'add'|'edit'} mode        添加 / 编辑（提示语不同）
 */
export function initGroupInput(inputEl, suggestEl, hintEl, mode) {
    const groups = () => Object.keys(state.allData).filter(g => state.allData[g]?.length > 0);

    function render(showPanel = true) {
        const q = inputEl.value.trim();
        const list = groups();
        // 只在输入内容时才做匹配搜索；没有输入时不列出任何分组
        const matches = q
            ? list.filter(g => g.toLowerCase().includes(q.toLowerCase())).slice(0, 30)
            : [];

        suggestEl.innerHTML = matches
            .map(g => `<div class="group-option" data-group="${escapeHtml(g)}">${escapeHtml(g)}</div>`)
            .join('');
        suggestEl.style.display = (showPanel && matches.length > 0) ? 'block' : 'none';

        suggestEl.querySelectorAll('.group-option').forEach(opt => {
            // 按下时阻止默认行为，避免输入框先失焦导致面板提前收起、点击落空
            opt.addEventListener('mousedown', (e) => e.preventDefault());
            opt.addEventListener('click', (e) => {
                e.preventDefault();
                inputEl.value = opt.dataset.group;
                render(false);   // 选中后直接收起面板
                inputEl.focus();
            });
        });

        // 底部提示：
        //   命中已有分组 → 绿色 ✓，明确告诉用户"已选中"
        //   否则 → 红色提示将新建分组
        const exactMatch = q && list.some(g => g === q);
        if (!q) {
            hintEl.style.display = 'none';
            hintEl.textContent = '';
            hintEl.classList.remove('ok');
        } else if (exactMatch) {
            hintEl.textContent = mode === 'edit'
                ? `✓ 该提示词将被保存到已有分组“${q}”`
                : `✓ 将保存到已有分组“${q}”`;
            hintEl.classList.add('ok');
            hintEl.style.display = 'block';
        } else {
            hintEl.textContent = mode === 'edit'
                ? `该提示词将被保存到新的“${q}”组`
                : `新的提示词将被保存到“${q}”组`;
            hintEl.classList.remove('ok');
            hintEl.style.display = 'block';
        }
    }

    function hidePanel() {
        suggestEl.style.display = 'none';
    }

    inputEl.addEventListener('input', () => render());
    inputEl.addEventListener('focus', () => render());
    // 离开输入框就把面板收起来（延迟一点，避免与点击面板项抢时序）
    inputEl.addEventListener('blur', () => {
        setTimeout(() => {
            if (document.activeElement !== inputEl) hidePanel();
        }, 150);
    });
    inputEl.addEventListener('keydown', (e) => {
        if (e.key === 'Escape') hidePanel();
    });

    return {
        reset(value) {
            inputEl.value = value || '';
            render();
        },
        getValue() {
            return inputEl.value.trim();
        },
        refresh: render
    };
}
