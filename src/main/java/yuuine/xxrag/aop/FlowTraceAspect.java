package yuuine.xxrag.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;
import yuuine.xxrag.aop.tracing.LayerResolver;
import yuuine.xxrag.aop.tracing.ModuleResolver;
import yuuine.xxrag.aop.tracing.TraceContext;
import yuuine.xxrag.aop.tracing.TraceNode;

@Aspect
@Component
@Slf4j
public class FlowTraceAspect {

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

            log.info("[TRACE] {}", TraceContext.prettyTrace());

            return pjp.proceed();

        } finally {
            TraceContext.pop();
            if (root) {
                TraceContext.clear();
            }
        }
    }
}
