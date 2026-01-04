package yuuine.xxrag.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class PerformanceAspect {
    @Around("execution(* yuuine.xxrag.app.application.service..*(..)) || " +
            "execution(* yuuine.xxrag.ingestion.domain.service..*(..)) || " +
            "execution(* yuuine.xxrag.vector.domain.embedding.service..*(..)) || " +
            "execution(* yuuine.xxrag.app.application.service.impl.RagInferenceServiceImpl.*(..))")
    public Object monitor(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        long startTime = System.currentTimeMillis();

        try {
            log.debug("开始执行 {}#{} 方法", className, methodName);
            Object result = joinPoint.proceed();
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            log.info("执行完成 {}#{} 方法, 耗时: {}ms", className, methodName, duration);
            return result;
        } catch (Throwable throwable) {
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            log.error("执行失败 {}#{} 方法, 耗时: {}ms", className, methodName, duration);
            throw throwable;
        }
    }
}