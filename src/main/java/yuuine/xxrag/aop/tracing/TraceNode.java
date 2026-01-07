package yuuine.xxrag.aop.tracing;

import org.jetbrains.annotations.NotNull;

public record TraceNode(
        String module,
        Layer layer,
        String className,
        String methodName
) {

    @NotNull
    @Override
    public String toString() {
        return "[" + module + ":" + layer + "] "
                + className + "." + methodName;
    }
}
