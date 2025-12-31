package yuuine.xxrag.inference.dto.request;

import lombok.Data;


@Data
public class InferenceRequest {

    private String query;

    private Integer topK;

}