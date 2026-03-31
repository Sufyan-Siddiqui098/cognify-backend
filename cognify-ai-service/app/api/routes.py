"""
FastAPI routes
"""
from fastapi import APIRouter, HTTPException
from uuid import UUID
import logging

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
from app.config import get_settings

logger = logging.getLogger(__name__)
router = APIRouter()
settings = get_settings()

doc_processor = DocumentProcessor()
chunking_service = ChunkingService(settings.CHUNK_SIZE, settings.CHUNK_OVERLAP)

# Deferred initialization or dynamic loading
# In a real app we'd use Dependency Injection `Depends()`, but for this script we load directly
# We initialize these cautiously in routes to avoid blocking import, but main.py handles preload.
embedding_service = EmbeddingService(settings.EMBEDDING_MODEL)
vector_store = VectorStore(settings.QDRANT_HOST, settings.QDRANT_PORT, settings.QDRANT_COLLECTION, settings.VECTOR_DIMENSION)
llm_service = LLMService(settings.OLLAMA_URL, settings.OLLAMA_MODEL)
rag_service = RAGService(embedding_service, vector_store, llm_service)

@router.post("/process-document", response_model=ProcessDocumentResponse)
async def process_document(request: ProcessDocumentRequest):
    try:
        logger.info(f"Processing document: {request.document_id}")
        
        full_text, page_map = doc_processor.process_document(request.file_path, request.document_type)
        chunks = chunking_service.chunk_text(full_text, page_map)
        
        chunk_texts = [chunk[0] for chunk in chunks]
        embeddings = await embedding_service.generate_embeddings_batch(chunk_texts)
        
        chunk_metadata_list = []
        qdrant_metadata = []
        
        import uuid
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
            total_chunks=len(chunk_metadata_list),
            message="Document processed successfully via Local Service"
        )
    except Exception as e:
        logger.error(f"Error: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail=str(e))

@router.post("/search", response_model=SearchResponse)
async def search(request: SearchRequest):
    try:
        logger.info(f"Search request: {request.query}")
        results = await rag_service.search(request.query, str(request.user_id), request.top_k)
        
        search_results = []
        for result in results:
            search_results.append(SearchResult(
                chunk_id=result['chunk_id'],
                document_id=result['metadata']['document_id'],
                document_name="document", # Could pass real name if we stored it
                content=result['metadata']['content'],
                page_number=result['metadata'].get('page_number'),
                relevance_score=result['score']
            ))
        
        return SearchResponse(
            results=search_results,
            query=request.query,
            total_results=len(search_results)
        )
    except Exception as e:
        logger.error(f"Error: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail=str(e))

@router.post("/rag/generate", response_model=RAGGenerateResponse)
async def generate_rag_response(request: RAGGenerateRequest):
    try:
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
