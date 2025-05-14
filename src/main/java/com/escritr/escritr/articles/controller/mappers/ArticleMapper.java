package com.escritr.escritr.articles.controller.mappers;

import com.escritr.escritr.articles.model.Article;
import com.escritr.escritr.articles.controller.DTOs.ArticlePostDTO;
import com.escritr.escritr.articles.controller.DTOs.ArticleResponseDTO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring",uses = {AuthorMapper.class})
public interface ArticleMapper {

    Article articlePostDTOtoArticle(ArticlePostDTO dto);

    ArticleResponseDTO articleToResponseDTO(Article article);

}
