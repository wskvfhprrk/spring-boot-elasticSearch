package com.hejz.springbootelasticsearch;

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
}
