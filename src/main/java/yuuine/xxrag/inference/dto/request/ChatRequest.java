package yuuine.xxrag.inference.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class ChatRequest {
    private String model;
    private List<Message> messages;
    private Double temperature;
    private Integer max_tokens;
    private boolean stream = false;

    @Data
    public static class Message {
        private String role;     // "user" 或 "system"
        private String content;

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }
}
