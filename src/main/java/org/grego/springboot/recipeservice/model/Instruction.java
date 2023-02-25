package org.grego.springboot.recipeservice.model;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

import lombok.*;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Table(name = "instructions")
public class Instruction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "instruction_id")
    private Long instructionId;

    @Column(name = "instruction_number", columnDefinition="INT CONSTRAINT positive_instruction_number CHECK (instruction_number > 0)")
    private int instructionNumber;

    @Column(name = "instruction")
    @NotNull
    private String instruction;

//    @ManyToOne(cascade = {CascadeType.ALL})
//    @JoinColumn(name = "recipe_id")
//    private Recipe recipe;

    public static Instruction fromRow(Map<String, Object> row) {
        if (row.get("instruction_id") != null) {
            return Instruction.builder()
                    .instructionId((Long.parseLong(row.get("instruction_id").toString())))
                    .instructionNumber(Integer.parseInt(row.get("instruction_number").toString()))
                    .instruction((String) row.get("instruction"))
                    .build();
        } else {
            return null;
        }
    }
}
