"""
LOCAL LLM using Ollama
"""
import logging
import re
from typing import List, Dict

import requests

logger = logging.getLogger(__name__)


class LLMUnavailableError(Exception):
    """Raised when Ollama is unreachable or times out."""


class LLMService:
    def __init__(self, ollama_url: str = "http://localhost:11434", model: str = "llama3.2"):
        self.ollama_url = ollama_url
        self.model = model
        logger.info(f"LLM Service initialized: {model} at {ollama_url}")

    async def generate_response(
        self,
        user_message: str,
        context: str,
        chat_history: List[Dict[str, str]] = [],
        temperature: float = 0.7,
        max_tokens: int = 1000,
    ) -> Dict[str, any]:
        try:
            logger.info(f"Generating response with {self.model} (LOCAL)")

            parsed_chunks = self._extract_source_chunks(context)
            system_prompt = self._build_system_prompt()
            prompt = self._build_prompt(system_prompt, parsed_chunks, chat_history, user_message)

            response = requests.post(
                f"{self.ollama_url}/api/generate",
                json={
                    "model": self.model,
                    "prompt": prompt,
                    "stream": False,
                    "options": {
                        "temperature": temperature,
                        "num_predict": max_tokens,
                    },
                },
                timeout=(5, 120),
            )

            if response.status_code != 200:
                raise Exception(f"Ollama error: {response.text}")

            result = response.json()
            answer_text = result.get("response", "").strip()
            structured_response = self.compose_structured_response(parsed_chunks, answer_text)

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
            logger.warning("Ollama unavailable at %s: %s", self.ollama_url, e)
            raise LLMUnavailableError("OLLAMA_UNAVAILABLE") from e
        except requests.exceptions.Timeout as e:
            logger.warning("Ollama request timeout at %s: %s", self.ollama_url, e)
            raise LLMUnavailableError("OLLAMA_TIMEOUT") from e
        except Exception as e:
            logger.error(f"LLM generation error: {e}")
            raise

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

If evidence is missing, say so plainly rather than guessing."""

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
        return "semantic-search"

    def _intent_style_prompt(self, intent: str) -> str:
        styles = {
            "exam": (
                "Style: exam-ready. First give a one-line direct answer, then 3-5 key bullet points, "
                "then one short example if available in the chunks."
            ),
            "summary": "Style: concise summary with 3-6 bullets and no extra filler.",
            "compare": "Style: comparison table-like bullets (A vs B) using only chunk evidence.",
            "steps": "Style: numbered steps, clear sequence, practical wording.",
            "definition": "Style: definition first, then core components and one chunk-based example.",
            "semantic-search": (
                "Style: concise and directly useful for retrieval tasks; answer first, then supporting "
                "points grounded in the chunks."
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
                    "content": match.group("content").strip(),
                }
            )

        if chunks:
            return chunks

        cleaned = context.strip()
        if cleaned:
            return [{"document_name": "Uploaded document", "page_number": "", "content": cleaned}]

        return []

    def _format_top_matches(self, chunks: List[Dict[str, str]]) -> str:
        lines = ["---", "📄 TOP MATCHES FROM YOUR DOCUMENTS:"]

        if not chunks:
            lines.append("1. No chunks were retrieved from your documents.")
            return "\n".join(lines)

        for index, chunk in enumerate(chunks, start=1):
            document_name = chunk.get("document_name") or "Unknown document"
            page_number = chunk.get("page_number") or "N/A"
            content = (chunk.get("content") or "").strip()
            excerpt = content if len(content) <= 280 else content[:277].rstrip() + "..."
            lines.append(f"{index}. [{document_name}, Page {page_number}]")
            lines.append(f"   {excerpt}")

        return "\n".join(lines)

    def compose_structured_response(self, chunks: List[Dict[str, str]], answer_text: str) -> str:
        top_matches_block = self._format_top_matches(chunks)
        answer_body = (answer_text or "").strip() or (
            "Your documents don't contain a direct answer to this query. However, the closest information found suggests: "
            "the retrieved chunks are related, but not sufficient to answer exactly. You may want to search with a different hint."
        )

        return f"{top_matches_block}\n\n---\n💡 ANSWER:\n{answer_body}"

    def _build_prompt(self, system_prompt: str, chunks: List[Dict[str, str]], chat_history: List[Dict], user_message: str) -> str:
        intent = self._detect_intent(user_message)
        style_prompt = self._intent_style_prompt(intent)

        prompt = f"{system_prompt}\n\n"
        prompt += "Read all provided chunks carefully. Then do exactly one of the following:\n"
        prompt += "CASE 1 - If any chunk directly or closely answers the query: Reformulate the answer clearly in your own words using ONLY the information present in the chunks. End with: Source: {document_name}, Page {page_number}\n"
        prompt += "CASE 2 - If the chunks are related to the topic but don't directly answer the query: Say: \"Your documents don't contain a direct answer to this query. However, the closest information found suggests: [brief summary of what the chunks do say]. You may want to search with a different hint.\"\n"
        prompt += "CASE 3 - If the chunks are clearly unrelated to the query: Say: \"This specific information does not appear to be in your uploaded documents. The closest matches found were about [topic of chunks]. Try uploading relevant documents or rephrasing your query.\"\n"
        prompt += "Never add information from outside the provided chunks. Never guess or infer beyond what the text explicitly states. Keep the response concise and professional.\n\n"
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
