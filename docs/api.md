# XX-RAG API 文档

## 目录

- [RAG 控制器](#rag-控制器)
  - [文件上传 `/xx/upload`](#文件上传)
  - [获取文档列表 `/xx/getDoc`](#获取文档列表)
  - [删除文档 `/xx/delete`](#删除文档)
  - [提问请求 `/xx/search`](#提问请求)

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

### 提问请求

#### POST `/xx/search`

用户提问，经过处理后基于知识库回答。

##### 请求参数

| 参数名   | 类型                  | 必填 | 描述     |
|-------|---------------------|----|--------|
| query | VectorSearchRequest | 是  | 搜索请求对象 |

##### VectorSearchRequest 结构

| 字段名   | 类型      | 描述      |
|-------|---------|---------|
| query | String  | 搜索查询字符串 |
| topK  | Integer | 返回结果数量  |

##### 请求示例

```http
POST /xx/search
Content-Type: application/json

{
  "query": "什么是大语言模型？",
  "topK": 5
}
```

##### 响应

| 状态码 | 类型             | 描述   |
|-----|----------------|------|
| 200 | Result<Object> | 搜索结果 |

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

- `code`: 响应状态码
- `message`: 响应消息
- `data`: 响应数据，具体结构取决于API端点