-- Spring Session tables for OAuth2/JWT session management
-- Required for distributed session storage across multiple instances
-- As specified in PRD Section 27 - Authentication & Session Management

CREATE TABLE IF NOT EXISTS spring_session (
    primary_id            CHAR(36) NOT NULL,
    session_id            CHAR(36) NOT NULL,
    creation_time         BIGINT NOT NULL,
    last_access_time      BIGINT NOT NULL,
    max_inactive_interval INT NOT NULL,
    expiry_time           BIGINT NOT NULL,
    principal_name        VARCHAR(100),
    CONSTRAINT spring_session_pk PRIMARY KEY (primary_id)
);

CREATE UNIQUE INDEX IF NOT EXISTS spring_session_ix1 ON spring_session (session_id);
CREATE INDEX IF NOT EXISTS spring_session_ix2 ON spring_session (expiry_time);
CREATE INDEX IF NOT EXISTS spring_session_ix3 ON spring_session (principal_name);

CREATE TABLE IF NOT EXISTS spring_session_attributes (
    session_primary_id CHAR(36) NOT NULL,
    attribute_name     VARCHAR(200) NOT NULL,
    attribute_bytes    BYTEA NOT NULL,
    CONSTRAINT spring_session_attributes_pk PRIMARY KEY (session_primary_id, attribute_name),
    CONSTRAINT spring_session_attributes_fk FOREIGN KEY (session_primary_id) REFERENCES spring_session(primary_id) ON DELETE CASCADE
);

COMMENT ON TABLE spring_session IS 'Stores HTTP session data for Spring Session JDBC backend';
COMMENT ON TABLE spring_session_attributes IS 'Stores session attribute data keyed by session ID';
COMMENT ON INDEX spring_session_ix1 IS 'Fast lookup by session ID';
COMMENT ON INDEX spring_session_ix2 IS 'Efficient expiry query for session cleanup';
COMMENT ON INDEX spring_session_ix3 IS 'Principal-based session lookup for user management';