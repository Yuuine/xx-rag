# 潇潇 RAG

潇潇 RAG 是一个基于检索增强生成（Retrieval-Augmented Generation）技术的智能问答系统，能够处理多种格式的文档并提供基于文档内容的智能问答功能。

## 功能特性

- **多格式文档支持**：支持 PDF、Word、Excel、Markdown、TXT 等多种文档格式
- **智能文档处理**：自动将文档分割成块（chunk），提取文本内容
- **向量检索**：使用 Elasticsearch 作为向量数据库，支持混合检索（BM25 + kNN）
- **Rerank 重排序**：集成 BCE Reranker 模型，对检索结果进行重排序优化
- **智能问答**：集成 DeepSeek LLM 模型，提供基于文档内容的智能问答
- **对话历史管理**：全局内存缓存对话历史，自动持久化到本地文件
- **文件管理**：支持文档上传（拖拽上传）、删除、列表查看等功能
- **Apple 风格 UI**：采用 Apple 官网风格设计语言，简洁优雅的交互体验
- **实时通信**：基于 WebSocket 实现流式响应，低延迟交互

## 技术栈

### 后端

- **框架**：Spring Boot 4.0.1
- **语言**：Java 17
- **数据库**：MySQL 8.0+
- **向量数据库**：Elasticsearch 9.1.4
- **文档解析**：Apache Tika 3.2.2, Apache POI 5.4.0, Apache PDFBox
- **AI/LLM**：DeepSeek API, DashScope SDK (Embedding)
- **Rerank**：BCE Reranker Base v1 (Python FastAPI 微服务)
- **ORM**：MyBatis Spring Boot Starter 4.0.0
- **流式处理**：Spring WebFlux
- **实时通信**：WebSocket (Spring Boot Starter WebSocket)
- **消息队列**：RabbitMQ (Spring AMQP)
- **架构**：Spring Modulith 模块化设计

### 前端

- **框架**：Vue 3.5.30 (Composition API)
- **语言**：TypeScript 5.9.3
- **构建工具**：Vite 8.0.1
- **路由**：Vue Router 4.6.4
- **状态管理**：Pinia 3.0.4
- **HTTP 客户端**：Axios 1.13.6
- **Markdown 渲染**：Marked 17.0.5
- **样式**：原生 CSS + Apple 设计系统

## 系统架构

采用 Spring Modulith 模块化设计：

1. **文档接入模块 (ingestion)**：负责文档上传、解析、分块
2. **向量模块 (vector)**：负责向量化处理、存储和检索
3. **推理模块 (inference)**：负责与 LLM 交互，生成回答
4. **重排序模块 (rerank)**：负责对检索结果进行重排序优化
5. **应用模块 (app)**：协调各组件，提供统一 API 和业务逻辑

## RAG 检索链路

```
用户查询
    ↓
意图判断（闲聊/知识查询）
    ↓
向量检索（BM25 + kNN 混合检索）
    ↓
Rerank 重排序（BCE Reranker）
    ↓
Prompt 构建（历史消息 + 检索上下文）
    ↓
DeepSeek LLM 推理
    ↓
流式响应返回（WebSocket）
```

## 项目结构

```
xx-rag/
├── src/main/java/yuuine/xxrag/     # 后端源码
│   ├── app/                         # 应用模块
│   ├── ingestion/                   # 文档接入模块
│   ├── vector/                      # 向量模块
│   ├── inference/                   # 推理模块
│   ├── rerank/                      # 重排序模块
│   ├── websocket/                   # WebSocket 处理
│   ├── config/                      # 配置类
│   └── exception/                   # 异常处理
├── src/main/resources/              # 配置文件
├── frontend/                        # 前端项目
│   ├── src/
│   │   ├── components/              # Vue 组件
│   │   ├── composables/             # 组合式函数
│   │   ├── api/                     # API 接口
│   │   └── constants/               # 常量定义
│   └── package.json
├── rerank-service/                  # Python Rerank 微服务
│   ├── main.py                      # FastAPI 入口
│   ├── service.py                   # Rerank 服务
│   └── config.py                    # 配置
├── docs/                            # 文档
├── data/                            # 数据目录
│   └── chat_history.json            # 对话历史持久化
├── docker-compose.yml               # Docker 编排
├── deploy.sh                        # 部署脚本
└── init-es-index.sh                 # ES 索引初始化脚本
```

