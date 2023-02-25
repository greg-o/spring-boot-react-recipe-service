package org.grego.springboot.recipeservice.service;

import org.grego.springboot.recipeservice.model.Recipe;
import org.grego.springboot.recipeservice.repository.RecipeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.relational.repository.Lock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.data.relational.core.sql.LockMode;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.stream.IntStream;

@Transactional
@Service
public class RecipeService implements IRecipeService {

    @Autowired
    private RecipeRepository recipeRepository;

    @Override
    @Transactional
    @Lock(LockMode.PESSIMISTIC_READ)
    public Flux<Recipe> getAllRecipes() {
        return recipeRepository.findAll();
    }

    @Override
    @Transactional
    @Lock(LockMode.PESSIMISTIC_READ)
    public Mono<Boolean> recipeExistsById(long id) {
        return recipeRepository.existsById(id);
    }

    @Override
    @Transactional
    @Lock(LockMode.PESSIMISTIC_READ)
    public Mono<Recipe> getRecipeById(long id) {
        return recipeRepository.findById(id);
    }

    @Override
    @Modifying
    @Lock(LockMode.PESSIMISTIC_WRITE)
    public Mono<Recipe> addRecipe(Recipe recipe) {
        recipe.setCreationDateTime(LocalDateTime.now());
        recipe.setLastModifiedDateTime(recipe.getCreationDateTime());
        IntStream
                .range(0, recipe.getIngredients().size())
                .forEach(idx -> recipe.getIngredients().get(idx).setIngredientNumber(idx + 1));
        IntStream
                .range(0, recipe.getInstructions().size())
                .forEach(idx -> recipe.getInstructions().get(idx).setInstructionNumber(idx + 1));

        return recipeRepository.save(recipe);
    }

    @Override
    @Modifying
    @Lock(LockMode.PESSIMISTIC_WRITE)
    public Mono<Recipe> updateRecipe(Recipe recipe) {
        return recipeRepository.findById(recipe.getRecipeId()).map(Optional::of).defaultIfEmpty(Optional.empty())
                .flatMap(optionalRecipe -> {
                    recipe.setCreationDateTime(optionalRecipe.get().getCreationDateTime());
                    recipe.setLastModifiedDateTime(LocalDateTime.now());
                    if (optionalRecipe.isPresent()) {
                        IntStream
                                .range(0, recipe.getIngredients().size())
                                .forEach(idx -> recipe.getIngredients().get(idx).setIngredientNumber(idx + 1));
                        IntStream
                                .range(0, recipe.getInstructions().size())
                                .forEach(idx -> recipe.getInstructions().get(idx).setInstructionNumber(idx + 1));

                        return recipeRepository.save(recipe);
                    }

                    return Mono.empty();
                });
    }

    @Override
    @Modifying
    @Lock(LockMode.PESSIMISTIC_WRITE)
    public Mono<Void> deleteRecipeById(long id) {
        return recipeRepository.deleteById(id);
    }
}
