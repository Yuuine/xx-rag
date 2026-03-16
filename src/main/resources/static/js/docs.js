(function() {
    'use strict';

    var PAGE_SIZE = 10;
    var MAX_PAGE_BUTTONS = 15;

    var state = {
        currentPage: 1,
        allDocs: [],
        filteredDocs: [],
        toDeleteMd5s: []
    };

    var elements = {};

    function init() {
        cacheElements();
        bindEvents();
        loadDocs();
    }

    function cacheElements() {
        elements.docTable = document.getElementById('docTable');
        elements.tbody = document.querySelector('#docTable tbody');
        elements.emptyMsg = document.getElementById('emptyMsg');
        elements.noResultsMsg = document.getElementById('noResultsMsg');
        elements.totalCount = document.getElementById('totalCount');
        elements.selectAll = document.getElementById('selectAll');
        elements.batchDeleteBtn = document.getElementById('batchDeleteBtn');
        elements.searchInput = document.getElementById('searchInput');
        elements.searchBtn = document.getElementById('searchBtn');
        elements.paginationControls = document.getElementById('paginationControls');
        elements.prevPage = document.getElementById('prevPage');
        elements.nextPage = document.getElementById('nextPage');
        elements.pageNumbers = document.getElementById('pageNumbers');
        elements.confirmModal = document.getElementById('confirmModal');
        elements.modalMessage = document.getElementById('modalMessage');
        elements.cancelDelete = document.getElementById('cancelDelete');
        elements.confirmDeleteBtn = document.getElementById('confirmDelete');
    }

    function bindEvents() {
        elements.selectAll.addEventListener('change', function() {
            var checkboxes = document.querySelectorAll('.row-checkbox');
            checkboxes.forEach(function(cb) {
                cb.checked = elements.selectAll.checked;
            });
            updateBatchButton();
        });

        document.addEventListener('change', function(e) {
            if (e.target.classList.contains('row-checkbox')) {
                updateBatchButton();
                var checkboxes = document.querySelectorAll('.row-checkbox');
                var allChecked = Array.prototype.every.call(checkboxes, function(cb) {
                    return cb.checked;
                });
                elements.selectAll.checked = allChecked;
            }
        });

        elements.batchDeleteBtn.addEventListener('click', function() {
            var checked = document.querySelectorAll('.row-checkbox:checked');
            if (checked.length === 0) return;
            var md5s = Array.prototype.map.call(checked, function(cb) {
                return cb.value;
            });
            openConfirmModal(md5s, true);
        });

        elements.cancelDelete.addEventListener('click', function() {
            elements.confirmModal.classList.remove('active');
            state.toDeleteMd5s = [];
        });

        elements.confirmDeleteBtn.addEventListener('click', performDelete);

        elements.confirmModal.addEventListener('click', function(e) {
            if (e.target === elements.confirmModal) {
                elements.confirmModal.classList.remove('active');
                state.toDeleteMd5s = [];
            }
        });

        elements.searchBtn.addEventListener('click', function() {
            filterDocs(elements.searchInput.value);
        });

        elements.searchInput.addEventListener('keydown', function(e) {
            if (e.key === 'Enter') {
                filterDocs(e.target.value);
            }
        });

        elements.searchInput.addEventListener('input', function(e) {
            filterDocs(e.target.value);
        });

        elements.prevPage.addEventListener('click', function() {
            if (state.currentPage > 1) {
                state.currentPage--;
                renderCurrentPage();
            }
        });

        elements.nextPage.addEventListener('click', function() {
            var totalPages = Math.ceil(state.filteredDocs.length / PAGE_SIZE);
            if (state.currentPage < totalPages) {
                state.currentPage++;
                renderCurrentPage();
            }
        });
    }

    function formatDateTime(dateTimeStr) {
        return dateTimeStr || '未知时间';
    }

    function renderCurrentPage() {
        elements.tbody.innerHTML = '';

        var start = (state.currentPage - 1) * PAGE_SIZE;
        var end = start + PAGE_SIZE;
        var pageData = state.filteredDocs.slice(start, end);

        pageData.forEach(function(doc) {
            var tr = document.createElement('tr');
            tr.innerHTML = '<td><input type="checkbox" class="row-checkbox" value="' + XXRAG.escapeHtml(doc.fileMd5) + '"></td>' +
                '<td>' + XXRAG.escapeHtml(doc.fileName || '未知文件名') + '</td>' +
                '<td>' + XXRAG.escapeHtml(formatDateTime(doc.createdAt)) + '</td>' +
                '<td><button class="delete-btn btn-danger" data-md5="' + XXRAG.escapeHtml(doc.fileMd5) + '">删除</button></td>';

            tr.addEventListener('click', function(e) {
                if (e.target.tagName === 'BUTTON' || e.target.tagName === 'INPUT') return;
                var highlights = document.querySelectorAll('tr.highlight');
                highlights.forEach(function(r) {
                    r.classList.remove('highlight');
                });
                tr.classList.add('highlight');
            });

            elements.tbody.appendChild(tr);
        });

        var deleteButtons = document.querySelectorAll('.delete-btn');
        deleteButtons.forEach(function(btn) {
            btn.addEventListener('click', function(e) {
                e.stopPropagation();
                openConfirmModal([btn.dataset.md5]);
            });
        });

        updatePaginationUI();
        updateBatchButton();
    }

    function generatePageButtons() {
        elements.pageNumbers.innerHTML = '';

        var totalItems = state.filteredDocs.length;
        if (totalItems === 0) return;

        var totalPages = Math.ceil(totalItems / PAGE_SIZE);
        if (totalPages <= 1) return;

        var startPage = Math.max(1, state.currentPage - Math.floor(MAX_PAGE_BUTTONS / 2));
        var endPage = startPage + MAX_PAGE_BUTTONS - 1;

        if (endPage > totalPages) {
            endPage = totalPages;
            startPage = Math.max(1, endPage - MAX_PAGE_BUTTONS + 1);
        }

        if (startPage > 1) {
            elements.pageNumbers.appendChild(createPageButton(1));
            if (startPage > 2) {
                elements.pageNumbers.appendChild(createEllipsis());
            }
        }

        for (var i = startPage; i <= endPage; i++) {
            var btn = createPageButton(i);
            if (i === state.currentPage) {
                btn.classList.add('active');
                btn.disabled = true;
            }
            elements.pageNumbers.appendChild(btn);
        }

        if (endPage < totalPages) {
            if (endPage < totalPages - 1) {
                elements.pageNumbers.appendChild(createEllipsis());
            }
            elements.pageNumbers.appendChild(createPageButton(totalPages));
        }
    }

    function createPageButton(page) {
        var btn = document.createElement('button');
        btn.className = 'page-btn';
        btn.textContent = page;
        btn.addEventListener('click', function() {
            state.currentPage = page;
            renderCurrentPage();
        });
        return btn;
    }

    function createEllipsis() {
        var span = document.createElement('span');
        span.className = 'ellipsis';
        span.textContent = '...';
        return span;
    }

    function updatePaginationUI() {
        var totalItems = state.filteredDocs.length;
        var totalPages = Math.max(1, Math.ceil(totalItems / PAGE_SIZE));

        if (state.currentPage > totalPages) state.currentPage = totalPages;
        if (state.currentPage < 1) state.currentPage = 1;

        elements.totalCount.textContent = totalItems;

        var showPagination = totalPages > 1 && totalItems > 0;
        elements.paginationControls.style.display = showPagination ? 'flex' : 'none';

        elements.prevPage.disabled = state.currentPage <= 1;
        elements.nextPage.disabled = state.currentPage >= totalPages;

        generatePageButtons();
    }

    function filterDocs(keyword) {
        keyword = (keyword || '').trim().toLowerCase();

        if (!keyword) {
            state.filteredDocs = state.allDocs.slice();
        } else {
            state.filteredDocs = state.allDocs.filter(function(doc) {
                return (doc.fileName || '').toLowerCase().indexOf(keyword) !== -1;
            });
        }

        state.currentPage = 1;
        renderCurrentPage();

        elements.noResultsMsg.style.display = (state.filteredDocs.length === 0 && keyword !== '') ? 'block' : 'none';
        elements.docTable.style.display = state.filteredDocs.length > 0 ? 'table' : 'none';
        elements.emptyMsg.style.display = 'none';
    }

    function loadDocs() {
        fetch('/xx/getDoc')
            .then(function(res) { return res.json(); })
            .then(function(data) {
                if (data.code === 0 && data.data && data.data.docs) {
                    state.allDocs = data.data.docs;
                    state.filteredDocs = state.allDocs.slice();

                    if (state.allDocs.length > 0) {
                        state.currentPage = 1;
                        renderCurrentPage();
                        elements.emptyMsg.style.display = 'none';
                    } else {
                        elements.emptyMsg.style.display = 'block';
                        elements.docTable.style.display = 'none';
                        elements.paginationControls.style.display = 'none';
                    }
                } else {
                    XXRAG.showToast('获取文档列表失败', 'error');
                    elements.emptyMsg.textContent = '加载失败，请刷新重试';
                    elements.emptyMsg.style.display = 'block';
                }
            })
            .catch(function(err) {
                console.error('加载文档列表失败:', err);
                XXRAG.showToast('网络错误，请稍后重试', 'error');
                elements.emptyMsg.style.display = 'block';
            });

        elements.selectAll.checked = false;
        elements.batchDeleteBtn.disabled = true;
    }

    function openConfirmModal(md5s, isBatch) {
        state.toDeleteMd5s = md5s;
        if (isBatch && md5s.length > 1) {
            elements.modalMessage.innerHTML = '确定要删除这 <strong>' + md5s.length + '</strong> 个文档吗？<br>删除后无法恢复！';
        } else {
            elements.modalMessage.innerHTML = '确定要删除该文档吗？<br>删除后无法恢复！';
        }
        elements.confirmModal.classList.add('active');
    }

    function performDelete() {
        if (state.toDeleteMd5s.length === 0) return;

        fetch('/xx/delete', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(state.toDeleteMd5s)
        })
            .then(function(res) { return res.json(); })
            .then(function(data) {
                if (data.code === 0) {
                    XXRAG.showToast('删除成功（' + state.toDeleteMd5s.length + ' 个）', 'success');
                    loadDocs();
                } else {
                    XXRAG.showToast(data.msg || '删除失败', 'error');
                }
            })
            .catch(function(err) {
                XXRAG.showToast('删除请求失败，请检查网络', 'error');
            })
            .finally(function() {
                elements.confirmModal.classList.remove('active');
                state.toDeleteMd5s = [];
            });
    }

    function updateBatchButton() {
        var checked = document.querySelectorAll('.row-checkbox:checked');
        elements.batchDeleteBtn.disabled = checked.length === 0;
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
