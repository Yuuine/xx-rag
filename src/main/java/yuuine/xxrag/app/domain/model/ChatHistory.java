package yuuine.xxrag.app.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatHistory {
    private Long id;
    private String sessionId;
    private String role;
    private String content;
    private LocalDateTime createdAt;
}
