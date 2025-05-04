CREATE TABLE refresh_token (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    token VARCHAR(255) NOT NULL UNIQUE,
    expiry_date TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_user
        FOREIGN KEY(user_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);


CREATE INDEX idx_refresh_token_token ON refresh_token(token);