package yuuine.xxrag.app.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import yuuine.xxrag.app.config.ChatHistoryProperties;
import yuuine.xxrag.dto.request.InferenceRequest;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class ChatHistoryPersistenceService {

    private final ChatHistoryProperties properties;
    private final ObjectMapper objectMapper;
    private final Path historyFilePath;

    public ChatHistoryPersistenceService(ChatHistoryProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.historyFilePath = Paths.get(properties.getHistoryFilePath());
    }

    @PostConstruct
    public void init() {
        try {
            Path parentDir = historyFilePath.getParent();
            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
                log.info("创建历史记录存储目录: {}", parentDir);
            }
        } catch (IOException e) {
            log.error("创建历史记录存储目录失败: {}", historyFilePath.getParent(), e);
        }
    }

    @SuppressWarnings("unchecked")
    public List<InferenceRequest.Message> loadHistory() {
        if (!Files.exists(historyFilePath)) {
            log.debug("历史记录文件不存在，返回空列表: {}", historyFilePath);
            return new ArrayList<>();
        }

        try {
            String content = Files.readString(historyFilePath);
            Map<String, Object> data = objectMapper.readValue(content, LinkedHashMap.class);

            List<Map<String, Object>> messagesData = (List<Map<String, Object>>) data.get("messages");
            List<InferenceRequest.Message> messages = new ArrayList<>();

            if (messagesData != null) {
                for (Map<String, Object> msgData : messagesData) {
                    String role = (String) msgData.get("role");
                    String contentStr = (String) msgData.get("content");
                    messages.add(new InferenceRequest.Message(role, contentStr));
                }
            }

            log.info("从文件加载历史记录成功，数量: {}", messages.size());
            return messages;
        } catch (IOException e) {
            log.error("读取历史记录文件失败: {}", historyFilePath, e);
            return new ArrayList<>();
        }
    }

    public void saveHistory(List<InferenceRequest.Message> messages) {
        if (messages == null || messages.isEmpty()) {
            log.debug("没有消息需要持久化");
            return;
        }

        try {
            List<Map<String, String>> messagesData = new ArrayList<>();
            for (InferenceRequest.Message msg : messages) {
                Map<String, String> msgMap = new LinkedHashMap<>();
                msgMap.put("role", msg.getRole());
                msgMap.put("content", msg.getContent());
                messagesData.add(msgMap);
            }

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("lastUpdated", LocalDateTime.now().toString());
            data.put("messages", messagesData);

            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(data);
            Files.writeString(historyFilePath, json);

            log.info("历史记录持久化成功，数量: {}，文件: {}", messages.size(), historyFilePath);
        } catch (IOException e) {
            log.error("写入历史记录文件失败: {}", historyFilePath, e);
        }
    }
}
