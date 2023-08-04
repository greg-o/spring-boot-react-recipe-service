/**
 * Recipe Service.
 * Copyright: none
 *
 * @author Greg-O
 */
package org.grego.recipeservice.mapping;

import org.grego.recipeservice.document.InstructionDoc;
import org.grego.recipeservice.model.Instruction;
import org.instancio.Instancio;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mapstruct.factory.Mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test functionality of InstructionMapper class.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag("UnitTests")
public class InstructionMapperTest {

    /**
     * InstructionMapper for mapping Instruction to InstructionDoc and vice versa.
     */
    private InstructionMapper instructionMapper = Mappers.getMapper(InstructionMapper.class);

    /**
     * Testing toDoc method.
     */
    @Test
    public void testToDoc() {
        var instruction = Instancio.create(Instruction.class);

        InstructionDoc instructionDoc = instructionMapper.toDoc(instruction);

        assertEquals(instructionDoc.getInstructionId(), instruction.getInstructionId());
        assertEquals(instructionDoc.getInstructionNumber(), instruction.getInstructionNumber());
        assertTrue(instructionDoc.getInstruction().equals(instruction.getInstruction()));
    }

    /**
     * Testing toDoc method with null Recipe.
     */
    @Test
    public void testToDocNullRecipe() {
        assertNull(instructionMapper.toDoc(null));
    }

    /**
     * Test toModelMethod.
     */
    @Test
    public void testToModel() {
        var instructionDoc = Instancio.create(InstructionDoc.class);

        Instruction instruction = instructionMapper.toModel(instructionDoc);

        assertEquals(instruction.getInstructionId(), instructionDoc.getInstructionId());
        assertEquals(instruction.getInstructionNumber(), instructionDoc.getInstructionNumber());
        assertTrue(instruction.getInstruction().equals(instructionDoc.getInstruction()));
    }

    /**
     * Testing toModel method with null Recipe.
     */
    @Test
    public void testToModelNullRecipeDoc() {
        assertNull(instructionMapper.toModel(null));
    }
}
