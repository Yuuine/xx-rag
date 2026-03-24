package yuuine.xxrag.aop;

import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import yuuine.xxrag.aop.config.AopProperties;

@RequiredArgsConstructor
public abstract class BaseAspect {

    protected final AopProperties aopProperties;

    protected String getMethodInfo(ProceedingJoinPoint joinPoint) {
        String className = joinPoint.getSignature().getDeclaringType().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        return className + "#" + methodName;
    }

    protected Object proceedWithTiming(ProceedingJoinPoint joinPoint, TimingCallback callback) throws Throwable {
        long startTime = System.currentTimeMillis();
        try {
            callback.beforeProceed();
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - startTime;
            callback.afterProceed(duration);
            return result;
        } catch (Throwable throwable) {
            long duration = System.currentTimeMillis() - startTime;
            callback.onError(duration, throwable);
            throw throwable;
        }
    }

    protected interface TimingCallback {
        default void beforeProceed() {}
        void afterProceed(long durationMs);
        void onError(long durationMs, Throwable throwable);
    }
}
