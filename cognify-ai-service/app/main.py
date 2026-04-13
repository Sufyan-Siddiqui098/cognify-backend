"""
Cognify Local AI Service - Privacy-First implementation
NO EXTERNAL API CALLS
"""
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from contextlib import asynccontextmanager
import logging

from app.config import get_settings
from app.api.routes import router, initialize_runtime_services

# Setup logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

@asynccontextmanager
async def lifespan(app: FastAPI):
    """Startup checks"""
    settings = get_settings()
    logging.getLogger().setLevel(getattr(logging, settings.LOG_LEVEL.upper(), logging.INFO))
    logging.getLogger("httpx").setLevel(logging.WARNING)
    logging.getLogger("sentence_transformers").setLevel(logging.WARNING)
    logging.getLogger("transformers").setLevel(logging.WARNING)
    logging.getLogger("huggingface_hub").setLevel(logging.WARNING)

    logger.info("Starting Cognify Local AI Service (Privacy-First Edition)")
    logger.info(f"Embeddings: {settings.EMBEDDING_MODEL}")
    logger.info(f"Vector DB: Qdrant at {settings.QDRANT_HOST}:{settings.QDRANT_PORT}")
    logger.info(f"LLM: {settings.OLLAMA_MODEL} via {settings.OLLAMA_URL}")
    
    try:
        embedding_service, vector_store, _ = initialize_runtime_services()
        await embedding_service.health_check()
        logger.info(f"Embedding model loaded ({embedding_service.dimensions} dim)")
        
        qdrant_ok = await vector_store.health_check()
        if qdrant_ok:
            logger.info("Qdrant collection setup done.")
        else:
            logger.warning("Qdrant is not reachable yet. AI service will stay up and retry on requests.")
        
    except Exception as e:
        logger.error(f"Initialization failed: {e}")
        # We don't raise here strictly to prevent boot loops if qdrant is slow to start, but better handling could be added.
    
    yield
    logger.info("Shutting down...")

app = FastAPI(
    title="Cognify Local AI Service",
    description="Privacy-First RAG System with local parsing, Qdrant, and Ollama",
    version="1.0",
    lifespan=lifespan
)

# Load settings once at module import for middleware configuration.
settings = get_settings()

# Restrict CORS to trusted backend origins from environment.
allowed_origins = [origin.strip() for origin in settings.CORS_ALLOWED_ORIGINS.split(",") if origin.strip()]

# CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=allowed_origins,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(router, prefix="/api/v1")

@app.get("/health")
async def health_check():
    return {
        "status": "healthy",
        "service": "cognify-local-ai",
        "version": "1.0"
    }
