package yuuine.xxrag.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
@ConditionalOnProperty(prefix = "aop", name = "performance-enabled", havingValue = "true", matchIfMissing = true)
public class PerformanceAspect {

    @Around("execution(* yuuine.xxrag.app.application.service..*(..)) || " +
            "execution(* yuuine.xxrag.ingestion.domain.service..*(..)) || " +
            "execution(* yuuine.xxrag.vector.domain.embedding.service..*(..))")
    public Object monitor(ProceedingJoinPoint joinPoint) throws Throwable {

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