import pytest

from app.services.document_processor import DocumentProcessor


def test_corrupted_pdf_raises_exception(tmp_path):
    processor = DocumentProcessor()
    corrupted_pdf = tmp_path / "corrupted.pdf"
    corrupted_pdf.write_bytes(b"this is not a real pdf")

    with pytest.raises(Exception):
        processor.process_document(str(corrupted_pdf), "PDF")


def test_empty_txt_extracts_empty_content(tmp_path):
    processor = DocumentProcessor()
    empty_txt = tmp_path / "empty.txt"
    empty_txt.write_text("", encoding="utf-8")

    content, page_map = processor.process_document(str(empty_txt), "TXT")

    assert content == ""
    assert page_map == {0: 1}
