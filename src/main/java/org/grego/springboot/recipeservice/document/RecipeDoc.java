package org.grego.springboot.recipeservice.document;

import lombok.*;
import org.grego.springboot.recipeservice.model.Ingredient;
import org.grego.springboot.recipeservice.model.Instruction;
import org.grego.springboot.recipeservice.model.Recipe;

import java.time.ZoneOffset;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.springframework.data.elasticsearch.annotations.Document;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Document(indexName = "recipes")
public class RecipeDoc {
    private Long id;

    private String name;

    private int variation;

    private String description;

    private Long version;

    private Date creationDateTime;

    private Date lastModifiedDateTime;

    private List<Ingredient> ingredients = Collections.emptyList();

    private List<Instruction> instructions = Collections.emptyList();

    public static RecipeDoc create(Recipe recipe) {
        return RecipeDoc.builder()
                .id(recipe.getRecipeId())
                .name(recipe.getName())
                .variation(recipe.getVariation())
                .description(recipe.getDescription())
                .creationDateTime(Date.from(recipe.getCreationDateTime().toInstant(ZoneOffset.UTC)))
                .lastModifiedDateTime(Date.from(recipe.getLastModifiedDateTime().toInstant(ZoneOffset.UTC)))
                .ingredients(recipe.getIngredients())
                .instructions(recipe.getInstructions())
                .build();
    }
}
