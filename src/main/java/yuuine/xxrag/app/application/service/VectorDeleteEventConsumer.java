package yuuine.xxrag.app.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import yuuine.xxrag.app.domain.model.VectorDeleteOutboxEvent;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class VectorDeleteEventConsumer {

    private final VectorDeleteOutboxService outboxService;
    private final RagVectorService ragVectorService;

    @RabbitListener(queues = "#{T(yuuine.xxrag.config.RabbitMQConfig).TASK_QUEUE}")
    public void consumeVectorDeleteEvent(Long eventId) {
        if (eventId == null) {
            return;
        }

        VectorDeleteOutboxEvent event = outboxService.getById(eventId);
        if (event == null) {
            log.warn("未找到 outbox 事件，跳过消费, eventId={}", eventId);
            return;
        }
        if ("SUCCESS".equals(event.getStatus())) {
            log.debug("outbox 事件已成功处理，跳过重复消费, eventId={}", eventId);
            return;
        }
        if (!VectorDeleteOutboxService.EVENT_TYPE_VECTOR_DELETE.equals(event.getEventType())) {
            log.warn("不支持的 outbox 事件类型，eventId={}, eventType={}", eventId, event.getEventType());
            return;
        }

        try {
            List<String> fileMd5s = outboxService.parseFileMd5s(event.getPayload());
            ragVectorService.deleteChunksByFileMd5s(fileMd5s);
            outboxService.markSuccess(eventId);
            log.info("vector 删除 outbox 事件处理成功, eventId={}, md5Size={}", eventId, fileMd5s.size());
        } catch (Exception e) {
            int retry = event.getRetryCount() == null ? 1 : event.getRetryCount() + 1;
            outboxService.markFailed(eventId, retry, e.getMessage());
            log.error("vector 删除 outbox 事件处理失败, eventId={}, retry={}", eventId, retry, e);
            throw e;
        }
    }
}

