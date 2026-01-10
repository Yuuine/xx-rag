package yuuine.xxrag.app.application.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import yuuine.xxrag.app.application.service.DocService;
import yuuine.xxrag.app.application.dto.response.DocList;
import yuuine.xxrag.dto.common.Result;

import java.util.List;

/**
 * 文档管理服务组件
 * 负责文档的增删查等管理操作
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DocumentManagementService {

    private final DocService docService;

    /**
     * 获取文档列表
     */
    public Result<Object> getDocList() {
        DocList docList = docService.getDoc();
        return Result.success(docList);
    }

    /**
     * 删除文档
     */
    public Result<Object> deleteDocuments(List<String> fileMd5s) {
        if (fileMd5s == null || fileMd5s.isEmpty()) {
            return Result.error("fileMd5 列表不能为空");
        }
        docService.deleteDocuments(fileMd5s);
        return Result.success();
    }
}