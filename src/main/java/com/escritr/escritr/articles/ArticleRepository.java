package com.escritr.escritr.articles;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNullApi;
import org.springframework.stereotype.Repository;


import java.util.UUID;

@Repository
public interface ArticleRepository extends JpaRepository<Article, UUID>{


//    @Query("""
//            SELECT a FROM article a
//            LEFT JOIN FETCH a.author
//            ORDER BY a.createdAt DESC
//            """)
//    Page<Article> findRecentArticles(Pageable pageable);

    Page<Article> findByAuthorUsername(String username, Pageable pageable);



}
