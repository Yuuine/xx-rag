# XX-RAG API 文档

## 目录

- [RAG 控制器](#rag-控制器)
  - [文件上传 `/xx/upload`](#文件上传)
  - [获取文档列表 `/xx/getDoc`](#获取文档列表)
  - [删除文档 `/xx/delete`](#删除文档)
- [WebSocket 服务](#websocket-服务)
  - [WebSocket 连接 `/ws-chat`](#websocket-连接)
- [静态资源](#静态资源)
  - [首页 `/index.html`](#首页)
  - [文档列表页 `/docs.html`](#文档列表页)

## RAG 控制器

基础路径: `/xx`

### 文件上传

#### POST `/xx/upload`

上传文件到RAG知识库中。

##### 请求参数

| 参数名   | 类型                  | 必填 | 描述       |
|-------|---------------------|----|----------|
| files | List<MultipartFile> | 是  | 要上传的文件列表 |

##### 请求示例

```http
POST /xx/upload
Content-Type: multipart/form-data
```

##### 响应

| 状态码 | 类型             | 描述   |
|-----|----------------|------|
| 200 | Result<Object> | 上传结果 |

### 获取文档列表

#### GET `/xx/getDoc`

获取已上传文档的列表。

##### 请求示例

```http
GET /xx/getDoc
```

##### 响应

| 状态码 | 类型             | 描述     |
|-----|----------------|--------|
| 200 | Result<Object> | 文档列表结果 |

### 删除文档

#### POST `/xx/delete`

从RAG知识库中删除指定的文件。

##### 请求参数

| 参数名      | 类型           | 必填 | 描述           |
|----------|--------------|----|--------------|
| fileMd5s | List<String> | 是  | 要删除的文件MD5值列表 |

##### 请求示例

```http
POST /xx/delete
Content-Type: application/json

[
  "md5checksum1",
  "md5checksum2"
]
```

##### 响应

| 状态码 | 类型             | 描述   |
|-----|----------------|------|
| 200 | Result<Object> | 删除结果 |

## WebSocket 服务

### WebSocket 连接

#### WebSocket `/ws-chat`

WebSocket端点，用于处理流式AI对话。

##### 连接方式

通过WebSocket协议连接到 `/ws-chat` 端点。

##### 客户端发送消息

- 发送纯文本消息作为用户查询

##### 服务器发送消息

服务器发送JSON格式的消息，包含以下字段：

| 字段名          | 类型     | 描述                            |
|--------------|--------|-------------------------------|
| content      | String | 流式返回的AI回复内容，为空字符串表示消息结束       |
| finishReason | String | 结束原因，如"stop"表示正常结束，null表示中间内容 |
| message      | String | 错误信息，正常情况下为null，错误时包含错误描述     |

##### 消息示例

开始流式响应：
```json
{
  "content": "人工智能是",
  "finishReason": null,
  "message": null
}
```

结束流式响应：
```json
{
  "content": "",
  "finishReason": "stop",
  "message": null
}
```

错误响应：
```json
{
  "content": "",
  "finishReason": null,
  "message": "错误描述信息"
}
```

## 通用响应格式

所有API端点都返回统一的响应格式：

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

其中：

- `code`: 响应状态码，0表示成功，非0表示失败
- `message`: 响应消息
- `data`: 响应数据，具体结构取决于API端点