/**
 * Recipe Service.
 * Copyright: none
 *
 * @author Greg-O
 */
package org.grego.recipeservice.mapping;

import org.grego.recipeservice.document.IngredientDoc;
import org.grego.recipeservice.model.Ingredient;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

/**
 * Ingredient mapper class used in mapping instances of Ingredient class to instances of IngredientDoc
 * and vice versa.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface IngredientMapper {
    /**
     * Map an instance of Ingredient class to an instance of IngredientDoc.
     * @param ingredient
     * @return IngredientDoc for the Ingredient.
     */
    IngredientDoc toDoc(Ingredient ingredient);

    /**
     * Map an instance of IngredientDoc class to an instance of Ingredient.
     * @param ingredientDoc
     * @return Ingredient for the IngredientDoc.
     */
    Ingredient toModel(IngredientDoc ingredientDoc);
}
