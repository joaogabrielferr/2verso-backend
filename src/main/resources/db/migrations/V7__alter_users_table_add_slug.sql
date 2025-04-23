ALTER TABLE articles
ADD COLUMN slug VARCHAR(250);


UPDATE articles
SET slug = id::VARCHAR(250)
WHERE slug IS NULL;


ALTER TABLE articles
ADD CONSTRAINT uk_article_slug UNIQUE (slug);


ALTER TABLE articles
ALTER COLUMN slug SET NOT NULL;