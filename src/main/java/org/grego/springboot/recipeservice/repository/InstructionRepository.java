package org.grego.springboot.recipeservice.repository;

import org.grego.springboot.recipeservice.model.Instruction;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public interface InstructionRepository extends R2dbcRepository<Instruction, Long> {
//    Flux<Instruction> findAllByRecipeId(long recipeId);
}
