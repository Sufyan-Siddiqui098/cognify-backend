-- Add soft-delete support for documents
ALTER TABLE documents
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;

-- Expand status check constraint to include DELETED
ALTER TABLE documents DROP CONSTRAINT IF EXISTS documents_status_check;
ALTER TABLE documents
    ADD CONSTRAINT documents_status_check
        CHECK (status IN ('PROCESSING', 'INDEXED', 'FAILED', 'DELETED'));

CREATE INDEX IF NOT EXISTS idx_documents_deleted_at ON documents(deleted_at);
