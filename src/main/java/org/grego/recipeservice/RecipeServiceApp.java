/**
 * Recipe Service.
 * Copyright: none
 *
 * @author Greg-O
 */
package org.grego.recipeservice;

import io.r2dbc.spi.ConnectionFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.r2dbc.connection.init.ConnectionFactoryInitializer;
import org.springframework.r2dbc.connection.init.ResourceDatabasePopulator;
import org.springframework.web.reactive.config.EnableWebFlux;

/**
 * Recipe service application.
 */
@SpringBootApplication
@EnableWebFlux
@EnableR2dbcRepositories
@EnableAutoConfiguration(exclude = {WebMvcAutoConfiguration.class})
public class RecipeServiceApp {

    /**
     * Main function.
     * @param args
     */
    public static void main(final String[] args) {
        SpringApplication.run(RecipeServiceApp.class, args);
    }

    /**
     * Define bean for connection factory initializer.
     * @param connectionFactory
     * @return an initializer used to configure connections
     */
    @Bean
    ConnectionFactoryInitializer initializer(final ConnectionFactory connectionFactory) {

        ConnectionFactoryInitializer initializer = new ConnectionFactoryInitializer();
        initializer.setConnectionFactory(connectionFactory);
        initializer.setDatabasePopulator(new ResourceDatabasePopulator(new ClassPathResource("schema.sql")));

        return initializer;
    }
}
