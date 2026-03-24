package yuuine.xxrag.inference.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class ChatRequest {
    private String model;
    private List<Message> messages;
    private Double temperature;
    private Integer max_tokens;
    private boolean stream;
    private List<Tool> tools;
    private Object response_format;

    @Data
    public static class Message {
        private String role;
        private String content;

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }

    @Data
    public static class Tool {
        private String type;
        private Function function;

        @Data
        public static class Function {
            private String name;
            private String description;
            private Object parameters;
        }
    }
}
