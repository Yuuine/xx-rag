package yuuine.xxrag.dto.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.modulith.NamedInterface;

@Data
@NoArgsConstructor
@AllArgsConstructor
@NamedInterface("StreamResult")
public class StreamResult<T> {

    private String type;    // delta / done / error
    private String message; // 错误信息
    private T data;         // 数据

    public static <T> StreamResult<T> delta(T data) {
        return new StreamResult<>("delta", null, data);
    }

    public static <T> StreamResult<T> done() {
        return new StreamResult<>("done", null, null);
    }

    public static <T> StreamResult<T> error(String message) {
        return new StreamResult<>("error", message, null);
    }
}
