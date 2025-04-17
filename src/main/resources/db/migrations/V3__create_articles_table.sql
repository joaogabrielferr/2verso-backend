CREATE TABLE ARTICLES(
    id UUID PRIMARY KEY,
    author_id UUID REFERENCES users(id),
    title VARCHAR(200) NOT NULL,
    subtitle TEXT,
    content TEXT NOT NULL,
    first_paragraph TEXT NOT NULL,
    thumbnail_url TEXT,
    created_at TIMESTAMP DEFAULT now(),
    updated_at TIMESTAMP DEFAULT now()
);