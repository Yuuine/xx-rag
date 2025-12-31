package yuuine.xxrag.vector.domain.es.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.Instant;

// ES index: rag_chunks 的 mapping
@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(indexName = "rag_chunks")
public class RagChunkDocument {

    @Id
    @Field(type = FieldType.Keyword)
    private String chunkId;

    @Field(type = FieldType.Keyword)
    private String fileMd5;

    @Field(type = FieldType.Text)
    private String source;

    @Field(type = FieldType.Integer)
    private Integer chunkIndex;

    @Field(type = FieldType.Text, analyzer = "ik_max_word")
    private String content;

    @Field(type = FieldType.Integer)
    private Integer charCount;

    @Field(type = FieldType.Dense_Vector, dims = 1024, similarity = "cosine")
    private float[] embedding;

    @Field(type = FieldType.Integer)
    private Integer embeddingDim;

    @Field(type = FieldType.Keyword)
    private String model;

    @Field(type = FieldType.Date, format = DateFormat.date_time)
    private Instant createdAt;
}