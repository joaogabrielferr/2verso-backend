package com._verso._verso.articles.mappers;

import com._verso._verso.articles.Article;
import com._verso._verso.articles.DTOs.ArticlePostDTO;
import com._verso._verso.articles.DTOs.ArticleResponseDTO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring",uses = {AuthorMapper.class})
public interface ArticleMapper {

    Article articlePostDTOtoArticle(ArticlePostDTO dto);

    ArticleResponseDTO articleToResponseDTO(Article article);

}
