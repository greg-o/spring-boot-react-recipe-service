package org.grego.springboot.recipeservice.model;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import javax.persistence.*;
import javax.persistence.Transient;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import lombok.*;

import org.springframework.data.annotation.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Data
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Table(name = Recipe.RECIPES_TABLE_NAME,
        uniqueConstraints = {@UniqueConstraint(name = "unique_recipe_name_and_variation",
                columnNames = {"name", "variation"})})
public class Recipe {
    public static final String RECIPES_TABLE_NAME = "recipes";
    public static final String NAME_COLUMN_NAME = "name";
    public static final String RECIPE_ID_COLUMN_NAME = "recipe_id";
    public static final String DESCRIPTION_COLUMN_NAME = "description";
    public static final String LAST_MODIFIED_DATE_TIME_COLUMN_NAME = "last_modified_date_time";
    public static final String VARIATION_COLUMN_NAME = "variation";
    public static final String VERSION_COLUMN_NAME = "version";
    public static final String CREATION_DATE_TIME_COLUMN_NAME = "creation_date_time";
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = RECIPE_ID_COLUMN_NAME)
    private Long recipeId;

    @Size(max=256)
    @NotBlank
    @Column(name = NAME_COLUMN_NAME, length=256)
    private String name;

    @Column(name = VARIATION_COLUMN_NAME, nullable = false)
    private int variation;

    @NotNull
    @Column(name = DESCRIPTION_COLUMN_NAME)
    private String description;

    @Version
    @Column(name = VERSION_COLUMN_NAME)
    private long version;

    @CreatedDate
    @Column(name = CREATION_DATE_TIME_COLUMN_NAME, columnDefinition = "TIMESTAMP", nullable = false)
    private LocalDateTime creationDateTime;

    @LastModifiedDate
    @Column(name = LAST_MODIFIED_DATE_TIME_COLUMN_NAME, columnDefinition = "TIMESTAMP", nullable = false)
    private LocalDateTime lastModifiedDateTime;

    @Transient
    @ReadOnlyProperty
    private List<Ingredient> ingredients = Collections.emptyList();

    @Transient
    @ReadOnlyProperty
    private List<Instruction> instructions = Collections.emptyList();
}
