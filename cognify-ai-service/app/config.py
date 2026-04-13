"""
Configuration for local AI service
"""
from pydantic_settings import BaseSettings
from functools import lru_cache

class Settings(BaseSettings):
    # Service
    SERVICE_PORT: int = 8000
    CORS_ALLOWED_ORIGINS: str = "http://localhost:8082,http://localhost:8080"
    
    # Qdrant (Local Vector DB)
    QDRANT_HOST: str = "localhost"
    QDRANT_PORT: int = 6333
    QDRANT_COLLECTION: str = "cognify_vectors"
    
    # Ollama (Local LLM)
    OLLAMA_URL: str = "http://localhost:11434"
    OLLAMA_MODEL: str = "llama3.2"  # Pulled automatically via docker-compose
    
    # Embeddings (Local Model)
    EMBEDDING_MODEL: str = "sentence-transformers/all-mpnet-base-v2"
    VECTOR_DIMENSION: int = 768
    
    # Processing
    CHUNK_SIZE: int = 500
    CHUNK_OVERLAP: int = 50
    
    # Storage
    STORAGE_BASE_PATH: str = "C:/cognify-uploads"
    
    # Logging
    LOG_LEVEL: str = "INFO"
    
    class Config:
        env_file = ".env"

@lru_cache()
def get_settings() -> Settings:
    return Settings()
