import torch
from typing import List
from transformers import AutoTokenizer, AutoModelForSequenceClassification

from schemas import RerankResult
from config import MAX_SEQ_LENGTH


def truncate_text(text: str, max_len: int = 50) -> str:
    if len(text) <= max_len:
        return text
    return text[:max_len] + "..."


def build_pairs(query: str, documents: List[str]) -> List[List[str]]:
    return [[query, doc] for doc in documents]


def compute_scores(
    tokenizer: AutoTokenizer,
    model: AutoModelForSequenceClassification,
    pairs: List[List[str]]
) -> List[float]:
    with torch.no_grad():
        inputs = tokenizer(
            pairs,
            padding=True,
            truncation=True,
            return_tensors="pt",
            max_length=MAX_SEQ_LENGTH
        )
        outputs = model(**inputs)
        scores = outputs.logits.squeeze(-1).float().numpy()
    return [float(score) for score in scores]


def build_results(documents: List[str], scores: List[float]) -> List[RerankResult]:
    results = []
    for idx, (doc, score) in enumerate(zip(documents, scores)):
        doc_preview = doc[:80].replace('\n', ' ')
        print(f"[Rerank] 索引: {idx} | 分数: {score:.4f} | 内容: {doc_preview}{'...' if len(doc) > 80 else ''}")
        results.append(RerankResult(index=idx, document=doc, score=score))
    return results


def sort_and_filter_results(results: List[RerankResult], top_k: int) -> List[RerankResult]:
    results.sort(key=lambda x: x.score, reverse=True)
    return results[:top_k]


def process_rerank(
    tokenizer: AutoTokenizer,
    model: AutoModelForSequenceClassification,
    query: str,
    documents: List[str],
    top_k: int
) -> List[RerankResult]:
    if not documents:
        print("[Rerank] 文档列表为空，返回空结果")
        return []

    query_preview = truncate_text(query, 50)
    print(f"[Rerank] 收到请求 | query: {query_preview} | documents数量: {len(documents)} | top_k: {top_k}")

    pairs = build_pairs(query, documents)
    scores = compute_scores(tokenizer, model, pairs)
    results = build_results(documents, scores)
    filtered_results = sort_and_filter_results(results, top_k)

    print(f"[Rerank] 排序完成，返回 {len(filtered_results)} 个结果")
    return filtered_results
