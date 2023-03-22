package org.grego.springboot.recipeservice.service;

import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.grego.springboot.recipeservice.document.RecipeDoc;
import org.grego.springboot.recipeservice.model.Ingredient;
import org.grego.springboot.recipeservice.model.Instruction;
import org.grego.springboot.recipeservice.model.Recipe;
import org.grego.springboot.recipeservice.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.relational.repository.Lock;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.data.relational.core.sql.LockMode;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuple3;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Transactional
@Service
@RequiredArgsConstructor
public class RecipeService implements IRecipeService {
    private static final String INGREDIENTS_QUERY = """
        select i.ingredient_id, i.ingredient_number, i.ingredient, i.quantity_specifier, i.quantity
        from recipes_ingredients ri
        join ingredients i on i.ingredient_id = ri.ingredients_ingredient_id
    """;
    private static final String INSTRUCTIONS_QUERY = """
        select i.instruction_id , i.instruction_number, i.instruction
        from recipes_instructions ri
        join instructions i on i.instruction_id  = ri.instructions_instruction_id
    """;
    private static final String INGREDIENT_IDS_QUERY = """
        select i.ingredient_id
        from recipes_ingredients ri
        join ingredients i on i.ingredient_id = ri.ingredients_ingredient_id
    """;
    private static final String INSTRUCTION_IDS_QUERY = """
        select i.instruction_id
        from recipes_instructions ri
        join instructions i on i.instruction_id  = ri.instructions_instruction_id
    """;
    private static final String MATCH_RECIPE_ID = "WHERE ri.recipe_recipe_id = :recipeId";
    private static final String INGREDIENTS_MATCHING_QUERY =
            String.format("%s %s", INGREDIENTS_QUERY, MATCH_RECIPE_ID);
    private static final String INSTRUCTIONS_MATCHING_QUERY =
            String.format("%s %s", INSTRUCTIONS_QUERY, MATCH_RECIPE_ID);
    private static final String INGREDIENT_IDS_MATCHING_QUERY =
            String.format("%s %s", INGREDIENT_IDS_QUERY, MATCH_RECIPE_ID);
    private static final String INSTRUCTION_IDS_MATCHING_QUERY =
            String.format("%s %s", INSTRUCTION_IDS_QUERY, MATCH_RECIPE_ID);
    private static final String INSERT_RECIPES_INGREDIENTS =
            "INSERT INTO recipes_ingredients (recipe_recipe_id, ingredients_ingredient_id) VALUES ";
    public static final String INSERT_RECIPES_INSTRUCTIONS =
            "INSERT INTO recipes_instructions (recipe_recipe_id, instructions_instruction_id) VALUES ";
    public static final String DELETE_RECIPES_INGREDIENTS =
            "DELETE FROM recipes_ingredients WHERE recipe_recipe_id IN (%s)";
    public static final String DELETE_RECIPES_INSTRUCTIONS =
            "DELETE FROM recipes_instructions WHERE recipe_recipe_id IN (%s)";
    public static final String RECIPE_ID = "recipeId";
    public static final String DELIMITER = ", ";
    public static final String INSERT_VALUES_FORMAT = "(%d, %d)";

    @Autowired
    private RecipeRepository recipeRepository;
    @Autowired
    private IngredientRepository ingredientRepository;
    @Autowired
    private InstructionRepository instructionRepository;
    @Autowired
    private ElasticSearchRepository elasticSearchRepository;
    @Autowired
    private DatabaseClient client;

    @Override
    @Transactional
    @Lock(LockMode.PESSIMISTIC_READ)
    public Flux<Recipe> getAllRecipes(long startPage, int pageSize) {

        return recipeRepository.findAll(startPage, pageSize).flatMap(recipe ->
            Mono.zip(
                    Mono.just(recipe),
                    getIngredients(recipe.getRecipeId()),
                    getInstructions(recipe.getRecipeId()))
                .map(mergeRecipeWithIngredientsAndInstructions()));
    }

