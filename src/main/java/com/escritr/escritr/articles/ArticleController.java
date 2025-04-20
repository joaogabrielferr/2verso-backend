package com.escritr.escritr.articles;

import com.escritr.escritr.articles.DTOs.ArticlePostDTO;
import com.escritr.escritr.articles.DTOs.ArticleResponseDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
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
    public ResponseEntity<ArrayList<ArticleResponseDTO>> list(){
        ArrayList<ArticleResponseDTO> articles = articleService.list();
        return ResponseEntity.ok(articles);
    }
    @GetMapping("/{id}")
    public ResponseEntity<ArticleResponseDTO> find(@PathVariable UUID id){

        ArticleResponseDTO article = articleService.find(id);
        return ResponseEntity.ok(article);

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




}
