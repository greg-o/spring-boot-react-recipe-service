package org.grego.springboot.recipeservice;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ReactiveElasticsearchClient;
import org.springframework.data.elasticsearch.client.elc.ReactiveElasticsearchTemplate;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchClients;
import org.springframework.data.elasticsearch.core.convert.MappingElasticsearchConverter;
import org.springframework.data.elasticsearch.core.mapping.SimpleElasticsearchMappingContext;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.data.elasticsearch.support.HttpHeaders;

import java.time.Duration;

@Configuration
@EnableElasticsearchRepositories(basePackages = "org.grego.springboot.recipeservice.repository")
public class ElasticsearchConfig {
    public static final String ELASTICSEARCH_HEADERS = "application/vnd.elasticsearch+json;compatible-with=7";
    @Value("${spring.data.elasticsearch.client.reactive.endpoints}")
    private String endpoints;
    @Value("${spring.data.elasticsearch.client.reactive.username}")
    private String username;
    @Value("${spring.data.elasticsearch.client.reactive.password}")
    private String password;


    @Bean
    public ReactiveElasticsearchClient client() throws Exception {
        HttpHeaders compatibilityHeaders = new HttpHeaders();
        compatibilityHeaders.add(javax.ws.rs.core.HttpHeaders.ACCEPT, ELASTICSEARCH_HEADERS);
        compatibilityHeaders.add(javax.ws.rs.core.HttpHeaders.CONTENT_TYPE, ELASTICSEARCH_HEADERS);

        final ClientConfiguration clientConfiguration =
            ClientConfiguration
                .builder()
                .connectedTo(endpoints)
                .usingSsl()
                .withConnectTimeout(Duration.ofSeconds(5))
                .withSocketTimeout(Duration.ofSeconds(3))
                .withBasicAuth(username, password)
                .withDefaultHeaders(compatibilityHeaders)
                .build();

        return ElasticsearchClients.createReactive(clientConfiguration);
    }

    @Bean("reactiveElasticsearchTemplate")
    public ReactiveElasticsearchTemplate elasticsearchTemplate() throws Exception {
        return new ReactiveElasticsearchTemplate(client(), new MappingElasticsearchConverter(new SimpleElasticsearchMappingContext()));
    }
}
