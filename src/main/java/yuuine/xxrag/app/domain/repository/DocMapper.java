package yuuine.xxrag.app.domain.repository;

import org.apache.ibatis.annotations.*;
import yuuine.xxrag.app.domain.model.RagDocuments;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface DocMapper {


    @Insert("insert into rag_documents(" +
            "file_md5, file_name, created_at) " +
            "values(#{fileMd5}, #{fileName}, #{now})")
    void saveDoc(String fileMd5, String fileName, LocalDateTime now);


    @Select("SELECT id, file_md5, file_name, created_at " +
            "FROM rag_documents ORDER BY created_at DESC")
    List<RagDocuments> getDoc();

    int batchDeleteByFileMd5(@Param("list") List<String> fileMd5s);


    @Select("SELECT COUNT(*) FROM rag_documents WHERE file_md5 = #{fileMd5}")
    int countByFileMd5(String fileMd5);

}
