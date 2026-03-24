from fastapi import FastAPI, HTTPException
import uvicorn

from config import SERVICE_MODEL_DIR, HOST, PORT, DEFAULT_TOP_K
from schemas import RerankRequest, RerankResponse
from model_loader import load_model
from service import process_rerank


app = FastAPI(title="Rerank Service", version="1.0.0")


@app.get("/health")
async def health_check():
    return {"status": "healthy", "model_path": SERVICE_MODEL_DIR}


@app.post("/rerank", response_model=RerankResponse)
async def rerank(request: RerankRequest):
    try:
        tokenizer, model = load_model()
        
        query = request.query
        documents = request.documents
        top_k = min(request.top_k if request.top_k is not None else DEFAULT_TOP_K, len(documents))
        
        results = process_rerank(tokenizer, model, query, documents, top_k)
        
        return RerankResponse(results=results)
    
    except Exception as e:
        print(f"Error in rerank: {e}")
        raise HTTPException(status_code=500, detail=str(e))


if __name__ == "__main__":
    uvicorn.run(app, host=HOST, port=PORT)
