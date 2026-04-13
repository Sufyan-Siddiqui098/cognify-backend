-- Preserve message source history even if chunks are deleted during reindex/document delete.
ALTER TABLE message_sources
    ADD COLUMN IF NOT EXISTS document_id UUID,
    ADD COLUMN IF NOT EXISTS document_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS page_number INTEGER,
    ADD COLUMN IF NOT EXISTS snippet TEXT;

-- Backfill existing rows while chunk records still exist.
UPDATE message_sources ms
SET document_id = c.document_id,
    page_number = c.page_number,
    snippet = CASE
        WHEN c.content IS NULL THEN NULL
        WHEN length(c.content) > 200 THEN substring(c.content from 1 for 200) || '...'
        ELSE c.content
    END
FROM chunks c
WHERE ms.chunk_id = c.id
  AND (ms.document_id IS NULL OR ms.page_number IS NULL OR ms.snippet IS NULL);

UPDATE message_sources ms
SET document_name = d.name
FROM documents d
WHERE ms.document_id = d.id
  AND ms.document_name IS NULL;

DO $$
DECLARE
    fk_name text;
BEGIN
    SELECT con.conname
    INTO fk_name
    FROM pg_constraint con
    JOIN pg_class rel ON rel.oid = con.conrelid
    JOIN pg_namespace nsp ON nsp.oid = rel.relnamespace
    JOIN pg_attribute att ON att.attrelid = rel.oid AND att.attnum = ANY(con.conkey)
    WHERE con.contype = 'f'
      AND nsp.nspname = 'public'
      AND rel.relname = 'message_sources'
      AND att.attname = 'chunk_id'
    LIMIT 1;

    IF fk_name IS NOT NULL THEN
        EXECUTE format('ALTER TABLE message_sources DROP CONSTRAINT %I', fk_name);
    END IF;
END $$;

ALTER TABLE message_sources
    ALTER COLUMN chunk_id DROP NOT NULL;

ALTER TABLE message_sources
    ADD CONSTRAINT fk_message_sources_chunk
    FOREIGN KEY (chunk_id) REFERENCES chunks(id) ON DELETE SET NULL;
