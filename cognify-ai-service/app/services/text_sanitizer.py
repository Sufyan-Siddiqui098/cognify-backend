"""
Shared text cleanup for document extraction and chunking.
Removes pymupdf4llm image placeholders and other noise before embedding.
"""
import re

# pymupdf4llm markers, e.g. "==> picture [720 x 540] intentionally omitted <=="
_PYMUPDF_MARKER = re.compile(
    r"==>\s*(?:picture|image|figure|diagram)[\s\S]*?<==",
    re.IGNORECASE,
)

# Broader fallback: any ==> ... <== block
_PYMUPDF_MARKER_FALLBACK = re.compile(r"==>[\s\S]*?<==", re.IGNORECASE)

_PICTURE_TEXT_LINE = re.compile(r"end of picture text", re.IGNORECASE)
_INTENTIONALLY_OMITTED_LINE = re.compile(
    r"^\s*.*\bintentionally\s+omitted\b.*$",
    re.IGNORECASE | re.MULTILINE,
)
# _HTML_TAG = re.compile(r"<[^>]+>")
_EXCESS_BLANK_LINES = re.compile(r"\n{3,}")


def strip_pymupdf_markers(text: str) -> str:
    if not text:
        return text
    cleaned = _PYMUPDF_MARKER.sub(" ", text)
    cleaned = _PYMUPDF_MARKER_FALLBACK.sub(" ", cleaned)
    return cleaned


def sanitize_indexed_text(text: str) -> str:
    """
    Clean text that will be chunked or embedded.
    Safe to run on full documents and individual chunks.
    """
    if not text:
        return text

    cleaned = text.replace("\r\n", "\n").replace("\r", "\n")
    cleaned = strip_pymupdf_markers(cleaned)
    cleaned = _PICTURE_TEXT_LINE.sub(" ", cleaned)
    cleaned = _INTENTIONALLY_OMITTED_LINE.sub("\n", cleaned)
    # cleaned = _HTML_TAG.sub(" ", cleaned)
    cleaned = _EXCESS_BLANK_LINES.sub("\n\n", cleaned)

    lines = []
    for line in cleaned.split("\n"):
        stripped = line.strip()
        if not stripped:
            lines.append("")
            continue
        # Drop lines that are only a marker fragment after partial removal
        if re.match(r"^==>\s*$", stripped) or re.match(r"^<==\s*$", stripped):
            continue
        if re.search(r"\bintentionally\s+omitted\b", stripped, re.IGNORECASE) and len(stripped) < 120:
            continue
        lines.append(stripped)

    return "\n".join(lines).strip()
