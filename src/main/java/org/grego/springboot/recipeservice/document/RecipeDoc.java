package org.grego.springboot.recipeservice.document;

import lombok.*;
import org.grego.springboot.recipeservice.model.Ingredient;
import org.grego.springboot.recipeservice.model.Instruction;
import org.grego.springboot.recipeservice.model.Recipe;

import java.time.ZoneOffset;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Document(indexName = "recipes")
public class RecipeDoc extends ElasticsearchDoc {
    @Id
    private Long id;

    private String name;

    private int variation;

    private String description;

    private Date creationDateTime;

    private Date lastModifiedDateTime;

    @Field(type = FieldType.Nested, includeInParent = true)
    private List<IngredientDoc> ingredients = Collections.emptyList();

    @Field(type = FieldType.Nested, includeInParent = true)
    private List<InstructionDoc> instructions = Collections.emptyList();

    public static RecipeDoc create(Recipe recipe) {
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
