package yuuine.xxrag.app.application.service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import yuuine.xxrag.app.config.ChatHistoryProperties;
import yuuine.xxrag.dto.request.InferenceRequest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 个人使用、全局单人对话：全进程共享一份消息列表，所有 WebSocket 连接读写同一上下文。
 * 多标签页会共享历史；不提供按连接隔离。持久化由 {@link ChatHistoryPersistenceService} 完成。
 */
@Service
@Slf4j
public class ChatSessionService {

    private final ChatHistoryPersistenceService persistenceService;
    private final ChatHistoryProperties chatHistoryProperties;
    private final List<InferenceRequest.Message> messages;

    public ChatSessionService(ChatHistoryProperties chatHistoryProperties, ChatHistoryPersistenceService persistenceService) {
        this.chatHistoryProperties = chatHistoryProperties;
        this.persistenceService = persistenceService;
        this.messages = new CopyOnWriteArrayList<>();
    }

    private int flushThreshold() {
        int t = chatHistoryProperties.getFlushThreshold();
        return t < 1 ? 1 : t;
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

        if (messages.size() >= flushThreshold()) {
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

        log.info("缓存消息达到阈值 {}，开始持久化", flushThreshold());
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
