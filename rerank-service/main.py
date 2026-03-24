from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from typing import List, Tuple
import os
import sys

app = FastAPI(title="Rerank Service", version="1.0.0")

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SERVICE_MODEL_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "models", "bce-reranker-base_v1")
MODEL_NAME = "maidalun1020/bce-reranker-base_v1"

tokenizer = None
model = None


class RerankRequest(BaseModel):
    query: str
    documents: List[str]
    top_k: int = 5


class RerankResult(BaseModel):
    index: int
    document: str
    score: float


class RerankResponse(BaseModel):
    results: List[RerankResult]


def load_model():
    global tokenizer, model
    if tokenizer is None or model is None:
        try:
            from transformers import AutoTokenizer, AutoModelForSequenceClassification
            import torch
            from huggingface_hub import snapshot_download

            if not os.path.exists(SERVICE_MODEL_DIR):
                print(f"Model not found locally, downloading from HuggingFace...")
                os.makedirs(os.path.dirname(SERVICE_MODEL_DIR), exist_ok=True)
                snapshot_download(
                    repo_id=MODEL_NAME,
                    local_dir=SERVICE_MODEL_DIR,
                    local_dir_use_symlinks=False
                )

            print(f"Loading rerank model from: {SERVICE_MODEL_DIR}")
            tokenizer = AutoTokenizer.from_pretrained(SERVICE_MODEL_DIR)
            model = AutoModelForSequenceClassification.from_pretrained(SERVICE_MODEL_DIR)
            model.eval()
            print("Rerank model loaded successfully!")
        except Exception as e:
            print(f"Error loading model: {e}")
            raise


@app.get("/health")
async def health_check():
    return {"status": "healthy", "model_path": SERVICE_MODEL_DIR}


@app.post("/rerank", response_model=RerankResponse)
async def rerank(request: RerankRequest):
    try:
        load_model()

        query = request.query
        documents = request.documents
        top_k = min(request.top_k, len(documents))

        print(f"[Rerank] 收到请求 | query: {query[:50]}{'...' if len(query) > 50 else ''} | documents数量: {len(documents)} | top_k: {top_k}")

        if not documents:
            print("[Rerank] 文档列表为空，返回空结果")
            return RerankResponse(results=[])

        # 准备输入
        pairs = [[query, doc] for doc in documents]

        # 推理
        import torch
        with torch.no_grad():
            inputs = tokenizer(
                pairs,
                padding=True,
                truncation=True,
                return_tensors="pt",
                max_length=512
            )
            outputs = model(**inputs)
            scores = outputs.logits.squeeze(-1).float().numpy()

        # 构建结果并排序
        results = []
        for idx, (doc, score) in enumerate(zip(documents, scores)):
            doc_preview = doc[:80].replace('\n', ' ')
            print(f"[Rerank] 索引: {idx} | 分数: {score:.4f} | 内容: {doc_preview}{'...' if len(doc) > 80 else ''}")
            results.append(RerankResult(index=idx, document=doc, score=float(score)))

        # 按分数降序排序
        results.sort(key=lambda x: x.score, reverse=True)

        # 返回 top_k
        results = results[:top_k]
        print(f"[Rerank] 排序完成，返回 {len(results)} 个结果")

        return RerankResponse(results=results)

    except Exception as e:
        print(f"Error in rerank: {e}")
        raise HTTPException(status_code=500, detail=str(e))


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8082)
