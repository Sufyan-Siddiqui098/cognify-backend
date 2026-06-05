"""
FastAPI routes for the Cognify AI service.
Handles document processing, semantic search, RAG generation, and vector cleanup.
"""
from fastapi import APIRouter, HTTPException, Body
from uuid import UUID
import logging
from pydantic import BaseModel
from typing import Optional
from functools import lru_cache

from app.models import (
    ProcessDocumentRequest, ProcessDocumentResponse, ChunkMetadata,
    SearchRequest, SearchResponse, SearchResult,
    RAGGenerateRequest, RAGGenerateResponse
)
from app.services.document_processor import DocumentProcessor
from app.services.chunking_service import ChunkingService
from app.services.embedding_service import EmbeddingService
from app.services.vector_store import VectorStore
from app.services.llm_service import LLMService
from app.services.rag_service import RAGService
from app.services.chunk_quality import should_keep_chunk
from app.config import get_settings

logger = logging.getLogger(__name__)
router = APIRouter()
settings = get_settings()

doc_processor = DocumentProcessor()
chunking_service = ChunkingService(settings.CHUNK_SIZE, settings.CHUNK_OVERLAP)

@lru_cache(maxsize=1)
def get_runtime_services():
    """Create long-lived service singletons once per process."""
    embedding_service = EmbeddingService(
        settings.EMBEDDING_MODEL,
        local_files_only=settings.EMBEDDING_LOCAL_FILES_ONLY,
        cache_folder=settings.EMBEDDING_CACHE_DIR,
    )
    vector_dimension = embedding_service.dimensions or settings.VECTOR_DIMENSION
    vector_store = VectorStore(
        settings.QDRANT_HOST,
        settings.QDRANT_PORT,
        settings.QDRANT_COLLECTION,
        vector_dimension,
    )
    # Initialize LLM service with configurable timeouts and retry logic
    llm_service = LLMService(
        ollama_url=settings.OLLAMA_URL,
        model=settings.OLLAMA_MODEL,
        timeout_first_request=settings.LLM_TIMEOUT_FIRST_REQUEST,
        timeout_subsequent=settings.LLM_TIMEOUT_SUBSEQUENT,
        request_retries=settings.LLM_REQUEST_RETRIES,
        retry_delay=settings.LLM_RETRY_DELAY,
    )
    rag_service = RAGService(embedding_service, vector_store, llm_service)
    return embedding_service, vector_store, rag_service


def initialize_runtime_services():
    """Warm service singletons during app startup and verify Ollama is healthy."""
    embedding_service, vector_store, rag_service = get_runtime_services()
    
    # Health check Ollama during startup
    llm_service = rag_service.llm_service
    if not llm_service.health_check():
        logger.warning(
            "⚠️  Ollama is not accessible at startup. The service will continue, "
            "but requests will fail until Ollama becomes available. "
            "Start Ollama with: ollama serve"
        )
    
    return embedding_service, vector_store, rag_service

class DeleteVectorsRequest(BaseModel):
    """Request model for deleting vectors associated with a document."""
    document_id: str
    user_id: str

