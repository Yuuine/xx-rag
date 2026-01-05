package yuuine.xxrag.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class ThreadPoolConfig {

    // 用于向量检索、嵌入生成等中等耗时操作
    @Bean("vectorTaskExecutor")
    public TaskExecutor vectorTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(20);          // 核心线程数
        executor.setMaxPoolSize(100);          // 最大线程数
        executor.setQueueCapacity(200);        // 队列容量
        executor.setThreadNamePrefix("vector-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy()); // 拒绝策略：降速保护
        executor.initialize();
        return executor;
    }

    // 用于DeepSeek LLM推理等高延迟操作
    @Bean("inferenceTaskExecutor")
    public TaskExecutor inferenceTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("inference-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
