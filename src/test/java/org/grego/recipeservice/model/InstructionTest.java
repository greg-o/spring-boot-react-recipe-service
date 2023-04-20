package org.grego.recipeservice.model;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.grego.recipeservice.document.InstructionDoc;
import org.instancio.Instancio;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils;

import java.util.Collections;
import java.util.Map;
import java.util.Random;

/**
 * Test functionality of Instruction class.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag("UnitTests")
public class InstructionTest {
    /**
     * Length of the instruction.
     */
    public static final int INSTRUCTION_LENGTH = 30;

    /**
     * Test Instruction class.
     */
    @Test
    public void testInstruction() {
        // Test no arguments constructor, getters, setters, hashCode, equals, and toString.
        assertThat(Instruction.class, allOf(
            hasValidBeanConstructor(),
            hasValidGettersAndSetters(),
            hasValidBeanHashCode(),
            hasValidBeanEquals(),
            hasValidBeanToString()
        ));

        // Test all arguments constructor and builder
        var random = new Random();
        Long instructionId = random.nextLong();
        int instructionNumber = random.nextInt();
        String instruction = RandomStringUtils.randomAlphabetic(INSTRUCTION_LENGTH);
        Map<String, Object> row = Map.of(
            Instruction.INSTRUCTION_ID_COLUMN_NAME, instructionId,
            Instruction.INSTRUCTION_NUMBER_COLUMN_NAME, instructionNumber,
            Instruction.INSTRUCTION_COLUMN_NAME, instruction
        );

        var allArgsConstInstruction = new Instruction(instructionId, instructionNumber, instruction);
        var builderInstruction = Instruction.builder()
            .instructionId(instructionId)
            .instructionNumber(instructionNumber)
            .instruction(instruction)
            .build();
        var fromRowInstruction = Instruction.fromRow(row);
        Instruction.builder()
            .instructionId(instructionId)
            .instructionNumber(instructionNumber)
            .instruction(instruction)
            .toString();

        assertEquals(allArgsConstInstruction, builderInstruction);
        assertEquals(allArgsConstInstruction, fromRowInstruction);
        assertNull(Instruction.fromRow(Collections.emptyMap()));
        assertNotEquals(allArgsConstInstruction, Instancio.create(Instruction.class));
        assertNotEquals(allArgsConstInstruction, InstructionDoc.create(allArgsConstInstruction));
    }
}
