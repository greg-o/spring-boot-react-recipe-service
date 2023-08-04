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
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.Collections;
import java.util.Date;
import java.util.List;

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
}