@router.post("/process-document", response_model=ProcessDocumentResponse)
@router.post("/process", response_model=ProcessDocumentResponse)
async def process_document(request: ProcessDocumentRequest = Body(...)):
    try:
        embedding_service, vector_store, _ = get_runtime_services()
        logger.info(f"Processing document: {request.document_id}")
        
        full_text, page_map = doc_processor.process_document(request.file_path, request.document_type)
        if not full_text or not full_text.strip():
            raise HTTPException(
                status_code=400,
                detail={"code": "EMPTY_DOCUMENT", "message": "Document contains no extractable text."}
            )

        chunks = chunking_service.chunk_text(full_text, page_map)
        if not chunks:
            raise HTTPException(
                status_code=400,
                detail={"code": "EMPTY_DOCUMENT", "message": "Document contains no extractable text."}
            )

        # ── Quality gate ────────────────────────────────────────────────────
        # Drop junk chunks before embedding so they never pollute Qdrant.
        # The threshold is tunable via CHUNK_QUALITY_THRESHOLD in .env.
        quality_threshold = getattr(settings, "CHUNK_QUALITY_THRESHOLD", 0.35)
        total_raw = len(chunks)
        chunks = [(text, start, end) for text, start, end in chunks
                  if should_keep_chunk(text, quality_threshold)]
        dropped = total_raw - len(chunks)
        if dropped:
            logger.info(
                "Quality gate dropped %d/%d chunks for document %s",
                dropped, total_raw, request.document_id,
            )
        if not chunks:
            raise HTTPException(
                status_code=400,
                detail={
                    "code": "LOW_QUALITY_DOCUMENT",
                    "message": (
                        f"All {total_raw} extracted chunks failed the quality gate. "
                        "The document may be image-only, corrupted, or contain only formatting noise."
                    ),
                }
            )
        # ── End quality gate ────────────────────────────────────────────────

        chunk_texts = [chunk[0] for chunk in chunks]
        embeddings = await embedding_service.generate_embeddings_batch(chunk_texts)
        
        chunk_metadata_list = []
        qdrant_metadata = []
        
        import uuid
        # Extract document name from file path
        import os
        document_name = os.path.basename(request.file_path)
        
        for idx, (chunk_data, embedding) in enumerate(zip(chunks, embeddings)):
            chunk_text, start_char, end_char = chunk_data
            chunk_uuid = str(uuid.uuid4())
            page_num = chunking_service.get_page_number(page_map, start_char, end_char)
            
            chunk_metadata_list.append(ChunkMetadata(
                chunk_id=UUID(chunk_uuid),
                chunk_index=idx,
                content=chunk_text,
                page_number=page_num,
                token_count=len(chunk_text) // 4
            ))
            
            qdrant_metadata.append({
                'chunk_id': chunk_uuid,
                'document_id': str(request.document_id),
                'user_id': str(request.user_id),
                'document_name': document_name,
                'content': chunk_text,
                'page_number': page_num,
                'chunk_index': idx
            })
        
        vector_ids = await vector_store.upsert_vectors(embeddings, qdrant_metadata)
        logger.info(f"Document processed: {len(chunk_metadata_list)} chunks")
        
        return ProcessDocumentResponse(
            success=True,
            document_id=request.document_id,
            chunks=chunk_metadata_list,
            total_chunks=total_raw,
            indexed_chunks=len(chunk_metadata_list),
            message=(
                f"Document processed successfully via Local Service "
                f"({len(chunk_metadata_list)}/{total_raw} chunks indexed after quality gate)"
            )
        )
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Error: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail=str(e))

@router.post("/search", response_model=SearchResponse)
async def search(request: SearchRequest):
    try:
        _, _, rag_service = get_runtime_services()
        logger.info(f"Search request: {request.query}")
        results = await rag_service.search(request.query, str(request.user_id), request.top_k)
        
        search_results = []
        for result in results:
            search_results.append(SearchResult(
                chunk_id=result['chunk_id'],
                document_id=result['metadata']['document_id'],
                document_name=result['metadata'].get('document_name', 'document'),
                content=result['metadata']['content'],
                page_number=result['metadata'].get('page_number'),
                relevance_score=result['score']
            ))
        
        return SearchResponse(
            results=search_results,
            query=request.query,
            total_results=len(search_results)
        )
    except ValueError as e:
        if str(e) == "NO_DOCUMENTS":
            raise HTTPException(
                status_code=400,
                detail={
                    "code": "NO_DOCUMENTS",
                    "message": "No indexed documents found for this user."
                }
            )
        raise
    except Exception as e:
        logger.error(f"Error: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail=str(e))

@router.post("/rag/generate", response_model=RAGGenerateResponse)
@router.post("/generate", response_model=RAGGenerateResponse)
async def generate_rag_response(request: RAGGenerateRequest):
    try:
        _, _, rag_service = get_runtime_services()
        logger.info("RAG generate request")
        result = await rag_service.generate(
            request.user_message,
            request.context,
            request.chat_history,
            request.temperature,
            request.max_tokens
        )
        return RAGGenerateResponse(**result)
    except Exception as e:
        logger.error(f"Error: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail=str(e))

@router.post("/delete-vectors")
async def delete_vectors(request: DeleteVectorsRequest = Body(...)):
    """Delete all vectors belonging to a specific document from Qdrant."""
    try:
        _, vector_store, _ = get_runtime_services()
        logger.info(f"Deleting vectors for document: {request.document_id}")
        deleted_count = await vector_store.delete_vectors_by_document(
            request.document_id, request.user_id
        )
        return {
            "success": True,
            "deleted_count": deleted_count,
            "message": f"Deleted vectors for document {request.document_id}"
        }
    except Exception as e:
        logger.error(f"Error deleting vectors: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail=str(e))
