package org.grego.recipeservice.repository;

import org.grego.recipeservice.model.Instruction;
import org.instancio.Instancio;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.OngoingStubbing;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.r2dbc.core.ReactiveDeleteOperation;
import org.springframework.data.r2dbc.core.ReactiveInsertOperation;
import org.springframework.data.r2dbc.core.ReactiveUpdateOperation;
import org.springframework.data.relational.core.query.Query;
import org.springframework.data.relational.core.query.Update;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Random;
import java.util.stream.LongStream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Test the InstructionRepository.
 */
@ExtendWith(MockitoExtension.class)
@RunWith(PowerMockRunner.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag("UnitTests")
public class InstructionRepositoryTest {
    /**
     * At least create this many instructions.
     */
    public static final int AT_LEAST_INSTRUCTIONS = 2;

    /**
     * Range for the number of additional instructions.
     */
    public static final int NUMBER_OF_ADDITIONAL_INSTRUCTIONS_RANGE = 3;
    /**
     * The Rddbc entity template used to perform reactive database operations.
     */
    @Mock
    private R2dbcEntityTemplate template;

    /**
     * Reactive insert.
     */
    @Mock
    private ReactiveInsertOperation.ReactiveInsert<Instruction> reactiveInsert;

    /**
     * Terminating insert.
     */
    @Mock
    private ReactiveInsertOperation.TerminatingInsert<Instruction> terminatingInsert;

    /**
     * Reactive update.
     */
    @Mock
    private ReactiveUpdateOperation.ReactiveUpdate reactiveUpdate;

    /**
     * Update with query.
     */
    @Mock
    private ReactiveUpdateOperation.UpdateWithQuery updateWithQuery;

    /**
     * Terminating update.
     */
    @Mock
    private ReactiveUpdateOperation.UpdateWithQuery terminatingUpdate;

    /**
     * Reactive delete.
     */
    @Mock
    private ReactiveDeleteOperation.ReactiveDelete reactiveDelete;

    /**
     * Delete with query.
     */
    @Mock
    private ReactiveDeleteOperation.DeleteWithQuery deleteWithQuery;

    /**
     * Terminating delete.
     */
    @Mock
    private ReactiveDeleteOperation.TerminatingDelete terminatingDelete;

    /**
     * Test save.
     */
    @Test
    void testSave() {

        InstructionRepository instructionRepository = new InstructionRepository(template);
        Instruction instruction = Instancio.create(Instruction.class);

        when(template.insert(eq(Instruction.class))).thenReturn(reactiveInsert);
        when(reactiveInsert.into(eq(Instruction.INSTRUCTIONS_TABLE_NAME))).thenReturn(terminatingInsert);
        when(terminatingInsert.using(any(Instruction.class))).thenReturn(Mono.just(instruction));

        var response = instructionRepository.save(instruction);

        StepVerifier.create(response)
                .expectNextMatches(result -> result.equals(instruction))
                .verifyComplete();

        verify(template, times(1)).insert(eq(Instruction.class));
        verify(reactiveInsert, times(1)).into(eq(Instruction.INSTRUCTIONS_TABLE_NAME));
        verify(terminatingInsert, times(1)).using(any(Instruction.class));
        verifyNoMoreInteractions(template);
    }

    /**
     * Test saveAll.
     */
    @Test
    void testSaveAll() {

        InstructionRepository instructionRepository = new InstructionRepository(template);
        List<Instruction> instructions =
                Instancio.ofList(Instruction.class)
                        .size(AT_LEAST_INSTRUCTIONS + new Random().nextInt(NUMBER_OF_ADDITIONAL_INSTRUCTIONS_RANGE))
                        .create();

        when(template.insert(eq(Instruction.class))).thenReturn(reactiveInsert);
        when(reactiveInsert.into(eq(Instruction.INSTRUCTIONS_TABLE_NAME))).thenReturn(terminatingInsert);
        OngoingStubbing<Mono<Instruction>> when = when(terminatingInsert.using(any(Instruction.class)));
        for (var instruction : instructions) {
            when = when.thenReturn(Mono.just(instruction));
        }

        var response = instructionRepository.saveAll(instructions);

        StepVerifier.FirstStep<Instruction> verifier = StepVerifier.create(response);

        for (var instruction : instructions) {
            verifier.expectNextMatches(result -> result.equals(instruction));
        }
        verifier.verifyComplete();

        verify(template, times(instructions.size())).insert(eq(Instruction.class));
        verify(reactiveInsert, times(instructions.size())).into(eq(Instruction.INSTRUCTIONS_TABLE_NAME));
        verify(terminatingInsert, times(instructions.size())).using(any(Instruction.class));
        verifyNoMoreInteractions(template);
    }

    /**
     * Test update.
     */
    @Test
    void testUpdate() {

        InstructionRepository instructionRepository = new InstructionRepository(template);
        Instruction instruction = Instancio.create(Instruction.class);

        when(template.update(eq(ReactiveUpdateOperation.UpdateWithTable.class))).thenReturn(reactiveUpdate);
        when(reactiveUpdate.inTable(eq(Instruction.INSTRUCTIONS_TABLE_NAME))).thenReturn(updateWithQuery);
        when(updateWithQuery.matching(any(Query.class))).thenReturn(terminatingUpdate);
        when(terminatingUpdate.apply(any(Update.class))).thenReturn(Mono.just(instruction.getInstructionId()));

        var response = instructionRepository.update(instruction);

        StepVerifier.create(response)
                .expectNextMatches(result -> result == instruction.getInstructionId())
                .verifyComplete();

        verify(template, times(1)).update(eq(ReactiveUpdateOperation.UpdateWithTable.class));
        verify(reactiveUpdate, times(1)).inTable(eq(Instruction.INSTRUCTIONS_TABLE_NAME));
        verify(updateWithQuery, times(1)).matching(any(Query.class));
        verify(terminatingUpdate, times(1)).apply(any(Update.class));
        verifyNoMoreInteractions(template);
    }

    /**
     * Test updateAll.
     */
    @Test
    void testUpdateAll() {

        InstructionRepository instructionRepository = new InstructionRepository(template);
        List<Instruction> instructions =
                Instancio.ofList(Instruction.class)
                        .size(AT_LEAST_INSTRUCTIONS + new Random().nextInt(NUMBER_OF_ADDITIONAL_INSTRUCTIONS_RANGE))
                        .create();

        when(template.update(eq(ReactiveUpdateOperation.UpdateWithTable.class))).thenReturn(reactiveUpdate);
        when(reactiveUpdate.inTable(eq(Instruction.INSTRUCTIONS_TABLE_NAME))).thenReturn(updateWithQuery);
        when(updateWithQuery.matching(any(Query.class))).thenReturn(terminatingUpdate);
        OngoingStubbing<Mono<Long>> when = when(terminatingUpdate.apply(any(Update.class)));
        for (var instruction : instructions) {
            when = when.thenReturn(Mono.just(instruction.getInstructionId()));
        }

        var response = instructionRepository.updateAll(instructions);

        StepVerifier.FirstStep<Long> verifier = StepVerifier.create(response);
        for (var instruction : instructions) {
            verifier.expectNextMatches(result -> result == instruction.getInstructionId());
        }
        verifier.verifyComplete();

        verify(template, times(instructions.size())).update(eq(ReactiveUpdateOperation.UpdateWithTable.class));
        verify(reactiveUpdate, times(instructions.size())).inTable(eq(Instruction.INSTRUCTIONS_TABLE_NAME));
        verify(updateWithQuery, times(instructions.size())).matching(any(Query.class));
        verify(terminatingUpdate, times(instructions.size())).apply(any(Update.class));
        verifyNoMoreInteractions(template);
    }

    /**
     * Test deleteById.
     */
    @Test
    void testDeleteById() {

        InstructionRepository instructionRepository = new InstructionRepository(template);
        var recipeId = new Random().nextLong();

        when(template.delete(eq(Instruction.class))).thenReturn(reactiveDelete);
        when(reactiveDelete.from(eq(Instruction.INSTRUCTIONS_TABLE_NAME))).thenReturn(deleteWithQuery);
        when(deleteWithQuery.matching(any(Query.class))).thenReturn(terminatingDelete);
        when(terminatingDelete.all()).thenReturn(Mono.just(recipeId));

        var response = instructionRepository.deleteById(recipeId);

        StepVerifier.create(response)
                .expectNextMatches(result -> result == recipeId)
                .verifyComplete();

        verify(template, times(1)).delete(eq(Instruction.class));
        verify(reactiveDelete, times(1)).from(eq(Instruction.INSTRUCTIONS_TABLE_NAME));
        verify(deleteWithQuery, times(1)).matching(any(Query.class));
        verify(terminatingDelete, times(1)).all();
        verifyNoMoreInteractions(template);
    }

    /**
     * Test deleteAllByIds.
     */
    @Test
    void testDeleteAllByIds() {

        InstructionRepository instructionRepository = new InstructionRepository(template);
        var instructionIds =
                LongStream
                        .range(0, AT_LEAST_INSTRUCTIONS + new Random().nextInt(NUMBER_OF_ADDITIONAL_INSTRUCTIONS_RANGE))
                        .boxed()
                        .toList();
        when(template.delete(eq(Instruction.class))).thenReturn(reactiveDelete);
        when(reactiveDelete.from(eq(Instruction.INSTRUCTIONS_TABLE_NAME))).thenReturn(deleteWithQuery);
        when(deleteWithQuery.matching(any(Query.class))).thenReturn(terminatingDelete);
        OngoingStubbing<Mono<Long>> when = when(terminatingDelete.all());
        for (var instructionId : instructionIds) {
            when = when.thenReturn(Mono.just(instructionId));
        }

        var response = instructionRepository.deleteAllByIds(instructionIds);

        StepVerifier.FirstStep<Long> verifier = StepVerifier.create(response);
        for (var instructionId : instructionIds) {
            verifier.expectNextMatches(result -> result == instructionId);
        }
        verifier.verifyComplete();

        verify(template, times(instructionIds.size())).delete(eq(Instruction.class));
        verify(reactiveDelete, times(instructionIds.size())).from(eq(Instruction.INSTRUCTIONS_TABLE_NAME));
        verify(deleteWithQuery, times(instructionIds.size())).matching(any(Query.class));
        verify(terminatingDelete, times(instructionIds.size())).all();
        verifyNoMoreInteractions(template);
    }
}
