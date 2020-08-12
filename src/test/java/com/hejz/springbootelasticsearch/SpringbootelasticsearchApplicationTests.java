package com.hejz.springbootelasticsearch;

import com.hejz.springbootelasticsearch.repository.ArticleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;

import java.util.Optional;

import static java.util.Arrays.asList;
import static org.elasticsearch.index.query.QueryBuilders.matchQuery;
import static org.elasticsearch.index.query.QueryBuilders.regexpQuery;

@SpringBootTest
class SpringbootelasticsearchApplicationTests {

    @Autowired
    ElasticsearchRestTemplate elasticsearchTemplate;
    @Autowired
    ArticleRepository articleRepository;

    @Test
    void contextLoads() {
    }

    @Test
    public void create() {
        //通过模版方式建立索引会报错——索引名称是随机的驼峰字符串
        elasticsearchTemplate.indexOps(Article.class).create();
    }

    @Test
    public void delete() {
        elasticsearchTemplate.indexOps(Article.class).delete();
    }

    @Test
    public void save() {
        Article article = new Article("Spring Data Elasticsearch");
        article.setAuthors(asList(new Author("John"), new Author("Doe")));
        articleRepository.save(article);
    }
    @Test
    public void update(){
        //为了更新文档，我们首先必须检索它
        String articleTitle = "Spring Data Elasticsearch";
        NativeSearchQuery searchQuery = new NativeSearchQueryBuilder()
                .withQuery(matchQuery("title", articleTitle).minimumShouldMatch("75%"))
                .build();

        SearchHits<Article> articles =
                elasticsearchTemplate.search(searchQuery, Article.class, IndexCoordinates.of("blog"));
        Article article = articles.getSearchHit(0).getContent();
        System.out.println(article);
        //我们可以仅通过使用其评估器编辑对象的内容来对文档进行更改
        article.setTitle("Getting started with Search Engines");
        articleRepository.save(article);
        Optional<Article> byId = articleRepository.findById(article.getId());
        System.out.println(byId);
    }

    @Test
    public void deleteById(){
        //为了更新文档，我们首先必须检索它
        String articleTitle = "Spring Data Elasticsearch";
        NativeSearchQuery searchQuery = new NativeSearchQueryBuilder()
                .withQuery(matchQuery("title", articleTitle).minimumShouldMatch("75%"))
                .build();

        SearchHits<Article> articles =
                elasticsearchTemplate.search(searchQuery, Article.class, IndexCoordinates.of("blog"));
        Article article = articles.getSearchHit(0).getContent();
        System.out.println(article);
        articleRepository.deleteByTitle(article.getTitle());
        Optional<Article> byTitle = articleRepository.findByTitle(article.getTitle());
        System.out.println(byTitle);
    }


    @Test
    public void select() {
        String nameToFind = "John Smith";
        Page<Article> articleByAuthorName
                = articleRepository.findByAuthorsName(nameToFind, PageRequest.of(0, 10));
        articleByAuthorName.forEach((s) -> {
            System.out.println(s);
        });
    }

    @Test
    public void select2() {
        String name = "John";
        Page<Article> byAuthorsNameUsingCustomQuery = articleRepository.findByAuthorsNameUsingCustomQuery(name, PageRequest.of(0, 20));
        byAuthorsNameUsingCustomQuery.forEach((s) -> {
            System.out.println(s);
        });
    }
    @Test
    public void select3(){
        //另一个选择是使用查询生成器来创建我们的自定义查询。
        //必须搜索标题中带有“ data ” 一词的文章，我们只需创建一个在标题上带有Filter 的NativeSearchQueryBuilder即可
        NativeSearchQuery searchQuery = new NativeSearchQueryBuilder()
                .withFilter(regexpQuery("title", ".*data.*"))
                .build();
        SearchHits<Article> articles =
                elasticsearchTemplate.search(searchQuery, Article.class, IndexCoordinates.of("blog"));
//        System.out.println(articles);
        articles.forEach((s)->{
            System.out.println(s);
        });
    }
}
