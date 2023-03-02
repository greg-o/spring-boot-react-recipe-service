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
@Table(name = Instruction.INSTRUCTIONS_TABLE_NAME)
public class Instruction {
    public static final String INSTRUCTIONS_TABLE_NAME = "instructions";
    public static final String INSTRUCTION_ID_COLUMN_NAME = "instruction_id";
    public static final String INSTRUCTION_COLUMN_NAME = "instruction";
    public static final String INSTRUCTION_NUMBER_COLUMN_NAME = "instruction_number";
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = INSTRUCTION_ID_COLUMN_NAME)
    private Long instructionId;

    @Column(name = INSTRUCTION_NUMBER_COLUMN_NAME, columnDefinition="INT CONSTRAINT positive_instruction_number CHECK (instruction_number > 0)")
    private int instructionNumber;

    @Column(name = INSTRUCTION_COLUMN_NAME)
    @NotNull
    private String instruction;

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
