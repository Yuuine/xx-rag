# 潇潇 RAG

潇潇 RAG 是一个基于检索增强生成（Retrieval-Augmented Generation）技术的智能问答系统。 是一个基于检索增强生成（Retrieval-Augmented Generation）技术的智能问答系统，能够处理多种格式的文档并提供基于文档内容的智能问答功能。

## 功能特性

- **多格式文档支持**：支持 PDF、Word、文本等多种文档格式
- **智能文档处理**：自动将文档分割成块（chunk），提取文本内容
- **向量检索**：使用 Elasticsearch 作为向量数据库，支持混合检索
- **智能问答**：集成 LLM 模型，提供基于文档内容的智能问答
- **文件管理**：支持文档上传、删除、列表查看等功能

## 技术栈

- **后端框架**：Spring Boot 4.0.1
- **数据库**：MySQL
- **向量数据库**：Elasticsearch
- **文档解析**：Apache Tika, Apache POI, Tess4J (OCR)
- **AI/LLM**：LangChain4j, DeepSeek API, DashScope SDK
- **ORM**：MyBatis
- **微服务通信**：Spring Cloud OpenFeign
- **异步处理**：Spring WebFlux

## 系统架构

Spring multipart 模块化设计

1. **文档接入服务 (RagIngestService)**：负责文档上传、解析、分割
2. **向量服务 (RagVectorService)**：负责向量化处理、存储和检索
3. **推理服务 (RagInferenceService)**：负责与 LLM 交互，生成回答
4. **文档管理服务 (DocService)**：负责文档元数据管理
5. **应用服务 (AppService)**：协调各组件，提供统一 API

## 环境要求

- Java 17
- Maven 3.6+
- MySQL 8.0+
- Elasticsearch 9.0+
- OCR 引擎 (Tesseract)


## 配置文件

在 `src/main/resources/application.yml` 中配置：

```yaml
rag:
  prompt:
    system-prompt:                # 自定义提示词

app:
  rag:
    retrieval:
      hybrid-enabled: true        # 是否启用混合检索
      recall-top-k: 5             # 检索时返回的 Top-K 结果数量
      candidate-multiplier: 10    # kNN 候选集倍数
      rrf:
        k: 60                     # 平滑常数
        text-weight: 1.0          # 文本检索（BM25）结果的权重
        vector-weight: 1.0        # 向量检索（kNN）结果的权重
        final-top-k: 10           # 最终返回的 Top-K 结果数量
  text-chunker:
    chunk-size: 512               # 文本分块大小
    overlap: 100                  # 文本分块重叠大小
```

## API 接口

完整 API 说明请参阅：[API 文档](./docs/api.md)

## 数据库设计

```mysql
CREATE TABLE rag_documents (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    file_md5 VARCHAR(64) NOT NULL UNIQUE,
    file_name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

