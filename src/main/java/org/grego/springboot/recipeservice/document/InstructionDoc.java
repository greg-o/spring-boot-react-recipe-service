package org.grego.springboot.recipeservice.document;

import lombok.*;
import org.grego.springboot.recipeservice.model.Ingredient;
import org.grego.springboot.recipeservice.model.Instruction;

import javax.persistence.Column;
import javax.validation.constraints.NotNull;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class InstructionDoc extends ElasticsearchDoc {

    private Long instructionId;

    private int instructionNumber;

    private String instruction;

    public static InstructionDoc create(Instruction instruction) {
        return InstructionDoc.builder()
                .instructionId(instruction.getInstructionId())
                .instructionNumber(instruction.getInstructionNumber())
                .instruction(instruction.getInstruction())
                .build();
    }
}
