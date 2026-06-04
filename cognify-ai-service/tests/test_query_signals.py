from app.services.query_signals import (
    chunks_imply_multi_part_synthesis,
    query_implies_broad_coverage,
    resolve_summary_intent,
)


def test_summarize_generations_uses_comprehensive_without_keyword_hack():
    query = "summarize the generation of computer"
    assert query_implies_broad_coverage(query)
    intent = resolve_summary_intent(query, [])
    assert intent == "summary_comprehensive"


def test_summarize_chapter_history_is_comprehensive():
    query = "summarize the history of networking"
    assert resolve_summary_intent(query, []) == "summary_comprehensive"


def test_brief_summary_stays_short():
    query = "briefly summarize the introduction"
    assert resolve_summary_intent(query, [{"content": "x " * 40}]) == "summary"


def test_plain_summarize_uses_chunk_structure():
    query = "summarize the main points"
    chunks = [
        {"content": "## Part 1\nFirst point"},
        {"content": "## Part 2\nSecond point"},
        {"content": "## Part 3\nThird point"},
    ]
    assert chunks_imply_multi_part_synthesis(chunks)
    assert resolve_summary_intent(query, chunks) == "summary_comprehensive"
