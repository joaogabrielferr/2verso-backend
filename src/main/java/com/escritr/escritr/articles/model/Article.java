package com.escritr.escritr.articles.model;

import com.escritr.escritr.user.domain.User;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "articles",schema = "escritr")
public class Article{

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "author_id")
    private User author;
    private String title;
    private String subtitle;
    private String content;
    private String firstParagraph;
    private String thumbnailUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private String slug;

    public Article() {
    }

    public Article(User author, String title, String subtitle, String content, String firstParagraph, String thumbNailUrl, LocalDateTime createdAt, LocalDateTime updatedAt,String slug) {
        this.author = author;
        this.title = title;
        this.subtitle = subtitle;
        this.content = content;
        this.firstParagraph = firstParagraph;
        this.thumbnailUrl = thumbNailUrl;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.slug = slug;
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

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
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

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Article article)) return false;
        return Objects.equals(getAuthor(), article.getAuthor()) && Objects.equals(getTitle(), article.getTitle()) && Objects.equals(getSubtitle(), article.getSubtitle()) && Objects.equals(getContent(), article.getContent()) && Objects.equals(getFirstParagraph(), article.getFirstParagraph()) && Objects.equals(getThumbnailUrl(), article.getThumbnailUrl()) && Objects.equals(getCreatedAt(), article.getCreatedAt()) && Objects.equals(getUpdatedAt(), article.getUpdatedAt());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getAuthor(), getTitle(), getSubtitle(), getContent(), getFirstParagraph(), getThumbnailUrl(), getCreatedAt(), getUpdatedAt());
    }
}
