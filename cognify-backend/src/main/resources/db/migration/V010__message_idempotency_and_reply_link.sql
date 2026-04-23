ALTER TABLE messages
    ADD COLUMN client_request_id VARCHAR(100),
    ADD COLUMN reply_to_message_id UUID;

CREATE INDEX idx_messages_reply_to_message_id
    ON messages(reply_to_message_id);

CREATE UNIQUE INDEX uq_messages_conversation_user_request
    ON messages(conversation_id, sender, client_request_id)
    WHERE sender = 'USER' AND client_request_id IS NOT NULL;
