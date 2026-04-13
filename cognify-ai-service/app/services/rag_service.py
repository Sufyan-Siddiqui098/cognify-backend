"""
RAG orchestration 
"""
import logging
from typing import List, Dict
from app.services.embedding_service import EmbeddingService
from app.services.vector_store import VectorStore
from app.services.llm_service import LLMService, LLMUnavailableError

logger = logging.getLogger(__name__)

class RAGService:
    def __init__(self, embedding_service: EmbeddingService, vector_store: VectorStore, llm_service: LLMService):
        self.embedding_service = embedding_service
        self.vector_store = vector_store
        self.llm_service = llm_service
    
    async def search(self, query: str, user_id: str, top_k: int = 5) -> List[Dict]:
        logger.info(f"RAG search for user: {user_id}")
        query_embedding = await self.embedding_service.generate_embedding(query)
        results = await self.vector_store.search(query_embedding, user_id, top_k)
        return results
    
    async def generate(
        self,
        user_message: str,
        context: str,
        chat_history: List[Dict[str, str]],
        temperature: float = 0.7,
        max_tokens: int = 1000
    ) -> Dict:
        logger.info("Generating RAG response (LOCAL)")
        try:
            result = await self.llm_service.generate_response(
                user_message, context, chat_history, temperature, max_tokens
            )
            return result
        except LLMUnavailableError as e:
            logger.warning("LLM unavailable (%s). Returning context-based fallback response.", e)
            return {
                "response": (
                    "I found relevant information in your documents, but the AI summary service is "
                    "temporarily unavailable.\n\n" + context
                ),
                "model": "fallback-context",
                "usage": {
                    "prompt_tokens": 0,
                    "completion_tokens": 0,
                    "total_tokens": 0,
                },
            }
