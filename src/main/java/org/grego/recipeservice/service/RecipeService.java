/**
 * Recipe Service.
 * Copyright: none
 *
 * @author Greg-O
 */
package org.grego.recipeservice.service;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryStringQuery;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.search.ResponseBody;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.grego.recipeservice.document.RecipeDoc;
import org.grego.recipeservice.mapping.RecipeMapper;
import org.grego.recipeservice.repository.IngredientRepository;
import org.grego.recipeservice.repository.InstructionRepository;
import org.grego.recipeservice.repository.RecipeRepository;
import org.grego.recipeservice.model.Ingredient;
import org.grego.recipeservice.model.Instruction;
import org.grego.recipeservice.model.Recipe;
import org.grego.recipeservice.repository.RecipeSearchRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.client.elc.ReactiveElasticsearchClient;
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.relational.core.sql.LockMode;
import org.springframework.data.relational.repository.Lock;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuple3;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Transactional
@Service
@RequiredArgsConstructor
public class RecipeService implements IRecipeService {
    /**
     * SQL command for inserting to recipes_instructions table.
     */
    public static final String INSERT_RECIPES_INSTRUCTIONS =
            "INSERT INTO recipes_instructions (recipe_recipe_id, instructions_instruction_id) VALUES ";

    /**
     * SQL command for deleting recipes_ingredients.
     */
    public static final String DELETE_RECIPES_INGREDIENTS =
            "DELETE FROM recipes_ingredients WHERE recipe_recipe_id IN (%s)";
    /**
     * SQL command for deleting recipes_instructions.
     */
    public static final String DELETE_RECIPES_INSTRUCTIONS =
            "DELETE FROM recipes_instructions WHERE recipe_recipe_id IN (%s)";

    /**
     * Name for recipe id.
     */
    public static final String RECIPE_ID = "recipeId";

    /**
     * Delimiter used in SQL statements.
     */
    public static final String DELIMITER = ", ";

    /**
     * Insert values format string.
     */
    public static final String INSERT_VALUES_FORMAT = "(%d, %d)";

    /**
     * SQL fragment to select recipes_ingredients.
     */
    private static final String INGREDIENTS_QUERY = """
                select i.ingredient_id, i.ingredient_number, i.ingredient, i.quantity_specifier, i.quantity
                from recipes_ingredients ri
                join ingredients i on i.ingredient_id = ri.ingredients_ingredient_id
            """;

    /**
     * SQL fragment to select recipes_instructions.
     */
    private static final String INSTRUCTIONS_QUERY = """
                select i.instruction_id , i.instruction_number, i.instruction
                from recipes_instructions ri
                join instructions i on i.instruction_id  = ri.instructions_instruction_id
            """;

    /**
     * SQL fragment to select ingredients ids.
     */
    private static final String INGREDIENT_IDS_QUERY = """
                select i.ingredient_id
                from recipes_ingredients ri
                join ingredients i on i.ingredient_id = ri.ingredients_ingredient_id
            """;

    /**
     * SQL fragment to select instruction ids.
     */
    private static final String INSTRUCTION_IDS_QUERY = """
                select i.instruction_id
                from recipes_instructions ri
                join instructions i on i.instruction_id  = ri.instructions_instruction_id
            """;

    /**
     * SQL fragment to match recipe id.
     */
    private static final String MATCH_RECIPE_ID = "WHERE ri.recipe_recipe_id = :recipeId";

    /**
     * SQL comment to get ingredients that match the recipe id.
     */
    static final String INGREDIENTS_MATCHING_QUERY =
            String.format("%s %s", INGREDIENTS_QUERY, MATCH_RECIPE_ID);

    /**
     * SQL command to get the instructions that match the recipe id.
     */
    static final String INSTRUCTIONS_MATCHING_QUERY =
            String.format("%s %s", INSTRUCTIONS_QUERY, MATCH_RECIPE_ID);

    /**
     * SQL command to get the ingredient ids that match the recipe id.
     */
    private static final String INGREDIENT_IDS_MATCHING_QUERY =
            String.format("%s %s", INGREDIENT_IDS_QUERY, MATCH_RECIPE_ID);

    /**
     * SQL command to get the instruction ids that match the recipe id.
     */
    private static final String INSTRUCTION_IDS_MATCHING_QUERY =
            String.format("%s %s", INSTRUCTION_IDS_QUERY, MATCH_RECIPE_ID);

