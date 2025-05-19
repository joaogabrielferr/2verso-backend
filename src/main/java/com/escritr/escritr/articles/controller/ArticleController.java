package com.escritr.escritr.articles.controller;

import com.escritr.escritr.articles.service.ArticleService;
import com.escritr.escritr.articles.controller.DTOs.ArticlePostDTO;
import com.escritr.escritr.articles.controller.DTOs.ArticleResponseDTO;
import com.escritr.escritr.aws.s3.S3Service;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("api/articles")
public class ArticleController {

    private final ArticleService articleService;
    private final S3Service s3Service;

    ArticleController(
        ArticleService articleService,
        S3Service s3Service
    ){
        this.articleService = articleService;
        this.s3Service = s3Service;
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

    @PostMapping("/image")
    public ResponseEntity<?> uploadImage(@RequestParam("file")MultipartFile file){
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "File is empty"));
        }
        String fileUrl = s3Service.uploadFile(file);
        return ResponseEntity.ok(Map.of("url", fileUrl));
    }


    @DeleteMapping("/image")
    public ResponseEntity<?> deleteImageByUrl(@RequestParam("url") String fileUrl) {
        if (fileUrl == null || fileUrl.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "File URL is required."));
        }
        String fileKey = s3Service.extractKeyFromUrl(fileUrl);
        System.out.println(fileKey);
        if (fileKey == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Could not extract S3 key from URL."));
        }
        try {
            s3Service.deleteFile(fileKey);
            return ResponseEntity.ok(Map.of("message", "File deleted successfully from URL: " + fileUrl));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to delete file: " + e.getMessage()));
        }
    }






}
