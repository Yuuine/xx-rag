# 潇潇 RAG

潇潇 RAG 是一个基于检索增强生成（Retrieval-Augmented Generation）技术的智能问答系统，能够处理多种格式的文档并提供基于文档内容的智能问答功能。

## 功能特性

- **多格式文档支持**：支持 PDF、Word、Excel、Markdown、TXT 等多种文档格式
- **智能文档处理**：自动将文档分割成块（chunk），提取文本内容
- **向量检索**：使用 Elasticsearch 作为向量数据库，支持混合检索（BM25 + kNN）
- **智能问答**：集成 LLM 模型，提供基于文档内容的智能问答
- **会话管理**：支持多轮对话，保持上下文关联，会话持久化到本地存储
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
- **AI/LLM**：LangChain4j 1.9.1, DeepSeek API, DashScope SDK
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
4. **应用模块 (app)**：协调各组件，提供统一 API 和业务逻辑

## 项目结构

```
xx-rag/
├── src/main/java/yuuine/xxrag/     # 后端源码
│   ├── app/                         # 应用模块
│   ├── ingestion/                   # 文档接入模块
│   ├── vector/                      # 向量模块
│   ├── inference/                   # 推理模块
│   ├── websocket/                   # WebSocket 处理
│   ├── config/                      # 配置类
│   └── exception/                   # 异常处理
├── src/main/resources/              # 配置文件
├── frontend/                        # 前端项目
│   ├── src/
│   │   ├── components/              # Vue 组件
│   │   │   ├── icons/               # SVG 图标组件
│   │   │   ├── ChatPanel.vue        # 聊天面板
│   │   │   └── DocsPanel.vue        # 文档面板
│   │   ├── pages/                   # 页面组件
│   │   ├── api/                     # API 接口
│   │   ├── router/                  # 路由配置
│   │   └── styles/                  # 样式文件
│   └── package.json
├── docs/                            # 文档
├── docker-compose.yml               # Docker 编排
├── deploy.sh                        # 部署脚本
└── init-es-index.sh                 # ES 索引初始化脚本
```

## 配置文件

在 `src/main/resources/application.yml` 中配置：

**核心配置项**
```yaml
rag:
  prompt:
    knowledge-system-prompt:      # 自定义知识库提示词
    ordinary-system-prompt:       # 自定义普通对话提示词

app:
  rag:
    retrieval:
      hybrid-enabled: true        # 是否启用混合检索
      recall-top-k: 5             # 检索时返回的 Top-K 结果数量
      candidate-multiplier: 10    # kNN 候选集倍数
      rrf:
        k: 60                     # 平滑常数
        text-weight: 1.0          # 文本检索（BM25）权重
        vector-weight: 1.0        # 向量检索（kNN）权重
        final-top-k: 10           # 最终返回的 Top-K 结果数量
  text-chunker:
    chunk-size: 512               # 文本分块大小
    overlap: 100                  # 文本分块重叠大小
  chat:
    history:
      flush-threshold: 10           # 历史记录缓存阈值
      session-expiry-minutes: 30    # 会话过期时间
      persistence-enabled: true     # 是否启用会话持久化
      max-history-messages: 10      # 每次返回给模型的最大历史消息数
      max-history-echo-messages: 20  # UI 回显最大历史消息数
```

## 环境要求

- Java 17
- Maven 3.6+
- Node.js 18+ (前端开发)
- MySQL 8.0+
- Elasticsearch 9.1.4
- RabbitMQ 3.x

## 本地开发

### 后端启动

```bash
# 1. 配置数据库和 ES 连接信息
# 编辑 src/main/resources/application.yml

# 2. 启动 MySQL、Elasticsearch、RabbitMQ

# 3. 初始化 Elasticsearch 索引
./init-es-index.sh

# 4. 运行应用
./mvnw spring-boot:run
# 或
mvn spring-boot:run
```

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
>注意：`.env`的换行符必须使用 LF（Unix）换行符，否则会读取失败

运行以下命令进行一键部署：

```bash
# 给脚本添加执行权限
chmod +x deploy.sh

# 一键部署应用
./deploy.sh deploy
```

特别说明：
1. ES 容器的版本是 9.1.4，需要下载对应版本的 IK 分词器，否则无法一键部署
2. ES 9+ 默认开启安全，所有节点通信、客户端访问默认加密 + 认证，`docker-compose.yml` 中关闭了 ES 的安全设置，如有需要可开启
```yaml
      - xpack.security.enabled=false
      - xpack.security.http.ssl.enabled=false
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
- `my-rabbitmq`：RabbitMQ 消息队列容器

各个服务的启动参数可根据实际情况调整

## API 接口

完整 API 说明请参阅：[API 文档](./docs/api.md)

提供了简单的静态页面，可在项目启动后访问

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

**rag.rag_sessions** - 会话表
```mysql
CREATE TABLE IF NOT EXISTS rag_sessions
(
    session_id VARCHAR(64) PRIMARY KEY,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_updated_at (updated_at)
);
```

**rag.rag_chat_history** - 对话记录表
```mysql
CREATE TABLE IF NOT EXISTS rag_chat_history
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id VARCHAR(64)                          NOT NULL,
    role       ENUM ('user', 'assistant', 'system') NOT NULL,
    content    TEXT                                 NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_session_created (session_id, created_at)
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

## 前端特性

### Apple 风格设计
- 采用 Apple 官网设计语言，简洁、优雅、现代
- 精心设计的配色方案、阴影效果和过渡动画
- 响应式布局，适配不同屏幕尺寸

### 核心功能
- **实时聊天**：WebSocket 流式响应，支持打字机效果
- **消息持久化**：聊天记录保存到 localStorage，刷新不丢失
- **时间戳显示**：可切换显示/隐藏消息时间戳
- **推理耗时**：显示每次 AI 回复的推理时间
- **文档管理**：模态框形式展示文档列表，支持分页
- **拖拽上传**：支持拖拽文件到上传区域
- **模态框交互**：所有弹窗具备遮罩层，点击外部自动关闭

### 组件架构
- **ChatPanel**：聊天主面板，包含消息列表和输入区域
- **DocsPanel**：文档管理模态框，支持文件列表和分页
- **Icon 组件库**：可复用的 SVG 图标组件

## 致谢

- 分布式向量搜索引擎 [Elasticsearch](https://www.elastic.co/elasticsearch/)
- 阿里云百炼大模型平台（Embedding 服务） [DashScope](https://dashscope.aliyun.com/)
- 语言模型服务（LLM） [DeepSeek](https://www.deepseek.com/)
- IK 分词器 [IK Analyzer](https://github.com/medcl/elasticsearch-analysis-ik)
- 通用文档检测与提取库 [Apache Tika](https://github.com/apache/tika)
- PDF 文档处理库 [Apache PDFBox](https://github.com/apache/pdfbox)
- Microsoft 文档处理库 [Apache POI](https://github.com/apache/poi)
- RabbitMQ 消息队列服务 [RabbitMQ](https://www.rabbitmq.com/)
- Vue.js 渐进式 JavaScript 框架 [Vue.js](https://vuejs.org/)
- Vite 下一代前端构建工具 [Vite](https://vitejs.dev/)
