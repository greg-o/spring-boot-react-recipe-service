/**
 * Recipe Service.
 * Copyright: none
 *
 * @author Greg-O
 */
package org.grego.recipeservice.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.grego.recipeservice.model.Instruction;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.r2dbc.core.ReactiveUpdateOperation;
import org.springframework.data.relational.core.query.Update;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.stream.Collectors;

import static org.springframework.data.relational.core.query.Criteria.where;
import static org.springframework.data.relational.core.query.Query.query;

/**
 * InstructionRepository for managing Instruction objects in the database.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class InstructionRepository {
    /**
     * Reactive database template used to query the database.
     */
    private final R2dbcEntityTemplate template;

    /**
     * Save instruction.
     * @param instruction
     * @return the Instruction that was saved
     */
    public Mono<Instruction> save(final Instruction instruction) {
        return template.insert(Instruction.class)
                .into(Instruction.INSTRUCTIONS_TABLE_NAME)
                .using(instruction);
    }

    /**
     * Save instructions.
     * @param instructionList
     * @return the Instructions that were saved
     */
    public Flux<Instruction> saveAll(final Collection<Instruction> instructionList) {
        return Flux.concat(instructionList.stream().map(this::save).collect(Collectors.toList()));
    }

    /**
     * Update instruction.
     * @param instruction
     * @return Long.
     */
    public Mono<Long> update(final Instruction instruction) {
        return template.update(ReactiveUpdateOperation.UpdateWithTable.class)
                .inTable(Instruction.INSTRUCTIONS_TABLE_NAME)
                .matching(query(where(Instruction.INSTRUCTION_ID_COLUMN_NAME).is(instruction.getInstructionId())))
                .apply(Update.update(Instruction.INSTRUCTION_COLUMN_NAME, instruction.getInstruction())
                        .set(Instruction.INSTRUCTION_NUMBER_COLUMN_NAME, instruction.getInstructionNumber()));
    }

    /**
     * Update instructions.
     * @param instructionList
     * @return ids for updated instructions
     */
    public Flux<Long> updateAll(final Collection<Instruction> instructionList) {
        return Flux.concat(instructionList.stream().map(this::update).collect(Collectors.toList()));
    }

    /**
     * Delete instruction by instruction id.
     * @param instructionId
     * @return id for deleted instruction
     */
    public Mono<Long> deleteById(final long instructionId) {
        return template.delete(Instruction.class)
                .from(Instruction.INSTRUCTIONS_TABLE_NAME)
                .matching(query(where(Instruction.INSTRUCTION_ID_COLUMN_NAME).is(instructionId)))
                .all();
    }

    /**
     * Delete instructions by instruction ids.
     * @param instructionIdList
     * @return ids for deleted instructions
     */
    public Flux<Long> deleteAllByIds(final Collection<Long> instructionIdList) {
        return Flux.concat(instructionIdList.stream().map(this::deleteById)
                .collect(Collectors.toList()));
    }
}
