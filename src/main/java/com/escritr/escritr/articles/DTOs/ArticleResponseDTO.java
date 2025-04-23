package com.escritr.escritr.articles.DTOs;


import java.util.UUID;

public record ArticleResponseDTO(
        UUID id,
        String title,
        String subtitle,
        String content,
        String firstParagraph,
        String thumbnailUrl,
        String slug,
        String createdAt,
        String updatedAt,
        AuthorResponseDTO author) { }
