import os
import threading
from typing import Tuple, Optional
from transformers import AutoTokenizer, AutoModelForSequenceClassification
from huggingface_hub import snapshot_download

from config import SERVICE_MODEL_DIR, MODEL_NAME


_tokenizer: Optional[AutoTokenizer] = None
_model: Optional[AutoModelForSequenceClassification] = None
_load_lock = threading.Lock()
_initialized = False


def download_model() -> None:
    if not os.path.exists(SERVICE_MODEL_DIR):
        print(f"Model not found locally, downloading from HuggingFace...")
        os.makedirs(os.path.dirname(SERVICE_MODEL_DIR), exist_ok=True)
        snapshot_download(
            repo_id=MODEL_NAME,
            local_dir=SERVICE_MODEL_DIR,
            local_dir_use_symlinks=False
        )


def load_model() -> Tuple[AutoTokenizer, AutoModelForSequenceClassification]:
    global _tokenizer, _model, _initialized
    
    if _initialized:
        return _tokenizer, _model
    
    with _load_lock:
        if _initialized:
            return _tokenizer, _model
        
        try:
            download_model()
            
            print(f"Loading rerank model from: {SERVICE_MODEL_DIR}")
            _tokenizer = AutoTokenizer.from_pretrained(SERVICE_MODEL_DIR)
            _model = AutoModelForSequenceClassification.from_pretrained(SERVICE_MODEL_DIR)
            _model.eval()
            _initialized = True
            print("Rerank model loaded successfully!")
        except Exception as e:
            print(f"Error loading model: {e}")
            raise
    
    return _tokenizer, _model


def get_model() -> Tuple[Optional[AutoTokenizer], Optional[AutoModelForSequenceClassification]]:
    return _tokenizer, _model
