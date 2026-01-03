# 潇潇 RAG

潇潇 RAG 是一个基于检索增强生成（Retrieval-Augmented Generation）技术的智能问答系统，能够处理多种格式的文档并提供基于文档内容的智能问答功能。

## 配置文件

在 `src/main/resources/application.yml` 中配置：

```yaml
rag:
  prompt:
    knowledge-system-prompt:      # 自定义提示词
    ordinary-system-prompt:       # 自定义提示词

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
提供了简单的静态页面，可在项目启动后访问

## Docker 部署（Linux）

### 环境准备

安装 Docker 和 Docker Compose

### 一键部署

克隆项目到本地：
```git
git clone https://github.com/Yuuine/xx-rag.git
```

修改配置文件中的数据库和 api-key 为自己的，或添加 `.env` 环境变量文件到项目根目录下，脚本会自动检测并加载 `.env` 文件中的环境变量

示例：
```text
# .env
MYSQL_ROOT_PASSWORD=
ES_PASSWORD=
EMBEDDING_API_KEY=
DEEPSEEK_API_KEY=
```
>注意：`.env`的换行符必须使用 LF（Unix）换行符，否则会读取失败

```bash

运行以下命令进行一键部署：

```bash
# 给脚本添加执行权限
chmod +x deploy.sh

# 一键部署应用
./deploy.sh deploy
```

特别说明： 
1. es 容器的版本是9.1.4，需要下载对应版本的 ik 分词器，否则无法一键部署
2. es9+ 默认开启安全，所有节点通信、客户端访问默认加密 + 认证，`docker-compose.yml` 中关闭了es的安全设置，如有需要可开启
```yaml
      - xpack.security.enabled=false
      - xpack.security.http.ssl.enabled=false
```
### ik 分词器插件下载和创建索引映射

第一次启动时，由于缺少 ik 分词器和没有创建 es 索引映射，无法启动成功。

处理方法：
1. 在 docker 启动失败后，`Ctrl+C` 退出，`docker compose stop` 停止所有容器

2. 启动 my-es 容器，进入容器内部，使用命令安装 ik 分词器插件
```bash
docker start my-es
docker exec -it my-es /bin/bash
bin/elasticsearch-plugin install https://get.infini.cloud/elasticsearch/analysis-ik/9.1.4
```
3. 重启 my-es 容器，运行 [init-es-index.sh](init-es-index.sh) 脚本创建索引映射
```bash
docker restart my-es
chmod +x init-es-index.sh
./init-es-index.sh
```

在项目主目录运行 `./deploy.sh deploy` 成功启动应用

### 热更新脚本

运行以下命令启动热更新脚本：
```bash
chmod +x hot-update.sh
./hot-update.sh
```

**热更新流程**

1. 检查 `main` 分支是否有代码更新
2. 如果有更新，拉取最新的代码
3. 构建新的应用镜像
4. 重启 `app` 服务容器(使用新镜像)
5. 等待应用端口就绪
6. 显示更新后的服务状态

**注意事项**

- 热更新只适用于应用代码的更新，如果修改了 Dockerfile 或其他基础配置，需要重新部署
- 热更新过程中，仅重启 `app` 服务容器，数据库和 Elasticsearch 服务不会重启
- 脚本会自动备份当前镜像为 `xx-rag-app:backup`，在新版本出现问题时回滚

### 容器说明

- `xx-rag-app`：主应用容器，运行 Spring Boot 应用
- `my-mysql`：MySQL 数据库容器
- `my-elasticsearch`：Elasticsearch 向量数据库容器

各个服务的启动参数可根据实际情况调整

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
- **文档解析**：Apache Tika, Apache POI, Apache PDFBox
- **AI/LLM**：LangChain4j, DeepSeek API, DashScope SDK
- **ORM**：MyBatis
- **微服务通信**：Spring Cloud OpenFeign
- **异步处理**：Spring WebFlux

## 系统架构

Spring modulith 模块化设计

1. **文档接入服务 (RagIngestService)**：负责文档上传、解析、分割
2. **向量服务 (RagVectorService)**：负责向量化处理、存储和检索
3. **推理服务 (RagInferenceService)**：负责与 LLM 交互，生成回答
4. **文档管理服务 (DocService)**：负责文档元数据管理
5. **应用服务 (AppService)**：协调各组件，提供统一 API

## 环境要求

- Java 17
- Maven 3.6+
- MySQL 8.0+
- Elasticsearch 9.1.4

## 数据库设计

MySQL

rag.rag_documents
```mysql
create table rag.rag_documents
(
    id         bigint auto_increment
        primary key,
    file_md5   char(32)                           not null,
    file_name  varchar(255)                       not null,
    created_at datetime default CURRENT_TIMESTAMP null,
    constraint file_md5
        unique (file_md5)
);

create index idx_created
    on rag.rag_documents (created_at);
```

Elasticsearch

rag_chunks
```json
{
  "settings": {
    "number_of_shards": 1,
    "number_of_replicas": 0
  },
  "mappings": {
    "properties": {
      "chunkId": {
        "type": "keyword"
      },
      "fileMd5": {
        "type": "keyword"
      },
      "source": {
        "type": "text"
      },
      "chunkIndex": {
        "type": "integer"
      },
      "content": {
        "type": "text",
        "analyzer": "ik_max_word",
        "search_analyzer": "ik_smart"
      },
      "charCount": {
        "type": "integer"
      },
      "embedding": {
        "type": "dense_vector",
        "dims": 1024,
        "index": true,
        "similarity": "cosine"
      },
      "embeddingDim": {
        "type": "integer"
      },
      "model": {
        "type": "keyword"
      },
      "createdAt": {
        "type": "date"
      }
    }
  }
}
```

## 致谢

- 分布式向量搜索引擎 [Elasticsearch](https://www.elastic.co/elasticsearch/)
- 阿里云百炼大模型平台（Embedding 服务） [DashScope](https://dashscope.aliyun.com/)
- 语言模型服务（LLM） [DeepSeek](https://www.deepseek.com/)
- ik 分词器 [IK Analyzer](https://github.com/medcl/elasticsearch-analysis-ik)
- 通用文档检测与提取库 [Apache Tika](https://github.com/apache/tika)
- PDF文档处理库 [Apache PDFBox](https://github.com/apache/pdfbox)
- Microsoft 文档处理库 [Apache POI](https://github.com/apache/poi)
