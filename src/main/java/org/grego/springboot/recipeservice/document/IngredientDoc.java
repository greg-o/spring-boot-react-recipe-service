package org.grego.springboot.recipeservice.document;

import lombok.*;
import org.grego.springboot.recipeservice.model.Ingredient;
import org.grego.springboot.recipeservice.model.QuantitySpecifier;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class IngredientDoc extends ElasticsearchDoc {

    private Long ingredientId;

    private int ingredientNumber;

    private QuantitySpecifier quantitySpecifier;

    private Double quantity;

    private String ingredient;

    public static IngredientDoc create(Ingredient ingredient) {
        return IngredientDoc.builder()
                .ingredientId(ingredient.getIngredientId())
                .ingredientNumber(ingredient.getIngredientNumber())
                .quantity(ingredient.getQuantity())
                .ingredient(ingredient.getIngredient())
                .build();
    }
}
