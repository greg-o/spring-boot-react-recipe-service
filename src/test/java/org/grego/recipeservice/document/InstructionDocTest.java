package org.grego.recipeservice.document;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils;

import java.util.Random;

/**
 * Test functionality of InstructionDoc class.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag("UnitTests")
public class InstructionDocTest {
    /**
     * Length of the instruction.
     */
    public static final int INSTRUCTION_LENGTH = 30;

    /**
     * Test InstructionDoc class.
     */
    @Test
    public void testRecipeDoc() {
        // Test no arguments constructor, getters, setters, hashCode, equals, and toString.
        assertThat(InstructionDoc.class, allOf(
                hasValidBeanConstructor(),
                hasValidGettersAndSetters()
        ));

        // Test all arguments constructor and builder
        var random = new Random();
        Long instructionId = random.nextLong();
        int instructionNumber = random.nextInt();
        String instruction = RandomStringUtils.randomAlphabetic(INSTRUCTION_LENGTH);

        var allArgsConstInstructionDoc = new InstructionDoc(instructionId, instructionNumber, instruction);
        var builderInstructionDoc = InstructionDoc.builder()
                .instructionId(instructionId)
                .instructionNumber(instructionNumber)
                .instruction(instruction)
                .build();
        InstructionDoc.builder()
                .instructionId(instructionId)
                .instructionNumber(instructionNumber)
                .instruction(instruction)
                .toString();

        assertEquals(allArgsConstInstructionDoc.toString(), builderInstructionDoc.toString());
    }
}
