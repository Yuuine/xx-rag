package yuuine.xxrag.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String TASK_EXCHANGE = "business.task.exchange";
    public static final String TASK_QUEUE    = "business.task.queue";
    public static final String ROUTING_KEY   = "task.process";   // 路由键

    @Bean
    public DirectExchange taskExchange() {
        return ExchangeBuilder.directExchange(TASK_EXCHANGE)
                .durable(true)     // 持久化，重启 broker 后还在
                .build();
    }

    @Bean
    public Queue taskQueue() {
        return QueueBuilder.durable(TASK_QUEUE)
                .build();
    }

    @Bean
    public Binding taskBinding() {
        return BindingBuilder.bind(taskQueue())
                .to(taskExchange())
                .with(ROUTING_KEY);
    }
}