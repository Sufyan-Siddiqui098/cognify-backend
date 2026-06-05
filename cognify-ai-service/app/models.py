"""
Pydantic models for API
"""
from pydantic import BaseModel
from typing import List, Optional, Dict
from uuid import UUID
from enum import Enum

class DocumentType(str, Enum):
    PDF = "PDF"
    DOCX = "DOCX"
    TXT = "TXT"
    WHATSAPP = "WHATSAPP"

# Request Models
class ProcessDocumentRequest(BaseModel):
    document_id: UUID
    file_path: str
    user_id: UUID
    document_type: DocumentType

class SearchRequest(BaseModel):
    query: str
    user_id: UUID
    top_k: int = 5

class RAGGenerateRequest(BaseModel):
    user_message: str
    context: str
    chat_history: List[Dict[str, str]] = []
    temperature: float = 0.7
    max_tokens: int = 1000

# Response Models
class ChunkMetadata(BaseModel):
    chunk_id: UUID
    chunk_index: int
    content: str
    page_number: Optional[int]
    token_count: int

class ProcessDocumentResponse(BaseModel):
    success: bool
    document_id: UUID
    chunks: List[ChunkMetadata]
    total_chunks: int
    indexed_chunks: Optional[int] = None  # Chunks that passed the quality gate
    message: str


class SearchResult(BaseModel):
    chunk_id: str
    document_id: str
    document_name: str
    content: str
    page_number: Optional[int]
    relevance_score: float

class SearchResponse(BaseModel):
    results: List[SearchResult]
    query: str
    total_results: int

class RAGGenerateResponse(BaseModel):
    response: str
    model: str
    usage: Dict[str, int]
