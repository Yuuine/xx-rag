# Rerank Service

基于 BCEmbedding 的重排序微服务，用于优化 RAG 检索结果。

## 环境要求

- Python 3.10+
- pip

## 快速启动

### Windows

双击运行 `start.bat` 或在命令行中执行：

```cmd
start.bat
```

### Linux/macOS

```bash
chmod +x start.sh
./start.sh
```

## 手动启动

```bash
# 创建虚拟环境（推荐）
python -m venv venv
source venv/bin/activate  # Linux/macOS
# 或
venv\Scripts\activate  # Windows

# 安装依赖
pip install -r requirements.txt

# 启动服务
python main.py
```

## 模型说明

服务使用 [BCEmbedding](https://huggingface.co/maidalun1020/bce-reranker-base_v1) 的 `bce-reranker-base_v1` 模型：

- **首次运行**：模型会自动从 HuggingFace 下载到 `models/bce-reranker-base_v1/` 目录（约1GB）
- **后续运行**：直接使用本地缓存的模型

### 手动下载模型

如果网络访问 HuggingFace 较慢，可以手动下载：

```bash
# 使用 huggingface-cli
pip install huggingface-hub
huggingface-cli download maidalun1020/bce-reranker-base_v1 --local-dir models/bce-reranker-base_v1
```

或使用镜像站：

```bash
export HF_ENDPOINT=https://hf-mirror.com
python -c "from huggingface_hub import snapshot_download; snapshot_download('maidalun1020/bce-reranker-base_v1', local_dir='models/bce-reranker-base_v1')"
```

## API 接口

服务默认运行在 `http://localhost:8082`

### 健康检查

```bash
curl http://localhost:8082/health
```

响应：
```json
{
  "status": "healthy",
  "model_path": "/path/to/models/bce-reranker-base_v1"
}
```

### 重排序接口

```bash
curl -X POST http://localhost:8082/rerank \
  -H "Content-Type: application/json" \
  -d '{
    "query": "什么是机器学习？",
    "documents": [
      "机器学习是人工智能的一个分支...",
      "深度学习是机器学习的子领域...",
      "自然语言处理是..."
    ],
    "top_k": 2
  }'
```

响应：
```json
{
  "results": [
    {
      "index": 0,
      "document": "机器学习是人工智能的一个分支...",
      "score": 0.85
    },
    {
      "index": 1,
      "document": "深度学习是机器学习的子领域...",
      "score": 0.72
    }
  ]
}
```

## 配置说明

| 参数 | 默认值 | 说明 |
|------|--------|------|
| `host` | 0.0.0.0 | 监听地址 |
| `port` | 8082 | 监听端口 |
| `model_name` | maidalun1020/bce-reranker-base_v1 | HuggingFace 模型名称 |
| `model_dir` | models/bce-reranker-base_v1 | 本地模型存储目录 |

## 故障排除

### 模型下载失败

1. 检查网络连接
2. 使用 HuggingFace 镜像：`export HF_ENDPOINT=https://hf-mirror.com`
3. 手动下载模型文件

### 内存不足

模型加载需要约 1.5GB 内存，确保系统有足够可用内存。

### 端口被占用

修改 `main.py` 最后一行的端口号：
```python
uvicorn.run(app, host="0.0.0.0", port=8083)
```
