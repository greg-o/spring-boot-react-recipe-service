package org.grego.springboot.recipeservice.model;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
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
@Table(name = "ingredients")
public class Ingredient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ingredient_id")
    private Long ingredientId;

    @Column(name = "ingredient_number", columnDefinition="INT CONSTRAINT positive_ingredient_number CHECK (ingredient_number > 0)")
    private int ingredientNumber;

    @Column(name = "quantity_specifier")
    @Enumerated(EnumType.STRING)
    @NotNull
    private QuantitySpecifier quantitySpecifier;

    @Column(name = "quantity")
    @NotNull
    private Double quantity;

    @Column(name = "ingredient", length=256)
    @NotNull
    private String ingredient;

//    @ManyToOne(cascade = {CascadeType.ALL})
//    @JoinColumn(name = "recipe_id")
//    private Recipe recipe;

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
