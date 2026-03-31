"""
Cognify Local AI Service - Privacy-First implementation
NO EXTERNAL API CALLS
"""
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from contextlib import asynccontextmanager
import logging

from app.config import get_settings
from app.api.routes import router
from app.services.embedding_service import EmbeddingService
from app.services.vector_store import VectorStore

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
    logger.info("Starting Cognify Local AI Service (Privacy-First Edition)")
    logger.info(f"Embeddings: {settings.EMBEDDING_MODEL} ({settings.VECTOR_DIMENSION} dim)")
    logger.info(f"Vector DB: Qdrant at {settings.QDRANT_HOST}:{settings.QDRANT_PORT}")
    logger.info(f"LLM: {settings.OLLAMA_MODEL} via {settings.OLLAMA_URL}")
    
    try:
        # Load embedding model immediately when starting
        embedding_service = EmbeddingService(settings.EMBEDDING_MODEL)
        await embedding_service.health_check()
        logger.info("Embedding model loaded")
        
        # Initialize Vector Store index
        vector_store = VectorStore(settings.QDRANT_HOST, settings.QDRANT_PORT, settings.QDRANT_COLLECTION, settings.VECTOR_DIMENSION)
        await vector_store.health_check()
        logger.info("Qdrant collection setup done.")
        
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

# CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"], # Should be locked down to spring boot URL
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
