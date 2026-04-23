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
    pass

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
        max_tokens: int = 1000
    ) -> Dict[str, any]:
        try:
            logger.info(f"Generating response with {self.model} (LOCAL)")
            
            system_prompt = self._build_system_prompt()
            prompt = self._build_prompt(system_prompt, context, chat_history, user_message)
            
            response = requests.post(
                f"{self.ollama_url}/api/generate",
                json={
                    "model": self.model,
                    "prompt": prompt,
                    "stream": False,
                    "options": {
                        "temperature": temperature,
                        "num_predict": max_tokens
                    }
                },
                timeout=(5, 120)  # fast connect timeout, slower read timeout
            )
            
            if response.status_code != 200:
                raise Exception(f"Ollama error: {response.text}")
            
            result = response.json()
            
            return {
                "response": result.get('response', ''),
                "model": self.model,
                "usage": {
                    "prompt_tokens": result.get('prompt_eval_count', 0),
                    "completion_tokens": result.get('eval_count', 0),
                    "total_tokens": result.get('prompt_eval_count', 0) + result.get('eval_count', 0)
                }
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
        return """You are a helpful AI assistant for document-grounded semantic search.

Guidelines:
1. Base your answers ON THE PROVIDED CONTEXT from the user's documents.
2. If the context doesn't contain relevant information to answer the question, say so politely. Do not invent facts.
3. Prefer factual, directly grounded responses over generic explanations.
4. Adapt style to user intent (exam prep, summary, comparison, definition, step-by-step, quick answer).
5. Default style for normal semantic search: concise, structured, and practical.

Remember: You are helping the user understand THEIR documents, not providing general knowledge."""

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
                "then one short example if available in context."
            ),
            "summary": "Style: concise summary with 3-6 bullets and no extra filler.",
            "compare": "Style: comparison table-like bullets (A vs B) using only context evidence.",
            "steps": "Style: numbered steps, clear sequence, practical wording.",
            "definition": "Style: definition first, then core components and one context-based example.",
            "semantic-search": (
                "Style: concise and directly useful for retrieval tasks; answer first, then supporting "
                "points grounded in context."
            ),
        }
        return styles.get(intent, styles["semantic-search"])
    
    def _build_prompt(self, system_prompt: str, context: str, chat_history: List[Dict], user_message: str) -> str:
        intent = self._detect_intent(user_message)
        style_prompt = self._intent_style_prompt(intent)

        prompt = f"{system_prompt}\\n\\n"
        prompt += f"{style_prompt}\n"
        prompt += "If a specific detail is missing from context, say it is not present in uploaded documents.\n\n"
        
        for msg in chat_history[-5:]:
            role = "User" if msg.get('role', 'user').lower() == 'user' else "Assistant"
            prompt += f"{role}: {msg.get('content', '')}\\n"
        
        prompt += f"\\nContext from the user's documents:\\n{context}\\n\\n"
        prompt += f"User: {user_message}\\n"
        prompt += "Assistant:"
        
        return prompt
