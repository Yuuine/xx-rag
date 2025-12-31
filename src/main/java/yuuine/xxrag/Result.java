package yuuine.xxrag;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
//实体类，统一响应结果
public class Result<T> {
    private Integer code;//响应码
    private String message;//响应信息
    private T data;//响应数据

    public static <E> Result<E> success(E data) {

        return new Result<>(0, "success", data);
    }

    public static Result<Object> success() {

        return new Result<>(0, "success", null);
    }

    public static Result<Object> error(String message) {

        return new Result<>(1, message, null);
    }

    public static Result<Object> error(int code, String message) {

        return new Result<>(code, message, null);
    }

}