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
import org.springframework.data.annotation.Id;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import java.util.Map;

/**
 * Instruction class used in rest api and database serialization.
 */
@Data
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode
@Table(name = Instruction.INSTRUCTIONS_TABLE_NAME)
public class Instruction {
    /**
     * The name of the instructions table in the database.
     */
    public static final String INSTRUCTIONS_TABLE_NAME = "instructions";

    /**
     * The name of the instruction id column in the database.
     */
    public static final String INSTRUCTION_ID_COLUMN_NAME = "instruction_id";

    /**
     * The name of the instruction column in the database.
     */
    public static final String INSTRUCTION_COLUMN_NAME = "instruction";

    /**
     * The name of the instruction number column in the database.
     */
    public static final String INSTRUCTION_NUMBER_COLUMN_NAME = "instruction_number";

    /**
     * The instruction id field.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = INSTRUCTION_ID_COLUMN_NAME)
    private Long instructionId;

    /**
     * The instruction number field.
     */
    @Column(name = INSTRUCTION_NUMBER_COLUMN_NAME,
            columnDefinition = "INT CONSTRAINT positive_instruction_number CHECK (instruction_number > 0)")
    private int instructionNumber;

    /**
     * The instruction field.
     */
    @Column(name = INSTRUCTION_COLUMN_NAME)
    @NotNull
    private String instruction;

    /**
     * Create an instruction from a map object.
     * @param row
     * @return The instruction
     */
    public static Instruction fromRow(final Map<String, Object> row) {
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