    @Override
    @Transactional
    @Lock(LockMode.PESSIMISTIC_READ)
    public Mono<Long> getRecipeCount() {

        return recipeRepository.countAll();
    }

    @Override
    @Transactional
    @Lock(LockMode.PESSIMISTIC_READ)
    public Mono<Recipe> getRecipeById(long recipeId) {
        return Mono.zip(
                recipeRepository.findById(recipeId),
                getIngredients(recipeId),
                getInstructions(recipeId))
            .map(mergeRecipeWithIngredientsAndInstructions());
    }

    @Override
    @Modifying
    @Lock(LockMode.PESSIMISTIC_WRITE)
    public Mono<Recipe> addRecipe(Recipe recipe) {
        recipe.setCreationDateTime(LocalDateTime.now());
        recipe.setLastModifiedDateTime(recipe.getCreationDateTime());
        orderIngredientsAndInstructions(recipe);

        return recipeRepository.findAllByName(recipe.getName())
                .collectList()
                .flatMap(recipesWithSameName -> {
            var nextVariation = recipesWithSameName
                    .stream()
                    .mapToInt(Recipe::getVariation).max().orElse(0) + 1;

            recipe.setVariation(nextVariation);

            return saveRecipe(recipe).flatMap(savedRecipe -> {
                return elasticSearchRepository.save(RecipeDoc.create(savedRecipe))
                        .then(Mono.just(savedRecipe));
            });
        });
    }

    @Override
    @Modifying
    @Lock(LockMode.PESSIMISTIC_WRITE)
    public Mono<Recipe> updateRecipe(Recipe recipe) {
        return Mono.zip(
                recipeRepository.findById(recipe.getRecipeId()),
                getIngredients(recipe.getRecipeId()),
                getInstructions(recipe.getRecipeId())
            )
            .flatMap(tuple -> {
                tuple.getT1().setIngredients(tuple.getT2());
                tuple.getT1().setInstructions(tuple.getT3());

                orderIngredientsAndInstructions(recipe);

                var existingIngredientIds = tuple.getT2()
                        .stream()
                        .map(Ingredient::getIngredientId)
                        .collect(Collectors.toSet());
                var updatedIngredientsIds = recipe.getIngredients()
                        .stream()
                        .map(Ingredient::getIngredientId)
                        .collect(Collectors.toSet());
                var abandonedIngredientIds =
                        CollectionUtils.subtract(existingIngredientIds, updatedIngredientsIds);
                var ingredientsToUpdate = recipe.getIngredients()
                        .stream()
                        .filter(ingredient -> existingIngredientIds.contains(ingredient.getIngredientId()))
                        .collect(Collectors.toList());
                var ingredientsToAdd = recipe.getIngredients()
                        .stream()
                        .filter(ingredient -> !existingIngredientIds.contains(ingredient.getIngredientId()))
                        .collect(Collectors.toList());

                var existingInstructionIds = tuple.getT3()
                        .stream()
                        .map(Instruction::getInstructionId)
                        .collect(Collectors.toSet());
                var updatedInstructionIds = recipe.getInstructions()
                        .stream()
                        .map(Instruction::getInstructionId)
                        .collect(Collectors.toSet());
                var abandonedInstructionIds =
                        CollectionUtils.subtract(existingInstructionIds, updatedInstructionIds);
                var instructionsToUpdate = recipe.getInstructions()
                        .stream()
                        .filter(instruction -> existingInstructionIds.contains(instruction.getInstructionId()))
                        .collect(Collectors.toList());
                var instructionsToAdd = recipe.getInstructions()
                        .stream()
                        .filter(instruction -> !existingInstructionIds.contains(instruction.getInstructionId()))
                        .collect(Collectors.toList());

                return Mono.zip(
                        recipeRepository.update(recipe),
                        deleteIngredientsByIds(abandonedIngredientIds).collectList(),
                        deleteInstructionsByIds(abandonedInstructionIds).collectList(),
                        updateIngredients(ingredientsToUpdate).collectList(),
                        updateInstructions(instructionsToUpdate).collectList(),
                        saveIngredients(recipe.getRecipeId(), ingredientsToAdd).collectList(),
                        saveInstructions(recipe.getRecipeId(), instructionsToAdd).collectList()
                    ).then(elasticSearchRepository.save(RecipeDoc.create(recipe)).then(Mono.just(recipe)));
            });
    }

