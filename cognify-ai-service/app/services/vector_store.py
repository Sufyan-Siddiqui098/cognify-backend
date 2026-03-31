"""
LOCAL Qdrant vector database
"""
import logging
from typing import List, Dict, Any
from qdrant_client import QdrantClient
from qdrant_client.models import Distance, VectorParams, PointStruct, Filter, FieldCondition, MatchValue

logger = logging.getLogger(__name__)

class VectorStore:
    def __init__(self, host: str = "localhost", port: int = 6333, collection: str = "cognify_documents", dimension: int = 768):
        logger.info(f"Connecting to Qdrant at {host}:{port}")
        self.client = QdrantClient(host=host, port=port)
        self.collection = collection
        self.dimension = dimension
        
    async def health_check(self) -> bool:
        """Test Qdrant connection and init collection if needed"""
        try:
            collections = self.client.get_collections().collections
            exists = any(c.name == self.collection for c in collections)
            
            if not exists:
                logger.info(f"Creating collection: {self.collection}")
                self.client.create_collection(
                    collection_name=self.collection,
                    vectors_config=VectorParams(size=self.dimension, distance=Distance.COSINE)
                )
                logger.info("Collection created")
            else:
                logger.info(f"Collection exists: {self.collection}")
            return True
        except Exception as e:
            logger.error(f"Qdrant health check failed: {e}")
            return False

    async def upsert_vectors(self, vectors: List[List[float]], metadatas: List[Dict[str, Any]]) -> List[str]:
        try:
            logger.info(f"Upserting {len(vectors)} vectors to Qdrant")
            
            points = []
            for idx, (vector, metadata) in enumerate(zip(vectors, metadatas)):
                point = PointStruct(
                    id=metadata['chunk_id'],
                    vector=vector,
                    payload=metadata
                )
                points.append(point)
            
            self.client.upsert(collection_name=self.collection, points=points)
            
            vector_ids = [p.id for p in points]
            logger.info(f"Upserted {len(vector_ids)} vectors")
            return vector_ids
        except Exception as e:
            logger.error(f"Upsert error: {e}")
            raise
    
    async def search(self, query_vector: List[float], user_id: str, top_k: int = 5) -> List[Dict[str, Any]]:
        try:
            logger.info(f"Searching Qdrant (top_k={top_k}, user_id={user_id})")
            
            query_filter = Filter(
                must=[FieldCondition(key="user_id", match=MatchValue(value=user_id))]
            )
            
            results = self.client.search(
                collection_name=self.collection,
                query_vector=query_vector,
                query_filter=query_filter,
                limit=top_k,
                with_payload=True
            )
            
            formatted_results = []
            for result in results:
                formatted_results.append({
                    'chunk_id': result.id,
                    'score': result.score,
                    'metadata': result.payload
                })
            
            logger.info(f"Found {len(formatted_results)} results")
            return formatted_results
        except Exception as e:
            logger.error(f"Search error: {e}")
            raise
