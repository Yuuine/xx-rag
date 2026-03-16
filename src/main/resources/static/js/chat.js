(function() {
    'use strict';

    var SID_LOCALSTORAGE_KEY = 'chat_sid';
    var MAX_RECONNECT_ATTEMPTS = 5;
    var RECONNECT_INTERVAL = 3000;
    var TIMEOUT_DURATION = 60 * 60 * 1000;
    var UPDATE_INTERVAL_MS = 100;

    var state = {
        ws: null,
        wsReconnectAttempts: 0,
        sessionId: null,
        showTimestamps: false,
        isStreaming: false,
        currentStreamStartTime: null,
        currentAiMessageElement: null,
        currentAiContentElement: null,
        streamBuffer: '',
        dirty: false,
        timeoutId: null,
        updateIntervalId: null
    };

    var elements = {};

    function init() {
        cacheElements();
        setupMarked();
        bindEvents();
        bindDragDrop();
        startUpdateInterval();
        connectWebSocket();
    }

    function cacheElements() {
        elements.messagesContainer = document.getElementById('messagesContainer');
        elements.questionInput = document.getElementById('questionInput');
        elements.sendBtn = document.getElementById('sendBtn');
        elements.fileInput = document.getElementById('fileInput');
        elements.uploadBtn = document.getElementById('uploadBtn');
        elements.uploadTrigger = document.getElementById('uploadTrigger');
        elements.uploadModal = document.getElementById('uploadModal');
        elements.uploadArea = document.getElementById('uploadArea');
        elements.newSessionBtn = document.getElementById('newSessionBtn');
        elements.cleanupBtn = document.getElementById('cleanupBtn');
        elements.timeToggle = document.getElementById('timeToggle');
        elements.viewDocsTrigger = document.getElementById('viewDocsTrigger');
        elements.dragOverlay = document.getElementById('dragOverlay');
    }

    function setupMarked() {
        if (typeof marked !== 'undefined') {
            marked.setOptions({
                gfm: true,
                breaks: true,
                headerIds: false,
                mangle: false
            });
        }
    }

    function getSessionId() {
        if (state.sessionId) return state.sessionId;
        try {
            state.sessionId = localStorage.getItem(SID_LOCALSTORAGE_KEY);
        } catch (e) {
            console.warn('读取 localStorage 失败', e);
        }
        if (!state.sessionId) {
            state.sessionId = XXRAG.generateSessionId();
            try {
                localStorage.setItem(SID_LOCALSTORAGE_KEY, state.sessionId);
            } catch (e) {
                console.warn('写入 localStorage 失败', e);
            }
        }
        return state.sessionId;
    }

    function clearSession() {
        state.sessionId = null;
        try {
            localStorage.removeItem(SID_LOCALSTORAGE_KEY);
        } catch (e) {
            console.warn('移除 sid 失败', e);
        }
        XXRAG.showToast('会话已清除', 'success');
        elements.messagesContainer.innerHTML = '';
        reconnectWebSocket();
    }

    function connectWebSocket() {
        if (state.ws && (state.ws.readyState === WebSocket.OPEN || state.ws.readyState === WebSocket.CONNECTING)) {
            return;
        }

        var currentSessionId = getSessionId();
        var protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
        var wsUrl = protocol + '//' + window.location.host + '/ws-chat?sid=' + encodeURIComponent(currentSessionId);

        try {
            state.ws = new WebSocket(wsUrl);

            state.ws.onopen = function() {
                console.log('WebSocket连接已建立，会话ID:', currentSessionId);
                state.wsReconnectAttempts = 0;
                XXRAG.showToast('连接已建立', 'success');
            };

            state.ws.onmessage = function(event) {
                try {
                    var data = JSON.parse(event.data);

                    if (data && data.type === 'history' && Array.isArray(data.messages)) {
                        data.messages.forEach(function(msg) {
                            if (!msg || !msg.role) return;
                            if (msg.role === 'user') {
                                addUserMessage(msg.content || '');
                            } else if (msg.role === 'assistant') {
                                renderAiMessage(msg.content || '', 0);
                            }
                        });
                        return;
                    }

                    if (data.finishReason === 'stop') {
                        finalizeStream();
                    } else if (data.message && data.message.startsWith('Error:')) {
                        renderAiMessage('错误：' + data.message.replace('Error: ', ''));
                        finalizeStream();
                    } else if (data.content) {
                        processStreamChunk(data.content);
                        resetTimeoutTimer();
                    }
                } catch (e) {
                    console.error('解析消息失败:', e);
                    renderAiMessage('收到无法解析的消息');
                    finalizeStream();
                }
            };

            state.ws.onclose = function(event) {
                console.log('WebSocket关闭', event.code, event.reason);
                if (state.isStreaming) finalizeStream();
                clearTimeoutTimer();

                if (event.code !== 1000 && state.wsReconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
                    state.wsReconnectAttempts++;
                    XXRAG.showToast('连接断开，' + (RECONNECT_INTERVAL / 1000) + '秒后重连 (' + state.wsReconnectAttempts + '/' + MAX_RECONNECT_ATTEMPTS + ')', 'error');
                    setTimeout(connectWebSocket, RECONNECT_INTERVAL);
                } else if (state.wsReconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
                    XXRAG.showToast('重连次数超限，请刷新页面', 'error');
                }
            };

            state.ws.onerror = function(error) {
                console.error('WebSocket错误:', error);
                XXRAG.showToast('连接错误', 'error');
            };
        } catch (e) {
            console.error('创建WebSocket失败:', e);
            XXRAG.showToast('连接失败', 'error');
        }
    }

    function reconnectWebSocket() {
        if (state.ws) {
            state.ws.onopen = state.ws.onmessage = state.ws.onclose = state.ws.onerror = null;
            if (state.ws.readyState === WebSocket.OPEN || state.ws.readyState === WebSocket.CONNECTING) {
                state.ws.close(1000, 'client_reconnect');
            }
            state.ws = null;
        }
        setTimeout(function() {
            state.wsReconnectAttempts = 0;
            connectWebSocket();
        }, 200);
    }

    function setupTimeoutTimer() {
        if (state.timeoutId) clearTimeout(state.timeoutId);
        state.timeoutId = setTimeout(function() {
            if (state.isStreaming) {
                XXRAG.showToast('请求超时', 'error');
                finalizeStream();
                if (state.ws && state.ws.readyState === WebSocket.OPEN) {
                    state.ws.send(JSON.stringify({type: 'cancel'}));
                }
            }
        }, TIMEOUT_DURATION);
    }

    function resetTimeoutTimer() {
        if (state.isStreaming) setupTimeoutTimer();
    }

    function clearTimeoutTimer() {
        if (state.timeoutId) {
            clearTimeout(state.timeoutId);
            state.timeoutId = null;
        }
    }

    function startUpdateInterval() {
        state.updateIntervalId = setInterval(function() {
            if (state.dirty && state.currentAiContentElement) {
                var shouldScroll = elements.messagesContainer.scrollTop + elements.messagesContainer.clientHeight >= elements.messagesContainer.scrollHeight - 20;
                state.currentAiContentElement.innerHTML = safeParseMarkdown(state.streamBuffer);
                addCopyButtonsToCodeBlocks(state.currentAiContentElement);
                if (shouldScroll) {
                    scrollToBottom();
                }
                state.dirty = false;
            }
        }, UPDATE_INTERVAL_MS);
    }

    function safeParseMarkdown(text) {
        if (typeof marked === 'undefined') {
            return XXRAG.escapeHtml(text);
        }
        return marked.parse(text);
    }

    function addCopyButtonsToCodeBlocks(container) {
        var preElements = container.querySelectorAll('pre');
        preElements.forEach(function(pre) {
            if (pre.querySelector('.copy-btn')) return;
            
            var code = pre.querySelector('code');
            var codeText = code ? code.textContent : pre.textContent;
            
            var copyBtn = document.createElement('button');
            copyBtn.className = 'copy-btn';
            copyBtn.textContent = '复制';
            copyBtn.addEventListener('click', function(e) {
                e.stopPropagation();
                navigator.clipboard.writeText(codeText).then(function() {
                    copyBtn.textContent = '已复制';
                    copyBtn.classList.add('copied');
                    setTimeout(function() {
                        copyBtn.textContent = '复制';
                        copyBtn.classList.remove('copied');
                    }, 2000);
                }).catch(function() {
                    XXRAG.showToast('复制失败', 'error');
                });
            });
            
            pre.style.position = 'relative';
            pre.appendChild(copyBtn);
        });
    }

    function processStreamChunk(chunk) {
        if (!chunk) return;
        state.streamBuffer += chunk;
        state.dirty = true;
    }

    function startNewStream() {
        state.isStreaming = true;
        state.currentStreamStartTime = new Date();
        state.streamBuffer = '';
        state.dirty = false;

        state.currentAiMessageElement = document.createElement('div');
        state.currentAiMessageElement.className = 'message ai';

        state.currentAiContentElement = document.createElement('div');
        state.currentAiMessageElement.appendChild(state.currentAiContentElement);

        var cursor = document.createElement('span');
        cursor.className = 'typing-cursor';
        state.currentAiContentElement.appendChild(cursor);

        var timestamp = document.createElement('div');
        timestamp.className = 'timestamp';
        timestamp.textContent = new Date().toLocaleString();
        timestamp.style.display = state.showTimestamps ? 'block' : 'none';
        state.currentAiMessageElement.appendChild(timestamp);

        var footnote = document.createElement('div');
        footnote.className = 'footnote';
        footnote.textContent = '正在思考中...';
        state.currentAiMessageElement.appendChild(footnote);

        elements.messagesContainer.appendChild(state.currentAiMessageElement);
        scrollToBottom();

        setupTimeoutTimer();
    }

    function finalizeStream() {
        if (!state.isStreaming) return;

        if (state.currentAiContentElement) {
            state.currentAiContentElement.innerHTML = safeParseMarkdown(state.streamBuffer);
            addCopyButtonsToCodeBlocks(state.currentAiContentElement);
        }

        var duration = state.currentStreamStartTime ? ((new Date() - state.currentStreamStartTime) / 1000).toFixed(2) : '0.00';
        var footnote = state.currentAiMessageElement.querySelector('.footnote');
        if (footnote) {
            footnote.innerHTML = '<span>推理耗时: ' + duration + ' 秒</span><button class="copy-btn-inline" onclick="navigator.clipboard.writeText(' + JSON.stringify(state.streamBuffer.replace(/'/g, "\\'")) + ').then(function(){XXRAG.showToast(\'已复制\',\'success\')}).catch(function(){XXRAG.showToast(\'复制失败\',\'error\')})">复制</button>';
        }

        clearTimeoutTimer();
        state.isStreaming = false;
        state.currentStreamStartTime = null;
        state.currentAiMessageElement = null;
        state.currentAiContentElement = null;
        state.streamBuffer = '';

        elements.sendBtn.disabled = false;
        elements.sendBtn.textContent = '发送';
        elements.questionInput.focus();
    }

    function addUserMessage(text) {
        var div = document.createElement('div');
        div.className = 'message user';
        div.textContent = text;
        var timestamp = document.createElement('div');
        timestamp.className = 'timestamp';
        timestamp.textContent = new Date().toLocaleString();
        timestamp.style.display = state.showTimestamps ? 'block' : 'none';
        div.appendChild(timestamp);
        
        elements.messagesContainer.appendChild(div);
        scrollToBottom();
    }

    function renderAiMessage(text, duration, references) {
        references = references || [];
        var div = document.createElement('div');
        div.className = 'message ai';
        div.innerHTML = safeParseMarkdown(text);
        addCopyButtonsToCodeBlocks(div);

        var timestamp = document.createElement('div');
        timestamp.className = 'timestamp';
        timestamp.textContent = new Date().toLocaleString();
        timestamp.style.display = state.showTimestamps ? 'block' : 'none';
        div.appendChild(timestamp);

        var footnote = document.createElement('div');
        footnote.className = 'footnote';
        footnote.innerHTML = '<span>推理耗时: ' + duration + ' 秒</span><button class="copy-btn-inline" onclick="navigator.clipboard.writeText(' + JSON.stringify(text.replace(/'/g, "\\'")) + ').then(function(){XXRAG.showToast(\'已复制\',\'success\')}).catch(function(){XXRAG.showToast(\'复制失败\',\'error\')})">复制</button>';
        div.appendChild(footnote);

        if (references.length > 0) {
            var uniqueSources = [];
            var seen = {};
            references.forEach(function(r) {
                if (!seen[r.source]) {
                    seen[r.source] = true;
                    uniqueSources.push(r.source);
                }
            });
            if (uniqueSources.length > 0) {
                var refsDiv = document.createElement('div');
                refsDiv.className = 'references';
                refsDiv.innerHTML = '<strong>参考来源：</strong>';
                var ol = document.createElement('ol');
                uniqueSources.forEach(function(s) {
                    var li = document.createElement('li');
                    li.textContent = s;
                    ol.appendChild(li);
                });
                refsDiv.appendChild(ol);
                div.appendChild(refsDiv);
            }
        }

        elements.messagesContainer.appendChild(div);
        scrollToBottom();
    }

    function scrollToBottom() {
        elements.messagesContainer.scrollTop = elements.messagesContainer.scrollHeight;
    }

    function sendQuestion() {
        var question = elements.questionInput.value.trim();
        if (!question) {
            XXRAG.showToast('请输入问题', 'error');
            return;
        }

        addUserMessage(question);
        elements.questionInput.value = '';
        elements.questionInput.style.height = 'auto';
        elements.sendBtn.disabled = true;
        elements.sendBtn.innerHTML = '<span class="loading"></span>思考中';

        startNewStream();

        if (state.ws && state.ws.readyState === WebSocket.OPEN) {
            state.ws.send(question);
        } else {
            XXRAG.showToast('连接未就绪', 'error');
            elements.sendBtn.disabled = false;
            elements.sendBtn.textContent = '发送';
            finalizeStream();
        }
    }

    function uploadFiles(files) {
        if (!files || files.length === 0) return;
        
        var formData = new FormData();
        for (var i = 0; i < files.length; i++) {
            formData.append('files', files[i]);
        }
        
        elements.uploadBtn.disabled = true;
        elements.uploadBtn.textContent = '上传中...';
        
        fetch('/xx/upload', {method: 'POST', body: formData})
            .then(function(res) { return res.json(); })
            .then(function(data) {
                if (data.code === 0) {
                    XXRAG.showToast('上传成功', 'success');
                    elements.fileInput.value = '';
                } else {
                    XXRAG.showToast(data.msg || '上传失败', 'error');
                }
            })
            .catch(function() {
                XXRAG.showToast('上传失败', 'error');
            })
            .finally(function() {
                elements.uploadBtn.disabled = false;
                elements.uploadBtn.textContent = '上传文件';
            });
    }

    function bindDragDrop() {
        var dragCounter = 0;

        document.addEventListener('dragenter', function(e) {
            e.preventDefault();
            dragCounter++;
            if (elements.dragOverlay) {
                elements.dragOverlay.classList.add('active');
            }
        });

        document.addEventListener('dragleave', function(e) {
            e.preventDefault();
            dragCounter--;
            if (dragCounter === 0 && elements.dragOverlay) {
                elements.dragOverlay.classList.remove('active');
            }
        });

        document.addEventListener('dragover', function(e) {
            e.preventDefault();
        });

        document.addEventListener('drop', function(e) {
            e.preventDefault();
            dragCounter = 0;
            if (elements.dragOverlay) {
                elements.dragOverlay.classList.remove('active');
            }
            
            var files = e.dataTransfer.files;
            if (files && files.length > 0) {
                uploadFiles(files);
            }
        });

        if (elements.uploadArea) {
            elements.uploadArea.addEventListener('dragover', function(e) {
                e.preventDefault();
                elements.uploadArea.classList.add('drag-over');
            });

            elements.uploadArea.addEventListener('dragleave', function() {
                elements.uploadArea.classList.remove('drag-over');
            });

            elements.uploadArea.addEventListener('drop', function(e) {
                e.preventDefault();
                e.stopPropagation();
                elements.uploadArea.classList.remove('drag-over');
                var files = e.dataTransfer.files;
                if (files && files.length > 0) {
                    uploadFiles(files);
                }
            });
        }
    }

    function bindEvents() {
        elements.sendBtn.addEventListener('click', sendQuestion);

        elements.questionInput.addEventListener('keydown', function(e) {
            if (e.key === 'Enter' && !e.shiftKey) {
                e.preventDefault();
                sendQuestion();
            }
        });

        elements.questionInput.addEventListener('input', function() {
            this.style.height = 'auto';
            this.style.height = Math.min(this.scrollHeight, 120) + 'px';
        });

        elements.uploadTrigger.addEventListener('click', function(e) {
            e.stopPropagation();
            elements.uploadModal.classList.toggle('active');
        });

        elements.newSessionBtn.addEventListener('click', function(e) {
            e.stopPropagation();
            XXRAG.showModal({
                message: '请选择删除范围：输入天数删除早期记录，或勾选全部删除整个会话',
                showInput: true,
                showCheckbox: true,
                inputPlaceholder: '天数，例如 30'
            }).then(function(res) {
                if (!res.confirmed) return;
                var sid = getSessionId();
                if (!sid) {
                    XXRAG.showToast('无会话ID', 'success');
                    clearSession();
                    return;
                }
                if (res.all) {
                    fetch('/xx/deleteSession', {
                        method: 'POST',
                        headers: {'Content-Type': 'application/json'},
                        body: JSON.stringify({sessionId: sid})
                    }).then(function(r) { return r.json(); }).then(function(data) {
                        if (data && data.code === 0) {
                            XXRAG.showToast('会话已删除', 'success');
                        } else {
                            XXRAG.showToast('删除失败: ' + (data ? data.message : '未知'), 'error');
                        }
                    }).catch(function() {
                        XXRAG.showToast('删除失败', 'error');
                    }).finally(clearSession);
                } else {
                    var days = parseInt(res.value, 10);
                    if (isNaN(days) || days <= 0) {
                        XXRAG.showToast('请输入有效天数', 'error');
                        return;
                    }
                    var before = new Date();
                    before.setDate(before.getDate() - days);
                    fetch('/xx/deleteSessionBefore', {
                        method: 'POST',
                        headers: {'Content-Type': 'application/json'},
                        body: JSON.stringify({sessionId: sid, beforeDate: before.toISOString().slice(0, 19)})
                    }).then(function(r) { return r.json(); }).then(function(data) {
                        if (data && data.code === 0) {
                            XXRAG.showToast('早期记录已删除', 'success');
                        } else {
                            XXRAG.showToast('删除失败', 'error');
                        }
                    }).catch(function() {
                        XXRAG.showToast('删除失败', 'error');
                    }).finally(clearSession);
                }
            });
        });

        elements.cleanupBtn.addEventListener('click', function(e) {
            e.stopPropagation();
            XXRAG.showModal({
                message: '确认删除所有会话记录？请输入管理员密码',
                showPassword: true
            }).then(function(res) {
                if (!res.confirmed) {
                    return;
                }
                if (!res.password) {
                    XXRAG.showToast('需输入密码', 'error');
                    return;
                }
                fetch('/xx/deleteAllSessions', {
                    method: 'POST',
                    headers: {'Content-Type': 'application/json'},
                    body: JSON.stringify({password: res.password})
                }).then(function(r) { return r.json(); }).then(function(data) {
                    if (data && data.code === 0) {
                        XXRAG.showToast('所有记录已删除', 'success');
                    } else {
                        XXRAG.showToast('删除失败', 'error');
                    }
                }).catch(function() {
                    XXRAG.showToast('删除失败', 'error');
                });
            });
        });

        elements.timeToggle.addEventListener('change', function() {
            state.showTimestamps = elements.timeToggle.checked;
            document.querySelectorAll('.timestamp').forEach(function(ts) {
                ts.style.display = state.showTimestamps ? 'block' : 'none';
            });
            XXRAG.showToast(state.showTimestamps ? '时间显示已开启' : '时间显示已关闭', 'success');
        });

        document.addEventListener('click', function(e) {
            if (!elements.uploadModal.contains(e.target) && !elements.uploadTrigger.contains(e.target)) {
                elements.uploadModal.classList.remove('active');
            }
        });

        elements.uploadBtn.addEventListener('click', function() {
            if (elements.fileInput.files.length === 0) {
                XXRAG.showToast('请选择文件', 'error');
                return;
            }
            uploadFiles(elements.fileInput.files);
        });

        elements.viewDocsTrigger.addEventListener('click', function() {
            window.open('/docs.html', '_blank');
        });
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