    @Override
    @Modifying
    @Lock(LockMode.PESSIMISTIC_WRITE)
    public Mono<Long> deleteRecipeById(long recipeId) {

        var deleteRecipeIngredients = String.format(DELETE_RECIPES_INGREDIENTS, recipeId);
        var deleteRecipeInstructions = String.format(DELETE_RECIPES_INSTRUCTIONS, recipeId);

        return Mono.zip(getIngredientIds(recipeId),
                getInstructionIds(recipeId)
            )
            .flatMap(tuple ->
                Mono.zip(
                    client.sql(deleteRecipeIngredients).fetch().all().collectList(),
                    client.sql(deleteRecipeInstructions).fetch().all().collectList()).then(Mono.just(tuple))
            )
            .flatMap(tuple ->
                Mono.zip(
                    recipeRepository.deleteById(recipeId),
                    ingredientRepository.deleteAllByIds(tuple.getT1()).collectList(),
                    instructionRepository.deleteAllByIds(tuple.getT2()).collectList()
                )
                .map(Tuple2::getT1)
            );
    }

    private Mono<List<Ingredient>> getIngredients(long recipeId) {
        return client.sql(INGREDIENTS_MATCHING_QUERY)
            .bind(RECIPE_ID, recipeId)
            .fetch()
            .all()
            .map(Ingredient::fromRow)
            .collectList();
    }

    private Mono<List<Instruction>> getInstructions(long recipeId) {
        return client.sql(INSTRUCTIONS_MATCHING_QUERY)
            .bind(RECIPE_ID, recipeId)
            .fetch()
            .all()
            .map(Instruction::fromRow)
            .collectList();
    }

    private static Function<Tuple3<Recipe, List<Ingredient>, List<Instruction>>,
            Recipe> mergeRecipeWithIngredientsAndInstructions() {
        return tuple -> {
            tuple.getT1().setIngredients(tuple.getT2());
            tuple.getT1().setInstructions(tuple.getT3());
            return tuple.getT1();
        };
    }

    private static void orderIngredientsAndInstructions(Recipe recipe) {
        IntStream
                .range(0, recipe.getIngredients().size())
                .forEach(idx -> recipe.getIngredients().get(idx).setIngredientNumber(idx + 1));
        IntStream
                .range(0, recipe.getInstructions().size())
                .forEach(idx -> recipe.getInstructions().get(idx).setInstructionNumber(idx + 1));
    }

    private Mono<Recipe> saveRecipe(Recipe recipe) {
        return Mono.zip(recipeRepository.save(recipe),
                    ingredientRepository.saveAll(recipe.getIngredients()).collectList(),
                    instructionRepository.saveAll(recipe.getInstructions()).collectList()
                )
                .flatMap(tuple -> {
                    var ingredientsQuery = INSERT_RECIPES_INGREDIENTS.concat(
                        tuple.getT2()
                            .stream()
                            .map(ingredient -> String.format(INSERT_VALUES_FORMAT,
                                    tuple.getT1().getRecipeId(), ingredient.getIngredientId()))
                            .collect(Collectors.joining(DELIMITER)));
                    var instructionsQuery = INSERT_RECIPES_INSTRUCTIONS.concat(
                        tuple.getT3()
                            .stream()
                            .map(instruction -> String.format(INSERT_VALUES_FORMAT,
                                    tuple.getT1().getRecipeId(), instruction.getInstructionId()))
                            .collect(Collectors.joining(DELIMITER)));

                    return Mono.when(
                            client.sql(ingredientsQuery).fetch().all().collectList(),
                            client.sql(instructionsQuery).fetch().all().collectList())
                        .then(Mono.just(tuple.getT1()));
                });
    }

