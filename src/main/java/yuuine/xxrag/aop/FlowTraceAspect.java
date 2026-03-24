package yuuine.xxrag.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import yuuine.xxrag.aop.config.AopProperties;
import yuuine.xxrag.aop.tracing.LayerResolver;
import yuuine.xxrag.aop.tracing.ModuleResolver;
import yuuine.xxrag.aop.tracing.TraceContext;
import yuuine.xxrag.aop.tracing.TraceNode;

@Slf4j
@Aspect
@Component
public class FlowTraceAspect extends BaseAspect {

    private static final org.slf4j.Logger traceLogger =
            org.slf4j.LoggerFactory.getLogger("TRACING_LOGGER");

    public FlowTraceAspect(AopProperties aopProperties) {
        super(aopProperties);
    }

    @Pointcut("""
            (
                @annotation(org.springframework.web.bind.annotation.RequestMapping) ||
                @annotation(org.springframework.web.bind.annotation.GetMapping) ||
                @annotation(org.springframework.web.bind.annotation.PostMapping) ||
                @annotation(org.springframework.web.bind.annotation.PutMapping) ||
                @annotation(org.springframework.web.bind.annotation.DeleteMapping) ||
                @annotation(org.springframework.web.bind.annotation.PatchMapping)
            ) && execution(* yuuine.xxrag..*(..))
            """)
    public void controllerMethods() {
    }

    @Pointcut("""
            @within(org.springframework.stereotype.Service) &&
            execution(* yuuine.xxrag..*(..))
            """)
    public void serviceMethods() {
    }

    @Pointcut("controllerMethods() || serviceMethods()")
    public void traceableMethods() {
    }

    @Around("traceableMethods()")
    public Object trace(ProceedingJoinPoint pjp) throws Throwable {
        if (!aopProperties.getFlowTrace().isEnabled()) {
            return pjp.proceed();
        }

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

            if (!aopProperties.getFlowTrace().isLogRootOnly()) {
                traceLogger.info("[TRACE] {}", TraceContext.prettyTrace());
            }

            return pjp.proceed();

        } catch (Throwable throwable) {
            boolean shouldLogError = root || !aopProperties.getFlowTrace().isLogRootOnly();
            if (shouldLogError) {
                traceLogger.error("[TRACE-ERROR] {} - Exception: {}", 
                        TraceContext.prettyTrace(), throwable.getMessage());
            }
            throw throwable;
        } finally {
            if (root && aopProperties.getFlowTrace().isLogRootOnly()) {
                traceLogger.info("[TRACE] {}", TraceContext.prettyTrace());
            }
            cleanupTraceContext(root);
        }
    }

    private void cleanupTraceContext(boolean root) {
        try {
            TraceContext.pop();
            if (root) {
                TraceContext.clear();
            }
        } catch (Exception e) {
            traceLogger.warn("Cleanup trace context error: {}", e.getMessage());
        }
    }
}
