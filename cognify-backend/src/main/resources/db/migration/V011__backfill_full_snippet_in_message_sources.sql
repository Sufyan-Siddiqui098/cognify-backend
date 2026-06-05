-- V011: Backfill full chunk content into message_sources.snippet.
--
-- Context: V009 originally populated snippet with a 200-char truncation.
-- The Java saveMessageSources() now stores full content (snippet column is TEXT, no limit).
-- This migration backfills existing rows so historical messages show full evidence content.
--
-- Safe to run: only updates rows where chunk still exists (FK not null).
-- Rows where chunk was deleted (FK is NULL) keep their existing truncated snippet.

UPDATE message_sources ms
SET snippet = c.content
FROM chunks c
WHERE ms.chunk_id = c.id
  AND c.content IS NOT NULL
  AND c.content != ''
  AND (
      ms.snippet IS NULL
      OR ms.snippet != c.content
  );
