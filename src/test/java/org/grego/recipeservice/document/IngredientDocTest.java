package org.grego.recipeservice.document;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.grego.recipeservice.model.Ingredient;
import org.grego.recipeservice.model.QuantitySpecifier;
import org.instancio.Instancio;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils;

import java.util.Random;

/**
 * Test functionality of IngredientDoc class.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag("UnitTests")
public class IngredientDocTest {
    /**
     * Length of the ingredient.
     */
    public static final int INGREDIENT_LENGTH = 20;

    /**
     * Test IngredientDoc class.
     */
    @Test
    public void testRecipeDoc() {
        // Test no arguments constructor, getters, setters, hashCode, equals, and toString.
        assertThat(IngredientDoc.class, allOf(
                hasValidBeanConstructor(),
                hasValidGettersAndSetters()
        ));

        // Test all arguments constructor and builder
        var random = new Random();
        Long ingredientId = random.nextLong();
        int ingredientNumber = random.nextInt();
        QuantitySpecifier quantitySpecifier = QuantitySpecifier.Unspecified;
        Double quantity = random.nextDouble();
        String ingredient = RandomStringUtils.randomAlphabetic(INGREDIENT_LENGTH);

        var allArgsConstIngredientDoc = new IngredientDoc(ingredientId, ingredientNumber, quantitySpecifier,
                quantity, ingredient);
        var builderIngredientDoc = IngredientDoc.builder()
            .ingredientId(ingredientId)
            .ingredientNumber(ingredientNumber)
            .quantitySpecifier(quantitySpecifier)
            .quantity(quantity)
            .ingredient(ingredient)
            .build();
        IngredientDoc.builder()
            .ingredientId(ingredientId)
            .ingredientNumber(ingredientNumber)
            .quantitySpecifier(quantitySpecifier)
            .quantity(quantity)
            .ingredient(ingredient)
            .toString();

        assertEquals(allArgsConstIngredientDoc.toString(), builderIngredientDoc.toString());

        IngredientDoc.create(Instancio.create(Ingredient.class));
    }
}
