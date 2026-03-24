package yuuine.xxrag.common.util;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

public final class ValidationUtils {

    private ValidationUtils() {
    }

    public static boolean isNullOrEmpty(String str) {
        return str == null || str.isEmpty();
    }

    public static boolean isNullOrEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    public static boolean isNullOrEmpty(Map<?, ?> map) {
        return map == null || map.isEmpty();
    }

    public static boolean isNullOrEmpty(Object[] array) {
        return array == null || array.length == 0;
    }

    public static <T> T requireNonNull(T obj, String message) {
        if (obj == null) {
            throw new IllegalArgumentException(message);
        }
        return obj;
    }

    public static String requireNonEmpty(String str, String message) {
        if (isNullOrEmpty(str)) {
            throw new IllegalArgumentException(message);
        }
        return str;
    }

    public static <T> T getOrDefault(T value, T defaultValue) {
        return value != null ? value : defaultValue;
    }

    public static <T> Optional<T> optional(T value) {
        return Optional.ofNullable(value);
    }
}
