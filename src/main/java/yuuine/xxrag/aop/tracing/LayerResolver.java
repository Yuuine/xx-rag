package yuuine.xxrag.aop.tracing;

public final class LayerResolver {

    private LayerResolver() {
    }

    public static Layer resolve(Class<?> type) {
        String pkg = type.getPackageName();

        if (pkg.contains(".controller.")) {
            return Layer.CONTROLLER;
        }
        if (pkg.contains(".api.")) {
            return Layer.API;
        }
        if (pkg.contains(".application.")) {
            return Layer.APPLICATION;
        }
        if (pkg.contains(".domain.")) {
            return Layer.DOMAIN;
        }
        if (pkg.contains(".infrastructure.")) {
            return Layer.INFRASTRUCTURE;
        }
        if (pkg.contains(".config.")) {
            return Layer.CONFIG;
        }
        return Layer.UNKNOWN;
    }
}

