/**
 * Recipe Service.
 * Copyright: none
 *
 * @author Greg-O
 */
package org.grego.recipeservice;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchClients;
import org.springframework.data.elasticsearch.client.elc.ReactiveElasticsearchClient;
import org.springframework.data.elasticsearch.client.elc.ReactiveElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.convert.MappingElasticsearchConverter;
import org.springframework.data.elasticsearch.core.mapping.SimpleElasticsearchMappingContext;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.data.elasticsearch.support.HttpHeaders;

import java.time.Duration;

/**
 * Configuration for Elasticsearch.
 */
@Configuration
@EnableElasticsearchRepositories(basePackages = "org.grego.springboot.recipeservice.repository")
public class ElasticsearchConfig {
    /**
     * HTTP header for Elasticsearch.
     */
    public static final String ELASTICSEARCH_HEADERS = "application/vnd.elasticsearch+json;compatible-with=7";

    /**
     * Elasticsearch endpoint.
     */
    @Value("${spring.data.elasticsearch.client.reactive.endpoints}")
    private String endpoints;

    /**
     * Elasticsearch user name.
     */
    @Value("${spring.data.elasticsearch.client.reactive.username}")
    private String username;

    /**
     * Elasticsearch connection timeout milliseconds.
     */
    @Value("${connection.timeout.ms:5}")
    private String connectionTimeoutMs;
    /**
     * Elasticsearch socket timeout milliseconds.
     */
    @Value("${socket.timeout.ms:3}")
    private String socketTimeoutMs;

    /**
     * Elasticsearch password.
     */
    @Value("${spring.data.elasticsearch.client.reactive.password}")
    private String password;

    /**
     * Define bean for reactive Elasticsearch client.
     * @return The reactive Elasticsearch client
     * @throws Exception
     */
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
                        .withConnectTimeout(Duration.ofMillis(Integer.parseInt(connectionTimeoutMs)))
                        .withSocketTimeout(Duration.ofMillis(Integer.parseInt(socketTimeoutMs)))
                        .withBasicAuth(username, password)
                        .withDefaultHeaders(compatibilityHeaders)
                        .build();

        return ElasticsearchClients.createReactive(clientConfiguration);
    }

    /**
     * Bean definition for reactive Elasticsearch template.
     * @return The Reactive Elasticsearch template
     * @throws Exception
     */
    @Bean("reactiveElasticsearchTemplate")
    public ReactiveElasticsearchTemplate elasticsearchTemplate() throws Exception {
        return new ReactiveElasticsearchTemplate(client(),
                new MappingElasticsearchConverter(new SimpleElasticsearchMappingContext()));
    }
}
