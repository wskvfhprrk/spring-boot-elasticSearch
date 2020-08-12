package com.hejz.springbootelasticsearch;

import lombok.Builder;
import lombok.Data;

/**
 * @author: hejz
 * @Description:
 * @Date: 2020/8/11 10:30
 */
@Data
@Builder
public class Author {
    private String name;

    public Author(){}
    public Author(String author) {
        this.name = author;
    }
}
