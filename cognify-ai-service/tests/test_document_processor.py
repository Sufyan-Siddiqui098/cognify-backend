from app.services.document_processor import DocumentProcessor


def test_parse_whatsapp_export_strips_metadata():
    processor = DocumentProcessor()
    sample = """[08/04/2026, 10:11:12] Alice: Hello there
[08/04/2026, 10:12:01] Bob: Hi Alice
[08/04/2026, 10:12:55] Alice: How are you?
"""

    parsed = processor._process_whatsapp(sample)

    assert "Alice:" not in parsed
    assert "Bob:" not in parsed
    assert "10:11:12" not in parsed
    assert "Hello there" in parsed
    assert "Hi Alice" in parsed
    assert "How are you?" in parsed
