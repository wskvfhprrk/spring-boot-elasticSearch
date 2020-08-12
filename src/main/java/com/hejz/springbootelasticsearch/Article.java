package com.hejz.springbootelasticsearch;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.List;

/**
 * @author: hejz
 * @Description:
 * @Date: 2020/8/11 10:29
 */
@Data
@Builder
@Document(indexName = "blog")
public class Article {
    @Id
    private String id;
    private String title;
    @Field(type = FieldType.Nested, includeInParent = true)
    private List<Author> authors;

    public Article(){}

    public Article( String title) {
        this.title = title;
    }

    public Article(String title, List<Author> authors) {
        this.title = title;
        this.authors = authors;
    }

    public Article(String id, String title, List<Author> authors) {
        this.id = id;
        this.title = title;
        this.authors = authors;
    }
}
