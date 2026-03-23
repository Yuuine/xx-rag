package yuuine.xxrag.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableRabbit
public class RabbitMQConfig {

    public static final String TASK_EXCHANGE = "business.task.exchange";
    public static final String TASK_QUEUE    = "business.task.queue";
    public static final String ROUTING_KEY   = "task.process";

    public static final String TASK_DLX_EXCHANGE = "business.task.dlx.exchange";
    public static final String TASK_DLX_QUEUE = "business.task.dlx.queue";
    public static final String TASK_DLX_ROUTING_KEY = "task.process.dlx";

    @Bean
    public DirectExchange taskExchange() {
        return ExchangeBuilder.directExchange(TASK_EXCHANGE)
                .durable(true)     // 持久化，重启 broker 后还在
                .build();
    }

    @Bean
    public Queue taskQueue() {
        return QueueBuilder.durable(TASK_QUEUE)
                .deadLetterExchange(TASK_DLX_EXCHANGE)
                .deadLetterRoutingKey(TASK_DLX_ROUTING_KEY)
                .build();
    }

    @Bean
    public Binding taskBinding() {
        return BindingBuilder.bind(taskQueue())
                .to(taskExchange())
                .with(ROUTING_KEY);
    }

    @Bean
    public DirectExchange taskDlxExchange() {
        return ExchangeBuilder.directExchange(TASK_DLX_EXCHANGE)
                .durable(true)
                .build();
    }

    @Bean
    public Queue taskDlxQueue() {
        return QueueBuilder.durable(TASK_DLX_QUEUE).build();
    }

    @Bean
    public Binding taskDlxBinding() {
        return BindingBuilder.bind(taskDlxQueue())
                .to(taskDlxExchange())
                .with(TASK_DLX_ROUTING_KEY);
    }
}