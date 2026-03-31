"""
LOCAL embedding generation using sentence-transformers
NO EXTERNAL API CALLS
"""
import logging
from typing import List
from sentence_transformers import SentenceTransformer
import numpy as np

logger = logging.getLogger(__name__)

class EmbeddingService:
    def __init__(self, model_name: str = "all-MiniLM-L6-v2"):
        """
        Load local sentence-transformers model
        """
        logger.info(f"Loading embedding model: {model_name}")
        self.model = SentenceTransformer(model_name)
        self.dimensions = self.model.get_sentence_embedding_dimension()
        logger.info(f"Model loaded! Dimensions: {self.dimensions}")
    
    async def generate_embedding(self, text: str) -> List[float]:
        try:
            embedding = self.model.encode(text, convert_to_numpy=True)
            return embedding.tolist()
        except Exception as e:
            logger.error(f"Embedding generation error: {e}")
            raise
    
    async def generate_embeddings_batch(self, texts: List[str]) -> List[List[float]]:
        try:
            logger.info(f"Generating embeddings for {len(texts)} texts (LOCAL)")
            embeddings = self.model.encode(texts, convert_to_numpy=True, show_progress_bar=False)
            logger.info(f"Generated {len(embeddings)} embeddings")
            return embeddings.tolist()
        except Exception as e:
            logger.error(f"Batch embedding error: {e}")
            raise
    
    async def health_check(self) -> bool:
        try:
            await self.generate_embedding("test")
            return True
        except Exception as e:
            logger.error(f"Health check failed: {e}")
            return False
