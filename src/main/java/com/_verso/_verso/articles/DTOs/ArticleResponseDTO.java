package com._verso._verso.articles.DTOs;


import java.util.UUID;

public record ArticleResponseDTO(
        UUID id,
        String title,
        String subtitle,
        String content,
        String firstParagraph,
        String thumbnail,
        String createdAt,
        String updatedAt,
        AuthorResponseDTO author) { }
