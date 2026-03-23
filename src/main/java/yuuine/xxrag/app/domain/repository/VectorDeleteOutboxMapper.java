package yuuine.xxrag.app.domain.repository;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import yuuine.xxrag.app.domain.model.VectorDeleteOutboxEvent;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface VectorDeleteOutboxMapper {

    int insert(VectorDeleteOutboxEvent event);

    VectorDeleteOutboxEvent findById(@Param("id") Long id);

    int markPublished(@Param("id") Long id, @Param("updatedAt") LocalDateTime updatedAt);

    int markSuccess(@Param("id") Long id, @Param("updatedAt") LocalDateTime updatedAt);

    int markFailed(
            @Param("id") Long id,
            @Param("retryCount") Integer retryCount,
            @Param("errorMessage") String errorMessage,
            @Param("nextRetryAt") LocalDateTime nextRetryAt,
            @Param("updatedAt") LocalDateTime updatedAt
    );

    List<VectorDeleteOutboxEvent> findRetryableEvents(@Param("now") LocalDateTime now, @Param("limit") int limit);
}

