package yuuine.xxrag.app.application.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 定时任务服务 - 用于清理长期未使用的会话
 */
@Service
@Slf4j
public class SessionCleanupService {

    private final ChatSessionTransactionalService chatSessionTransactionalService;

    public SessionCleanupService(ChatSessionTransactionalService chatSessionTransactionalService) {
        this.chatSessionTransactionalService = chatSessionTransactionalService;
    }

    /**
     * 定时清理超过一个月未活动的会话
     * 每三小时执行一次
     */
    @Scheduled(cron = "0 0 */3 * * ?")
    public void cleanupInactiveSessions() {
        log.info("开始执行清理长期未活动会话的任务");

        try {
            // 计算一个月前的时间点
            LocalDateTime oneMonthAgo = LocalDateTime.now().minusMonths(1);

            // 查找超过一个月未更新的会话
            List<String> inactiveSessions = chatSessionTransactionalService.findInactiveSessions(oneMonthAgo);

            if (inactiveSessions.isEmpty()) {
                log.info("没有找到超过一个月未活动的会话，任务结束");
                return;
            }

            log.info("发现 {} 个超过一个月未活动的会话，开始清理", inactiveSessions.size());

            // 批量删除这些会话
            chatSessionTransactionalService.batchDeleteSessions(inactiveSessions);

            log.info("清理完成，共删除 {} 个长期未活动的会话", inactiveSessions.size());

        } catch (Exception e) {
            log.error("清理长期未活动会话时发生错误", e);
        }
    }
}
