-- Create documents table
CREATE TABLE documents (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL CHECK (type IN ('PDF', 'DOCX', 'TXT')),
    size BIGINT NOT NULL,
    status VARCHAR(50) DEFAULT 'PROCESSING' NOT NULL CHECK (status IN ('PROCESSING', 'INDEXED', 'FAILED')),
    chunks_count INTEGER DEFAULT 0,
    storage_path TEXT NOT NULL,
    summary TEXT,
    tags TEXT[],
    upload_started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    indexed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX idx_documents_user ON documents(user_id);
CREATE INDEX idx_documents_status ON documents(status);
CREATE INDEX idx_documents_created_at ON documents(created_at DESC);