## 配置文件

在 `src/main/resources/application.yml` 中配置：

**核心配置项**
```yaml
deepseek:
  api-key: ${DEEPSEEK_API_KEY:}      # DeepSeek API Key

app:
  rag:
    retrieval:
      hybrid-enabled: true           # 是否启用混合检索
      recall-top-k: 10               # 检索时返回的 Top-K 结果数量
      candidate-multiplier: 10       # kNN 候选集倍数
      rrf:
        k: 60                        # 平滑常数
        text-weight: 1.0             # 文本检索（BM25）权重
        vector-weight: 1.0           # 向量检索（kNN）权重
        final-top-k: 10              # 最终返回的 Top-K 结果数量
      rerank:
        enabled: true                # 是否启用 Rerank
        service-url: http://localhost:8082
        top-k: 5                     # Rerank 后返回数量
  text-chunker:
    chunk-size: 512                  # 文本分块大小
    overlap: 100                     # 文本分块重叠大小
  chat:
    history:
      flush-threshold: 20            # 历史记录缓存阈值（20条=10轮）
      persistence-enabled: true      # 是否启用持久化
      history-file-path: "./data/chat_history.json"
```

## 环境要求

- Java 17
- Maven 3.6+
- Node.js 18+ (前端开发)
- MySQL 8.0+
- Elasticsearch 9.1.4
- RabbitMQ 3.x
- Python 3.10+ (Rerank 服务，可选)

## 本地开发

### 后端启动

```bash
# 1. 配置数据库和 ES 连接信息
# 编辑 src/main/resources/application.yml

# 2. 设置环境变量
export DEEPSEEK_API_KEY=your-api-key

# 3. 启动 MySQL、Elasticsearch、RabbitMQ

# 4. 初始化 Elasticsearch 索引
./init-es-index.sh

# 5. 运行应用
mvn spring-boot:run
```

### Rerank 服务启动（可选）

```bash
cd rerank-service

# 安装依赖
pip install -r requirements.txt

# 启动服务
python main.py
# 或 Windows 下
start.bat
```

服务默认运行在 `http://localhost:8082`

### 前端启动

```bash
cd frontend

# 安装依赖
npm install

# 启动开发服务器
npm run dev

# 构建生产版本
npm run build
```

前端开发服务器默认运行在 `http://localhost:5173`

## Docker 部署（Linux）

### 环境准备

安装 Docker 和 Docker Compose

### 一键部署

克隆项目到本地：
```bash
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
> 注意：`.env`的换行符必须使用 LF（Unix）换行符，否则会读取失败

运行以下命令进行一键部署：

```bash
# 给脚本添加执行权限
chmod +x deploy.sh

