/**
 * Recipe Service.
 * Copyright: none
 *
 * @author Greg-O
 */
package org.grego.recipeservice.service;

import co.elastic.clients.elasticsearch.core.search.ResponseBody;
import org.grego.recipeservice.document.RecipeDoc;
import org.grego.recipeservice.model.Recipe;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Interface for recipe service.
 */
public interface IRecipeService {
    /**
     * Get all recipes.
     * @param startPage
     * @param pageSize
     * @return All the recipes for the page
     */
    Flux<Recipe> getAllRecipes(long startPage, int pageSize);

    /**
     * Get the number of recipes.
     * @return The number of recipes
     */
    Mono<Long> getRecipeCount();

    /**
     * Get recipe by recipe id.
     * @param recipeId
     * @return Recipe for the recipe id
     */
    Mono<Recipe> getRecipeById(long recipeId);

    /**
     * Add a recipe.
     * @param recipe
     * @return Recipe that was saved
     */
    Mono<Recipe> addRecipe(Recipe recipe);

    /**
     * Update a recipe.
     * @param recipe
     * @return Recipe that was updated
     */
    Mono<Recipe> updateRecipe(Recipe recipe);

    /**
     * Delete recipe by recipe id.
     * @param recipeId
     * @return Id of the deleted recipe
     */
    Mono<Long> deleteRecipeById(long recipeId);

    /**
     * Search for recipes by search string.
     * @param searchString
     * @return Search results with RecipeDocs for the search string
     */
    Mono<ResponseBody<RecipeDoc>> searchRecipes(String searchString);
}
