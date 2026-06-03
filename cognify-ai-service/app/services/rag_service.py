"""
RAG orchestration 
"""
import logging
from typing import List, Dict
from app.services.embedding_service import EmbeddingService
from app.services.vector_store import VectorStore
from app.services.llm_service import LLMService, LLMUnavailableError
from app.config import get_settings
import re

logger = logging.getLogger(__name__)

# Queries that benefit from reading-order (page/chunk index) rather than pure similarity rank.
_READING_ORDER_QUERY = re.compile(
    r"\b("
    r"generations?|history|timeline|chronolog|evolution|phases?|stages?|eras?|"
    r"versions?|chapters?|sections?|steps?|sequence|progression|development of|"
    r"over time|from .+ to|first|second|third|fourth|fifth|all\b.+\bof\b"
    r")\b",
    re.IGNORECASE,
)


class RAGService:
    def __init__(self, embedding_service: EmbeddingService, vector_store: VectorStore, llm_service: LLMService):
        self.embedding_service = embedding_service
        self.vector_store = vector_store
        self.llm_service = llm_service
    
    async def search(self, query: str, user_id: str, top_k: int = 5) -> List[Dict]:
        logger.info(f"RAG search for user: {user_id}")
        settings = get_settings()

        query_embedding = await self.embedding_service.generate_embedding(query)

        # Request a larger candidate pool so filtering can still return top_k results
        pool_multiplier = getattr(settings, "SEARCH_POOL_MULTIPLIER", 3)
        if self._prefers_reading_order(query):
            pool_multiplier = max(
                pool_multiplier,
                getattr(settings, "SEQUENTIAL_SEARCH_POOL_MULTIPLIER", 5),
            )
        pool_k = max(top_k * pool_multiplier, top_k)
        results = await self.vector_store.search(query_embedding, user_id, int(pool_k))

        # Filter by strict retrieval threshold first
        threshold = getattr(settings, "MIN_RETRIEVAL_SCORE", 0.6)
        filtered = [r for r in results if (r.get('score') or 0) >= threshold]

        # Remove very short/generic chunks that tend to dominate similarity
        min_len = getattr(settings, "MIN_CHUNK_CHAR_LENGTH", 50)
        if min_len:
            filtered = [
                r for r in filtered
                if len((r.get("metadata", {}).get("content") or "").strip()) >= min_len
            ]

        # Optionally enforce that query keywords appear in the evidence chunks
        if getattr(settings, "REQUIRE_EVIDENCE_KEYWORDS", False) and filtered:
            # Basic keyword extraction (drop short/common words)
            stopwords = {
                'the','and','for','with','that','this','from','what','when','where','which','have','into','there',
                'which','would','could','should','about','your','their','they','them','were','been','has','but'
            }
            words = [w for w in re.findall(r"\w+", query.lower()) if len(w) > 3 and w not in stopwords]
            if words:
                def has_keyword(res):
                    content = (res.get('metadata', {}).get('content') or '').lower()
                    return any(w in content for w in words)

                filtered = [r for r in filtered if has_keyword(r)]

        # If filtering removed everything, relax to fallback threshold
        if not filtered:
            fallback = getattr(settings, "MIN_RETRIEVAL_SCORE_FALLBACK", None)
            if fallback is not None:
                filtered = [r for r in results if (r.get('score') or 0) >= fallback]
                if min_len:
                    filtered = [
                        r for r in filtered
                        if len((r.get("metadata", {}).get("content") or "").strip()) >= min_len
                    ]

        filtered = self._order_results(filtered, query)
        return filtered[:top_k]

    @staticmethod
    def _prefers_reading_order(query: str) -> bool:
        if not query:
            return False
        return bool(_READING_ORDER_QUERY.search(query))

    @staticmethod
    def _reading_order_key(result: Dict) -> tuple:
        meta = result.get("metadata") or {}
        page = meta.get("page_number")
        chunk_index = meta.get("chunk_index")
        return (
            str(meta.get("document_id") or ""),
            int(page) if page is not None else 0,
            int(chunk_index) if chunk_index is not None else 0,
        )

    def _order_results(self, results: List[Dict], query: str) -> List[Dict]:
        if not results:
            return results

        if self._prefers_reading_order(query):
            ordered = sorted(results, key=self._reading_order_key)
            logger.info("Reordered %d retrieval hits by document reading order", len(ordered))
            return ordered

        results.sort(key=lambda x: x.get("score", 0), reverse=True)
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
            # Distinguish between timeout (model loading) and unavailable service
            if e.is_timeout:
                logger.warning("LLM timeout (%s). Returning fallback response.", e.reason)
                fallback_message = (
                    "⏳ The AI model is currently loading or taking longer than expected. "
                    "The retrieval system is working, but the AI answer generation timed out. "
                    "Here's what we found in your documents:"
                )
            else:
                logger.warning("LLM unavailable (%s). Returning fallback response.", e.reason)
                fallback_message = (
                    "❌ The local AI service is unavailable. "
                    "Here's what we found in your documents (without AI reformulation):"
                )
            
            return {
                "response": self.llm_service.compose_structured_response(
                    self.llm_service._extract_source_chunks(context),
                    fallback_message
                ),
                "model": "fallback-context",
                "error_reason": e.reason,
                "usage": {
                    "prompt_tokens": 0,
                    "completion_tokens": 0,
                    "total_tokens": 0,
                },
            }
