package org.grego.recipeservice.model;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.code.beanmatchers.BeanMatchers;
import com.google.code.beanmatchers.ValueGenerator;
import org.instancio.Instancio;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

/**
 * Test functionality of Recipe class.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag("UnitTests")
public class RecipeTest {
    /**
     * Length of the name.
     */
    public static final int NAME_LENGTH = 10;

    /**
     * Length of the description.
     */
    public static final int DESCRIPTION_LENGTH = 30;

    /**
     * Add generator for LocalDateTime to BeanMatchers.
     */
    @BeforeAll
    void init() {
        BeanMatchers.registerValueGenerator(new ValueGenerator<LocalDateTime>() {
            public LocalDateTime generate() {
                return Instancio.create(LocalDateTime.class);  // Change to generate random instance
            }
        }, LocalDateTime.class);
    }

    /**
     * Test Recipe class.
     */
    @Test
    public void testRecipe() {
        // Test no arguments constructor, getters, setters, hashCode, equals, and toString.
        assertThat(Recipe.class, allOf(
            hasValidBeanConstructor(),
            hasValidGettersAndSetters(),
            hasValidBeanHashCode(),
            hasValidBeanEquals(),
            hasValidBeanToString()
        ));

        // Test all arguments constructor and builder
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

        var allArgsConstRecipe = new Recipe(recipeId, name, variation, description, version,
                creationDateTime, lastModifiedDateTime, ingredients, instructions);
        var builderRecipe =  Recipe.builder()
            .recipeId(recipeId)
            .name(name)
            .variation(variation)
            .description(description)
            .version(version)
            .creationDateTime(creationDateTime)
            .lastModifiedDateTime(lastModifiedDateTime)
            .ingredients(ingredients)
            .instructions(instructions)
            .build();
        Recipe.builder()
            .recipeId(recipeId)
            .name(name)
            .variation(variation)
            .description(description)
            .version(version)
            .creationDateTime(creationDateTime)
            .lastModifiedDateTime(lastModifiedDateTime)
            .ingredients(ingredients)
            .instructions(instructions)
            .toString();

        assertEquals(allArgsConstRecipe, builderRecipe);
        assertNotEquals(allArgsConstRecipe, Instancio.create(Recipe.class));
    }
}
