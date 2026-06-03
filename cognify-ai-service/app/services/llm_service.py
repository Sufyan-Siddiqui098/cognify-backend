"""
LOCAL LLM using Ollama
"""
import logging
import re
import time
from typing import List, Dict, Optional

import requests

from app.config import get_settings
from app.services.text_sanitizer import sanitize_indexed_text

logger = logging.getLogger(__name__)


class LLMUnavailableError(Exception):
    """Raised when Ollama is unreachable or times out."""
    def __init__(self, reason: str = "OLLAMA_UNAVAILABLE", is_timeout: bool = False):
        self.reason = reason
        self.is_timeout = is_timeout
        super().__init__(reason)


class LLMService:
    def __init__(
        self, 
        ollama_url: str = "http://localhost:11434", 
        model: str = "llama3.2",
        timeout_first_request: int = 300,
        timeout_subsequent: int = 60,
        request_retries: int = 2,
        retry_delay: float = 2.0,
    ):
        self.ollama_url = ollama_url
        self.model = model
        self.timeout_first_request = timeout_first_request
        self.timeout_subsequent = timeout_subsequent
        self.request_retries = request_retries
        self.retry_delay = retry_delay
        self._first_request = True  # Flag to detect first request (model loading)
        logger.info(f"LLM Service initialized: {model} at {ollama_url}")
        logger.info(f"Timeout settings: first_request={timeout_first_request}s, subsequent={timeout_subsequent}s")

    def health_check(self) -> bool:
        """Verify Ollama is accessible and the model is available."""
        try:
            response = requests.get(f"{self.ollama_url}/api/tags", timeout=5)
            if response.status_code != 200:
                logger.error(f"Ollama health check failed: {response.status_code}")
                return False
            
            models = response.json().get("models", [])
            model_names = [m.get("name", "") for m in models]
            
            if self.model not in model_names:
                base_name = self.model.split(":")[0]
                matched = next(
                    (name for name in model_names if name == base_name or name.split(":")[0] == base_name),
                    None,
                )
                if matched:
                    logger.warning(
                        "Model %s not found in Ollama. Using available model %s.",
                        self.model,
                        matched,
                    )
                    self.model = matched
                else:
                    logger.warning(f"Model {self.model} not found in Ollama. Available: {model_names}")
                    return False
            
            logger.info(f"✓ Ollama is healthy. Model {self.model} available.")
            return True
        except Exception as e:
            logger.error(f"✗ Ollama health check failed: {e}")
            return False

    async def generate_response(
        self,
        user_message: str,
        context: str,
        chat_history: List[Dict[str, str]] = [],
        temperature: float = 0.7,
        max_tokens: int = 1000,
    ) -> Dict[str, any]:
        """Generate response with retry logic and exponential backoff."""
        
        # Use longer timeout for first request (model loading), shorter for subsequent
        timeout = self.timeout_first_request if self._first_request else self.timeout_subsequent
        attempt = 0
        last_error = None

        while attempt <= self.request_retries:
            try:
                if attempt > 0:
                    # Exponential backoff: 2s, 4s, 8s, etc.
                    wait_time = self.retry_delay * (2 ** (attempt - 1))
                    logger.info(f"Retry attempt {attempt}/{self.request_retries} after {wait_time}s...")
                    time.sleep(wait_time)

                logger.info(
                    f"Generating response with {self.model} (attempt {attempt + 1}/{self.request_retries + 1}, "
                    f"timeout={timeout}s)"
                )

                parsed_chunks = self._extract_source_chunks(context)
                settings = get_settings()
                llm_chunks = self._select_chunks_for_generation(
                    parsed_chunks,
                    getattr(settings, "LLM_MAX_CONTEXT_CHARS", 10000),
                )
                intent = self._detect_intent(user_message)
                effective_max_tokens = self._max_tokens_for_intent(
                    intent,
                    max_tokens,
                    settings,
                )
                system_prompt = self._build_system_prompt()
                prompt = self._build_prompt(
                    system_prompt,
                    llm_chunks,
                    chat_history,
                    user_message,
                    intent,
                )

                response = requests.post(
                    f"{self.ollama_url}/api/generate",
                    json={
                        "model": self.model,
                        "prompt": prompt,
                        "stream": False,
                        "options": {
                            "temperature": temperature,
                            "num_predict": effective_max_tokens,
                        },
                    },
                    timeout=(5, timeout),  # (connection timeout, read timeout)
                )

                if response.status_code != 200:
                    raise Exception(f"Ollama error: {response.text}")

                result = response.json()
                answer_text = result.get("response", "").strip()
                if self._is_no_answer_response(answer_text) and self._has_query_evidence(user_message, llm_chunks):
                    logger.info("LLM returned no-answer despite evidence; using extractive fallback.")
                    answer_text = self._build_extractive_answer(llm_chunks, intent=intent)
                display_snippet_chars = getattr(settings, "LLM_DISPLAY_SNIPPET_CHARS", 280)
                structured_response = self.compose_structured_response(
                    parsed_chunks,
                    answer_text,
                    display_snippet_chars=display_snippet_chars,
                )

                # Mark first request as complete on success
                if self._first_request:
                    self._first_request = False
                    logger.info("✓ First request completed. Model loaded into memory.")

                return {
                    "response": structured_response,
                    "model": self.model,
                    "usage": {
                        "prompt_tokens": result.get("prompt_eval_count", 0),
                        "completion_tokens": result.get("eval_count", 0),
                        "total_tokens": result.get("prompt_eval_count", 0) + result.get("eval_count", 0),
                    },
                }
            except requests.exceptions.ConnectionError as e:
                last_error = e
                logger.warning(f"Attempt {attempt + 1}: Connection error to Ollama: {e}")
                attempt += 1
                if attempt <= self.request_retries:
                    continue
                logger.error("Ollama unavailable after retries (UNAVAILABLE)")
                raise LLMUnavailableError("OLLAMA_UNAVAILABLE", is_timeout=False) from e
            except requests.exceptions.Timeout as e:
                last_error = e
                logger.warning(
                    f"Attempt {attempt + 1}: Ollama timeout (timeout={timeout}s): {e}"
                )
                attempt += 1
                if attempt <= self.request_retries:
                    continue
                logger.error(f"Ollama timeout after {self.request_retries + 1} attempts (TIMEOUT)")
                raise LLMUnavailableError("OLLAMA_TIMEOUT", is_timeout=True) from e
            except Exception as e:
                logger.error(f"LLM generation error: {e}")
                raise
        
        # Should not reach here, but handle just in case
        raise LLMUnavailableError("OLLAMA_UNKNOWN_ERROR", is_timeout=False) from last_error

    def _build_system_prompt(self) -> str:
        return """You are a precise document recall assistant for Cognify.

You help users rediscover information from their own uploaded documents.
You must answer only from the provided chunks.
Do not use outside knowledge.
Do not invent facts.
Do not guess beyond what the text explicitly states.

Return only the text for the ANSWER section.
Choose exactly one of these cases:
1. If any chunk directly or closely answers the query, reformulate the answer clearly in your own words using only the information present in the chunks and end with: Source: {document_name}, Page {page_number}
2. If the chunks are related to the topic but do not directly answer the query, use the exact fallback wording requested.
3. If the chunks are clearly unrelated to the query, use the exact unrelated wording requested.

If any chunk clearly contains the answer, you must use case 1. Do not claim the answer is missing when evidence is present."""

    def _has_query_evidence(self, user_message: str, chunks: List[Dict[str, str]]) -> bool:
        if not user_message or not chunks:
            return False

        stopwords = {
            "the", "and", "for", "with", "that", "this", "from", "what", "when", "where",
            "which", "would", "could", "should", "about", "your", "their", "they", "them",
            "were", "been", "has", "have", "but", "into", "there", "here", "then", "than",
            "also", "only", "just", "will", "dont", "does", "doesnt", "did", "didnt",
            "cant", "cannot", "its", "you", "are", "was", "who", "why", "how", "can"
        }
        keywords = [
            w for w in re.findall(r"\w+", user_message.lower())
            if len(w) > 3 and w not in stopwords
        ]
        if not keywords:
            return False

        combined = "\n".join((c.get("content") or "").lower() for c in chunks)
        return any(word in combined for word in keywords)

    def _is_no_answer_response(self, text: str) -> bool:
        if not text:
            return False
        lowered = text.lower()
        return any(
            phrase in lowered
            for phrase in [
                "don't contain a direct answer",
                "does not appear to be",
                "no direct answer",
                "could not find",
                "not found",
            ]
        )

    def _build_extractive_answer(
        self,
        chunks: List[Dict[str, str]],
        max_chars: int = 1200,
        intent: str = "semantic-search",
    ) -> str:
        if not chunks:
            return ""

        if intent == "summary":
            max_chars = min(max_chars, 700)
        elif intent in {"explain", "semantic-search"}:
            max_chars = min(max_chars * 2, 2400)

        parts = []
        used = 0
        for chunk in chunks[:4]:
            content = (chunk.get("content") or "").strip()
            if not content:
                continue
            remaining = max_chars - used
            if remaining <= 0:
                break
            if len(content) > remaining:
                content = content[:remaining].rstrip() + "..."
            parts.append(content)
            used += len(content)

        document_name = chunks[0].get("document_name") or "Unknown document"
        page_number = chunks[0].get("page_number") or ""
        source_line = f"Source: {document_name}"
        if page_number:
            source_line += f", Page {page_number}"

        return f"{' '.join(parts)}\n\n{source_line}"

    def _select_chunks_for_generation(
        self,
        chunks: List[Dict[str, str]],
        max_chars: int,
    ) -> List[Dict[str, str]]:
        if not chunks or max_chars <= 0:
            return chunks

        selected: List[Dict[str, str]] = []
        used = 0
        for chunk in chunks:
            content = (chunk.get("content") or "").strip()
            if not content:
                continue
            if used + len(content) > max_chars and selected:
                break
            if used + len(content) > max_chars:
                chunk = {**chunk, "content": content[: max_chars - used].rstrip() + "..."}
                selected.append(chunk)
                break
            selected.append(chunk)
            used += len(content)

        if not selected and chunks:
            selected.append(chunks[0])

        if len(selected) < len(chunks):
            logger.info(
                "LLM context budget: using %d/%d chunks (~%d chars)",
                len(selected),
                len(chunks),
                used,
            )
        return selected

    def _max_tokens_for_intent(self, intent: str, requested: int, settings) -> int:
        defaults = {
            "summary": getattr(settings, "LLM_MAX_TOKENS_SUMMARY", 450),
            "definition": 700,
            "exam": 900,
            "compare": 900,
            "steps": 900,
            "explain": getattr(settings, "LLM_MAX_TOKENS_DETAILED", 1600),
            "semantic-search": getattr(settings, "LLM_MAX_TOKENS_DEFAULT", 1200),
        }
        intent_cap = defaults.get(intent, defaults["semantic-search"])
        if requested <= 0:
            return min(intent_cap, 2048)
        return min(requested, intent_cap, 2048)

    def _detect_intent(self, user_message: str) -> str:
        text = user_message.lower()

        if re.search(r"\b(exam|test|viva|marks|write in exam|exam answer)\b", text):
            return "exam"
        if re.search(r"\b(summarize|summary|in short|briefly|tl;dr)\b", text):
            return "summary"
        if re.search(r"\b(compare|difference|vs\b|versus|contrast)\b", text):
            return "compare"
        if re.search(r"\b(step|how to|process|procedure|workflow)\b", text):
            return "steps"
        if re.search(r"\b(define|definition|what is|meaning)\b", text):
            return "definition"
        if re.search(
            r"\b(explain|describe|tell me about|overview|in detail|generations?|"
            r"history of|timeline|walk me through|elaborate)\b",
            text,
        ):
            return "explain"
        return "semantic-search"

    def _intent_style_prompt(self, intent: str) -> str:
        styles = {
            "exam": (
                "Length: medium. Give a one-line direct answer, then 3-5 bullet points, "
                "then one short example only if it appears in the chunks."
            ),
            "summary": (
                "Length: short. Provide a tight summary in 3-6 bullets or 4-5 sentences. "
                "No filler and no outside knowledge."
            ),
            "compare": (
                "Length: medium. Use comparison-style bullets (A vs B) using only chunk evidence."
            ),
            "steps": (
                "Length: medium. Use numbered steps in the order implied by the chunks."
            ),
            "definition": (
                "Length: medium. Start with a definition, then 2-4 supporting points from the chunks."
            ),
            "explain": (
                "Length: detailed. Write multiple coherent paragraphs that synthesize all relevant "
                "chunks. Cover distinct points from the evidence; use bullets only when listing items "
                "explicitly present in the text."
            ),
            "semantic-search": (
                "Length: adaptive. If the chunks cover several related points, use 2-4 short paragraphs "
                "or structured bullets. If only one fact is present, answer in 2-4 sentences."
            ),
        }
        return styles.get(intent, styles["semantic-search"])

    def _extract_source_chunks(self, context: str) -> List[Dict[str, str]]:
        if not context:
            return []

        pattern = re.compile(
            r"\[Source\s+\d+:\s*(?P<document_name>[^\],]+?)(?:,\s*Page\s*(?P<page_number>\d+))?\]\s*\n(?P<content>.*?)(?=\n\n\[Source\s+\d+:|\Z)",
            re.DOTALL,
        )

        chunks: List[Dict[str, str]] = []
        for match in pattern.finditer(context):
            chunks.append(
                {
                    "document_name": match.group("document_name").strip(),
                    "page_number": match.group("page_number") or "",
                    "content": sanitize_indexed_text(match.group("content").strip()),
                }
            )

        if chunks:
            return chunks

        cleaned = context.strip()
        if cleaned:
            return [{"document_name": "Uploaded document", "page_number": "", "content": cleaned}]

        return []

    def _truncate_snippet(self, text: str, max_chars: int) -> str:
        cleaned = (text or "").strip()
        if max_chars <= 0 or len(cleaned) <= max_chars:
            return cleaned
        return cleaned[:max_chars].rstrip() + "..."

    def _format_top_matches(
        self,
        chunks: List[Dict[str, str]],
        display_snippet_chars: Optional[int] = None,
    ) -> str:
        lines = ["---", "📄 TOP MATCHES FROM YOUR DOCUMENTS:"]

        if not chunks:
            lines.append("1. No chunks were retrieved from your documents.")
            return "\n".join(lines)

        for index, chunk in enumerate(chunks, start=1):
            document_name = chunk.get("document_name") or "Unknown document"
            page_number = chunk.get("page_number") or "N/A"
            content = (chunk.get("content") or "").strip()
            if display_snippet_chars:
                content = self._truncate_snippet(content, display_snippet_chars)
            lines.append(f"{index}. [{document_name}, Page {page_number}]")
            lines.append(f"   {content}")

        return "\n".join(lines)

    def compose_structured_response(
        self,
        chunks: List[Dict[str, str]],
        answer_text: str,
        display_snippet_chars: Optional[int] = None,
    ) -> str:
        top_matches_block = self._format_top_matches(chunks, display_snippet_chars)
        answer_body = (answer_text or "").strip() or (
            "The AI answer could not be generated, but the retrieved document evidence is shown above."
        )

        return f"{top_matches_block}\n\n---\n💡 ANSWER:\n{answer_body}"

    def _build_prompt(
        self,
        system_prompt: str,
        chunks: List[Dict[str, str]],
        chat_history: List[Dict],
        user_message: str,
        intent: str,
    ) -> str:
        style_prompt = self._intent_style_prompt(intent)

        prompt = f"{system_prompt}\n\n"
        prompt += "Read all provided chunks carefully. Then do exactly one of the following:\n"
        prompt += "CASE 1 - If any chunk directly or closely answers the query: Reformulate the answer clearly in your own words using ONLY the information present in the chunks. End with: Source: {document_name}, Page {page_number}\n"
        prompt += "CASE 2 - If the chunks are related to the topic but don't directly answer the query: Say: \"Your documents don't contain a direct answer to this query. However, the closest information found suggests: [brief summary of what the chunks do say]. You may want to search with a different hint.\"\n"
        prompt += "CASE 3 - If the chunks are clearly unrelated to the query: Say: \"This specific information does not appear to be in your uploaded documents. The closest matches found were about [topic of chunks]. Try uploading relevant documents or rephrasing your query.\"\n"
        prompt += "If any chunk contains the answer, do not use Case 2 or Case 3.\n"
        prompt += (
            "Never add information from outside the provided chunks. Never guess or infer beyond "
            "what the text explicitly states. Match the requested length style; do not artificially "
            "shorten a multi-part answer when multiple chunks contain distinct relevant facts.\n\n"
        )
        prompt += f"{style_prompt}\n\n"

        for msg in chat_history[-5:]:
            role = "User" if msg.get('role', 'user').lower() == 'user' else "Assistant"
            prompt += f"{role}: {msg.get('content', '')}\n"

        prompt += "\nCONTEXT (Top Matched Chunks):\n"
        for index, chunk in enumerate(chunks, start=1):
            document_name = chunk.get("document_name") or "Unknown document"
            page_number = chunk.get("page_number")
            prompt += f"[{index}] {document_name}"
            if page_number:
                prompt += f", Page {page_number}"
            prompt += "\n"
            prompt += f"{chunk.get('content', '')}\n\n"

        prompt += f"User: {user_message}\n"
        prompt += "Assistant: Return only the ANSWER section content, not the top matches heading."

        return prompt
