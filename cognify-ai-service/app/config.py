"""
Configuration for local AI service
"""
from pydantic_settings import BaseSettings
from functools import lru_cache

class Settings(BaseSettings):
    # Service
    SERVICE_PORT: int = 8000
    CORS_ALLOWED_ORIGINS: str = "http://localhost:8082,http://localhost:8080, http://localhost:3000,http://localhost:5173"
    
    # Qdrant (Local Vector DB)
    QDRANT_HOST: str = "localhost"
    QDRANT_PORT: int = 6333
    QDRANT_COLLECTION: str = "cognify_vectors"
    
    # Ollama (Local LLM)
    OLLAMA_URL: str = "http://localhost:11434"
    OLLAMA_MODEL: str = "llama3.2:latest"  # Pulled automatically via docker-compose
    # Timeout settings: first request loads model into memory (can take 30-60s)
    # Subsequent requests can still take 60-120s depending on query complexity and system load
    LLM_TIMEOUT_FIRST_REQUEST: int = 300  # 5 minutes for first load
    LLM_TIMEOUT_SUBSEQUENT: int = 180  # 3 minutes for normal requests (was too aggressive at 60s)
    LLM_REQUEST_RETRIES: int = 2  # Number of retries on timeout
    LLM_RETRY_DELAY: float = 2.0  # Initial delay in seconds (exponential backoff)
    
    # Embeddings (Local Model)
    EMBEDDING_MODEL: str = "sentence-transformers/all-mpnet-base-v2"
    VECTOR_DIMENSION: int = 768
    EMBEDDING_LOCAL_FILES_ONLY: bool = False
    EMBEDDING_CACHE_DIR: str | None = None
    
    # Processing
    CHUNK_SIZE: int = 750
    CHUNK_OVERLAP: int = 120
    MIN_CHUNK_CHAR_LENGTH: int = 50
    # Retrieval gating (tunable via .env)
    MIN_RETRIEVAL_SCORE: float = 0.60
    REQUIRE_EVIDENCE_KEYWORDS: bool = False
    MIN_RETRIEVAL_SCORE_FALLBACK: float = 0.45
    # When retrieving more candidates before filtering, multiply requested top_k
    SEARCH_POOL_MULTIPLIER: int = 3
    # Larger pool when the query implies chronological / in-document order (e.g. generations, timeline)
    SEQUENTIAL_SEARCH_POOL_MULTIPLIER: int = 5

    # LLM context budgeting (keeps Ollama within practical context limits)
    LLM_MAX_CONTEXT_CHARS: int = 10000
    LLM_DISPLAY_SNIPPET_CHARS: int = 280
    LLM_MAX_TOKENS_DEFAULT: int = 1200
    LLM_MAX_TOKENS_SUMMARY: int = 450
    LLM_MAX_TOKENS_DETAILED: int = 1600
    
    # Storage
    STORAGE_BASE_PATH: str = "./uploads"
    
    # Logging
    LOG_LEVEL: str = "INFO"
    
    # pydantic v2 style settings
    model_config = {
        "env_file": ".env",
        "extra": "ignore",
    }

@lru_cache()
def get_settings() -> Settings:
    return Settings()
