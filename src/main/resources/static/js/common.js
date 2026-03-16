(function(global) {
    'use strict';

    var XXRAG = global.XXRAG || {};

    XXRAG.escapeHtml = function(text) {
        if (text === null || text === undefined) return '';
        var div = document.createElement('div');
        div.textContent = String(text);
        return div.innerHTML;
    };

    XXRAG.showToast = function(message, type) {
        type = type || 'error';
        var toast = document.getElementById('toast');
        if (!toast) {
            console.warn('Toast element not found');
            return;
        }
        toast.textContent = message;
        toast.className = 'toast ' + type;
        toast.classList.add('show');
        setTimeout(function() {
            toast.classList.remove('show');
        }, 3000);
    };

    XXRAG.showModal = function(options) {
        return new Promise(function(resolve) {
            var modal = document.getElementById('confirmModal');
            var msgEl = document.getElementById('confirmModalMessage');
            var inputWrapper = document.getElementById('confirmModalInputWrapper');
            var inputEl = document.getElementById('confirmModalInput');
            var checkboxWrapper = document.getElementById('confirmModalCheckboxWrapper');
            var checkboxEl = document.getElementById('confirmModalCheckbox');
            var passwordWrapper = document.getElementById('confirmModalPasswordWrapper');
            var passwordEl = document.getElementById('confirmModalPassword');
            var btnCancel = document.getElementById('confirmModalCancel');
            var btnConfirm = document.getElementById('confirmModalConfirm');

            if (!modal) {
                console.error('Modal element not found');
                resolve({confirmed: false});
                return;
            }

            if (msgEl) msgEl.textContent = options.message || '';
            
            if (inputWrapper && inputEl) {
                if (options.showInput) {
                    inputWrapper.style.display = 'block';
                    inputEl.placeholder = options.inputPlaceholder || '';
                    inputEl.value = options.inputValue || '';
                    inputEl.focus();
                } else {
                    inputWrapper.style.display = 'none';
                }
            }
            
            if (checkboxWrapper && checkboxEl) {
                if (options.showCheckbox) {
                    checkboxWrapper.style.display = 'block';
                    checkboxEl.checked = !!options.checkboxChecked;
                } else {
                    checkboxWrapper.style.display = 'none';
                }
            }
            
            if (passwordWrapper && passwordEl) {
                if (options.showPassword) {
                    passwordWrapper.style.display = 'block';
                    passwordEl.value = '';
                    passwordEl.focus();
                } else {
                    passwordWrapper.style.display = 'none';
                }
            }

            function cleanup() {
                modal.classList.remove('active');
                if (btnCancel) btnCancel.removeEventListener('click', onCancel);
                if (btnConfirm) btnConfirm.removeEventListener('click', onConfirm);
            }

            function onCancel() {
                cleanup();
                resolve({confirmed: false});
            }

            function onConfirm() {
                var val = options.showInput && inputEl ? inputEl.value : null;
                var allChecked = options.showCheckbox && checkboxEl ? checkboxEl.checked : false;
                var pwd = options.showPassword && passwordEl ? passwordEl.value : null;
                cleanup();
                resolve({confirmed: true, value: val, all: allChecked, password: pwd});
            }

            if (btnCancel) btnCancel.addEventListener('click', onCancel);
            if (btnConfirm) btnConfirm.addEventListener('click', onConfirm);

            modal.classList.add('active');
        });
    };

    XXRAG.generateSessionId = function() {
        if (window.crypto && typeof window.crypto.randomUUID === 'function') {
            return window.crypto.randomUUID();
        }
        return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
            var r = Math.random() * 16 | 0;
            var v = c === 'x' ? r : (r & 0x3 | 0x8);
            return v.toString(16);
        });
    };

    XXRAG.formatDateTime = function(dateTimeStr) {
        return dateTimeStr || '未知时间';
    };

    global.XXRAG = XXRAG;

})(typeof window !== 'undefined' ? window : this);
