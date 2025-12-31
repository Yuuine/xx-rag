package yuuine.xxrag.ingestion.utils;

import java.util.UUID;

public class UUIDUtil {

    private UUIDUtil() {
        throw new UnsupportedOperationException("工具类不能实例化");
    }

    /**
     * 生成小写不带横线的32位UUID
     *
     * @return 32位小写UUID字符串
     */
    public static String UUIDGenerate() {
        UUID uuid = UUID.randomUUID();
        long most = uuid.getMostSignificantBits();
        long least = uuid.getLeastSignificantBits();
        return Long.toHexString(most) + Long.toHexString(least);
    }
}