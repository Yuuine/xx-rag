package yuuine.xxrag.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import yuuine.xxrag.app.application.service.VectorDeleteOutboxService;

@Component
@RequiredArgsConstructor
@Slf4j
public class RabbitPublisherCallbackConfig {

    private final RabbitTemplate rabbitTemplate;
    private final VectorDeleteOutboxService outboxService;

    @PostConstruct
    public void initCallbacks() {
        rabbitTemplate.setConfirmCallback(this::onConfirm);
        rabbitTemplate.setReturnsCallback(returned -> {
            Long eventId = parseCorrelationId(returned.getMessage().getMessageProperties().getMessageId());
            if (eventId != null) {
                outboxService.onPublishFailed(eventId, "消息退回: " + returned.getReplyText());
            }
            log.error("Rabbit 消息退回 exchange={}, routingKey={}, replyCode={}, replyText={}",
                    returned.getExchange(), returned.getRoutingKey(), returned.getReplyCode(), returned.getReplyText());
        });
    }

    private void onConfirm(CorrelationData correlationData, boolean ack, String cause) {
        Long eventId = correlationData == null ? null : parseCorrelationId(correlationData.getId());
        if (eventId == null) {
            return;
        }
        if (ack) {
            outboxService.onPublishConfirmed(eventId);
        } else {
            outboxService.onPublishFailed(eventId, cause == null ? "Rabbit confirm nack" : cause);
        }
    }

    private Long parseCorrelationId(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            log.warn("无法解析 Rabbit correlation id: {}", value);
            return null;
        }
    }
}

