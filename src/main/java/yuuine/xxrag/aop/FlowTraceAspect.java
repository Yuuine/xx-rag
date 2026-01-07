package yuuine.xxrag.aop;

import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import yuuine.xxrag.aop.tracing.LayerResolver;
import yuuine.xxrag.aop.tracing.ModuleResolver;
import yuuine.xxrag.aop.tracing.TraceContext;
import yuuine.xxrag.aop.tracing.TraceNode;

@Aspect
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "aop", name = "FlowTrace-enabled", havingValue = "true", matchIfMissing = true)
public class FlowTraceAspect {

    private static final org.slf4j.Logger traceLogger =
        org.slf4j.LoggerFactory.getLogger("TRACING_LOGGER");

    @Pointcut("""
                execution(* yuuine.xxrag..*(..))
                && !within(yuuine.xxrag.aop.tracing..*)
                && !within(yuuine.xxrag.aop..*)
            """)
    public void applicationFlow() {
    }

    @Around("applicationFlow()")
    public Object trace(ProceedingJoinPoint pjp) throws Throwable {

        Class<?> type = pjp.getSignature().getDeclaringType();

        TraceNode node = new TraceNode(
                ModuleResolver.resolve(type),
                LayerResolver.resolve(type),
                type.getSimpleName(),
                pjp.getSignature().getName()
        );

        boolean root = TraceContext.isEmpty();

        try {
            TraceContext.push(node);

            traceLogger.info("[TRACE] {}", TraceContext.prettyTrace());

            return pjp.proceed();

        } finally {
            TraceContext.pop();
            if (root) {
                TraceContext.clear();
            }
        }
    }
}
