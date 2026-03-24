package yuuine.xxrag.app.application.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yuuine.xxrag.app.application.service.VectorDeleteOutboxService;
import yuuine.xxrag.app.domain.repository.DocMapper;

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
    private final VectorDeleteOutboxService outboxService;

    /**
     * 安全删除文档 - 删除数据库记录并写入 outbox 事件
     * 向量删除由异步消费者执行，保证最终一致性
     */
    @Transactional
    public void deleteDocuments(List<String> fileMd5s) {
        log.info("开始批量删除文档，数量: {}", fileMd5s.size());

        // 1) 删除 MySQL 元数据
        int deletedCount = docMapper.batchDeleteByFileMd5(fileMd5s);
        log.info("MySQL 删除文档 {} 条", deletedCount);

        // 2) 记录 outbox 事件并在事务提交后投递消息
        Long outboxEventId = outboxService.saveDeleteEvent(fileMd5s);
        outboxService.publishAfterCommit(outboxEventId);
        log.info("已写入并准备发布 vector 删除 outbox 事件, eventId={}", outboxEventId);
    }

    /**
     * 清理孤立的向量数据（当数据库记录已删除但向量数据仍存在的场景）
     */
    public void cleanupOrphanedVectorData(List<String> fileMd5s) {
        log.info("触发孤立向量清理任务，文件数量: {}", fileMd5s.size());
        Long outboxEventId = outboxService.saveDeleteEvent(fileMd5s);
        outboxService.publish(outboxEventId);
        log.info("已发布孤立向量清理 outbox 事件, eventId={}", outboxEventId);
    }
}