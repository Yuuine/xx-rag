package yuuine.xxrag.common.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.DigestUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

@Slf4j
public class Md5Util {

    private Md5Util() {
        throw new UnsupportedOperationException("工具类不能实例化");
    }

    public static String computeMd5(byte[] fileBytes) {
        InputStream inputStream = new ByteArrayInputStream(fileBytes);
        return computeMd5(inputStream);
    }

    public static String computeMd5(InputStream inputStream) {
        String computeMd5;
        try {
            computeMd5 = DigestUtils.md5DigestAsHex(inputStream);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return computeMd5;
    }
}
