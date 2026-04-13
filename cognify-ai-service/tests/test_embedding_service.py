import numpy as np

from app.services import embedding_service as embedding_module


class FakeSentenceTransformer:
    def __init__(self, model_name: str):
        self.model_name = model_name

    def get_sentence_embedding_dimension(self):
        return 768

    def encode(self, text, convert_to_numpy=True, show_progress_bar=False):
        if isinstance(text, list):
            return np.array([[0.1] * 768 for _ in text], dtype=float)
        return np.array([0.1] * 768, dtype=float)


def test_generate_embedding_returns_768_dimensions(monkeypatch):
    monkeypatch.setattr(embedding_module, "SentenceTransformer", FakeSentenceTransformer)
    service = embedding_module.EmbeddingService("sentence-transformers/all-MiniLM-L6-v2")

    emb = __import__("asyncio").run(service.generate_embedding("hello"))

    assert isinstance(emb, list)
    assert len(emb) == 768
    assert all(isinstance(v, float) for v in emb)
