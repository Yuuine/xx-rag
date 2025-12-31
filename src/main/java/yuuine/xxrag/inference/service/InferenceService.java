package yuuine.xxrag.inference.service;


import yuuine.xxrag.dto.request.InferenceRequest;
import yuuine.xxrag.inference.dto.response.InferenceResponse;

public interface InferenceService {

    InferenceResponse infer(InferenceRequest request);

}
