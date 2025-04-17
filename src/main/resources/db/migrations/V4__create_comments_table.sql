CREATE TABLE comments(
    id UUID PRIMARY KEY,
    user_id UUID REFERENCES users(id),
    article_id UUID REFERENCES articles(id),
    content TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT now()
);