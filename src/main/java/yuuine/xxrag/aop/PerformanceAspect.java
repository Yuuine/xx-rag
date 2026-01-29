package yuuine.xxrag.aop;

import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import yuuine.xxrag.aop.config.AopProperties;

@Aspect
@Component
@RequiredArgsConstructor
public class PerformanceAspect {

    private final AopProperties aopProperties;

    // 使用特定的logger名称，以便在logback配置中定向到专用文件
    private static final org.slf4j.Logger log = 
        org.slf4j.LoggerFactory.getLogger("yuuine.xxrag.aop.PerformanceAspect");

    @Around(value = "(execution(* yuuine.xxrag.app.application.service..*(..)) || " +
            "execution(* yuuine.xxrag.ingestion.domain.service..*(..)) || " +
            "execution(* yuuine.xxrag.vector.domain.embedding.service..*(..))) " +
            "&& !within(yuuine.xxrag.websocket..*)")
    public Object monitor(ProceedingJoinPoint joinPoint) throws Throwable {

        // 检查是否启用了性能监控
        if (!aopProperties.isPerformanceEnabled()) {
            return joinPoint.proceed();
        }

        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        String fullMethod = className + "#" + methodName;
        String threadName = Thread.currentThread().getName();

        long startTime = System.currentTimeMillis();

        try {
            log.debug("开始执行 {} 方法, 线程: {}", fullMethod, threadName);

            Object result = joinPoint.proceed();

            long duration = System.currentTimeMillis() - startTime;
            log.info("执行完成 {} 方法, 耗时: {}ms, 线程: {}", fullMethod, duration, threadName);

            return result;
        } catch (Throwable throwable) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("执行失败 {} 方法, 耗时: {}ms, 线程: {}", fullMethod, duration, threadName, throwable);
            throw throwable;
        }
    }
}