from uuid import uuid4
import asyncio

import pytest
from fastapi import HTTPException

from app.api import routes
from app.models import ProcessDocumentRequest, SearchRequest


def test_process_empty_document_returns_400(monkeypatch):
    def fake_process_document(file_path, document_type):
        return "   ", {0: 1}

    monkeypatch.setattr(routes.doc_processor, "process_document", fake_process_document)

    request = ProcessDocumentRequest(
        document_id=uuid4(),
        file_path="dummy.txt",
        user_id=uuid4(),
        document_type="TXT",
    )

    with pytest.raises(HTTPException) as exc:
        asyncio.run(routes.process_document(request))

    assert exc.value.status_code == 400
    assert exc.value.detail["code"] == "EMPTY_DOCUMENT"


def test_search_no_documents_returns_400(monkeypatch):
    async def fake_search(query, user_id, top_k):
        raise ValueError("NO_DOCUMENTS")

    monkeypatch.setattr(routes.rag_service, "search", fake_search)

    request = SearchRequest(
        query="What happened?",
        user_id=uuid4(),
        top_k=5,
    )

    with pytest.raises(HTTPException) as exc:
        asyncio.run(routes.search(request))

    assert exc.value.status_code == 400
    assert exc.value.detail["code"] == "NO_DOCUMENTS"
