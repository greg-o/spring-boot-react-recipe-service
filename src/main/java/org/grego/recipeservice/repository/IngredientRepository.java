/**
 * Recipe Service.
 * Copyright: none
 *
 * @author Greg-O
 */
package org.grego.recipeservice.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.grego.recipeservice.model.Ingredient;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.r2dbc.core.ReactiveUpdateOperation;
import org.springframework.data.relational.core.query.Update;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.stream.Collectors;

import static org.springframework.data.relational.core.query.Criteria.where;
import static org.springframework.data.relational.core.query.Query.query;

/**
 * IngredientRepository for managing Ingredient objects in the database.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class IngredientRepository {
    /**
     * Reactive database template used to query the database.
     */
    private final R2dbcEntityTemplate template;

    /**
     * Save ingredient.
     * @param ingredient
     * @return Ingredient that was saved
     */
    public Mono<Ingredient> save(final Ingredient ingredient) {
        return template.insert(Ingredient.class)
                .into(Ingredient.INGREDIENTS_TABLE_NAME)
                .using(ingredient);
    }

    /**
     * Save ingredients.
     * @param ingredientList
     * @return List of Ingredients that were saved.
     */
    public Flux<Ingredient> saveAll(final Collection<Ingredient> ingredientList) {
        return Flux.concat(ingredientList.stream().map(this::save).collect(Collectors.toList()));
    }

    /**
     * Update ingredient.
     * @param ingredient
     * @return Id of updated ingredient
     */
    public Mono<Long> update(final Ingredient ingredient) {
        return template.update(ReactiveUpdateOperation.UpdateWithTable.class)
                .inTable(Ingredient.INGREDIENTS_TABLE_NAME)
                .matching(query(where(Ingredient.INGREDIENT_ID_COLUMN_NAME).is(ingredient.getIngredientId())))
                .apply(Update.update(Ingredient.INGREDIENT_COLUMN_NAME, ingredient.getIngredient())
                        .set(Ingredient.INGREDIENT_NUMBER_COLUMN_NAME, ingredient.getIngredientNumber())
                        .set(Ingredient.QUANTITY_SPECIFIER_COLUMN_NAME, ingredient.getQuantitySpecifier())
                        .set(Ingredient.QUANTITY_COLUMN_NAME, ingredient.getQuantity()));
    }

    /**
     * Update ingredients.
     * @param ingredientList
     * @return Ids of updated ingredients.
     */
    public Flux<Long> updateAll(final Collection<Ingredient> ingredientList) {
        return Flux.concat(ingredientList.stream().map(this::update).collect(Collectors.toList()));
    }

    /**
     * Delete ingredient by ingredient id.
     * @param ingredientId
     * @return Id of deleted ingredient
     */
    public Mono<Long> deleteById(final long ingredientId) {
        return template.delete(Ingredient.class)
                .from(Ingredient.INGREDIENTS_TABLE_NAME)
                .matching(query(where(Ingredient.INGREDIENT_ID_COLUMN_NAME).is(ingredientId)))
                .all();
    }

    /**
     * Delete ingredients by ingredient ids.
     * @param ingredientIdList
     * @return Ids of deleted ingredients
     */
    public Flux<Long> deleteAllByIds(final Collection<Long> ingredientIdList) {
        return Flux.concat(ingredientIdList.stream().map(this::deleteById)
                .collect(Collectors.toList()));
    }
}
