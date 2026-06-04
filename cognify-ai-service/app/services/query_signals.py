"""
Topic-agnostic signals derived from the user query and retrieved chunks.
Used for retrieval ordering, response length, and synthesis style — not hardcoded topics.
"""
from __future__ import annotations

import re
from typing import Dict, List, Sequence

_SUMMARY_BRIEF = re.compile(
    r"\b(in short|briefly|tl;dr|one paragraph|few sentences|short summary|quick summary)\b",
    re.IGNORECASE,
)
_SUMMARY_REQUEST = re.compile(r"\b(summarize|summary)\b", re.IGNORECASE)

# Broad scope: all/each, "summarize the X of Y", or structure words (history, phases, chapters, …)
_BROAD_COVERAGE = re.compile(
    r"\b(all|each|every|entire|whole|complete|full|overview of|outline of|list of)\b"
    r"|(?:summarize|summary of)\s+the\s+\w+(?:\s+\w+){0,3}\s+of\b"
    r"|\b(history|timeline|chronolog|evolution|phases?|stages?|eras?|versions?|"
    r"chapters?|sections?|steps?|types|kinds|categories|periods|components|parts|"
    r"aspects|sequence|progression|development)\b",
    re.IGNORECASE,
)

_READING_ORDER = re.compile(
    r"\b(history|timeline|chronolog|evolution|phases?|stages?|eras?|versions?|"
    r"chapters?|sections?|steps?|sequence|progression|development of|over time|"
    r"from .+ to|first|second|third|fourth|fifth)\b"
    r"|(?:summarize|summary of)\s+the\s+\w+(?:\s+\w+){0,3}\s+of\b",
    re.IGNORECASE,
)

_TOPIC_STOPWORDS = frozenset({
    "the", "and", "for", "with", "that", "this", "from", "what", "when", "where",
    "which", "would", "could", "should", "about", "your", "their", "they", "them",
    "were", "been", "has", "have", "but", "into", "there", "here", "then", "than",
    "also", "only", "just", "will", "dont", "does", "doesnt", "did", "didnt", "cant",
    "cannot", "its", "you", "are", "was", "who", "why", "how", "can", "summarize",
    "summary", "briefly", "short", "explain", "describe", "tell", "give", "please",
})


def topic_terms_from_query(query: str) -> List[str]:
    if not query:
        return []
    return [
        word
        for word in re.findall(r"\w+", query.lower())
        if len(word) > 3 and word not in _TOPIC_STOPWORDS
    ]


def query_requests_summary(query: str) -> bool:
    return bool(query and _SUMMARY_REQUEST.search(query))


def query_requests_brief_summary(query: str) -> bool:
    return bool(query and _SUMMARY_BRIEF.search(query))


def query_implies_broad_coverage(query: str) -> bool:
    return bool(query and _BROAD_COVERAGE.search(query))


def query_prefers_reading_order(query: str) -> bool:
    if not query:
        return False
    if _READING_ORDER.search(query):
        return True
    return query_requests_summary(query) and query_implies_broad_coverage(query)


def combined_chunk_text(chunks: Sequence[Dict[str, str]]) -> str:
    return "\n".join((chunk.get("content") or "") for chunk in chunks)


def chunks_imply_multi_part_synthesis(chunks: Sequence[Dict[str, str]]) -> bool:
    if len(chunks) >= 4:
        return True

    text = combined_chunk_text(chunks)
    if not text.strip():
        return False

    headers = len(re.findall(r"^#+\s", text, re.MULTILINE))
    numbered = len(re.findall(r"(?:^|\n)\s*\d+[\.\)]\s+", text))
    ordinals = len(
        re.findall(
            r"\b(first|second|third|fourth|fifth|sixth|1st|2nd|3rd|4th|5th)\b",
            text,
            re.IGNORECASE,
        )
    )
    return headers >= 2 or numbered >= 3 or ordinals >= 2


def resolve_summary_intent(query: str, chunks: Sequence[Dict[str, str]]) -> str:
    """
    summary          -> short answer (explicit brief wording only)
    summary_comprehensive -> cover all major parts found in evidence
    """
    if not query_requests_summary(query):
        return "semantic-search"

    if query_requests_brief_summary(query):
        return "summary"

    if query_implies_broad_coverage(query) or chunks_imply_multi_part_synthesis(chunks):
        return "summary_comprehensive"

    return "summary"


def document_focus_score(
    combined_text: str,
    topic_terms: List[str],
    chunk_count: int,
    best_similarity: float,
) -> tuple:
    lowered = combined_text.lower()
    term_hits = sum(lowered.count(term) for term in topic_terms) if topic_terms else 0
    structure = len(re.findall(r"^#+\s", combined_text, re.MULTILINE))
    structure += len(re.findall(r"(?:^|\n)\s*\d+[\.\)]\s+", combined_text))
    return (term_hits, structure, chunk_count, best_similarity)
