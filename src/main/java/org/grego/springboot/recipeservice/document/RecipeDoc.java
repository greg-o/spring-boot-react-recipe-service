package org.grego.springboot.recipeservice.document;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import org.grego.springboot.recipeservice.model.Ingredient;
import org.grego.springboot.recipeservice.model.Instruction;
import org.grego.springboot.recipeservice.model.Recipe;

import java.time.ZoneOffset;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.springframework.data.elasticsearch.annotations.DateFormat;
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
public class RecipeDoc {
    private Long id;

    private String name;

    private int variation;

    private String description;

    private Long version;

    @Field(type = FieldType.Date, format = {DateFormat.date_hour_minute_second_fraction})
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "uuuu-MM-dd'T'HH:mm:ss.SSS")
    private Date creationDateTime;

    @Field(type = FieldType.Date, format = {DateFormat.date_hour_minute_second_fraction})
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "uuuu-MM-dd'T'HH:mm:ss.SSS")
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
