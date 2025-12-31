package yuuine.xxrag.app;

import lombok.Data;


@Data
public class InferenceRequest {

    private String query;

    private Integer topK = 5;

}