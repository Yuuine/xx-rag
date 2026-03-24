package yuuine.xxrag.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import yuuine.xxrag.aop.config.AopProperties;

@Slf4j
@Aspect
@Component
public class PerformanceAspect extends BaseAspect {

    private static final org.slf4j.Logger perfLogger =
            org.slf4j.LoggerFactory.getLogger("PERFORMANCE_LOGGER");

    public PerformanceAspect(AopProperties aopProperties) {
        super(aopProperties);
    }

    @Pointcut("""
            @within(org.springframework.stereotype.Service) &&
            execution(* yuuine.xxrag..*(..))
            """)
    public void allServiceMethods() {
    }

    @Around("allServiceMethods()")
    public Object monitor(ProceedingJoinPoint joinPoint) throws Throwable {
        if (!aopProperties.getPerformance().isEnabled()) {
            return joinPoint.proceed();
        }

        String methodInfo = getMethodInfo(joinPoint);
        String threadName = Thread.currentThread().getName();

        return proceedWithTiming(joinPoint, new TimingCallback() {
            @Override
            public void beforeProceed() {
                perfLogger.debug("Start: {} [Thread: {}]", methodInfo, threadName);
            }

            @Override
            public void afterProceed(long durationMs) {
                if (durationMs >= aopProperties.getPerformance().getSlowThresholdMs()) {
                    perfLogger.warn("SLOW: {} took {}ms [Thread: {}]", methodInfo, durationMs, threadName);
                } else {
                    perfLogger.info("Completed: {} took {}ms [Thread: {}]", methodInfo, durationMs, threadName);
                }
            }

            @Override
            public void onError(long durationMs, Throwable throwable) {
                perfLogger.error("Failed: {} took {}ms [Thread: {}]", methodInfo, durationMs, threadName, throwable);
            }
        });
    }
}
