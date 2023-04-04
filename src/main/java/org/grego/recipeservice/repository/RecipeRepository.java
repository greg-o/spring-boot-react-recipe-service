/**
 * Recipe Service.
 * Copyright: none
 *
 * @author Greg-O
 */
package org.grego.recipeservice.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.grego.recipeservice.model.Recipe;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.r2dbc.core.ReactiveUpdateOperation;
import org.springframework.data.relational.core.query.Query;
import org.springframework.data.relational.core.query.Update;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

import static org.springframework.data.relational.core.query.Criteria.where;
import static org.springframework.data.relational.core.query.Query.query;

/**
 * RecipeRepository for managing Recipe objects in the database.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RecipeRepository {
    /**
     * Reactive database template used to query the database.
     */
    private final R2dbcEntityTemplate template;

    /**
     * Find all recipes by name.
     * @param name
     * @return Recipes that match the name
     */
    public Flux<Recipe> findAllByName(final String name) {
        return template
                .select(Recipe.class)
                .from(Recipe.RECIPES_TABLE_NAME)
                .matching(query(where(Recipe.NAME_COLUMN_NAME).is(name)))
                .all();
    }

    /**
     * Find all recipes.
     * @param startPage
     * @param pageSize
     * @return All recipes for the given page
     */
    public Flux<Recipe> findAll(final long startPage, final int pageSize) {
        var query = Query.empty();

        if (startPage > 0 && pageSize > 0) {
            query = query.offset((startPage - 1) * pageSize).limit(pageSize);
        }

        return template
                .select(Recipe.class)
                .from(Recipe.RECIPES_TABLE_NAME)
                .matching(query)
                .all();
    }

    /**
     * Get the number of all recipes.
     * @return The number of recipes
     */
    public Mono<Long> countAll() {
        return template
                .select(Recipe.class)
                .from(Recipe.RECIPES_TABLE_NAME)
                .count();
    }

    /**
     * Determine if a recipe with the recipe id exists.
     * @param recipeId
     * @return If a Recipe exists for the recipe id
     */
    public Mono<Boolean> existsById(final long recipeId) {
        return template
                .select(Recipe.class)
                .from(Recipe.RECIPES_TABLE_NAME)
                .matching(query(where(Recipe.RECIPE_ID_COLUMN_NAME).is(recipeId)))
                .exists();
    }

    /**
     * Get recipe by recipe id.
     * @param recipeId
     * @return Recipe for the recipe id
     */
    public Mono<Recipe> findById(final long recipeId) {
        return template
                .select(Recipe.class)
                .from(Recipe.RECIPES_TABLE_NAME)
                .matching(query(where(Recipe.RECIPE_ID_COLUMN_NAME).is(recipeId)))
                .one();
    }

    /**
     * Save recipe.
     * @param recipe
     * @return The saved Recipe
     */
    public Mono<Recipe> save(final Recipe recipe) {
        return template
                .insert(Recipe.class)
                .into(Recipe.RECIPES_TABLE_NAME)
                .using(recipe);
    }

    /**
     * Update recipe.
     * @param recipe
     * @return Id of the saved recipe
     */
    public Mono<Long> update(final Recipe recipe) {
        return template
                .update(ReactiveUpdateOperation.UpdateWithTable.class)
                .inTable(Recipe.RECIPES_TABLE_NAME)
                .matching(query(where(Recipe.RECIPE_ID_COLUMN_NAME).is(recipe.getRecipeId())))
                .apply(Update.update(Recipe.NAME_COLUMN_NAME, recipe.getName())
                        .set(Recipe.DESCRIPTION_COLUMN_NAME, recipe.getDescription())
                        .set(Recipe.LAST_MODIFIED_DATE_TIME_COLUMN_NAME, LocalDateTime.now()));
    }

    /**
     * Delete recipe by recipe id.
     * @param recipeId
     * @return Id of the deleted recipe
     */
    public Mono<Long> deleteById(final long recipeId) {
        return template
                .delete(Recipe.class)
                .from(Recipe.RECIPES_TABLE_NAME)
                .matching(query(where(Recipe.RECIPE_ID_COLUMN_NAME).is(recipeId)))
                .all();
    }
}
