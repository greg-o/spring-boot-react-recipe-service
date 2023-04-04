/**
 * Recipe Service.
 * Copyright: none
 *
 * @author Greg-O
 */
package org.grego.recipeservice.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.grego.recipeservice.model.Instruction;

/**
 * Elasticsearch document for instruction.
 */
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class InstructionDoc extends ElasticsearchDoc {
    /**
     * The indentifier for the instruction.
     */
    private Long instructionId;

    /**
     * The number of the order of the instruction.
     */
    private int instructionNumber;

    /**
     * The instruction.
     */
    private String instruction;

    /**
     * Create a InstructionDoc from an Instruction.
     * @param instruction
     * @return The instruction document used in the search engine
     */
    public static InstructionDoc create(final Instruction instruction) {
        return InstructionDoc.builder()
                .instructionId(instruction.getInstructionId())
                .instructionNumber(instruction.getInstructionNumber())
                .instruction(instruction.getInstruction())
                .build();
    }
}
