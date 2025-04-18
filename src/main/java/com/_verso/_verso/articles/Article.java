package com._verso._verso.articles;

import com._verso._verso.auth.model.User;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "articles",schema = "twoverso")
public class Article{

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "author_id")
    private User author;

    private String title;

    @Lob
    private String subtitle;

    @Lob
    private String content;

    @Lob
    private String firstParagraph;

    @Lob
    private String thumbnailUrl;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public Article() {
    }

    public Article(User author, String title, String subtitle, String content, String firstParagraph, String thumbNailUrl, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.author = author;
        this.title = title;
        this.subtitle = subtitle;
        this.content = content;
        this.firstParagraph = firstParagraph;
        this.thumbnailUrl = thumbNailUrl;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @Override
    public String toString(){
        return "id:" + getId() + ", title:" + getTitle();
    }


    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public User getAuthor() {
        return author;
    }

    public void setAuthor(User author) {
        this.author = author;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getFirstParagraph() {
        return firstParagraph;
    }

    public void setFirstParagraph(String firstParagraph) {
        this.firstParagraph = firstParagraph;
    }

    public String getThumbNailUrl() {
        return thumbnailUrl;
    }

    public void setThumbNailUrl(String thumbNailUrl) {
        this.thumbnailUrl = thumbNailUrl;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Article article)) return false;
        return Objects.equals(getAuthor(), article.getAuthor()) && Objects.equals(getTitle(), article.getTitle()) && Objects.equals(getSubtitle(), article.getSubtitle()) && Objects.equals(getContent(), article.getContent()) && Objects.equals(getFirstParagraph(), article.getFirstParagraph()) && Objects.equals(getThumbNailUrl(), article.getThumbNailUrl()) && Objects.equals(getCreatedAt(), article.getCreatedAt()) && Objects.equals(getUpdatedAt(), article.getUpdatedAt());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getAuthor(), getTitle(), getSubtitle(), getContent(), getFirstParagraph(), getThumbNailUrl(), getCreatedAt(), getUpdatedAt());
    }
}
