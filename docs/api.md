# XX-RAG API 文档

## 目录

- [XX-RAG API 文档](#xx-rag-api-文档)
  - [目录](#目录)
  - [RAG 控制器](#rag-控制器)
    - [文件上传](#文件上传)
      - [POST `/xx/upload`](#post-xxupload)
    - [获取文档列表](#获取文档列表)
      - [GET `/xx/getDoc`](#get-xxgetdoc)
    - [删除文档](#删除文档)
      - [POST `/xx/delete`](#post-xxdelete)
  - [WebSocket 服务](#websocket-服务)
    - [WebSocket 连接](#websocket-连接)
      - [端点](#端点)
        - [连接方式](#连接方式)
        - [客户端发送消息](#客户端发送消息)
        - [服务器发送（流式）消息格式](#服务器发送流式消息格式)
        - [心跳](#心跳)
  - [通用响应格式（服务端统一包装）](#通用响应格式服务端统一包装)
  - [静态资源](#静态资源)
  - [Rerank Service (重排序服务)](#rerank-service-重排序服务)
    - [健康检查](#健康检查)
      - [GET `/health`](#get-health)
    - [文档重排序](#文档重排序)
      - [POST `/rerank`](#post-rerank)


## RAG 控制器

基础路径: `/xx`

说明：控制器位于 `yuuine.xxrag.controller.RagController`，主要通过 `AppApi` 提供后端功能。

---

### 文件上传

#### POST `/xx/upload`

上传文件到 RAG 知识库。

请求参数（multipart/form-data）：

| 参数名   |                  类型 | 必填 | 描述       |
|-------|--------------------:|---:|----------|
| files | List<MultipartFile> |  是 | 要上传的文件列表 |

请求示例（概念）：

POST /xx/upload
Content-Type: multipart/form-data

响应

| 状态码 | 类型             | 描述           |
|-----|----------------|--------------|
| 200 | Result<Object> | 上传结果（统一返回包装） |

---

### 获取文档列表

#### GET `/xx/getDoc`

获取已上传文档的列表。

请求示例：

GET /xx/getDoc

响应

| 状态码 | 类型             | 描述                    |
|-----|----------------|-----------------------|
| 200 | Result<Object> | 文档列表结果（data 字段包含具体列表） |

---

### 删除文档

#### POST `/xx/delete`

从 RAG 知识库中删除指定的文件。

请求参数（application/json）：

| 参数名      |           类型 | 必填 | 描述             |
|----------|-------------:|---:|----------------|
| fileMd5s | List<String> |  是 | 要删除的文件 MD5 值列表 |

请求示例：

```json
[
  "md5checksum1",
  "md5checksum2"
]
```

响应

| 状态码 | 类型             | 描述   |
|-----|----------------|------|
| 200 | Result<Object> | 删除结果 |

---

## WebSocket 服务

### WebSocket 连接

#### 端点

- URI: `/ws-chat`
- 绑定类：`yuuine.xxrag.websocket.RagWebSocketHandler`（使用 `@ServerEndpoint("/ws-chat")`）

##### 连接方式

通过 WebSocket 协议连接到 `/ws-chat`：

示例（浏览器）：

```javascript
const ws = new WebSocket("ws://<host>:<port>/ws-chat");
```

##### 客户端发送消息

客户端直接发送纯文本消息（作为用户输入）。

##### 服务器发送（流式）消息格式

服务器使用 `StreamResponse` 对象流式发送给客户端，JSON 示例：

每次推送的普通片段：

```json
{
  "content": "这是部分回复文本",
  "finishReason": null,
  "message": null
}
```

最后一条（结束）片段：

```json
{
  "content": "",
  "finishReason": "stop",
  "message": null
}
```

错误情况示例：

```json
{
  "content": "",
  "finishReason": null,
  "message": "错误：描述信息"
}
```

字段说明：
- `content`: String，每次流式推送的文本片段；结束时可为空字符串；
- `finishReason`: String，正常结束时值为 `"stop"`，中间片段为 null；
- `message`: String，发生错误或异常时包含错误描述，正常为 null。

##### 心跳

有一个定时任务会每 60 秒广播一个心跳字符串：

```json
{"type":"heartbeat"}
```

客户端可根据该心跳实现连接状态检测。

---

## 通用响应格式（服务端统一包装）

服务使用统一的包装对象 `Result<T>`，实际结构如下：

```json
{
  "code": 0,
  "message": "success",
  "data": {...}
}
```

说明：
- `code`: Integer，0 表示成功，非 0 表示失败；
- `message`: String，描述信息；
- `data`: 具体业务数据，类型因接口而异。

示例（发生错误）：

```json
{
  "code": 1,
  "message": "错误描述",
  "data": null
}
```


## 静态资源

- 首页: `/index.html`（位于 resources/static/index.html）
- 文档页: `/docs.html`（位于 resources/static/docs.html）

---

## Rerank Service (重排序服务)

独立 Python 服务，基于 FastAPI 实现，提供文档重排序功能。

### 基本信息

- 基础路径: `/`
- 启动命令: `python main.py` 或 `start.bat`（Windows）
- 默认端口: `8082`
- 模型: BCE Reranker Base v1

配置文件位于 `rerank-service/config.py`：
- `HOST`: 服务地址（默认 `0.0.0.0`）
- `PORT`: 服务端口（默认 `8082`）
- `DEFAULT_TOP_K`: 默认返回数量（默认 `5`）
- `MAX_SEQ_LENGTH`: 最大序列长度（默认 `512`）

---

### 健康检查

#### GET `/health`

检查服务健康状态。

响应示例：

```json
{
  "status": "healthy",
  "model_path": "rerank-service/models/bce-reranker-base_v1"
}
```

---

### 文档重排序

#### POST `/rerank`

对文档列表进行重排序，基于查询相关性。

请求参数（application/json）：

| 参数名       |           类型 | 必填 | 描述                    |
|------------|-------------:|---:|-----------------------|
| query      | String       |  是 | 查询字符串                |
| documents  | List<String> |  是 | 待排序的文档列表            |
| top_k      | Integer      |  否 | 返回的相关文档数量（默认 5）    |

请求示例：

```json
{
  "query": "什么是人工智能",
  "documents": [
    "人工智能是计算机科学的一个分支。",
    "机器学习是人工智能的子领域。",
    "今天天气很好。"
  ],
  "top_k": 2
}
```

响应

```json
{
  "results": [
    {
      "index": 0,
      "document": "人工智能是计算机科学的一个分支。",
      "score": 0.9523
    },
    {
      "index": 1,
      "document": "机器学习是人工智能的子领域。",
      "score": 0.8901
    }
  ]
}
```

响应字段说明：

| 字段       |     类型 | 描述           |
|-----------|-------:|--------------|
| results   | Array  | 重排序后的结果列表  |
| index     | Integer | 原始文档索引     |
| document  | String | 文档内容        |
| score     | Float  | 相关性分数（越高越相关） |

---

## 对话历史管理

### 概述

系统采用全局对话历史管理机制，所有用户共享同一对话历史上下文。

### 存储机制

| 阶段 | 行为 |
|-----|------|
| 应用启动 | 从 `./data/chat_history.json` 加载历史消息到内存 |
| 收到新消息 | 添加到内存缓存，检查是否达到 20 条 |
| 达到 20 条 | 自动持久化到文件，清空内存缓存 |
| 应用关闭 | 持久化所有未保存的消息 |

### 持久化文件

- 位置: `./data/chat_history.json`
- 格式:
```json
{
  "lastUpdated": "2026-03-24T10:30:00",
  "messages": [
    {"role": "user", "content": "消息内容1"},
    {"role": "assistant", "content": "消息内容2"}
  ]
}
```

### 配置项

| 配置项 | 默认值 | 说明 |
|-------|-------|------|
| `app.chat.history.flush-threshold` | 20 | 持久化阈值（消息条数） |
| `app.chat.history.history-file-path` | `./data/chat_history.json` | 持久化文件路径 |

---
