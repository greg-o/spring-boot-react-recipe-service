package org.grego.springboot.recipeservice.repository;

import java.util.Collection;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.grego.springboot.recipeservice.model.Instruction;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.r2dbc.core.ReactiveUpdateOperation;
import org.springframework.data.relational.core.query.Update;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.springframework.data.relational.core.query.Criteria.where;
import static org.springframework.data.relational.core.query.Query.query;

@Component
@RequiredArgsConstructor
@Slf4j
public class InstructionRepository {
    private final R2dbcEntityTemplate template;

    public Mono<Instruction> save(Instruction instruction) {
        return template.insert(Instruction.class)
            .into(Instruction.INSTRUCTIONS_TABLE_NAME)
            .using(instruction);
    }

    public Flux<Instruction> saveAll(Collection<Instruction> instructionList) {
        return Flux.concat(instructionList.stream().map(this::save).collect(Collectors.toList()));
    }

    public Mono<Long> update(Instruction instruction) {
        return template.update(ReactiveUpdateOperation.UpdateWithTable.class)
            .inTable(Instruction.INSTRUCTIONS_TABLE_NAME)
            .matching(query(where(Instruction.INSTRUCTION_ID_COLUMN_NAME).is(instruction.getInstructionId())))
            .apply(Update.update(Instruction.INSTRUCTION_COLUMN_NAME, instruction.getInstruction())
                .set(Instruction.INSTRUCTION_NUMBER_COLUMN_NAME, instruction.getInstructionNumber()));
    }

    public Flux<Long> updateAll(Collection<Instruction> instructionList) {
        return Flux.concat(instructionList.stream().map(this::update).collect(Collectors.toList()));
    }

    public Mono<Long> deleteById(long instructionId) {
        return template.delete(Instruction.class)
            .from(Instruction.INSTRUCTIONS_TABLE_NAME)
            .matching(query(where(Instruction.INSTRUCTION_ID_COLUMN_NAME).is(instructionId)))
            .all();
    }

    public Flux<Long> deleteAllByIds(Collection<Long> instructionIdList) {
        return Flux.concat(instructionIdList.stream().map(this::deleteById)
            .collect(Collectors.toList()));
    }
}
