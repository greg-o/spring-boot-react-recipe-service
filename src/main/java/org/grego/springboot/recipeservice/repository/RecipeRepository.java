package org.grego.springboot.recipeservice.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.grego.springboot.recipeservice.model.Recipe;
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

@Component
@RequiredArgsConstructor
@Slf4j
public class RecipeRepository {
    private final R2dbcEntityTemplate template;

    public Flux<Recipe> findAllByName(String name) {
        return template
                .select(Recipe.class)
                .from(Recipe.RECIPES_TABLE_NAME)
                .matching(query(where(Recipe.NAME_COLUMN_NAME).is(name)))
                .all();
    }

    public Flux<Recipe> findAll(long startPage, int pageSize) {
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

    public Mono<Long> countAll() {
        return template
                .select(Recipe.class)
                .from(Recipe.RECIPES_TABLE_NAME)
                .count();
    }

    public Mono<Boolean> existsById(long recipeId) {
        return template
                .select(Recipe.class)
                .from(Recipe.RECIPES_TABLE_NAME)
                .matching(query(where(Recipe.RECIPE_ID_COLUMN_NAME).is(recipeId)))
                .exists();
    }

    public Mono<Recipe> findById(long recipeId) {
        return template
                .select(Recipe.class)
                .from(Recipe.RECIPES_TABLE_NAME)
                .matching(query(where(Recipe.RECIPE_ID_COLUMN_NAME).is(recipeId)))
                .one();
    }

    public Mono<Recipe> save(Recipe recipe) {
        return template
                .insert(Recipe.class)
                .into(Recipe.RECIPES_TABLE_NAME)
                .using(recipe);
    }

    public Mono<Long> update(Recipe recipe) {
        return template
                .update(ReactiveUpdateOperation.UpdateWithTable.class)
                .inTable(Recipe.RECIPES_TABLE_NAME)
                .matching(query(where(Recipe.RECIPE_ID_COLUMN_NAME).is(recipe.getRecipeId())))
                .apply(Update.update(Recipe.NAME_COLUMN_NAME, recipe.getName())
                    .set(Recipe.DESCRIPTION_COLUMN_NAME, recipe.getDescription())
                    .set(Recipe.LAST_MODIFIED_DATE_TIME_COLUMN_NAME, LocalDateTime.now()));
    }

    public Mono<Long> deleteById(long recipeId) {
        return template
                .delete(Recipe.class)
                .from(Recipe.RECIPES_TABLE_NAME)
                .matching(query(where(Recipe.RECIPE_ID_COLUMN_NAME).is(recipeId)))
                .all();
    }
}
