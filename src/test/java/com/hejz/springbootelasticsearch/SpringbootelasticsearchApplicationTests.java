package com.hejz.springbootelasticsearch;

import com.hejz.springbootelasticsearch.repository.ArticleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;

import static java.util.Arrays.asList;

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
    public void create(){
        //通过模版方式建立索引会报错——索引名称是随机的驼峰字符串
        elasticsearchTemplate.indexOps(Article.class).create();
    }
    @Test
    public void delete(){
        elasticsearchTemplate.indexOps(Article.class).delete();
    }

    @Test
    public void save() {
        Article article = new Article("Spring Data Elasticsearch");
        article.setAuthors(asList(new Author("John"), new Author("Doe")));
        articleRepository.save(article);
    }

    @Test
    public void select() {
        String nameToFind = "John Smith";
        Page<Article> articleByAuthorName
                = articleRepository.findByAuthorsName(nameToFind, PageRequest.of(0, 10));
        articleByAuthorName.forEach((s)->{
            System.out.println(s);
        });
    }

    @Test
    public void select2(){
        String name="John";
        Page<Article> byAuthorsNameUsingCustomQuery = articleRepository.findByAuthorsNameUsingCustomQuery(name, PageRequest.of(0, 20));
        byAuthorsNameUsingCustomQuery.forEach((s)->{
            System.out.println(s);
        });
    }
}
