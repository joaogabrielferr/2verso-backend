package com._verso._verso.articles;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/articles")
public class ArticleController {

    @GetMapping()
    public ResponseEntity teste() {

        return ResponseEntity.ok("ok");
    }
    @GetMapping("teste")
    public ResponseEntity teste2(){
        return ResponseEntity.ok("ok!!!!!!!");
    }

}
