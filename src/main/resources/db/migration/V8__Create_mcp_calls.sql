-- Audit trail of MCP tool calls (Dashboard > MCP Activity); arguments and responses are stored redacted and truncated
CREATE TABLE IF NOT EXISTS mcp_calls (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    tool VARCHAR(100) NOT NULL,
    arguments VARCHAR(2000),
    result_ok BOOLEAN NOT NULL,
    response_excerpt VARCHAR(2000),
    duration_ms INTEGER,
    client_name VARCHAR(100),
    token_suffix VARCHAR(8),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_mcp_calls_user_created ON mcp_calls(user_id, created_at);
