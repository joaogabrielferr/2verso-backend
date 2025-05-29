CREATE TABLE escritr.user_account_links (
    id UUID PRIMARY KEY UNIQUE NOT NULL,
    user_id UUID NOT NULL,
    provider_name VARCHAR(50) NOT NULL,
    provider_user_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_account_links_user FOREIGN KEY (user_id) REFERENCES escritr.users(id) ON DELETE CASCADE,
    CONSTRAINT uq_provider_user UNIQUE (provider_name, provider_user_id)
);

CREATE INDEX idx_user_account_links_user_id ON escritr.user_account_links(user_id);
CREATE INDEX idx_user_account_links_provider ON escritr.user_account_links(provider_name, provider_user_id);