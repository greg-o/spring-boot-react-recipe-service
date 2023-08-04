/**
 * Recipe Service.
 * Copyright: none
 *
 * @author Greg-O
 */
package org.grego.recipeservice.mapping;

import org.grego.recipeservice.document.IngredientDoc;
import org.grego.recipeservice.document.InstructionDoc;
import org.grego.recipeservice.document.RecipeDoc;
import org.grego.recipeservice.model.Ingredient;
import org.grego.recipeservice.model.Instruction;
import org.grego.recipeservice.model.Recipe;
import org.instancio.Instancio;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mapstruct.factory.Mappers;
import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test functionality of RecipeMapper class.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag("UnitTests")
public class RecipeMapperTest {
    /**
     * Length of the name.
     */
    public static final int NAME_LENGTH = 10;

    /**
     * Length of the description.
     */
    public static final int DESCRIPTION_LENGTH = 30;

    /**
     * Maximum difference for the comparing equality of doubles.
     */
    public static final double EPSILON = 0.0001;

    /**
     * RecipeMapper for mapping Recipe to RecipeDoc and vice versa.
     */
    private RecipeMapper recipeMapper = Mappers.getMapper(RecipeMapper.class);

    /**
     * Testing toDoc method.
     */
    @Test
    public void testToDoc() {
        var random = new Random();
        long recipeId = random.nextLong();
        String name = RandomStringUtils.randomAlphabetic(NAME_LENGTH);
        int variation = random.nextInt();
        String description = RandomStringUtils.randomAlphabetic(DESCRIPTION_LENGTH);
        Long version = random.nextLong();
        LocalDateTime creationDateTime = Instancio.create(LocalDateTime.class);
        LocalDateTime lastModifiedDateTime = Instancio.create(LocalDateTime.class);
        List<Ingredient> ingredients = Instancio.ofList(Ingredient.class).size(2).create();
        List<Instruction> instructions = Instancio.ofList(Instruction.class).size(2).create();

        var recipe = new Recipe(recipeId, name, variation, description, version,
                creationDateTime, lastModifiedDateTime, ingredients, instructions);

        RecipeDoc recipeDoc = recipeMapper.toDoc(recipe);

        assertEquals(recipeDoc.getId(), recipeId);
        assertEquals(recipeDoc.getName(), name);
        assertEquals(recipeDoc.getVariation(), variation);
        assertEquals(recipeDoc.getDescription(), description);
        assertEquals(
            recipeDoc
                .getCreationDateTime()
                .toInstant()
                .atZone(ZoneId.of(ZoneOffset.UTC.getId()))
                .toLocalDateTime()
                .truncatedTo(ChronoUnit.MILLIS),
            creationDateTime
                .truncatedTo(ChronoUnit.MILLIS));
        assertEquals(
            recipeDoc
                .getLastModifiedDateTime()
                .toInstant()
                    .atZone(ZoneId.of(ZoneOffset.UTC.getId()))
                    .toLocalDateTime()
                    .truncatedTo(ChronoUnit.MILLIS),
            lastModifiedDateTime
                .truncatedTo(ChronoUnit.MILLIS));

        assertEquals(recipeDoc.getIngredients().size(), recipe.getIngredients().size());
        assertTrue(IntStream.range(0, recipeDoc.getIngredients().size())
            .allMatch(i -> {
                var ingredientDoc = recipeDoc.getIngredients().get(i);
                var ingredient = recipe.getIngredients().get(i);

                return ingredientDoc.getIngredientId() == ingredient.getIngredientId()
                    && ingredientDoc.getIngredientNumber() == ingredient.getIngredientNumber()
                    && ingredientDoc.getIngredient().equals(ingredient.getIngredient())
                    && ingredientDoc.getQuantitySpecifier() == ingredient.getQuantitySpecifier()
                    && Math.abs(ingredientDoc.getQuantity() - ingredient.getQuantity()) < EPSILON;
            }));

        assertEquals(recipeDoc.getInstructions().size(), recipe.getInstructions().size());
        assertTrue(IntStream.range(0, recipeDoc.getInstructions().size())
                .allMatch(i -> {
                    var instructionDoc = recipeDoc.getInstructions().get(i);
                    var instruction = recipe.getInstructions().get(i);

                    return instructionDoc.getInstructionId() == instruction.getInstructionId()
                        && instructionDoc.getInstructionNumber() == instruction.getInstructionNumber()
                        && instructionDoc.getInstruction().equals(instruction.getInstruction());
                }));
    }

