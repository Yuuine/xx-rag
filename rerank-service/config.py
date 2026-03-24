import os


BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SERVICE_DIR = os.path.dirname(os.path.abspath(__file__))
SERVICE_MODEL_DIR = os.path.join(SERVICE_DIR, "models", "bce-reranker-base_v1")
MODEL_NAME = "maidalun1020/bce-reranker-base_v1"
MAX_SEQ_LENGTH = 512
DEFAULT_TOP_K = 5
HOST = "0.0.0.0"
PORT = 8082
