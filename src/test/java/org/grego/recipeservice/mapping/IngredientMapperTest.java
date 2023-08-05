/**
 * Recipe Service.
 * Copyright: none
 *
 * @author Greg-O
 */
package org.grego.recipeservice.mapping;


import org.grego.recipeservice.document.IngredientDoc;
import org.grego.recipeservice.model.Ingredient;
import org.instancio.Instancio;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mapstruct.factory.Mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test functionality of IngredientMapper class.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag("UnitTests")
public class IngredientMapperTest {

    /**
     * Maximum difference for the comparing equality of doubles.
     */
    public static final double EPSILON = 0.0001;

    /**
     * IngredientMapper for mapping Ingredient to IngredientDoc and vice versa.
     */
    private final IngredientMapper ingredientMapper = Mappers.getMapper(IngredientMapper.class);

    /**
     * Testing toDoc method.
     */
    @Test
    void testToDoc() {
        var ingredient = Instancio.create(Ingredient.class);

        IngredientDoc ingredientDoc = ingredientMapper.toDoc(ingredient);

        assertEquals(ingredientDoc.getIngredientId(), ingredient.getIngredientId());
        assertEquals(ingredientDoc.getIngredientNumber(), ingredient.getIngredientNumber());
        assertEquals(ingredientDoc.getQuantitySpecifier(), ingredient.getQuantitySpecifier());
        assertTrue(Math.abs(ingredientDoc.getQuantity() - ingredient.getQuantity()) < EPSILON);
        assertEquals(ingredientDoc.getIngredient(), ingredient.getIngredient());
    }

    /**
     * Testing toDoc method with null Recipe.
     */
    @Test
    void testToDocNullRecipe() {
        assertNull(ingredientMapper.toDoc(null));
    }

    /**
     * Test toModelMethod.
     */
    @Test
    void testToModel() {
        var ingredientDoc = Instancio.create(IngredientDoc.class);

        Ingredient ingredient = ingredientMapper.toModel(ingredientDoc);

        assertEquals(ingredient.getIngredientId(), ingredientDoc.getIngredientId());
        assertEquals(ingredient.getIngredientNumber(), ingredientDoc.getIngredientNumber());
        assertEquals(ingredient.getQuantitySpecifier(), ingredientDoc.getQuantitySpecifier());
        assertTrue(Math.abs(ingredient.getQuantity() - ingredientDoc.getQuantity()) < EPSILON);
        assertEquals(ingredient.getIngredient(), ingredientDoc.getIngredient());
    }

    /**
     * Testing toModel method with null Recipe.
     */
    @Test
    void testToModelNullRecipeDoc() {
        assertNull(ingredientMapper.toModel(null));
    }
}