    /**
     * Testing toDoc method with null Recipe.
     */
    @Test
    public void testToDocNullRecipe() {
        assertNull(recipeMapper.toDoc(null));
    }

    /**
     * Test toModelMethod.
     */
    @Test
    public void testToModel() {

        // Test all arguments constructor and builder
        var random = new Random();
        long recipeId = random.nextLong();
        String name = RandomStringUtils.randomAlphabetic(NAME_LENGTH);
        int variation = random.nextInt();
        String description = RandomStringUtils.randomAlphabetic(DESCRIPTION_LENGTH);
        Long version = random.nextLong();
        Date creationDateTime = Instancio.create(Date.class);
        Date lastModifiedDateTime = Instancio.create(Date.class);
        List<IngredientDoc> ingredients = Instancio.ofList(IngredientDoc.class).size(2).create();
        List<InstructionDoc> instructions = Instancio.ofList(InstructionDoc.class).size(2).create();

        var recipeDoc = new RecipeDoc(recipeId, name, variation, description,
                creationDateTime, lastModifiedDateTime, ingredients, instructions);

        Recipe recipe = recipeMapper.toModel(recipeDoc);

        assertEquals(recipe.getRecipeId(), recipeId);
        assertEquals(recipe.getName(), name);
        assertEquals(recipe.getVariation(), variation);
        assertEquals(recipe.getDescription(), description);
        assertEquals(
            recipe
                .getCreationDateTime()
                .truncatedTo(ChronoUnit.MILLIS),
            creationDateTime
                .toInstant()
                .atZone(ZoneId.of(ZoneOffset.UTC.getId()))
                .toLocalDateTime()
                .truncatedTo(ChronoUnit.MILLIS));
        assertEquals(
            recipe
                .getLastModifiedDateTime()
                .truncatedTo(ChronoUnit.MILLIS),
            lastModifiedDateTime
                .toInstant()
                .atZone(ZoneId.of(ZoneOffset.UTC.getId()))
                .toLocalDateTime()
                .truncatedTo(ChronoUnit.MILLIS));

        assertEquals(recipeDoc.getIngredients().size(), recipe.getIngredients().size());
        assertTrue(IntStream.range(0, recipeDoc.getIngredients().size())
                .allMatch(i -> {
                    var ingredientDoc = recipeDoc.getIngredients().get(i);
                    var ingredient = recipe.getIngredients().get(i);

                    return ingredientDoc.getIngredientId() == ingredient.getIngredientId()
                        && ingredientDoc.getIngredientNumber() == ingredient.getIngredientNumber()
                        && ingredientDoc.getIngredient().equals(ingredient.getIngredient())
                        && ingredientDoc.getQuantitySpecifier() == ingredient.getQuantitySpecifier()
                        && Math.abs(ingredientDoc.getQuantity() - ingredient.getQuantity()) < EPSILON;
                }));

        assertEquals(recipeDoc.getInstructions().size(), recipe.getInstructions().size());
        assertTrue(IntStream.range(0, recipeDoc.getInstructions().size())
                .allMatch(i -> {
                    var instructionDoc = recipeDoc.getInstructions().get(i);
                    var instruction = recipe.getInstructions().get(i);

                    return instructionDoc.getInstructionId() == instruction.getInstructionId()
                        && instructionDoc.getInstructionNumber() == instruction.getInstructionNumber()
                        && instructionDoc.getInstruction().equals(instruction.getInstruction());
                }));
    }

    /**
     * Testing toModel method with null Recipe.
     */
    @Test
    public void testToModelNullRecipeDoc() {
        assertNull(recipeMapper.toModel(null));
    }
}
