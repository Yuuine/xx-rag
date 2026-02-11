# XX-RAG API 文档

## 目录

- [RAG 控制器](#rag-控制器)
  - [文件上传 `/xx/upload`](#文件上传)
  - [获取文档列表 `/xx/getDoc`](#获取文档列表)
  - [删除文档 `/xx/delete`](#删除文档)
  - [删除会话 `/xx/deleteSession`](#删除会话)
  - [删除会话（按时间） `/xx/deleteSessionBefore`](#删除会话按时间)
  - [删除所有会话（管理员） `/xx/deleteAllSessions`](#删除所有会话管理员)
- [WebSocket 服务](#websocket-服务)
  - [WebSocket 连接 `/ws-chat`](#websocket-连接)
- [静态资源](#静态资源)
  - [首页 `/index.html`](#首页)
  - [文档列表页 `/docs.html`](#文档列表页)


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

[
  "md5checksum1",
  "md5checksum2"
]

响应

| 状态码 | 类型             | 描述   |
|-----|----------------|------|
| 200 | Result<Object> | 删除结果 |

---

### 删除会话

#### POST `/xx/deleteSession`

删除单个会话（通过业务会话 ID）。

请求参数（application/json）：

| 参数名       |     类型 | 必填 | 描述          |
|-----------|-------:|---:|-------------|
| sessionId | String |  是 | 要删除的业务会话 ID |

请求示例：

{
  "sessionId": "<businessSessionId>"
}

响应

| 状态码 | 类型             | 描述   |
|-----|----------------|------|
| 200 | Result<Object> | 操作结果 |

---

### 删除会话（按时间）

#### POST `/xx/deleteSessionBefore`

删除某一会话在指定时间点之前的历史记录。`beforeDate` 必须是 ISO_LOCAL_DATE_TIME 格式（例如：`2023-09-01T12:00:00`）。

请求参数（application/json）：

| 参数名        |     类型 | 必填 | 描述                                   |
|------------|-------:|---:|--------------------------------------|
| sessionId  | String |  是 | 目标业务会话 ID                            |
| beforeDate | String |  否 | ISO_LOCAL_DATE_TIME 格式的时间字符串，若为空则不删除 |

注意：如果无法解析 `beforeDate`，服务会返回错误：`无法解析 beforeDate，使用 ISO_LOCAL_DATE_TIME 格式`。

请求示例：

{
  "sessionId": "abcd1234...",
  "beforeDate": "2026-01-01T00:00:00"
}

响应

| 状态码 | 类型             | 描述        |
|-----|----------------|-----------|
| 200 | Result<Object> | 操作结果或错误信息 |

---

### 删除所有会话（管理员）

#### POST `/xx/deleteAllSessions`

危险操作：删除所有会话。只有在配置 `AdminProperties.cleanupPassword`（配置项名 `cleanup-password`）存在并且请求中提供正确密码时才会执行。

请求参数（application/json）：

| 参数名      |     类型 | 必填 | 描述               |
|----------|-------:|---:|------------------|
| password | String |  是 | 管理员清理密码（与服务配置比较） |

行为与错误情况：
- 若服务未配置清理密码，返回 `服务器未启用此操作`。
- 若提供的密码不正确，返回 `密码错误，拒绝执行`。
- 通过密码验证后会执行删除并返回成功信息。

请求示例：

{
  "password": "<admin-password>"
}

响应

| 状态码 | 类型             | 描述   |
|-----|----------------|------|
| 200 | Result<Object> | 操作结果 |


---

## WebSocket 服务

### WebSocket 连接

#### 端点

- URI: `/ws-chat`
- 绑定类：`yuuine.xxrag.websocket.RagWebSocketHandler`（使用 `@ServerEndpoint("/ws-chat")`）

##### 连接方式

通过 WebSocket 协议连接到 `/ws-chat`：

示例（浏览器）：

const ws = new WebSocket("ws://<host>:<port>/ws-chat?sid=<optional-uuid-or-32hex>");

可选查询参数 `sid`：
- 支持带短横线的 UUID（8-4-4-4-12）或不带短横线的 32 个十六进制字符；
- 服务端会尝试规范化并验证该 UUID；若合法则使用（并存储无短横线形式），否则生成新的会话 ID；
- 在浏览器中通常通过 localStorage 保存 sid，使同一浏览器多个标签页共享业务会话。

##### 服务器在 `onOpen` 时会：
- 为连接生成或获取业务会话 ID；
- 从 `ChatSessionService` 获取最近若干条历史（默认上限从 service 获取或 20）并主动推送给客户端；

##### 历史消息格式（服务器主动推送）

类型：JSON 对象，示例：

{
  "type": "history",
  "messages": [
    { "role": "user", "content": "用户之前的问题" },
    { "role": "assistant", "content": "之前的回答片段" }
  ]
}

字段说明：
- `type`: 字符串，值为 `history` 表示这是历史消息批量回显；
- `messages`: 数组，元素为 `{role, content}`，role 与 `InferenceRequest.Message` 对应。

##### 客户端发送消息

- 当前实现：客户端直接发送纯文本消息（作为用户输入）。
- 服务器在 `onMessage` 时会将文本加入会话历史并调用 `appApi.streamSearch(message, session.getId())` 来执行流式检索。

##### 服务器发送（流式）消息格式

服务器使用 `StreamResponse` 对象流式发送给客户端，JSON 示例：

每次推送的普通片段：

{
  "content": "这是部分回复文本",
  "finishReason": null,
  "message": null
}

最后一条（结束）片段：

{
  "content": "",
  "finishReason": "stop",
  "message": null
}

错误情况示例：

{
  "content": "",
  "finishReason": null,
  "message": "错误：描述信息"
}

字段说明：
- `content`: String，每次流式推送的文本片段；结束时可为空字符串；
- `finishReason`: String，正常结束时值为 `"stop"`，中间片段为 null；
- `message`: String，发生错误或异常时包含错误描述，正常为 null。

##### 心跳

有一个定时任务会每 60 秒广播一个心跳字符串：

{"type":"heartbeat"}

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

{
  "code": 1,
  "message": "错误描述",
  "data": null
}


## 静态资源

- 首页: `/index.html`（位于 resources/static/index.html）
- 文档页: `/docs.html`（位于 resources/static/docs.html）


---
