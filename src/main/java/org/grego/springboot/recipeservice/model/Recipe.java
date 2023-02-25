package org.grego.springboot.recipeservice.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

import lombok.*;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import reactor.core.publisher.Mono;

@Data
@Builder
@Getter
@Setter
@AllArgsConstructor
@ToString
@Table(name = "recipes", uniqueConstraints = {@UniqueConstraint(name = "unique_recipe_name_and_variation", columnNames = {"name", "variation"})})
public class Recipe {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "recipe_id")
    private Long recipeId;

    @Column(name = "name", length=256)
    @NotNull
    private String name;

    @Column(name = "variation", nullable = false)
    private int variation;

    @Column(name = "description")
    @NotNull
    private String description;

    @Version
    @Column(name = "version")
    private long version;

    @Column(name = "creation_date_time", columnDefinition = "TIMESTAMP", nullable = false)
    private LocalDateTime creationDateTime;

    @Column(name = "last_modified_date_time", columnDefinition = "TIMESTAMP", nullable = false)
    private LocalDateTime lastModifiedDateTime;

//    @OneToMany(targetEntity = Ingredient.class, cascade={CascadeType.ALL}, orphanRemoval = true, fetch = FetchType.LAZY)
//    @MapKey
    @Builder.Default
    private List<Ingredient> ingredients;

//    @OneToMany(targetEntity = Instruction.class, cascade= {CascadeType.ALL}, orphanRemoval = true, fetch = FetchType.LAZY)
//    @MapKey
    @Builder.Default
    private List<Instruction> instructions;

    public static Mono<Recipe> fromRows(List<Map<String, Object>> rows) {
        return Mono.just(Recipe.builder()
                .recipeId((Long.parseLong(rows.get(0).get("recipe_id").toString())))
                .name((String) rows.get(0).get("name"))
                .variation(Integer.parseInt(rows.get(0).get("variation").toString()))
                .description((String) rows.get(0).get("description"))
//                .version(Long.parseLong(rows.get(0).get("version").toString()))
                .creationDateTime(LocalDateTime.parse(rows.get(0).get("creation_date_time").toString()))
                .lastModifiedDateTime(LocalDateTime.parse(rows.get(0).get("last_modified_date_time").toString()))
                .ingredients(rows.stream()
                        .map(Ingredient::fromRow)
                        .filter(Objects::nonNull)
                        .toList())
                .instructions(rows.stream()
                        .map(Instruction::fromRow)
                        .filter(Objects::nonNull)
                        .toList())
                .build());
    }

}
