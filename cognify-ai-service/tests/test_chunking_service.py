from app.services.chunking_service import ChunkingService


def test_chunking_respects_size_and_overlap():
    service = ChunkingService(chunk_size=100, chunk_overlap=20)
    text = "Paragraph one. " * 30
    page_map = {0: 1}

    chunks = service.chunk_text(text, page_map)

    assert len(chunks) > 1
    for chunk_text, _, _ in chunks:
        assert len(chunk_text) <= 100

    # Verify there is at least one overlapping pair.
    first_chunk = chunks[0][0]
    second_chunk = chunks[1][0]
    assert first_chunk[-20:].strip() in second_chunk
