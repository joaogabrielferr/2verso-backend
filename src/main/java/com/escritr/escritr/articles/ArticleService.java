package com.escritr.escritr.articles;

import com.escritr.escritr.articles.DTOs.ArticlePostDTO;
import com.escritr.escritr.articles.DTOs.ArticleResponseDTO;
import com.escritr.escritr.articles.mappers.ArticleMapper;
import com.escritr.escritr.auth.model.User;
import com.escritr.escritr.auth.repository.UserRepository;
import com.escritr.escritr.common.HtmlParser;
import com.escritr.escritr.exceptions.BadRequestException;
import com.escritr.escritr.exceptions.InternalServerErrorException;
import com.escritr.escritr.exceptions.ResourceNotFoundException;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.UUID;

@Service
public class ArticleService {

    private final ArticleRepository articleRepository;

    private final UserRepository userRepository;
    private final ArticleMapper articleMapper;

    ArticleService(
            ArticleRepository articleRepository,
            ArticleMapper articleMapper,
            UserRepository userRepository
            ){
        this.articleRepository = articleRepository;
        this.userRepository = userRepository;
        this.articleMapper = articleMapper;
    }

    public ArticleResponseDTO create(@Valid ArticlePostDTO articlePostDTO){


        User author = this.userRepository.findByEmailOrUsername(articlePostDTO.authorUsername(),articlePostDTO.authorUsername()).orElseThrow(
                ()-> new ResourceNotFoundException("User not found with username:" + articlePostDTO.authorUsername())
        );

        Article article = articleMapper.articlePostDTOtoArticle(articlePostDTO);

        if(article == null){
            throw new InternalServerErrorException("Failed to map dto to entity");
        }

        article.setAuthor(author);

        article.setFirstParagraph(HtmlParser.extractFirstParagraph(article.getContent()));


        Article savedArticle = articleRepository.save(article);
        return articleMapper.articleToResponseDTO(savedArticle);
    }



    public Page<ArticleResponseDTO> list(int page, int size){

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Article> articles =  this.articleRepository.findAll(pageable);

        return articles.map(articleMapper::articleToResponseDTO);
    }

    public ArticleResponseDTO find(UUID id){

        Article article = this.articleRepository.findById(id).orElseThrow(()-> new ResourceNotFoundException("no article with id:" + id));
        return articleMapper.articleToResponseDTO(article);
    }

    public Page<ArticleResponseDTO> getArticlesByUsername(String username, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Article> articles = articleRepository.findByAuthorUsername(username, pageable);
        return articles.map(articleMapper::articleToResponseDTO);
    }

    public ArticleResponseDTO update(UUID id, @Valid ArticlePostDTO dto){

        User author = this.userRepository.findByEmailOrUsername(dto.authorUsername(),dto.authorUsername()).orElseThrow(
                ()-> new ResourceNotFoundException("No user found with username:" + dto.authorUsername())
        );

        Article article = this.articleRepository.findById(id).orElseThrow(()-> new ResourceNotFoundException("No article found with id:" + id));

        article.setTitle(dto.title());
        article.setSubtitle(dto.subtitle());
        article.setContent(dto.content());
        article.setAuthor(author);
        article.setUpdatedAt(LocalDateTime.now());

        article.setFirstParagraph(HtmlParser.extractFirstParagraph(article.getContent()));

        Article savedArticle = articleRepository.save(article);
        return articleMapper.articleToResponseDTO(savedArticle);


    }

    public void delete(UUID id){

        Article article = this.articleRepository.findById(id)
                .orElseThrow(()-> new ResourceNotFoundException("No article found with id:" + id));

        this.articleRepository.delete(article);
    }

    public String extractFirstParagraph(String html){
        if(html == null || html.isBlank())return "";

        return "";

    }







}
