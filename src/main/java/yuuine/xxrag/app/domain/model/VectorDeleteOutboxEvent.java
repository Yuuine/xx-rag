package yuuine.xxrag.app.domain.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class VectorDeleteOutboxEvent {

    private Long id;
    private String eventType;
    private String payload;
    private String status;
    private Integer retryCount;
    private String errorMessage;
    private LocalDateTime nextRetryAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

