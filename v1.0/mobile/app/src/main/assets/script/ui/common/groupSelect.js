import { state } from '../../state.js';

export function populateGroupSelect(selectEl, customInputEl, selectedValue, forceNew = false) {
    const groups = Object.keys(state.allData).filter(g => state.allData[g]?.length > 0);
    selectEl.innerHTML = '';
    groups.forEach(g => {
        const opt = document.createElement('option');
        opt.value = g;
        opt.textContent = g;
        if (g === selectedValue) opt.selected = true;
        selectEl.appendChild(opt);
    });
    const newOpt = document.createElement('option');
    newOpt.value = '__new__';
    newOpt.textContent = '+ 新建分组';
    selectEl.appendChild(newOpt);

    if (forceNew || (!groups.includes(selectedValue) && !selectedValue && groups.length === 0)) {
        selectEl.value = '__new__';
        customInputEl.style.display = 'block';
        customInputEl.value = forceNew ? '' : (selectedValue || '');
        customInputEl.focus();
    } else if (!groups.includes(selectedValue) && selectedValue) {
        selectEl.value = '__new__';
        customInputEl.value = selectedValue;
        customInputEl.style.display = 'block';
    } else {
        customInputEl.style.display = 'none';
        customInputEl.value = '';
    }

    selectEl.onchange = function() {
        if (this.value === '__new__') {
            customInputEl.style.display = 'block';
            customInputEl.focus();
        } else {
            customInputEl.style.display = 'none';
            customInputEl.value = '';
        }
    };
}