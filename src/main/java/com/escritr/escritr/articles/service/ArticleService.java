package com.escritr.escritr.articles.service;

import com.escritr.escritr.articles.repository.ArticleRepository;
import com.escritr.escritr.articles.controller.DTOs.ArticlePostDTO;
import com.escritr.escritr.articles.controller.DTOs.ArticleResponseDTO;
import com.escritr.escritr.articles.controller.mappers.ArticleMapper;
import com.escritr.escritr.articles.model.Article;
import com.escritr.escritr.auth.model.UserDetailsImpl;
import com.escritr.escritr.common.enums.ErrorAssetEnum;
import com.escritr.escritr.common.enums.ErrorCodeEnum;
import com.escritr.escritr.exceptions.AuthenticationTokenException;
import com.escritr.escritr.exceptions.WrongParameterException;
import com.escritr.escritr.user.domain.User;
import com.escritr.escritr.user.repository.UserRepository;
import com.escritr.escritr.common.helpers.HtmlParser;
import com.escritr.escritr.exceptions.InternalServerErrorException;
import com.escritr.escritr.exceptions.ResourceNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.client.RestTemplate;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class ArticleService {

    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]"); // Allow word chars and hyphen
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]+");
    private static final Pattern EDGES_HYPHEN = Pattern.compile("(^-|-$)");
    private static final Pattern CONSECUTIVE_HYPHENS = Pattern.compile("-{2,}");
    private static final int MAX_SLUG_ATTEMPTS = 100;
    private static final int MAX_TITLE_SIZE = 300;
    private static final int MAX_SUBTITLE_SIZE = 500;

    private static final Logger log = LoggerFactory.getLogger(ArticleService.class);

    @Value("${nextjs.revalidation.url}")
    private String nextjsRevalidationUrl;

    @Value("${nextjs.revalidation.secret}")
    private String nextjsRevalidationSecret;


    private final ArticleRepository articleRepository;

    private final UserRepository userRepository;
    private final ArticleMapper articleMapper;
    private final ObjectMapper objectMapper;


    ArticleService(
            ArticleRepository articleRepository,
            ArticleMapper articleMapper,
            UserRepository userRepository,
            ObjectMapper objectMapper
            ){
        this.articleRepository = articleRepository;
        this.userRepository = userRepository;
        this.articleMapper = articleMapper;
        this.objectMapper = objectMapper;
    }

    public ArticleResponseDTO create(@Valid ArticlePostDTO articlePostDTO, Authentication authentication){

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AuthenticationTokenException("Not authorized",ErrorAssetEnum.ARTICLE,ErrorCodeEnum.INVALID_CREDENTIALS);
        }

        String authenticatedUsername = extractUsername(authentication);

        log.info("creating article->user from dto:{}, user from token:{}",articlePostDTO.authorUsername(),authenticatedUsername);

        if(!articlePostDTO.authorUsername().equals(authenticatedUsername)){
            throw new AuthenticationTokenException("Not authorized",ErrorAssetEnum.ARTICLE,ErrorCodeEnum.INVALID_CREDENTIALS);
        }

        if(articlePostDTO.title().length() > MAX_TITLE_SIZE){
            throw new WrongParameterException("title is too long", ErrorAssetEnum.ARTICLE, ErrorCodeEnum.INPUT_FORMAT_ERROR);
        }

        if(articlePostDTO.subtitle() != null && !articlePostDTO.subtitle().isEmpty() && articlePostDTO.subtitle().length() > MAX_SUBTITLE_SIZE){
            throw new WrongParameterException("subtitle is too long",ErrorAssetEnum.ARTICLE, ErrorCodeEnum.INPUT_FORMAT_ERROR);
        }


        User author = this.userRepository.findByEmailOrUsername(articlePostDTO.authorUsername(),articlePostDTO.authorUsername()).orElseThrow(
                ()-> new ResourceNotFoundException("User not found with username:" + articlePostDTO.authorUsername())
        );

        try{
            log.debug("before mapping article");
            Article article = articleMapper.articlePostDTOtoArticle(articlePostDTO);
            log.debug("after mapping article");

            if(article == null){
                log.debug("Failed to map dto to entity");
                throw new InternalServerErrorException("An internal error have occurred");
            }

            article.setTitle(HtmlParser.cleanNormalText(article.getTitle()));

            if(article.getSubtitle() != null){
                article.setSubtitle(HtmlParser.cleanNormalText(article.getSubtitle()));
            }


            article.setContent(HtmlParser.cleanContent(article.getContent()));

            article.setAuthor(author);

            article.setFirstParagraph(HtmlParser.extractFirstParagraph(article.getContent()));

            String baseSlug = this.generateSlug(article.getTitle());
            String uniqueSlug = this.ensureUniqueSlug(baseSlug,null);
            article.setSlug(uniqueSlug);

            Article savedArticle = articleRepository.save(article);

            return articleMapper.articleToResponseDTO(savedArticle);

        }catch(Exception ex){
            log.debug(ex.getMessage());
            throw new RuntimeException();
        }

    }

    private String extractUsername(Authentication authentication){
        String authenticatedUsername;
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails) {
            authenticatedUsername = ((UserDetails) principal).getUsername();
        } else if (principal instanceof String) {
            authenticatedUsername = (String) principal;
        } else {
            throw new InternalServerErrorException("Invalid user information");
        }
        return authenticatedUsername;
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

    public ArticleResponseDTO update(UUID id, @Valid ArticlePostDTO dto,Authentication authentication){

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AuthenticationTokenException("Not authorized",ErrorAssetEnum.ARTICLE,ErrorCodeEnum.INVALID_CREDENTIALS);
        }

        String authenticatedUsername = extractUsername(authentication);

        if(dto.title().length() > MAX_TITLE_SIZE){
            throw new WrongParameterException("title is too long", ErrorAssetEnum.ARTICLE, ErrorCodeEnum.INPUT_FORMAT_ERROR);
        }

        if(dto.subtitle() != null && !dto.subtitle().isEmpty() && dto.subtitle().length() > MAX_SUBTITLE_SIZE){
            throw new WrongParameterException("subtitle is too long",ErrorAssetEnum.ARTICLE, ErrorCodeEnum.INPUT_FORMAT_ERROR);
        }

        User author = this.userRepository.findByEmailOrUsername(dto.authorUsername(),dto.authorUsername()).orElseThrow(
                ()-> new ResourceNotFoundException("No user found with username:" + dto.authorUsername())
        );

        Article article = this.articleRepository.findById(id).orElseThrow(()-> new ResourceNotFoundException("No article found with id:" + id));

        log.info("updating article->user from dto:{}, user from token:{}",dto.authorUsername(),authenticatedUsername);

        if(!article.getAuthor().getUsername().equals(authenticatedUsername)){
            throw new AuthenticationTokenException("Not authorized",ErrorAssetEnum.ARTICLE,ErrorCodeEnum.INVALID_CREDENTIALS);
        }

        try{
        article.setTitle(HtmlParser.cleanNormalText(dto.title()));

        if(dto.subtitle() != null){
            article.setSubtitle(HtmlParser.cleanNormalText(dto.subtitle()));
        }

        article.setContent(HtmlParser.cleanContent(dto.content()));
        article.setAuthor(author);
        article.setUpdatedAt(LocalDateTime.now());

        if(dto.thumbnailUrl() != null){
            article.setThumbnailUrl(dto.thumbnailUrl());
        }

        article.setFirstParagraph(HtmlParser.extractFirstParagraph(article.getContent()));
        log.info("saving content:{}",article.getContent());
        Article savedArticle = articleRepository.save(article);

        triggerNextJsRevalidation(savedArticle.getSlug(),authenticatedUsername);

        return articleMapper.articleToResponseDTO(savedArticle);

        }catch(Exception ex){
            log.debug(ex.getMessage());
            throw new RuntimeException();
        }



    }

    public void triggerNextJsRevalidation(String articleSlug,String username) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-revalidation-secret", nextjsRevalidationSecret);


        String requestBodyJson;
        try {
            //JSON object using Jackson
            Map<String, Object> requestBodyMap = new HashMap<>();
            requestBodyMap.put("articleSlug", articleSlug);
            if (username != null && !username.trim().isEmpty()) {
                requestBodyMap.put("username", username);
            }
            requestBodyJson = objectMapper.writeValueAsString(requestBodyMap);
        } catch (Exception e) {
            log.error("Error creating JSON request body for revalidation (slug: {}, username: {}): {}",
                    articleSlug, username, e.getMessage(), e);
            return;
        }


        HttpEntity<String> entity = new HttpEntity<>(requestBodyJson, headers);

        try {
            restTemplate.exchange(nextjsRevalidationUrl, HttpMethod.POST, entity, String.class);
            log.info("Successfully triggered revalidation for slug: {}",articleSlug);
        } catch (Exception e) {
            log.error("Error triggering Next.js revalidation for slug {} : {}",articleSlug,e.getMessage());
        }
    }

    public ArticleResponseDTO findBySlug(String slug){
        Article article = this.articleRepository.findBySlug(slug).orElseThrow(()-> new ResourceNotFoundException("No article found with slug:" + slug));
        return articleMapper.articleToResponseDTO(article);
    }

    public void delete(UUID id,Authentication authentication){


        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AuthenticationTokenException("Not authorized",ErrorAssetEnum.ARTICLE,ErrorCodeEnum.INVALID_CREDENTIALS);
        }

        String authenticatedUsername = extractUsername(authentication);


        Article article = this.articleRepository.findById(id)
                .orElseThrow(()-> new ResourceNotFoundException("No article found with id:" + id));


        if(!article.getAuthor().getUsername().equals(authenticatedUsername)){
            throw new AuthenticationTokenException("Not authorized",ErrorAssetEnum.ARTICLE,ErrorCodeEnum.INVALID_CREDENTIALS);
        }

        log.info("deleting article->user from dto:{}, user from token:{}",article.getAuthor().getUsername(),authenticatedUsername);
        this.articleRepository.delete(article);
    }

    private String generateSlug(String title){
        if(title == null)return "";

        String nowhitespace = WHITESPACE.matcher(title.trim()).replaceAll("-");
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD); // Separate accents
        String slug = NONLATIN.matcher(normalized).replaceAll(""); // Remove non-word/non-hyphen
        slug = slug.toLowerCase(Locale.ENGLISH);
        slug = CONSECUTIVE_HYPHENS.matcher(slug).replaceAll("-"); // Replace multiple hyphens
        slug = EDGES_HYPHEN.matcher(slug).replaceAll(""); // Trim leading/trailing hyphens
        //Truncate if too long
         int maxLength = 240;
         if (slug.length() > maxLength) {
             slug = slug.substring(0, maxLength);
             // Ensure it doesn't end mid-word or with hyphen if possible
             slug = slug.replaceAll("-$", "");
         }
        return slug.isBlank() ? "article" : slug ; // Default if title was only special chars


    }

    private String ensureUniqueSlug(String baseSlug, UUID articleIdToExcludeFromCheck) {
        String candidateSlug = baseSlug;

        for (int attempt = 0; attempt < MAX_SLUG_ATTEMPTS; attempt++) {
            if (attempt > 0) {
                candidateSlug = baseSlug + "-" + attempt;
            }

            log.debug("Attempt {}: Checking uniqueness for slug '{}'", attempt + 1, candidateSlug);
            Optional<Article> existing = articleRepository.findBySlug(candidateSlug);

            if (existing.isEmpty() || (articleIdToExcludeFromCheck != null && existing.get().getId().equals(articleIdToExcludeFromCheck))) {
                log.info("Unique slug found: '{}'", candidateSlug);
                return candidateSlug;
            }
        }

        log.warn("Could not generate a unique slug for base '{}' after {} attempts.", baseSlug, MAX_SLUG_ATTEMPTS);
        // generate a slug with a random suffix here as a fallback,
        return baseSlug + "-" + UUID.randomUUID().toString().substring(0,8);
    }








}
