from pydantic import BaseModel
from typing import List


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
