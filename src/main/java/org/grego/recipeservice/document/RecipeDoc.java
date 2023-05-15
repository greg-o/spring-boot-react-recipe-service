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
import org.grego.recipeservice.model.Recipe;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.ZoneOffset;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Elasticsearch document for recipe.
 */
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Document(indexName = "recipes")
public class RecipeDoc extends ElasticsearchDoc {
    /**
     * Identifier of the recipe.
     */
    @Id
    private Long id;

    /**
     * Name of the recipe.
     */
    private String name;

    /**
     * Variation of the recipe.
     */
    private int variation;

    /**
     * Description of the recipe.
     */
    private String description;

    /**
     * Date and time that the recipe was created.
     */
    private Date creationDateTime;

    /**
     * Date and time that the recipe was last modified.
     */
    private Date lastModifiedDateTime;

    /**
     * Ingredients for the recipe.
     */
    @Field(type = FieldType.Nested, includeInParent = true)
    @Builder.Default
    private List<IngredientDoc> ingredients = Collections.emptyList();

    /**
     * Instructions for the recipe.
     */
    @Field(type = FieldType.Nested, includeInParent = true)
    @Builder.Default
    private List<InstructionDoc> instructions = Collections.emptyList();

    /**
     * Create a RecipeDoc from a Recipe.
     * @param recipe
     * @return The recipe document used in the search engine.
     */
    public static RecipeDoc create(final Recipe recipe) {
        return RecipeDoc.builder()
                .id(recipe.getRecipeId())
                .name(recipe.getName())
                .variation(recipe.getVariation())
                .description(recipe.getDescription())
                .creationDateTime(Date.from(recipe.getCreationDateTime().toInstant(ZoneOffset.UTC)))
                .lastModifiedDateTime(Date.from(recipe.getLastModifiedDateTime().toInstant(ZoneOffset.UTC)))
                .ingredients(recipe.getIngredients().stream().map(IngredientDoc::create).collect(Collectors.toList()))
                .instructions(recipe.getInstructions().stream().map(InstructionDoc::create).collect(Collectors.toList()))
                .build();
    }
}