    /**
     * SQL fragment to insert into the recipes_ingredients table.
     */
    private static final String INSERT_RECIPES_INGREDIENTS =
            "INSERT INTO recipes_ingredients (recipe_recipe_id, ingredients_ingredient_id) VALUES ";

    /**
     * Elasticsearch operations.
     */
    @Autowired
    private ReactiveElasticsearchOperations elasticsearchOperations;

    /**
     * Elasticsearch client.
     */
    @Autowired
    private ReactiveElasticsearchClient elasticsearchClient;

    /**
     * Recipe repository to perform operations in the database on the recipe table.
     */
    @Autowired
    private RecipeRepository recipeRepository;


    /**
     * Ingredient repository to perform operations in the database on the ingredient table.
     */
    @Autowired
    private IngredientRepository ingredientRepository;


    /**
     * Instruction repository to perform operations in the database on the instruction table.
     */
    @Autowired
    private InstructionRepository instructionRepository;

    /**
     * Recipe search repository to perform operations in the search engine.
     */
    @Autowired
    private RecipeSearchRepository recipeSearchRepository;

    /**
     * Database client to perform operation in the database.
     */
    @Autowired
    private DatabaseClient client;

    /**
     * Mapper to convert Recipe to RecipeDoc.
     */
    @Autowired
    private RecipeMapper recipeMapper;

    private static Function<Tuple3<Recipe, List<Ingredient>, List<Instruction>>,
            Recipe> mergeRecipeWithIngredientsAndInstructions() {
        return tuple -> {
            tuple.getT1().setIngredients(tuple.getT2());
            tuple.getT1().setInstructions(tuple.getT3());
            return tuple.getT1();
        };
    }

    private static void orderIngredientsAndInstructions(final Recipe recipe) {
        IntStream
                .range(0, recipe.getIngredients().size())
                .forEach(idx -> recipe.getIngredients().get(idx).setIngredientNumber(idx + 1));
        IntStream
                .range(0, recipe.getInstructions().size())
                .forEach(idx -> recipe.getInstructions().get(idx).setInstructionNumber(idx + 1));
    }

    /**
     * Get all recipes.
     * @param startPage
     * @param pageSize
     * @return All the recipes for the page
     */
    @Override
    @Transactional
    @Lock(LockMode.PESSIMISTIC_READ)
    public Flux<Recipe> getAllRecipes(final long startPage, final int pageSize) {

        Flux<Recipe> results = recipeRepository.findAll(startPage, pageSize)
            .flatMap(recipe ->
                Mono.zip(
                        Mono.just(recipe),
                        getIngredients(recipe.getRecipeId()),
                        getInstructions(recipe.getRecipeId()))
                    .map(mergeRecipeWithIngredientsAndInstructions()));

        return results.switchIfEmpty(Flux.empty());
    }

    /**
     * Get the number of recipes.
     * @return The number of recipes
     */
    @Override
    @Transactional
    @Lock(LockMode.PESSIMISTIC_READ)
    public Mono<Long> getRecipeCount() {
        return recipeRepository.countAll();
    }

    /**
     * Get recipe by recipe id.
     * @param recipeId
     * @return Recipe for the recipe id
     */
    @Override
    @Transactional
    @Lock(LockMode.PESSIMISTIC_READ)
    public Mono<Recipe> getRecipeById(final long recipeId) {
        return Mono.zip(
                        recipeRepository.findById(recipeId),
                        getIngredients(recipeId),
                        getInstructions(recipeId))
                .map(mergeRecipeWithIngredientsAndInstructions());
    }

