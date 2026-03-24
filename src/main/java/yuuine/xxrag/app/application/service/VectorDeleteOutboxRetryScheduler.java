package yuuine.xxrag.app.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import yuuine.xxrag.app.domain.model.VectorDeleteOutboxEvent;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class VectorDeleteOutboxRetryScheduler {

    private final VectorDeleteOutboxService outboxService;

    @Scheduled(fixedDelay = 60000)
    public void retryFailedEvents() {
        List<VectorDeleteOutboxEvent> events;
        try {
            events = outboxService.findRetryableEvents(100);
        } catch (Exception e) {
            log.warn("outbox 重试扫描失败，跳过本轮调度", e);
            return;
        }
        if (events.isEmpty()) {
            return;
        }
        for (VectorDeleteOutboxEvent event : events) {
            try {
                outboxService.publish(event.getId());
            } catch (Exception e) {
                log.warn("重新发布 outbox 事件失败, eventId={}", event.getId(), e);
            }
        }
        log.info("outbox 重试扫描完成, republishCount={}", events.size());
    }
}

