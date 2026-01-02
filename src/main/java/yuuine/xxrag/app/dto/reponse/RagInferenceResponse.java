package yuuine.xxrag.app.dto.reponse;

import lombok.Data;

import java.util.List;


@Data
public class RagInferenceResponse {
    private String answer;          // LLM 生成的最终答案
    private String query;           // 原用户查询问题
    private List<Reference> references;  // 引用的知识来源

    @Data
    public static class Reference {
        private String chunkId;
        private String source;      // 文件名
        private Integer chunkIndex;
        private String content;     // 引用的 chunk 片段
        private Float score;        // 相似度分数
    }
}
