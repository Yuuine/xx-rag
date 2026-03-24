package yuuine.xxrag.common.util;

import java.util.UUID;

public class UUIDUtil {

    private UUIDUtil() {
        throw new UnsupportedOperationException("工具类不能实例化");
    }

    public static String uuidGenerate() {
        UUID uuid = UUID.randomUUID();
        long most = uuid.getMostSignificantBits();
        long least = uuid.getLeastSignificantBits();
        return Long.toHexString(most) + Long.toHexString(least);
    }
}
