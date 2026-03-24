CREATE TABLE IF NOT EXISTS rag_outbox_events (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    event_type VARCHAR(64) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(32) NOT NULL,
    retry_count INT NOT NULL DEFAULT 0,
    error_message VARCHAR(1000) NULL,
    next_retry_at DATETIME NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    INDEX idx_rag_outbox_status_retry (status, next_retry_at),
    INDEX idx_rag_outbox_created_at (created_at)
);

