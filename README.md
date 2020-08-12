# spring boot elasticsearch测试

## 1、docker安装elashticsearch

**尽管Elasticsearch可以使用几乎没有定义的架构，但通常的做法是设计一个并创建映射以指定某些字段中期望的数据类型**。对文档建立索引后，将根据其类型来处理其字段。例如，将根据映射规则对文本字段进行标记和过滤。我们还可以创建自己的过滤器和令牌生成器。

为了简单起见，我们将为我们的Elasticsearch实例使用一个docker映像，尽管**侦听端口9200的任何Elasticsearch实例都可以**。

我们首先启动Elasticsearch实例：

创建用户定义的网络（用于连接到连接到同一网络的其他服务（例如，Kibana））

```shell
docker network create somenetwork
```

```shell
docker run -d --name elasticsearch --net somenetwork -p 9200:9200 -e "discovery.type=single-node" elasticsearch:7.6.2
```

安装kibana

```
docker run -d --name kibana --net somenetwork -p 5601:5601 kibana:7.6.2
```



## 2、Maven依赖

```xml
<dependency>
    <groupId>org.springframework.data</groupId>
    <artifactId>spring-data-elasticsearch</artifactId>
    <version>4.0.0.RELEASE</version>
</dependency>
```

## 3、定义存储库接口

为了定义新的存储库，我们扩展了提供的存储库接口之一，用我们的实际文档和主键类型替换了通用类型。

重要的是要注意，*ElasticsearchRepository是*从*PagingAndSortingRepository*扩展的*。*这允许对分页和排序的内置支持。

在我们的示例中，我们将在自定义搜索方法中使用分页功能：

```java
public interface ArticleRepository extends ElasticsearchRepository<Article, String> {
 
    Page<Article> findByAuthorsName(String name, Pageable pageable);
 
    @Query("{\"bool\": {\"must\": [{\"match\": {\"authors.name\": \"?0\"}}]}}")
    Page<Article> findByAuthorsNameUsingCustomQuery(String name, Pageable pageable);
}
```

使用*findByAuthorsName*方法，存储库代理将基于方法名称创建一个实现。解析算法将确定需要访问*authors*属性，然后搜索每个项目的*name*属性。

第二种方法*findByAuthorsNameUsingCustomQuery*使用自定义的Elasticsearch布尔查询，该查询使用*@Query*注释定义，该查询要求作者的姓名和提供的*name*参数之间严格匹配。

## 4、Java配置

在我们的Java应用程序中配置Elasticsearch时，我们需要定义如何连接到Elasticsearch实例。为此，我们使用由Elasticsearch依赖项提供的  *RestHighLevelClient*：

```java
@Configuration
@EnableElasticsearchRepositories(basePackages = "com.baeldung.spring.data.es.repository")
@ComponentScan(basePackages = { "com.baeldung.spring.data.es.service" })
public class Config {
 
    @Bean
    public RestHighLevelClient client() {
        ClientConfiguration clientConfiguration 
            = ClientConfiguration.builder()
                .connectedTo("localhost:9200")
                .build();
 
        return RestClients.create(clientConfiguration).rest();
    }
 
    @Bean
    public ElasticsearchOperations elasticsearchTemplate() {
        return new ElasticsearchRestTemplate(client());
    }
}
```

我们正在使用标准的支持Spring的样式注释。*@EnableElasticsearchRepositories*将使Spring Data Elasticsearch扫描提供的Spring Data存储库包。

为了与我们的Elasticsearch服务器通信，我们使用一个简单的*RestHighLevelClient**。*尽管Elasticsearch提供了多种类型的客户端，但是使用  *RestHighLevelClient*是将来与服务器进行通信的一种好方法。

最后，我们设置了一个*ElasticsearchOperations* bean来在我们的服务器上执行操作。在这种情况下，我们实例化一个*ElasticsearchRestTemplate*。

## 5、映射

映射用于为我们的文档定义架构。通过为文档定义模式，我们可以保护它们免受不良后果的影响，例如映射到我们不希望的类型。

我们的实体是一个名为*Article*的简单文档，其中*id*为*String*类型。我们还指定了此类文档必须存储在*文章*类型内名为*blog*的索引中。

```java
@Document(indexName = "blog", type = "article")
public class Article {
 
    @Id
    private String id;
    
    private String title;
    
    @Field(type = FieldType.Nested, includeInParent = true)
    private List<Author> authors;
    
    // standard getters and setters
}
```

