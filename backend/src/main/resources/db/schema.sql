CREATE TABLE IF NOT EXISTS knowledge_base (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    description     VARCHAR(500),
    status          VARCHAR(30) NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         SMALLINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS document (
    id              BIGSERIAL PRIMARY KEY,
    kb_id           BIGINT NOT NULL,
    file_name       VARCHAR(255) NOT NULL,
    file_type       VARCHAR(30) NOT NULL,
    file_size       BIGINT,
    storage_path    VARCHAR(500),
    content_hash    VARCHAR(64),
    status          VARCHAR(30) NOT NULL,
    error_message   TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         SMALLINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS document_chunk (
    id              BIGSERIAL PRIMARY KEY,
    kb_id           BIGINT NOT NULL,
    document_id     BIGINT NOT NULL,
    chunk_index     INT NOT NULL,
    title_path      VARCHAR(500),
    content         TEXT NOT NULL,
    token_count     INT,
    content_hash    VARCHAR(64),
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         SMALLINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_document_kb_id ON document (kb_id);
CREATE INDEX IF NOT EXISTS idx_document_chunk_document_id ON document_chunk (document_id);
