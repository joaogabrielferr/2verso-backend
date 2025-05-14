package com.escritr.escritr.articles.controller;

import com.escritr.escritr.articles.service.ArticleService;
import com.escritr.escritr.articles.controller.DTOs.ArticlePostDTO;
import com.escritr.escritr.articles.controller.DTOs.ArticleResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("api/articles")
public class ArticleController {

    private final ArticleService articleService;

    ArticleController(
        ArticleService articleService
    ){
        this.articleService = articleService;
    }


    @PostMapping()
    public ResponseEntity<ArticleResponseDTO> create(@RequestBody ArticlePostDTO articlePostDto){

        ArticleResponseDTO response = this.articleService.create(articlePostDto);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @GetMapping()
    public ResponseEntity<Page<ArticleResponseDTO>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ){
        Page<ArticleResponseDTO> articles = articleService.list(page,size);
        return ResponseEntity.ok(articles);
    }
    @GetMapping("/{id}")
    public ResponseEntity<ArticleResponseDTO> find(@PathVariable UUID id){

        ArticleResponseDTO article = articleService.find(id);
        return ResponseEntity.ok(article);

    }

    @GetMapping("/user/{username}")
    public ResponseEntity<Page<ArticleResponseDTO>> findArticlesOfUser(
            @PathVariable String username,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size){
        Page<ArticleResponseDTO> articles = articleService.getArticlesByUsername(username,page,size);
        return ResponseEntity.ok(articles);
    }


    @PutMapping("/{id}")
    public ResponseEntity<ArticleResponseDTO> update(@PathVariable UUID id, @RequestBody ArticlePostDTO dto){

        ArticleResponseDTO article = articleService.update(id,dto);
        return ResponseEntity.ok(article);

    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Boolean> delete(@PathVariable UUID id){
        this.articleService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/slug/{slug}")
    public ResponseEntity<ArticleResponseDTO> findBySlug(@PathVariable String slug){

        ArticleResponseDTO article = this.articleService.findBySlug(slug);
        return ResponseEntity.ok(article);
    }




}
