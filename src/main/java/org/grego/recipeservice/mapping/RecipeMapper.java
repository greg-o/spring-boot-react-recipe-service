/**
 * Recipe Service.
 * Copyright: none
 *
 * @author Greg-O
 */
package org.grego.recipeservice.mapping;

import org.grego.recipeservice.document.RecipeDoc;
import org.grego.recipeservice.model.Recipe;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

/**
 * Recipe mapper class used in mapping instances of Recipe class to instances of RecipeDoc
 * and vice versa.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface RecipeMapper {
    /**
     * Map in instance of Recipe class to an instance of RecipeDoc.
     * @param recipe
     * @return RecipeDoc for the Recipe.
     */
    @Mapping(target = "id", source = "recipe.recipeId")
    RecipeDoc toDoc(Recipe recipe);

    /**
     * Map in instance of RecipeDoc class to an instance of Recipe.
     * @param recipeDoc
     * @return Recipe for the RecipeDoc.
     */
    @Mapping(target = "recipeId", source = "recipeDoc.id")
    Recipe toModel(RecipeDoc recipeDoc);
}
