package com.escritr.escritr.articles.mappers;

import com.escritr.escritr.articles.Article;
import com.escritr.escritr.articles.DTOs.ArticlePostDTO;
import com.escritr.escritr.articles.DTOs.ArticleResponseDTO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring",uses = {AuthorMapper.class})
public interface ArticleMapper {

    Article articlePostDTOtoArticle(ArticlePostDTO dto);

    ArticleResponseDTO articleToResponseDTO(Article article);

}
