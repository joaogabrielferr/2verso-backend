package com.escritr.escritr.articles.service;

import com.escritr.escritr.articles.repository.ArticleRepository;
import com.escritr.escritr.articles.controller.DTOs.ArticlePostDTO;
import com.escritr.escritr.articles.controller.DTOs.ArticleResponseDTO;
import com.escritr.escritr.articles.controller.mappers.ArticleMapper;
import com.escritr.escritr.articles.model.Article;
import com.escritr.escritr.user.domain.User;
import com.escritr.escritr.user.repository.UserRepository;
import com.escritr.escritr.common.helpers.HtmlParser;
import com.escritr.escritr.exceptions.InternalServerErrorException;
import com.escritr.escritr.exceptions.ResourceNotFoundException;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class ArticleService {

    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]"); // Allow word chars and hyphen
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]+");
    private static final Pattern EDGES_HYPHEN = Pattern.compile("(^-|-$)");
    private static final Pattern CONSECUTIVE_HYPHENS = Pattern.compile("-{2,}");
    private static final int MAX_SLUG_ATTEMPTS = 100;

    private static final Logger log = LoggerFactory.getLogger(ArticleService.class);

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

        article.setContent(HtmlParser.cleanContent(article.getContent()));

        article.setAuthor(author);

        article.setFirstParagraph(HtmlParser.extractFirstParagraph(article.getContent()));

        String baseSlug = this.generateSlug(article.getTitle());
        String uniqueSlug = this.ensureUniqueSlug(baseSlug,null);
        article.setSlug(uniqueSlug);

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
        article.setContent(HtmlParser.cleanContent(dto.content()));
        article.setAuthor(author);
        article.setUpdatedAt(LocalDateTime.now());

        article.setFirstParagraph(HtmlParser.extractFirstParagraph(article.getContent()));

        Article savedArticle = articleRepository.save(article);
        return articleMapper.articleToResponseDTO(savedArticle);


    }

    public ArticleResponseDTO findBySlug(String slug){
        Article article = this.articleRepository.findBySlug(slug).orElseThrow(()-> new ResourceNotFoundException("No article found with slug:" + slug));
        return articleMapper.articleToResponseDTO(article);
    }

    public void delete(UUID id){

        Article article = this.articleRepository.findById(id)
                .orElseThrow(()-> new ResourceNotFoundException("No article found with id:" + id));

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
