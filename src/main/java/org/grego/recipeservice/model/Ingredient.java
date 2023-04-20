/**
 * Recipe Service.
 * Copyright: none
 *
 * @author Greg-O
 */
package org.grego.recipeservice.model;

import javax.persistence.Column;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import org.springframework.data.annotation.Id;

/**
 * Ingredient class used in rest api and database serialization.
 */
@Data
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode
@Table(name = Ingredient.INGREDIENTS_TABLE_NAME)
public class Ingredient {
    /**
     * Name of the ingredients table in the database.
     */
    public static final String INGREDIENTS_TABLE_NAME = "ingredients";

    /**
     * Name of the ingredient id column in the database.
     */
    public static final String INGREDIENT_ID_COLUMN_NAME = "ingredient_id";

    /**
     * Name of the ingredient column in the database.
     */
    public static final String INGREDIENT_COLUMN_NAME = "ingredient";

    /**
     * Name of the ingredient number column in the database.
     */
    public static final String INGREDIENT_NUMBER_COLUMN_NAME = "ingredient_number";

    /**
     * Name of the quantity specifier column in the database.
     */
    public static final String QUANTITY_SPECIFIER_COLUMN_NAME = "quantity_specifier";

    /**
     * Name of the quantity column in the database.
     */
    public static final String QUANTITY_COLUMN_NAME = "quantity";

    /**
     * The length of the ingredient column.
     */
    public static final int INGREDIENT_COLUMN_LENGTH = 256;

    /**
     * The ingredient id field.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = INGREDIENT_ID_COLUMN_NAME)
    private Long ingredientId;

    /**
     * The ingredient number field.
     */
    @Column(name = INGREDIENT_NUMBER_COLUMN_NAME,
            columnDefinition = "INT CONSTRAINT positive_ingredient_number CHECK (ingredient_number > 0)")
    private int ingredientNumber;

    /**
     * The quantity specifier field.
     */
    @Column(name = QUANTITY_SPECIFIER_COLUMN_NAME)
    @Enumerated(EnumType.STRING)
    @NotNull
    private QuantitySpecifier quantitySpecifier;

    /**
     * The quantity field.
     */
    @Column(name = QUANTITY_COLUMN_NAME)
    @NotNull
    private Double quantity;

    /**
     * The ingredient field.
     */
    @Column(name = INGREDIENT_COLUMN_NAME, length = INGREDIENT_COLUMN_LENGTH)
    @NotNull
    private String ingredient;

    /**
     * Create and Ingredient from the map object.
     * @param row
     * @return The ingredient
     */
    public static Ingredient fromRow(final Map<String, Object> row) {
        if (row.get("ingredient_id") != null) {
            return Ingredient.builder()
                    .ingredientId((Long.parseLong(row.get("ingredient_id").toString())))
                    .ingredientNumber(Integer.parseInt(row.get("ingredient_number").toString()))
                    .quantitySpecifier(QuantitySpecifier.valueOf((String) row.get("quantity_specifier")))
                    .quantity(Double.parseDouble(row.get("quantity").toString()))
                    .ingredient((String) row.get("ingredient"))
                    .build();
        } else {
            return null;
        }
    }
}
