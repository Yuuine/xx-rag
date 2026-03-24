package yuuine.xxrag.aop.tracing;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.stream.Collectors;

public final class TraceContext {

    private static final ThreadLocal<Deque<TraceNode>> STACK =
            ThreadLocal.withInitial(ArrayDeque::new);

    private TraceContext() {
    }

    public static void push(TraceNode node) {
        STACK.get().push(node);
    }

    public static void pop() {
        STACK.get().pop();
    }

    public static boolean isEmpty() {
        return STACK.get().isEmpty();
    }

    public static String prettyTrace() {
        return STACK.get().stream()
                .map(TraceNode::toString)
                .collect(Collectors.joining(" -> "));
    }

    public static void clear() {
        STACK.remove();
    }
}
