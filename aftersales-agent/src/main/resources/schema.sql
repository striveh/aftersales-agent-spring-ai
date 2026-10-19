CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS hstore;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS policy_fts (
    id varchar(64) PRIMARY KEY,
    doc_id varchar(64) NOT NULL,
    doc_title text NOT NULL,
    section_id varchar(128) NOT NULL,
    section_title text NOT NULL,
    content text NOT NULL,
    content_tokens text NOT NULL,
    content_tsv tsvector GENERATED ALWAYS AS (to_tsvector('simple', coalesce(content_tokens, ''))) STORED
);

CREATE INDEX IF NOT EXISTS policy_fts_tsv_idx ON policy_fts USING GIN (content_tsv);
CREATE INDEX IF NOT EXISTS policy_fts_doc_idx ON policy_fts (doc_id);
