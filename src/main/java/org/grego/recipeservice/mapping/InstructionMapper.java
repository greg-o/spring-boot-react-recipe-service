/**
 * Recipe Service.
 * Copyright: none
 *
 * @author Greg-O
 */
package org.grego.recipeservice.mapping;

import org.grego.recipeservice.document.InstructionDoc;
import org.grego.recipeservice.model.Instruction;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

/**
 * Instruction mapper class used in mapping instances of Instruction class to instances of InstructionDoc
 * and vice versa.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface InstructionMapper {

    /**
     * Map an instance of Instruction class to an instance of InstructionDoc.
     * @param instruction
     * @return InstructionDoc for the Instruction.
     */
    InstructionDoc toDoc(Instruction instruction);

    /**
     * Map an instance of InstructionDoc class to an instance of Instruction.
     * @param instructionDoc
     * @return Instruction of the InstructionDoc.
     */
    Instruction toModel(InstructionDoc instructionDoc);
}
