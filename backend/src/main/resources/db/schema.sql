CREATE EXTENSION IF NOT EXISTS vector;

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

CREATE TABLE IF NOT EXISTS chunk_embedding (
    id                  BIGSERIAL PRIMARY KEY,
    kb_id               BIGINT NOT NULL,
    document_id         BIGINT NOT NULL,
    chunk_id            BIGINT NOT NULL,
    embedding_model     VARCHAR(100) NOT NULL,
    embedding_dimension INT NOT NULL,
    embedding           vector,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS chat_session (
    id                  BIGSERIAL PRIMARY KEY,
    kb_id               BIGINT NOT NULL,
    title               VARCHAR(200),
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted             SMALLINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS chat_message (
    id                  BIGSERIAL PRIMARY KEY,
    session_id          BIGINT NOT NULL,
    kb_id               BIGINT NOT NULL,
    role                VARCHAR(30) NOT NULL,
    content             TEXT NOT NULL,
    source_chunks       TEXT,
    prompt_tokens       INT,
    completion_tokens   INT,
    latency_ms          BIGINT,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted             SMALLINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_document_kb_id ON document (kb_id);
CREATE INDEX IF NOT EXISTS idx_document_chunk_document_id ON document_chunk (document_id);
CREATE INDEX IF NOT EXISTS idx_document_chunk_kb_id ON document_chunk (kb_id);
CREATE UNIQUE INDEX IF NOT EXISTS uk_chunk_embedding_chunk_id ON chunk_embedding (chunk_id);
CREATE INDEX IF NOT EXISTS idx_chunk_embedding_kb_id ON chunk_embedding (kb_id);
CREATE INDEX IF NOT EXISTS idx_chat_session_kb_id ON chat_session (kb_id);
CREATE INDEX IF NOT EXISTS idx_chat_message_session_id ON chat_message (session_id);
