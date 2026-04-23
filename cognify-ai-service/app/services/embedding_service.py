"""
LOCAL embedding generation using sentence-transformers
NO EXTERNAL API CALLS
"""
import logging
import os
from typing import List
from sentence_transformers import SentenceTransformer
import numpy as np

logger = logging.getLogger(__name__)

class EmbeddingService:
    def __init__(
        self,
        model_name: str = "sentence-transformers/all-mpnet-base-v2",
        local_files_only: bool = False,
        cache_folder: str | None = None,
    ):
        """
        Load local sentence-transformers model
        """
        logger.info(
            "Loading embedding model: %s (local_files_only=%s, cache_folder=%s)",
            model_name,
            local_files_only,
            cache_folder,
        )

        if local_files_only:
            # Avoid network checks in strict offline mode.
            os.environ.setdefault("HF_HUB_OFFLINE", "1")
            os.environ.setdefault("TRANSFORMERS_OFFLINE", "1")

        self.model = self._load_model(model_name, local_files_only, cache_folder)
        # sentence-transformers renamed this API; support both to stay compatible.
        if hasattr(self.model, "get_embedding_dimension"):
            self.dimensions = self.model.get_embedding_dimension()
        else:
            self.dimensions = self.model.get_sentence_embedding_dimension()
        logger.info(f"Model loaded! Dimensions: {self.dimensions}")

    def _load_model(self, model_name: str, local_files_only: bool, cache_folder: str | None) -> SentenceTransformer:
        try:
            return SentenceTransformer(
                model_name,
                cache_folder=cache_folder,
                local_files_only=local_files_only,
            )
        except Exception as primary_error:
            if local_files_only:
                raise RuntimeError(
                    "Embedding model is not available in local cache while EMBEDDING_LOCAL_FILES_ONLY=true. "
                    "Download once with internet, or disable strict offline mode."
                ) from primary_error

            logger.warning(
                "Primary embedding model load failed (%s). Retrying with local_files_only=True.",
                primary_error,
            )

            try:
                os.environ.setdefault("HF_HUB_OFFLINE", "1")
                os.environ.setdefault("TRANSFORMERS_OFFLINE", "1")
                return SentenceTransformer(
                    model_name,
                    cache_folder=cache_folder,
                    local_files_only=True,
                )
            except Exception as fallback_error:
                raise RuntimeError(
                    "Failed to load embedding model. Network access is unavailable and no local cache was found. "
                    "Pre-download the model once or configure a local model path/cache directory."
                ) from fallback_error
    
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
