package com._verso._verso.articles;

import com._verso._verso.articles.DTOs.ArticlePostDTO;
import com._verso._verso.articles.DTOs.ArticleResponseDTO;
import com._verso._verso.articles.mappers.ArticleMapper;
import com._verso._verso.auth.model.User;
import com._verso._verso.auth.repository.UserRepository;
import com._verso._verso.common.ErrorAssetEnum;
import com._verso._verso.common.ErrorMessage;
import com._verso._verso.exceptions.ResourceNotFoundException;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.DeleteMapping;

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

        try{
            User author = this.userRepository.findById(articlePostDTO.authorId()).orElseThrow(
                    ()-> new ResourceNotFoundException("User not found with id:" + articlePostDTO.authorId())
            );

            Article article = articleMapper.articlePostDTOtoArticle(articlePostDTO);
            article.setAuthor(author);

            //TODO: extract first paragraph from content
            article.setFirstParagraph(article.getContent());

            Article savedArticle = articleRepository.save(article);

            return articleMapper.articleToResponseDTO(savedArticle);
        }catch(Exception e){
            throw new RuntimeException("an internal error occurred.");
        }


    }


    public ArrayList<ArticleResponseDTO> list(){

        ArrayList<Article> articles = (ArrayList<Article>) this.articleRepository.findAll();

        ArrayList<ArticleResponseDTO> DTOs = new ArrayList<>();

        articles.forEach(a -> {
            DTOs.add(articleMapper.articleToResponseDTO(a));
        });

        return DTOs;
    }

    public ArticleResponseDTO find(UUID id){

        Article article = this.articleRepository.findById(id).orElseThrow(()-> new ResourceNotFoundException("no article with id:" + id));
        return articleMapper.articleToResponseDTO(article);
    }

    public ArticleResponseDTO update(UUID id, @Valid ArticlePostDTO dto){

        try{
            User author = this.userRepository.findById(dto.authorId()).orElseThrow(
                    ()-> new ResourceNotFoundException("No user found with id:" + dto.authorId())
            );

            Article article = this.articleRepository.findById(id).orElseThrow(()-> new ResourceNotFoundException("No article found with id:" + id));

            article.setTitle(dto.title());
            article.setSubtitle(dto.subtitle());
            article.setContent(dto.content());
            article.setAuthor(author);
            article.setUpdatedAt(LocalDateTime.now());

            //TODO: extract first paragraph from content
            article.setFirstParagraph(dto.content());
            Article savedArticle = articleRepository.save(article);
            return articleMapper.articleToResponseDTO(savedArticle);
        }catch(Exception e){
            throw new RuntimeException("an internal error occurred.");
        }

    }

    public void delete(UUID id){

        Article article = this.articleRepository.findById(id)
                .orElseThrow(()-> new ResourceNotFoundException("No article found with id:" + id));

        this.articleRepository.delete(article);
    }







}