索引可以有几种类型。我们可以使用该功能来实现层次结构。

该*作者*字段被标记为*FieldType.Nested*。这使我们可以分别定义*Author*类，但是当在Elasticsearch中对其进行索引时，可以在*Article*文档中嵌入author的各个实例。

## 6、索引文件

Spring Data Elasticsearch通常基于项目中的实体自动创建索引。但是，我们也可以通过客户端模板以编程方式创建索引：

```java
elasticsearchTemplate.indexOps(Article.class).create();
```

  **使用上一种方法创建索引会报错误（第8点介绍)**，但可以删除索引：

```java
elasticsearchTemplate.indexOps(Article.class).delete();
```

然后，我们可以将文档添加到索引：

```java
Article article = new Article("Spring Data Elasticsearch");
article.setAuthors(asList(new Author("John Smith"), new Author("John Doe")));
articleRepository.save(article);
```

## 7、查询

### 7.1 基于方法名称的查询

当使用基于方法名称的查询时，我们将编写定义要执行的查询的方法。在设置过程中，Spring Data将解析方法签名并相应地创建查询：

```java
String nameToFind = "John Smith";
Page<Article> articleByAuthorName
  = articleRepository.findByAuthorsName(nameToFind, PageRequest.of(0, 10));
```

通过使用*PageRequest*对象调用*findByAuthorsName*，我们获得结果的第一页（页码从零开始），该页最多包含10条文章。页面对象还提供查询的总点击数以及其他方便的分页信息。

### 7.2 自定义查询

有两种方法可以为Spring Data Elasticsearch存储库定义自定义查询。一种方法是使用*@Query*注释，如3节所示。

另一个选择是使用查询生成器来创建我们的自定义查询。

必须搜索标题中带有“ *data* ” 一词的文章，我们只需创建一个在*标题*上带有Filter 的*NativeSearchQueryBuilder即可**：*

```java
Query searchQuery = new NativeSearchQueryBuilder()
   .withFilter(regexpQuery("title", ".*data.*"))
   .build();
SearchHits<Article> articles = 
   elasticsearchTemplate.search(searchQuery, Article.class, IndexCoordinates.of("blog");
```

## 8、更新和删除

为了更新文档，我们首先必须检索它：

```java
String articleTitle = "Spring Data Elasticsearch";
Query searchQuery = new NativeSearchQueryBuilder()
  .withQuery(matchQuery("title", articleTitle).minimumShouldMatch("75%"))
  .build();
 
SearchHits<Article> articles = 
   elasticsearchTemplate.search(searchQuery, Article.class, IndexCoordinates.of("blog");
Article article = articles.getSearchHit(0).getContent();
```

然后，我们可以仅通过使用其评估器编辑对象的内容来对文档进行更改：

```java
article.setTitle("Getting started with Search Engines");
articleRepository.save(article);
```

至于删除，有几种选择。我们可以检索文档并使用*delete*方法将其*删除*：

```java
articleRepository.delete(article);
```

我们也可以通过已知的*ID*将其删除：

```java
articleRepository.deleteById("article_id");
```

还可以创建自定义*deleteBy*查询并利用Elasticsearch提供的批量删除功能：

```java
articleRepository.deleteByTitle("title");
```



## 9、遇到问题

### 建立索引出现的问题

```java
org.springframework.data.elasticsearch.UncategorizedElasticsearchException: Elasticsearch exception [type=resource_already_exists_exception
```

这个问题是由于建立elasticsearch索引要使用下划线，不能使用驼峰建立命名。

## 10、kibana的dev tools命令

- 查询所有索引：GET _cat/indices?v_
- _删除索引：DELETE blog_
- 查询某一索引所有值：GET blog/_search

## 11、使用建造者模式@Builder修改映射类，使用建造者模式save对象

在映射类上加入`lombok`中的`@Builder`标签，同用全部对象的构造方法，使之成为建造类，在保存数据时使用：

```java
articleRepository.save(Article.builder().title("java8实战").authors(asList(new Author("Rose"),new Author("Luwar"))).build());
articleRepository.save(Article.builder().title("jdk8实战").authors(asList(new Author("Rose1"),new Author("Luwar1"))).build());
```

查询所有值并打印出来：

```java
 Iterable<Article> all = articleRepository.findAll();
        all.forEach((s)->{
            System.out.println(s);
        });
```

