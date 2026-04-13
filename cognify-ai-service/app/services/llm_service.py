"""
LOCAL LLM using Ollama
"""
import logging
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
        return """You are a helpful AI assistant that answers questions strictly based on the user's uploaded documents.

Guidelines:
1. Base your answers ON THE PROVIDED CONTEXT from the user's documents.
2. If the context doesn't contain relevant information to answer the question, say so politely. Do not invent facts.
3. Cite sources implicitly by mentioning you found it in their documents.
4. Be concise but comprehensive.

Remember: You are helping the user understand THEIR documents, not providing general knowledge."""
    
    def _build_prompt(self, system_prompt: str, context: str, chat_history: List[Dict], user_message: str) -> str:
        prompt = f"{system_prompt}\\n\\n"
        
        for msg in chat_history[-5:]:
            role = "User" if msg.get('role', 'user').lower() == 'user' else "Assistant"
            prompt += f"{role}: {msg.get('content', '')}\\n"
        
        prompt += f"\\nContext from the user's documents:\\n{context}\\n\\n"
        prompt += f"User: {user_message}\\n"
        prompt += "Assistant:"
        
        return prompt
