package yuuine.xxrag.app.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import yuuine.xxrag.app.config.ChatHistoryProperties;
import yuuine.xxrag.dto.request.InferenceRequest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 个人版全局对话的本地文件持久化（JSON）。关闭 {@link ChatHistoryProperties#persistenceEnabled} 时不读写文件。
 */
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
        if (!properties.isPersistenceEnabled()) {
            log.debug("历史持久化已关闭，不加载文件");
            return new ArrayList<>();
        }
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

    public boolean saveHistory(List<InferenceRequest.Message> messages) {
        if (!properties.isPersistenceEnabled()) {
            log.debug("历史持久化已关闭，跳过写入");
            return false;
        }
        if (messages == null || messages.isEmpty()) {
            log.debug("没有消息需要持久化");
            return false;
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
            return true;
        } catch (IOException e) {
            log.error("写入历史记录文件失败: {}", historyFilePath, e);
            return false;
        }
    }
}
