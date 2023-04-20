/**
 * Recipe Service.
 * Copyright: none
 *
 * @author Greg-O
 */
package org.grego.recipeservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.ReadOnlyProperty;

import javax.persistence.Column;
import javax.persistence.GenerationType;
import javax.persistence.GeneratedValue;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.Version;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * Recipe class used in rest api and database serialization.
 */
@Data
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode
@Table(name = Recipe.RECIPES_TABLE_NAME,
        uniqueConstraints = {@UniqueConstraint(name = "unique_recipe_name_and_variation",
                columnNames = {"name", "variation"})})
public class Recipe {
    /**
     * The name of the recipes table in the database.
     */
    public static final String RECIPES_TABLE_NAME = "recipes";

    /**
     * The name of the name column in the database.
     */
    public static final String NAME_COLUMN_NAME = "name";

    /**
     * The name of the recipe id column in the database.
     */
    public static final String RECIPE_ID_COLUMN_NAME = "recipe_id";

    /**
     * The name of the description column in the database.
     */
    public static final String DESCRIPTION_COLUMN_NAME = "description";

    /**
     * The name of the last modified data time column in the database.
     */
    public static final String LAST_MODIFIED_DATE_TIME_COLUMN_NAME = "last_modified_date_time";

    /**
     * The name of the variation column in the database.
     */
    public static final String VARIATION_COLUMN_NAME = "variation";

    /**
     * The name of the version column.
     */
    public static final String VERSION_COLUMN_NAME = "version";

    /**
     * The name of the creation date time column in the database.
     */
    public static final String CREATION_DATE_TIME_COLUMN_NAME = "creation_date_time";

    /**
     * The length of the name column.
     */
    public static final int NAME_COLUMN_LENGTH = 256;

    /**
     * The recipe id field.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = RECIPE_ID_COLUMN_NAME)
    private long recipeId;

    /**
     * The name field.
     */
    @Size(max = NAME_COLUMN_LENGTH)
    @NotBlank
    @Column(name = NAME_COLUMN_NAME, length = NAME_COLUMN_LENGTH)
    private String name;

    /**
     * The variation field.
     */
    @Column(name = VARIATION_COLUMN_NAME, nullable = false)
    private int variation;

    /**
     * The description field.
     */
    @NotNull
    @Column(name = DESCRIPTION_COLUMN_NAME)
    private String description;

    /**
     * The version field.
     */
    @Version
    @Column(name = VERSION_COLUMN_NAME)
    private Long version;

    /**
     * The creation date time field.
     */
    @CreatedDate
    @Column(name = CREATION_DATE_TIME_COLUMN_NAME, columnDefinition = "TIMESTAMP", nullable = false)
    private LocalDateTime creationDateTime;

    /**
     * The last modified date time field.
     */
    @LastModifiedDate
    @Column(name = LAST_MODIFIED_DATE_TIME_COLUMN_NAME, columnDefinition = "TIMESTAMP", nullable = false)
    private LocalDateTime lastModifiedDateTime;

    /**
     * The ingredients of the recipe.
     */
    @Transient
    @ReadOnlyProperty
    private List<Ingredient> ingredients = Collections.emptyList();

    /**
     * The instructions of the recipe.
     */
    @Transient
    @ReadOnlyProperty
    private List<Instruction> instructions = Collections.emptyList();
}
