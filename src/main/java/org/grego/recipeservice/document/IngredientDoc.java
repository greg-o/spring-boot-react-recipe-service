/**
 * Recipe Service.
 * Copyright: none
 *
 * @author Greg-O
 */
package org.grego.recipeservice.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.grego.recipeservice.model.Ingredient;
import org.grego.recipeservice.model.QuantitySpecifier;

/**
 * Elasticsearch document for ingredient.
 */
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class IngredientDoc extends ElasticsearchDoc {
    /**
     * Identifier for the ingredient.
     */
    private Long ingredientId;

    /**
     * The number for the order of the ingredient.
     */
    private int ingredientNumber;

    /**
     * The quantity specifier for the ingredient.
     */
    private QuantitySpecifier quantitySpecifier;

    /**
     * The quantity of the ingredient.
     */
    private Double quantity;

    /**
     * The ingredient.
     */
    private String ingredient;

    /**
     * Create an IngredientDoc from an ingredient.
     * @param ingredient
     * @return IngredientDoc used in the search engine.
     */
    public static IngredientDoc create(final Ingredient ingredient) {
        return IngredientDoc.builder()
                .ingredientId(ingredient.getIngredientId())
                .ingredientNumber(ingredient.getIngredientNumber())
                .quantity(ingredient.getQuantity())
                .ingredient(ingredient.getIngredient())
                .build();
    }
}