    private Mono<List<Long>> getIngredientIds(long recipeId) {
        return client.sql(INGREDIENT_IDS_MATCHING_QUERY)
                .bind(RECIPE_ID, recipeId)
                .fetch()
                .all()
                .map(results -> Long.parseLong(results.get(Ingredient.INGREDIENT_ID_COLUMN_NAME).toString()))
                .collectList();
    }

    private Mono<List<Long>> getInstructionIds(long recipeId) {
        return client.sql(INSTRUCTION_IDS_MATCHING_QUERY)
                .bind(RECIPE_ID, recipeId)
                .fetch()
                .all()
                .map(results -> Long.parseLong(results.get(Instruction.INSTRUCTION_ID_COLUMN_NAME).toString()))
                .collectList();
    }

    private Flux<Long> deleteIngredientsByIds(Collection<Long> ingredientsIds) {
        if (ingredientsIds.isEmpty()) {
            return Flux.empty();
        } else {
            var deleteRecipeIngredients = String.format(DELETE_RECIPES_INGREDIENTS,
                    ingredientsIds
                            .stream()
                            .map(Object::toString)
                            .collect(Collectors.joining(DELIMITER)));

            return client.sql(deleteRecipeIngredients).fetch().all()
                .thenMany(ingredientRepository.deleteAllByIds(ingredientsIds));
        }
    }

    private Flux<Long> deleteInstructionsByIds(Collection<Long> instructionsIds) {
        if (instructionsIds.isEmpty()) {
            return Flux.empty();
        } else {
            var deleteRecipeInstructions = String.format(DELETE_RECIPES_INSTRUCTIONS,
                    instructionsIds
                            .stream()
                            .map(instructionId -> instructionId.toString())
                            .collect(Collectors.joining(DELIMITER)));

            return client.sql(deleteRecipeInstructions).fetch().all()
                    .thenMany(instructionRepository.deleteAllByIds(instructionsIds));
        }
    }

    private Flux<Long> updateIngredients(List<Ingredient> ingredients) {
        if (ingredients.isEmpty()) {
            return Flux.empty();
        } else {
            return ingredientRepository.updateAll(ingredients);
        }
    }

    private Flux<Long> updateInstructions(List<Instruction> instructions) {
        if (instructions.isEmpty()) {
            return Flux.empty();
        } else {
            return instructionRepository.updateAll(instructions);
        }
    }

    private Flux<Ingredient> saveIngredients(long recipeId, List<Ingredient> ingredients) {
        if (ingredients.isEmpty()) {
            return Flux.empty();
        } else {
            Flux<Ingredient> savedIngredients = ingredientRepository.saveAll(ingredients);

            return savedIngredients.map(ingredient -> {
                    var insertRecipeIngredients = INSERT_RECIPES_INGREDIENTS
                            .concat(String.format(INSERT_VALUES_FORMAT, recipeId, ingredient.getIngredientId()));

                    return client.sql(insertRecipeIngredients).fetch().all().then(Mono.just(ingredient));
                }).thenMany(savedIngredients);
        }
    }

    private Flux<Instruction> saveInstructions(long recipeId, List<Instruction> instructions) {
        if (instructions.isEmpty()) {
            return Flux.empty();
        } else {
            Flux<Instruction> savedInstructions = instructionRepository.saveAll(instructions);

            return savedInstructions.map(instruction -> {
                    var insertRecipeInstructions = INSERT_RECIPES_INSTRUCTIONS
                            .concat(String.format(INSERT_VALUES_FORMAT, recipeId, instruction.getInstructionId()));

                    return client.sql(insertRecipeInstructions).fetch().all().then(Mono.just(instruction));
                }).thenMany(savedInstructions);
        }
    }
}