# 一键部署应用
./deploy.sh deploy
```

### IK 分词器插件下载和创建索引映射

第一次启动时，由于缺少 IK 分词器和没有创建 ES 索引映射，无法启动成功。

处理方法：
1. 在 docker 启动失败后，`Ctrl+C` 退出，`docker compose stop` 停止所有容器

2. 启动 my-es 容器，进入容器内部，使用命令安装 IK 分词器插件
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

### 容器说明

- `xx-rag-app`：主应用容器，运行 Spring Boot 应用
- `my-mysql`：MySQL 数据库容器
- `my-elasticsearch`：Elasticsearch 向量数据库容器
- `my-rabbitmq`：RabbitMQ 消息队列容器

## API 接口

完整 API 说明请参阅：[API 文档](./docs/api.md)

### REST API

| 接口 | 方法 | 说明 |
|-----|------|------|
| `/xx/upload` | POST | 上传文档 |
| `/xx/getDoc` | GET | 获取文档列表 |
| `/xx/delete` | POST | 删除文档 |

### WebSocket

| 端点 | 说明 |
|-----|------|
| `/ws-chat` | 流式对话 |

## 数据库设计

### MySQL

**rag.rag_documents** - 文档表
```mysql
CREATE TABLE rag.rag_documents
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    file_md5   CHAR(32)                           NOT NULL,
    file_name  VARCHAR(255)                       NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP NULL,
    CONSTRAINT file_md5 UNIQUE (file_md5)
);
CREATE INDEX idx_created ON rag.rag_documents (created_at);
```

**rag.rag_outbox_events** - 向量删除事件表（Outbox 模式）
```mysql
CREATE TABLE rag.rag_outbox_events
(
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_type   VARCHAR(50)  NOT NULL,
    payload      TEXT         NOT NULL,
    status       ENUM('NEW', 'PROCESSING', 'COMPLETED', 'FAILED') DEFAULT 'NEW',
    retry_count  INT          DEFAULT 0,
    error_message TEXT,
    next_retry_at DATETIME,
    created_at   DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

### Elasticsearch

**rag_chunks** - 文档块索引
```json
{
  "settings": {
    "number_of_shards": 1,
    "number_of_replicas": 0
  },
  "mappings": {
    "properties": {
      "chunkId": { "type": "keyword" },
      "fileMd5": { "type": "keyword" },
      "source": { "type": "text" },
      "chunkIndex": { "type": "integer" },
      "content": {
        "type": "text",
        "analyzer": "ik_max_word",
        "search_analyzer": "ik_smart"
      },
      "charCount": { "type": "integer" },
      "embedding": {
        "type": "dense_vector",
        "dims": 1024,
        "index": true,
        "similarity": "cosine"
      },
      "embeddingDim": { "type": "integer" },
      "model": { "type": "keyword" },
      "createdAt": { "type": "date" }
    }
  }
}
```

## 对话历史管理

### 存储机制

| 阶段 | 行为 |
|-----|------|
| 应用启动 | 从 `./data/chat_history.json` 加载历史消息到内存 |
| 收到新消息 | 添加到内存缓存，检查是否达到 20 条 |
| 达到 20 条 | 自动持久化到文件，清空内存缓存 |
| 应用关闭 | 持久化所有未保存的消息 |

### 持久化文件格式

```json
{
  "lastUpdated": "2026-03-24T10:30:00",
  "messages": [
    {"role": "user", "content": "消息内容1"},
    {"role": "assistant", "content": "消息内容2"}
  ]
}
```

## 前端特性

### Apple 风格设计
- 采用 Apple 官网设计语言，简洁、优雅、现代
- 精心设计的配色方案、阴影效果和过渡动画
- 响应式布局，适配不同屏幕尺寸

### 核心功能
- **实时聊天**：WebSocket 流式响应，支持打字机效果
- **消息持久化**：聊天记录保存到 localStorage，刷新不丢失
- **文档管理**：模态框形式展示文档列表，支持分页
- **拖拽上传**：支持拖拽文件到上传区域
- **模态框交互**：所有弹窗具备遮罩层，点击外部自动关闭

## 致谢

- 分布式向量搜索引擎 [Elasticsearch](https://www.elastic.co/elasticsearch/)
- 阿里云百炼大模型平台（Embedding 服务） [DashScope](https://dashscope.aliyun.com/)
- 语言模型服务（LLM） [DeepSeek](https://www.deepseek.com/)
- Rerank 模型 [BCE Reranker](https://github.com/netease-youdao/BCEmbedding)
- IK 分词器 [IK Analyzer](https://github.com/medcl/elasticsearch-analysis-ik)
- 通用文档检测与提取库 [Apache Tika](https://github.com/apache/tika)
- PDF 文档处理库 [Apache PDFBox](https://github.com/apache/pdfbox)
- Microsoft 文档处理库 [Apache POI](https://github.com/apache/poi)
- RabbitMQ 消息队列服务 [RabbitMQ](https://www.rabbitmq.com/)
- Vue.js 渐进式 JavaScript 框架 [Vue.js](https://vuejs.org/)
- Vite 下一代前端构建工具 [Vite](https://vitejs.dev/)
