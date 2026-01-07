package yuuine.xxrag.aop.tracing;

public final class ModuleResolver {

    private static final String ROOT_PACKAGE = "xxrag";

    private ModuleResolver() {
    }

    public static String resolve(Class<?> type) {
        String pkg = type.getPackageName();

        // xxrag.ingestion.domain.service.Xxx
        if (!pkg.startsWith(ROOT_PACKAGE + ".")) {
            return "external";
        }

        String[] parts = pkg.split("\\.");

        // parts[0] = xxrag
        return parts.length > 1 ? parts[1] : "root";
    }
}
