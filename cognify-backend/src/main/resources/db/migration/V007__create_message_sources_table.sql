-- Create message_sources table (links messages to document chunks for RAG citations)
CREATE TABLE message_sources (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    message_id UUID NOT NULL REFERENCES messages(id) ON DELETE CASCADE,
    chunk_id UUID NOT NULL REFERENCES chunks(id) ON DELETE CASCADE,
    relevance_score FLOAT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    UNIQUE(message_id, chunk_id)
);

CREATE INDEX idx_message_sources_message ON message_sources(message_id);
CREATE INDEX idx_message_sources_chunk ON message_sources(chunk_id);
CREATE INDEX idx_message_sources_score ON message_sources(relevance_score DESC);
