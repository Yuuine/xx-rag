package yuuine.xxrag.app.application.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import yuuine.xxrag.config.RabbitMQConfig;

@Service
@Slf4j
public class VectorDeleteDeadLetterConsumer {

    @RabbitListener(queues = RabbitMQConfig.TASK_DLX_QUEUE)
    public void consumeDeadLetter(Long eventId) {
        log.error("检测到 vector 删除死信消息, eventId={}", eventId);
    }
}

