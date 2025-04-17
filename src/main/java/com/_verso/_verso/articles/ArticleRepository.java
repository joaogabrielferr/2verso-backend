package com._verso._verso.articles;


import org.springframework.data.jpa.repository.JpaRepository;
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

}
