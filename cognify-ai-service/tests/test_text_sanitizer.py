from app.services.text_sanitizer import sanitize_indexed_text, strip_pymupdf_markers


def test_strip_standalone_picture_marker():
    raw = "==> picture [720 x 540] intentionally omitted <=="
    assert strip_pymupdf_markers(raw).strip() == ""


def test_strip_inline_picture_marker():
    raw = (
        "A computer is an electronic device.\n"
        "==> picture [106 x 540] intentionally omitted <==\n"
        "Definition-2: An electronic device"
    )
    cleaned = strip_pymupdf_markers(raw)
    assert "==>" not in cleaned
    assert "intentionally omitted" not in cleaned
    assert "A computer is an electronic device" in cleaned
    assert "Definition-2" in cleaned


def test_sanitize_removes_multiple_markers():
    raw = """## Fundamentals of

==> picture [106 x 540] intentionally omitted <==

Com uter p

==> picture [720 x 540] intentionally omitted <==

A computer is an electronic device."""
    cleaned = sanitize_indexed_text(raw)
    assert "==>" not in cleaned
    assert "intentionally omitted" not in cleaned.lower()
    assert "A computer is an electronic device" in cleaned
