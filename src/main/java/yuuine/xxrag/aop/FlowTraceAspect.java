package yuuine.xxrag.aop;

import lombok.RequiredArgsConstructor;
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

@Aspect
@Component
@RequiredArgsConstructor
public class FlowTraceAspect {

    private final AopProperties aopProperties;

    private static final org.slf4j.Logger traceLogger =
        org.slf4j.LoggerFactory.getLogger("TRACING_LOGGER");

    @Pointcut("""
                execution(* yuuine.xxrag..*(..))
                && !within(yuuine.xxrag.aop.tracing..*)
                && !within(yuuine.xxrag.aop..*)
                && !within(yuuine.xxrag.websocket..*)
            """)
    public void applicationFlow() {
    }

    @Around("applicationFlow()")
    public Object trace(ProceedingJoinPoint pjp) throws Throwable {

        // 检查是否启用了流程追踪
        if (!aopProperties.isFlowTraceEnabled()) {
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

            // 避免在高频率调用时产生过多日志，只在根节点打印
            if (root) {
                traceLogger.info("[TRACE] {}", TraceContext.prettyTrace());
            }

            return pjp.proceed();

        } catch (Throwable throwable) {
            // 发生异常时也记录追踪信息
            if (root) {
                traceLogger.error("[TRACE-ERROR] {} - Exception occurred: {}", TraceContext.prettyTrace(), throwable.getMessage());
            }
            throw throwable;
        } finally {
            // 确保在任何情况下都弹出节点
            try {
                TraceContext.pop();
                if (root) {
                    // 只有在根节点结束时才清空上下文
                    TraceContext.clear();
                }
            } catch (Exception e) {
                // 防止在finally块中的异常干扰主流程
                traceLogger.warn("Error occurred while cleaning up trace context: {}", e.getMessage());
            }
        }
    }
}