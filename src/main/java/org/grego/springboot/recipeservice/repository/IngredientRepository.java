package org.grego.springboot.recipeservice.repository;

import lombok.RequiredArgsConstructor;
import org.grego.springboot.recipeservice.model.Ingredient;
import org.grego.springboot.recipeservice.model.Instruction;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public interface IngredientRepository extends R2dbcRepository<Ingredient, Long> {
//    Flux<Ingredient> findAllByRecipeId(long recipeId);
}
