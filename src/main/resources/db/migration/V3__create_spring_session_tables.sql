"-- Spring Session tables for OAuth2/JWT session management\n" +
"-- Required for distributed session storage across multiple instances\n" +
"-- As specified in PRD Section 27 - Authentication & Session Management\n\n" +
"CREATE TABLE IF NOT EXISTS spring_session (\n" +
"    primary_id            CHAR(36) NOT NULL,\n" +
"    session_id            CHAR(36) NOT NULL,\n" +
"    creation_time         BIGINT NOT NULL,\n" +
"    last_access_time      BIGINT NOT NULL,\n" +
"    max_inactive_interval INT NOT NULL,\n" +
"    expiry_time           BIGINT NOT NULL,\n" +
"    principal_name        VARCHAR(100),\n" +
"    CONSTRAINT spring_session_pk PRIMARY KEY (primary_id)\n" +
");\n\n" +
"CREATE UNIQUE INDEX IF NOT EXISTS spring_session_ix1 ON spring_session (session_id);\n" +
"CREATE INDEX IF NOT EXISTS spring_session_ix2 ON spring_session (expiry_time);\n" +
"CREATE INDEX IF NOT EXISTS spring_session_ix3 ON spring_session (principal_name);\n\n" +
"CREATE TABLE IF NOT EXISTS spring_session_attributes (\n" +
"    session_primary_id CHAR(36) NOT NULL,\n" +
"    attribute_name     VARCHAR(200) NOT NULL,\n" +
"    attribute_bytes    BYTEA NOT NULL,\n" +
"    CONSTRAINT spring_session_attributes_pk PRIMARY KEY (session_primary_id, attribute_name),\n" +
"    CONSTRAINT spring_session_attributes_fk FOREIGN KEY (session_primary_id) REFERENCES spring_session(primary_id) ON DELETE CASCADE\n" +
");\n\n" +
"COMMENT ON TABLE spring_session IS 'Stores HTTP session data for Spring Session JDBC backend';\n" +
"COMMENT ON TABLE spring_session_attributes IS 'Stores session attribute data keyed by session ID';\n" +
"COMMENT ON INDEX spring_session_ix1 IS 'Fast lookup by session ID';\n" +
"COMMENT ON INDEX spring_session_ix2 IS 'Efficient expiry query for session cleanup';\n" +
"COMMENT ON INDEX spring_session_ix3 IS 'Principal-based session lookup for user management';\n"