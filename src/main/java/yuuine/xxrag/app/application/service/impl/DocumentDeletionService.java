package yuuine.xxrag.app.application.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yuuine.xxrag.app.application.service.RagVectorService;
import yuuine.xxrag.app.domain.repository.DocMapper;
import yuuine.xxrag.exception.BusinessException;

import java.util.List;

/**
 * 文档删除服务组件
 * 负责安全地删除文档及其关联数据
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentDeletionService {

    private final DocMapper docMapper;
    private final RagVectorService ragVectorService;

    /**
     * 安全删除文档 - 先删除数据库记录，再删除向量数据
     * 这样即使向量删除失败，也不会丢失文档元数据
     */
    @Transactional
    public void deleteDocuments(List<String> fileMd5s) {
        log.info("开始批量删除文档，数量: {}", fileMd5s.size());

        // 首先删除 MySQL 数据库中的文档记录
        int deletedCount = docMapper.batchDeleteByFileMd5(fileMd5s);
        log.info("MySQL 删除文档 {} 条", deletedCount);

        // 然后删除向量库中的对应数据
        try {
            log.debug("准备删除向量库中的文件: {}", fileMd5s);
            ragVectorService.deleteChunksByFileMd5s(fileMd5s);
            log.info("向量库 chunks 删除完成，文件数量: {}", fileMd5s.size());
        } catch (Exception e) {
            log.error("向量库删除失败，但数据库记录已删除", e);
            throw new BusinessException("向量库删除失败，但数据库记录已删除，请检查向量库状态", e);
        }
    }

    /**
     * 清理孤立的向量数据（当数据库记录已删除但向量数据仍存在的场景）
     */
    public void cleanupOrphanedVectorData(List<String> fileMd5s) {
        log.info("清理孤立的向量数据，文件数量: {}", fileMd5s.size());
        
        try {
            ragVectorService.deleteChunksByFileMd5s(fileMd5s);
            log.info("孤立向量数据清理完成");
        } catch (Exception e) {
            log.error("清理孤立向量数据失败", e);
        }
    }
}