package yuuine.xxrag.app.application.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import yuuine.xxrag.app.config.ChatHistoryProperties;
import yuuine.xxrag.dto.request.InferenceRequest;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
@Slf4j
public class ChatSessionService {

    private static final int MAX_MESSAGES = 20;

    private final ChatHistoryPersistenceService persistenceService;
    private final ChatHistoryProperties chatHistoryProperties;
    private final List<InferenceRequest.Message> messages;

    public ChatSessionService(ChatHistoryProperties chatHistoryProperties, ChatHistoryPersistenceService persistenceService) {
        this.chatHistoryProperties = chatHistoryProperties;
        this.persistenceService = persistenceService;
        this.messages = new CopyOnWriteArrayList<>();
    }

    @PostConstruct
    public void init() {
        List<InferenceRequest.Message> loadedMessages = persistenceService.loadHistory();
        messages.addAll(loadedMessages);
        log.info("ChatSessionService 初始化完成，已加载 {} 条历史消息", messages.size());
    }

    public void addUserMessage(String content) {
        addMessage("user", content);
    }

    public void addAssistantMessage(String content) {
        addMessage("assistant", content);
    }

    private void addMessage(String role, String content) {
        InferenceRequest.Message message = new InferenceRequest.Message(role, content);
        messages.add(message);
        log.debug("添加消息到缓存，角色: {}, 当前缓存消息数: {}", role, messages.size());

        if (messages.size() >= MAX_MESSAGES) {
            persistAllAndClear();
        }
    }

    public List<InferenceRequest.Message> getMessages() {
        return Collections.unmodifiableList(new ArrayList<>(messages));
    }

    public int getMessageCount() {
        return messages.size();
    }

    private void persistAllAndClear() {
        if (messages.isEmpty()) {
            return;
        }

        log.info("缓存消息达到阈值 {}，开始持久化", MAX_MESSAGES);
        List<InferenceRequest.Message> messagesToPersist = new ArrayList<>(messages);
        persistenceService.saveHistory(messagesToPersist);
        messages.clear();
        log.info("持久化完成，缓存已清空");
    }

    @PreDestroy
    public void onDestroy() {
        if (!messages.isEmpty()) {
            log.info("应用关闭前，持久化剩余 {} 条消息", messages.size());
            List<InferenceRequest.Message> messagesToPersist = new ArrayList<>(messages);
            persistenceService.saveHistory(messagesToPersist);
            messages.clear();
        } else {
            log.info("应用关闭，没有需要持久化的消息");
        }
    }
}
