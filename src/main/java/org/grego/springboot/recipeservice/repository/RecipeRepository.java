package org.grego.springboot.recipeservice.repository;

import org.grego.springboot.recipeservice.model.Recipe;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public interface RecipeRepository {
    Flux<Recipe> findAllByName(String name);
    Flux<Recipe> findAll();

    Mono<Boolean> existsById(long recpieId);
    Mono<Recipe> findById(long recipeId);
    Mono<Recipe> save(Recipe recipe);
    Mono<Void> deleteById(long recipeId);
}
