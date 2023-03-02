package org.grego.springboot.recipeservice.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.grego.springboot.recipeservice.model.Ingredient;
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

@Component
@RequiredArgsConstructor
@Slf4j
public class IngredientRepository {
    private final R2dbcEntityTemplate template;

    public Mono<Ingredient> save(Ingredient ingredient) {
        return template.insert(Ingredient.class)
            .into(Ingredient.INGREDIENTS_TABLE_NAME)
            .using(ingredient);
    }

    public Flux<Ingredient> saveAll(Collection<Ingredient> ingredientList) {
        return Flux.concat(ingredientList.stream().map(this::save).collect(Collectors.toList()));
    }

    public Mono<Long> update(Ingredient ingredient) {
        return template.update(ReactiveUpdateOperation.UpdateWithTable.class)
            .inTable(Ingredient.INGREDIENTS_TABLE_NAME)
            .matching(query(where(Ingredient.INGREDIENT_ID_COLUMN_NAME).is(ingredient.getIngredientId())))
            .apply(Update.update(Ingredient.INGREDIENT_COLUMN_NAME, ingredient.getIngredient())
                .set(Ingredient.INGREDIENT_NUMBER_COLUMN_NAME, ingredient.getIngredientNumber())
                .set(Ingredient.QUANTITY_SPECIFIER_COLUMN_NAME, ingredient.getQuantitySpecifier())
                .set(Ingredient.QUANTITY_COLUMN_NAME, ingredient.getQuantity()));
    }

    public Flux<Long> updateAll(Collection<Ingredient> ingredientList) {
        return Flux.concat(ingredientList.stream().map(this::update).collect(Collectors.toList()));
    }

    public Mono<Long> deleteById(long ingredientId) {
        return template.delete(Ingredient.class)
            .from(Ingredient.INGREDIENTS_TABLE_NAME)
            .matching(query(where(Ingredient.INGREDIENT_ID_COLUMN_NAME).is(ingredientId)))
            .all();
    }

    public Flux<Long> deleteAllByIds(Collection<Long> ingredientIdList) {
        return Flux.concat(ingredientIdList.stream().map(ingredientId -> deleteById(ingredientId))
            .collect(Collectors.toList()));
    }
}