    /**
     * Add a recipe.
     * @param recipe
     * @return Recipe that was saved
     */
    @Override
    @Modifying
    @Lock(LockMode.PESSIMISTIC_WRITE)
    public Mono<Recipe> addRecipe(final Recipe recipe) {
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

                    return saveRecipe(recipe).flatMap(savedRecipe ->
                            recipeSearchRepository.save(recipeMapper.toDoc(savedRecipe))
                                    .then(Mono.just(savedRecipe)));
                });
    }

    /**
     * Update a recipe.
     * @param recipe
     * @return Recipe that was updated
     */
    @Override
    @Modifying
    @Lock(LockMode.PESSIMISTIC_WRITE)
    public Mono<Recipe> updateRecipe(final Recipe recipe) {
        return Mono.zip(
                        recipeRepository.findById(recipe.getRecipeId()),
                        getIngredients(recipe.getRecipeId()),
                        getInstructions(recipe.getRecipeId())
                )
                .flatMap(tuple -> {
                    recipe.setVariation(tuple.getT1().getVariation());
                    recipe.setCreationDateTime(tuple.getT1().getCreationDateTime());
                    recipe.setLastModifiedDateTime(LocalDateTime.now());

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
                    ).map(tuple7 -> recipeSearchRepository.save(recipeMapper.toDoc(recipe)).then())
                    .then(Mono.just(recipe));
                });
    }

    /**
     * Delete recipe by recipe id.
     * @param recipeId
     * @return Id of the deleted recipe
     */
    @Override
    @Modifying
    @Lock(LockMode.PESSIMISTIC_WRITE)
    public Mono<Long> deleteRecipeById(final long recipeId) {

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
                                        instructionRepository.deleteAllByIds(tuple.getT2()).collectList(),
                                        recipeSearchRepository.deleteById(recipeId)
                                )
                                .map(Tuple2::getT1)
                );
    }

    /**
     * Search for recipes by search string.
     * @param searchText
     * @return Search results with RecipeDocs for the search string
     */
    @Override
    public Mono<ResponseBody<RecipeDoc>> searchRecipes(final String searchText) {
        QueryStringQuery queryString = new QueryStringQuery.Builder()
                .query(searchText)
                .build();
        Query query = new Query.Builder()
                .queryString(queryString)
                .build();
        SearchRequest searchRequest = new SearchRequest.Builder()
                .index("recipes")
                .query(query)
                .explain(true)
                .build();
        return elasticsearchClient.search(searchRequest, RecipeDoc.class);
    }

    private Mono<List<Ingredient>> getIngredients(final long recipeId) {
        return client.sql(INGREDIENTS_MATCHING_QUERY)
                .bind(RECIPE_ID, recipeId)
                .fetch()
                .all()
                .map(Ingredient::fromRow)
                .filter(Objects::nonNull)
                .collectList();
    }

    private Mono<List<Instruction>> getInstructions(final long recipeId) {
        return client.sql(INSTRUCTIONS_MATCHING_QUERY)
                .bind(RECIPE_ID, recipeId)
                .fetch()
                .all()
                .map(Instruction::fromRow)
                .filter(Objects::nonNull)
                .collectList();
    }

    private Mono<Recipe> saveRecipe(final Recipe recipe) {
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

    private Mono<List<Long>> getIngredientIds(final long recipeId) {
        return client.sql(INGREDIENT_IDS_MATCHING_QUERY)
                .bind(RECIPE_ID, recipeId)
                .fetch()
                .all()
                .map(results -> Long.parseLong(results.get(Ingredient.INGREDIENT_ID_COLUMN_NAME).toString()))
                .collectList();
    }

    private Mono<List<Long>> getInstructionIds(final long recipeId) {
        return client.sql(INSTRUCTION_IDS_MATCHING_QUERY)
                .bind(RECIPE_ID, recipeId)
                .fetch()
                .all()
                .map(results -> Long.parseLong(results.get(Instruction.INSTRUCTION_ID_COLUMN_NAME).toString()))
                .collectList();
    }

    private Flux<Long> deleteIngredientsByIds(final Collection<Long> ingredientsIds) {
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

    private Flux<Long> deleteInstructionsByIds(final Collection<Long> instructionsIds) {
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

    private Flux<Long> updateIngredients(final List<Ingredient> ingredients) {
        if (ingredients.isEmpty()) {
            return Flux.empty();
        } else {
            return ingredientRepository.updateAll(ingredients);
        }
    }

    private Flux<Long> updateInstructions(final List<Instruction> instructions) {
        if (instructions.isEmpty()) {
            return Flux.empty();
        } else {
            return instructionRepository.updateAll(instructions);
        }
    }

    private Flux<Ingredient> saveIngredients(final long recipeId, final List<Ingredient> ingredients) {
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

    private Flux<Instruction> saveInstructions(final long recipeId, final List<Instruction> instructions) {
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
