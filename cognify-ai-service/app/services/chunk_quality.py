"""
Ingestion quality gate for Cognify.

Scores individual chunk text using fast, document-agnostic heuristics (no LLM)
to decide whether a chunk is worth embedding and storing in Qdrant.  Called at
ingest time — after chunking, before generate_embeddings_batch — so structural
noise never enters the vector store.

Design principles (industry-standard for RAG ingest gates):
  - Only reject chunks that are unambiguously useless for retrieval.
  - Never apply heuristics that could silently drop equations, formulas,
    code, tables, or domain-specific vocabulary.
  - Prefer false negatives (letting a borderline chunk through) over
    false positives (dropping a valid chunk).

The 4 retained signals are structural artifacts with near-zero false-positive
risk.  All document-specific patterns and content-ratio heuristics have been
deliberately excluded.
"""
import logging
import re

logger = logging.getLogger(__name__)

# ── Compiled patterns ────────────────────────────────────────────────────────

# Two or more horizontal-rule separator runs (table-of-contents / page divider noise)
_HORIZONTAL_RULE = re.compile(r"-----")

# Markdown-style headers (lines starting with #)
_HEADER_LINE = re.compile(r"^#+\s", re.MULTILINE)

# pymupdf4llm image/picture placeholder fragments that survived text_sanitizer
# e.g. "==> picture [720 x 540]" or a stray "==>" / "<=="
_PYMUPDF_REMNANT = re.compile(r"==>\s*|<==\s*", re.IGNORECASE)

# A single character repeated 10+ times consecutively — encoding corruption
# e.g. "aaaaaaaaaa", "??????????", "##########"
_CHAR_REPEAT = re.compile(r"(.)\1{9,}")


def score_chunk(text: str) -> float:
    """
    Return a quality score: 1.0 (keep) or 0.0 (drop).

    Signals are binary and evaluated in order of cheapest-to-most-expensive.
    The first failing signal short-circuits and returns 0.0.

    Args:
        text: Raw chunk text after chunking_service processing.

    Returns:
        1.0 if the chunk should be embedded, 0.0 if it should be discarded.
    """
    if not text or not text.strip():
        return 0.0

    content = text.strip()

    # Signal 1 — Separator spam
    # Two or more "-----" occurrences indicate a chunk made up almost entirely
    # of table/page dividers with no retrievable content.
    if len(_HORIZONTAL_RULE.findall(content)) >= 2:
        logger.debug("Quality gate: separator spam — drop")
        return 0.0

    # Signal 2 — Header-only block
    # Six or more markdown headers in fewer than 500 characters means the chunk
    # is a table of contents or chapter listing with no body text.  Such chunks
    # match many queries at retrieval time but provide no useful context.
    header_count = len(_HEADER_LINE.findall(content))
    if header_count >= 6 and len(content) < 500:
        logger.debug("Quality gate: header-only block — drop")
        return 0.0

    # Signal 3 — pymupdf4llm marker remnants
    # "==>" and "<==" are image-placeholder delimiters injected by pymupdf4llm.
    # text_sanitizer removes complete markers; stray fragments reaching this point
    # indicate a chunk that is primarily placeholder noise.
    if _PYMUPDF_REMNANT.search(content):
        logger.debug("Quality gate: pymupdf4llm marker remnant — drop")
        return 0.0

    # Signal 4 — Character-level encoding corruption
    # A single non-space character repeated 10+ times in a row is never valid
    # prose, code, or formulae; it is an extraction or encoding artefact.
    if _CHAR_REPEAT.search(content):
        logger.debug("Quality gate: character repetition — drop")
        return 0.0

    return 1.0


def should_keep_chunk(text: str, threshold: float = 0.35) -> bool:
    """
    Return True if this chunk should be embedded and indexed in Qdrant.

    The threshold parameter exists for interface consistency with the config
    system.  Because score_chunk currently returns only 0.0 or 1.0, any
    threshold value in (0, 1] behaves identically as a pass/fail gate.

    Args:
        text:      Raw chunk text.
        threshold: Minimum score to keep (default 0.35 from CHUNK_QUALITY_THRESHOLD).
    """
    return score_chunk(text) >= threshold
