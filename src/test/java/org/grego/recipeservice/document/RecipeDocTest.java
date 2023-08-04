package org.grego.recipeservice.document;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.code.beanmatchers.BeanMatchers;
import com.google.code.beanmatchers.ValueGenerator;
import org.instancio.Instancio;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils;

import java.util.Date;
import java.util.List;
import java.util.Random;

/**
 * Test functionality of RecipeDoc class.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag("UnitTests")
public class RecipeDocTest {
    /**
     * Length of the name.
     */
    public static final int NAME_LENGTH = 10;

    /**
     * Length of the description.
     */
    public static final int DESCRIPTION_LENGTH = 30;

    /**
     * Add generator for Date to BeanMatchers.
     */
    @BeforeAll
    void init() {
        BeanMatchers.registerValueGenerator(new ValueGenerator<Date>() {
            public Date generate() {
                return Instancio.create(Date.class);  // Change to generate random instance
            }
        }, Date.class);
    }

    /**
     * Test RecipeDoc class.
     */
    @Test
    public void testRecipeDoc() {
        // Test no arguments constructor, getters, setters, hashCode, equals, and toString.
        assertThat(RecipeDoc.class, allOf(
                hasValidBeanConstructor(),
                hasValidGettersAndSetters()
        ));

        // Test all arguments constructor and builder
        var random = new Random();
        Long id = random.nextLong();
        String name = RandomStringUtils.randomAlphabetic(NAME_LENGTH);
        int variation = random.nextInt();
        String description = RandomStringUtils.randomAlphabetic(DESCRIPTION_LENGTH);
        Date creationDateTime = Instancio.create(Date.class);
        Date lastModifiedDateTime = Instancio.create(Date.class);
        List<IngredientDoc> ingredients = Instancio.ofList(IngredientDoc.class).size(2).create();
        List<InstructionDoc> instructions = Instancio.ofList(InstructionDoc.class).size(2).create();

        var allArgsConstrRecipeDoc = new RecipeDoc(id, name, variation, description,
                creationDateTime, lastModifiedDateTime, ingredients, instructions);
        var builderRecipeDoc = RecipeDoc.builder()
                .id(id)
                .name(name)
                .variation(variation)
                .description(description)
                .creationDateTime(creationDateTime)
                .lastModifiedDateTime(lastModifiedDateTime)
                .ingredients(ingredients)
                .instructions(instructions)
                .build();
        RecipeDoc.builder()
                .id(id)
                .name(name)
                .variation(variation)
                .description(description)
                .creationDateTime(creationDateTime)
                .lastModifiedDateTime(lastModifiedDateTime)
                .ingredients(ingredients)
                .instructions(instructions)
                .toString();

        assertEquals(allArgsConstrRecipeDoc.toString(), builderRecipeDoc.toString());
    }
}
