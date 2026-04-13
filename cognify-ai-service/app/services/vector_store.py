"""
LOCAL Qdrant vector database
"""
import logging
from typing import List, Dict, Any
from qdrant_client import QdrantClient
from qdrant_client.models import Distance, VectorParams, PointStruct, Filter, FieldCondition, MatchValue, FilterSelector

logger = logging.getLogger(__name__)

class VectorStore:
    def __init__(self, host: str = "localhost", port: int = 6333, collection: str = "cognify_vectors", dimension: int = 768):
        logger.info(f"Connecting to Qdrant at {host}:{port}")
        self.client = QdrantClient(host=host, port=port, check_compatibility=False)
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
                collection_info = self.client.get_collection(self.collection)
                vectors_config = collection_info.config.params.vectors
                current_dim = None
                if hasattr(vectors_config, "size"):
                    current_dim = vectors_config.size
                elif isinstance(vectors_config, dict):
                    first_vector = next(iter(vectors_config.values()), None)
                    current_dim = getattr(first_vector, "size", None)

                if current_dim is not None and current_dim != self.dimension:
                    logger.warning(
                        "Collection %s dimension mismatch (%s != %s). Recreating collection.",
                        self.collection,
                        current_dim,
                        self.dimension,
                    )
                    self.client.delete_collection(self.collection)
                    self.client.create_collection(
                        collection_name=self.collection,
                        vectors_config=VectorParams(size=self.dimension, distance=Distance.COSINE)
                    )
                    logger.info("Collection recreated with correct dimension")
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

            count_result = self.client.count(
                collection_name=self.collection,
                count_filter=query_filter,
                exact=False,
            )
            if count_result.count == 0:
                raise ValueError("NO_DOCUMENTS")
            
            results = self._search_points(query_vector, query_filter, top_k)
            
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

    def _search_points(self, query_vector: List[float], query_filter: Filter, top_k: int):
        """Compatibility wrapper for qdrant-client API changes across versions."""
        if hasattr(self.client, "search"):
            return self.client.search(
                collection_name=self.collection,
                query_vector=query_vector,
                query_filter=query_filter,
                limit=top_k,
                with_payload=True
            )

        if hasattr(self.client, "query_points"):
            response = self.client.query_points(
                collection_name=self.collection,
                query=query_vector,
                query_filter=query_filter,
                limit=top_k,
                with_payload=True
            )
            if hasattr(response, "points"):
                return response.points
            return response

        raise RuntimeError("Unsupported qdrant-client version: no search/query_points method available")

    async def delete_vectors_by_document(self, document_id: str, user_id: str) -> int:
        """Delete all vectors associated with a specific document from Qdrant."""
        try:
            self.client.delete(
                collection_name=self.collection,
                points_selector=FilterSelector(
                    filter=Filter(
                        must=[
                            FieldCondition(
                                key="document_id",
                                match=MatchValue(value=document_id)
                            ),
                            FieldCondition(
                                key="user_id",
                                match=MatchValue(value=user_id)
                            )
                        ]
                    )
                )
            )
            logger.info(f"Deleted vectors for document {document_id}")
            return 1
        except Exception as e:
            logger.error(f"Error deleting vectors: {e}")
            raise
