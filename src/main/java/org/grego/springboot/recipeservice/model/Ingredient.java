package org.grego.springboot.recipeservice.model;

import java.util.Map;
import javax.persistence.*;
import javax.validation.constraints.NotNull;

import lombok.*;

import org.springframework.data.annotation.Id;


@Data
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Table(name = Ingredient.INGREDIENTS_TABLE_NAME)
public class Ingredient {

    public static final String INGREDIENTS_TABLE_NAME = "ingredients";
    public static final String INGREDIENT_ID_COLUMN_NAME = "ingredient_id";
    public static final String INGREDIENT_COLUMN_NAME = "ingredient";
    public static final String INGREDIENT_NUMBER_COLUMN_NAME = "ingredient_number";
    public static final String QUANTITY_SPECIFIER_COLUMN_NAME = "quantity_specifier";
    public static final String QUANTITY_COLUMN_NAME = "quantity";
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = INGREDIENT_ID_COLUMN_NAME)
    private Long ingredientId;

    @Column(name = INGREDIENT_NUMBER_COLUMN_NAME, columnDefinition="INT CONSTRAINT positive_ingredient_number CHECK (ingredient_number > 0)")
    private int ingredientNumber;

    @Column(name = QUANTITY_SPECIFIER_COLUMN_NAME)
    @Enumerated(EnumType.STRING)
    @NotNull
    private QuantitySpecifier quantitySpecifier;

    @Column(name = QUANTITY_COLUMN_NAME)
    @NotNull
    private Double quantity;

    @Column(name = INGREDIENT_COLUMN_NAME, length=256)
    @NotNull
    private String ingredient;

    public static Ingredient fromRow(Map<String, Object> row) {
        if (row.get("ingredient_id") != null) {
            var quantity = row.get("quantity");
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
