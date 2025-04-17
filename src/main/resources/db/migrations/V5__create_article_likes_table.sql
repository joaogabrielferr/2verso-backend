CREATE TABLE article_likes(
    user_id UUID REFERENCES users(id),
    article_id UUID REFERENCES articles(id),
    PRIMARY KEY (user_id,article_id),
    liked_at TIMESTAMP DEFAULT now()
);