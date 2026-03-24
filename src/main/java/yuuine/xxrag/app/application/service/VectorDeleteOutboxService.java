package yuuine.xxrag.app.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import yuuine.xxrag.app.domain.model.VectorDeleteOutboxEvent;
import yuuine.xxrag.app.domain.repository.VectorDeleteOutboxMapper;
import yuuine.xxrag.config.RabbitMQConfig;
import yuuine.xxrag.exception.BusinessException;
import yuuine.xxrag.exception.ErrorCode;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class VectorDeleteOutboxService {

    public static final String EVENT_TYPE_VECTOR_DELETE = "VECTOR_DELETE";

    private final VectorDeleteOutboxMapper outboxMapper;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    public Long saveDeleteEvent(List<String> fileMd5s) {
        VectorDeleteOutboxEvent event = new VectorDeleteOutboxEvent();
        event.setEventType(EVENT_TYPE_VECTOR_DELETE);
        event.setPayload(toPayload(fileMd5s));
        event.setStatus("NEW");
        event.setRetryCount(0);
        event.setErrorMessage(null);
        event.setNextRetryAt(LocalDateTime.now());
        event.setCreatedAt(LocalDateTime.now());
        event.setUpdatedAt(LocalDateTime.now());
        outboxMapper.insert(event);
        return event.getId();
    }

    public void publishAfterCommit(Long eventId) {
        if (eventId == null) {
            return;
        }
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publish(eventId);
                }
            });
        } else {
            publish(eventId);
        }
    }

    public void publish(Long eventId) {
        CorrelationData correlationData = new CorrelationData(String.valueOf(eventId));
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.TASK_EXCHANGE,
                RabbitMQConfig.ROUTING_KEY,
                eventId,
                message -> {
                    MessageProperties props = message.getMessageProperties();
                    props.setMessageId(String.valueOf(eventId));
                    props.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                    return message;
                },
                correlationData
        );
        log.info("已投递 vector 删除 outbox 事件到 Rabbit, eventId={}", eventId);
    }

    public VectorDeleteOutboxEvent getById(Long eventId) {
        return outboxMapper.findById(eventId);
    }

    public List<String> parseFileMd5s(String payload) {
        try {
            return objectMapper.readValue(payload, new TypeReference<List<String>>() {
            });
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.OUTBOX_PAYLOAD_PARSE_ERROR, "解析 outbox payload 失败", e);
        }
    }

    public void markSuccess(Long eventId) {
        outboxMapper.markSuccess(eventId, LocalDateTime.now());
    }

    public void markFailed(Long eventId, int retryCount, String errorMessage) {
        LocalDateTime nextRetryAt = LocalDateTime.now().plusMinutes(Math.min(30, retryCount * 2L));
        outboxMapper.markFailed(eventId, retryCount, truncateError(errorMessage), nextRetryAt, LocalDateTime.now());
    }

    public List<VectorDeleteOutboxEvent> findRetryableEvents(int limit) {
        return outboxMapper.findRetryableEvents(LocalDateTime.now(), limit);
    }

    private String toPayload(List<String> fileMd5s) {
        try {
            return objectMapper.writeValueAsString(fileMd5s);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.OUTBOX_PAYLOAD_SERIALIZE_ERROR, "序列化 outbox payload 失败", e);
        }
    }

    public void onPublishConfirmed(Long eventId) {
        outboxMapper.markPublished(eventId, LocalDateTime.now());
        log.debug("outbox 事件发布确认成功, eventId={}", eventId);
    }

    public void onPublishFailed(Long eventId, String reason) {
        VectorDeleteOutboxEvent event = outboxMapper.findById(eventId);
        int retry = event == null || event.getRetryCount() == null ? 1 : event.getRetryCount() + 1;
        markFailed(eventId, retry, reason == null ? "Rabbit publish failed" : reason);
        log.warn("outbox 事件发布确认失败, eventId={}, retry={}, reason={}", eventId, retry, reason);
    }

    private String truncateError(String errorMessage) {
        if (errorMessage == null) {
            return null;
        }
        return errorMessage.length() > 1000 ? errorMessage.substring(0, 1000) : errorMessage;
    }
}

