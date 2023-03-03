package org.grego.springboot.recipeservice.service;

import org.grego.springboot.recipeservice.model.Recipe;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface IRecipeService {
    Flux<Recipe> getAllRecipes(long startPage, int pageSize);

    Mono<Long> getRecipeCount();

    Mono<Recipe> getRecipeById(long id);

    Mono<Recipe> addRecipe(Recipe recipe);

    Mono<Recipe> updateRecipe(Recipe recipe);

    Mono<Long> deleteRecipeById(long id);
}
