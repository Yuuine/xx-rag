-- 创建文档表
CREATE TABLE IF NOT EXISTS rag.rag_documents
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    file_md5   CHAR(32)                           NOT NULL,
    file_name  VARCHAR(255)                       NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP NULL,

    CONSTRAINT file_md5 UNIQUE (file_md5),
    INDEX idx_created (created_at)
);


-- 对话会话表
CREATE TABLE IF NOT EXISTS rag_sessions
(
    session_id VARCHAR(64) PRIMARY KEY,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_updated_at (updated_at)
);


-- 对话历史记录表
CREATE TABLE IF NOT EXISTS rag_chat_history
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id VARCHAR(64)                          NOT NULL,
    role       ENUM ('user', 'assistant', 'system') NOT NULL,
    content    TEXT                                 NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_session_created (session_id, created_at)
);
