package org.grego.recipeservice.service;

import co.elastic.clients.elasticsearch._types.ShardStatistics;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.grego.recipeservice.document.RecipeDoc;
import org.grego.recipeservice.model.Ingredient;
import org.grego.recipeservice.model.Instruction;
import org.grego.recipeservice.model.Recipe;
import org.grego.recipeservice.repository.IngredientRepository;
import org.grego.recipeservice.repository.InstructionRepository;
import org.grego.recipeservice.repository.RecipeRepository;
import org.grego.recipeservice.repository.RecipeSearchRepository;
import org.instancio.Instancio;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.data.elasticsearch.client.elc.ReactiveElasticsearchClient;
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.r2dbc.core.FetchSpec;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests for the RecipeService using mock ReactiveElasticsearchOperations,
 * ReactiveElasticsearchClient, RecipeRepository, IngredientRepository,
 * InstructionRepository, RecipeSearchRepository, and DatabaseClient.
 */
@ExtendWith(MockitoExtension.class)
@RunWith(PowerMockRunner.class)
//@PrepareForTest( { Service.class })
public class RecipeServiceTest {
    /**
     * Count of zero.
     */
    public static final long COUNT_ZERO = 0;
    /**
     * First page.
     */
    private static final int PAGE_NUMBER_1 = 1;

    /**
     * Page size of 10.
     */
    private static final int PAGE_SIZE_10 = 10;

    /**
     * Recipe id for a recipe that doesn't exist.
     */
    public static final long NON_EXISTENT_RECIPE_ID = 0;

    /**
     * The value returned for update.
     */
    public static final long LONG_RETURN_VALUE = 0L;

    /**
     * Search text to search Elasticsearch.
     */
    public static final String SEARCH_TEXT = "search text";

    /**
     * Elasticsearch took.
     */
    public static final int TOOK_ELASTICSEARCH = 3;

    /**
     * Ingredient field name mapping.
     */
    private static final Map<String, String> INGREDIENT_FIELD_NAME_MAPPING = Map.of(
            "ingredientId", Ingredient.INGREDIENT_ID_COLUMN_NAME,
            "quantity", Ingredient.QUANTITY_COLUMN_NAME,
            "ingredient", Ingredient.INGREDIENT_COLUMN_NAME,
            "ingredientNumber", Ingredient.INGREDIENT_NUMBER_COLUMN_NAME,
            "quantitySpecifier", Ingredient.QUANTITY_SPECIFIER_COLUMN_NAME
    );

    /**
     * Instruction field name mapping.
     */
    private static final Map<String, String> INSTRUCTION_FIELD_NAME_MAPPING = Map.of(
            "instructionId", Instruction.INSTRUCTION_ID_COLUMN_NAME,
            "instructionNumber", Instruction.INSTRUCTION_NUMBER_COLUMN_NAME,
            "instruction", Instruction.INSTRUCTION_COLUMN_NAME
    );

    /**
     * Instance of the RecipeService that is being tested.
     */
    @InjectMocks
    private RecipeService recipeService;

    /**
     *  Elasticsearch operations.
     */
    @Mock
    private ReactiveElasticsearchOperations elasticsearchOperations;

    /**
     * Elasticsearch client.
     */
    @Mock
    private ReactiveElasticsearchClient elasticsearchClient;

    /**
     * Recipe repository to perform operations in the database on the recipe table.
     */
    @Mock
    private RecipeRepository recipeRepository;


    /**
     * Ingredient repository to perform operations in the database on the ingredient table.
     */
    @Mock
    private IngredientRepository ingredientRepository;


    /**
     * Instruction repository to perform operations in the database on the instruction table.
     */
    @Mock
    private InstructionRepository instructionRepository;

    /**
     * Recipe search repository to perform operations in the search engine.
     */
    @Mock
    private RecipeSearchRepository recipeSearchRepository;

    /**
     * Database client to perform operation in the database.
     */
    @Mock
    private DatabaseClient client;

    /**
     * Execute spec for ingredients.
     */
    @Mock
    private DatabaseClient.GenericExecuteSpec ingredientsExecuteSpec;

    /**
     * Execute spec for instructions.
     */
    @Mock
    private DatabaseClient.GenericExecuteSpec instructionsExecuteSpec;

    /**
     * Fetch spec for ingredients.
     */
    @Mock
    private FetchSpec<Map<String, Object>> ingredientsFetchSpec;

    /**
     * Fetch spec for instructions.
     */
    @Mock
    private FetchSpec<Map<String, Object>> instructionsFetchSpec;

    /**
     * To convert objects to JSON and JSON to maps.
     */
    private ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Test getAllRecipes with no recipes.
     */
    @Test
    void testGetAllRecipesEmpty() {

        Flux<Recipe> recipeFlux = Flux.empty();

        when(recipeRepository.findAll(anyLong(), anyInt())).thenReturn(recipeFlux);

        var response = recipeService.getAllRecipes(PAGE_NUMBER_1, PAGE_SIZE_10);

        StepVerifier.create(response).verifyComplete();

        verify(recipeRepository, times(1)).findAll(anyLong(), anyInt());
        verifyNoMoreInteractions(elasticsearchOperations, elasticsearchClient, recipeRepository,
            ingredientRepository, instructionRepository, recipeSearchRepository, client);
    }

    /**
     * Test getAllRecipes with one recipe.
     */
    @Test
    void testGetAllRecipesOneRecipe() {

        var recipe = Instancio.create(Recipe.class);

        Flux<Recipe> recipeFlux = Flux.just(getRecipeWithoutIngredientsOrInstructions(recipe));
        Flux<Map<String, Object>> ingredientsMapFlux = Flux.fromIterable(getIngredientMaps(recipe.getIngredients()));
        Flux<Map<String, Object>> instructionsMapFlux = Flux.fromIterable(getInstructionMaps(recipe.getInstructions()));

        when(recipeRepository.findAll(anyLong(), anyInt())).thenReturn(recipeFlux);
        when(client.sql(eq(RecipeService.INGREDIENTS_MATCHING_QUERY))).thenReturn(ingredientsExecuteSpec);
        when(ingredientsExecuteSpec.bind(anyString(), anyLong())).thenReturn(ingredientsExecuteSpec);
        when(ingredientsExecuteSpec.fetch()).thenReturn(ingredientsFetchSpec);
        when(ingredientsFetchSpec.all()).thenReturn(ingredientsMapFlux);
        when(client.sql(eq(RecipeService.INSTRUCTIONS_MATCHING_QUERY))).thenReturn(instructionsExecuteSpec);
        when(instructionsExecuteSpec.bind(anyString(), anyLong())).thenReturn(instructionsExecuteSpec);
        when(instructionsExecuteSpec.fetch()).thenReturn(instructionsFetchSpec);
        when(instructionsFetchSpec.all()).thenReturn(instructionsMapFlux);

        var response = recipeService.getAllRecipes(PAGE_NUMBER_1, PAGE_SIZE_10);

        StepVerifier.create(response)
            .expectNextMatches(result -> {
                return result.equals(recipe);
            })
            .verifyComplete();

        verify(recipeRepository, times(1)).findAll(anyLong(), anyInt());
        verify(recipeRepository, times(1)).findAll(anyLong(), anyInt());
        verify(client, times(1)).sql(eq(RecipeService.INGREDIENTS_MATCHING_QUERY));
        verify(ingredientsExecuteSpec, times(1)).bind(anyString(), anyLong());
        verify(ingredientsExecuteSpec, times(1)).fetch();
        verify(ingredientsFetchSpec, times(1)).all();
        verify(client, times(1)).sql(eq(RecipeService.INSTRUCTIONS_MATCHING_QUERY));
        verify(instructionsExecuteSpec, times(1)).bind(anyString(), anyLong());
        verify(instructionsExecuteSpec, times(1)).fetch();
        verify(instructionsFetchSpec, times(1)).all();
        verifyNoMoreInteractions(elasticsearchOperations, elasticsearchClient, recipeRepository,
            ingredientRepository, instructionRepository, recipeSearchRepository, client);
    }

    /**
     * Test getRecipeCount.
     */
    @Test
    void testGetRecipeCount() {

        when(recipeRepository.countAll()).thenReturn(Mono.just(COUNT_ZERO));

        var response = recipeService.getRecipeCount();

        StepVerifier.create(response)
            .expectNextMatches(result -> result == COUNT_ZERO)
            .verifyComplete();

        verify(recipeRepository, times(1)).countAll();
        verifyNoMoreInteractions(elasticsearchOperations, elasticsearchClient, recipeRepository,
            ingredientRepository, instructionRepository, recipeSearchRepository, client);
    }

    /**
     * Test getRecipeById where recipe does not exist.
     */
    @Test
    void testGetRecipesByIdDoesNotExist() {

        Mono<Recipe> recipeMono = Mono.empty();
        Flux<Map<String, Object>> ingredientsMapFlux = Flux.empty();
        Flux<Map<String, Object>> instructionsMapFlux = Flux.empty();

        when(recipeRepository.findById(anyLong())).thenReturn(recipeMono);
        when(client.sql(eq(RecipeService.INGREDIENTS_MATCHING_QUERY))).thenReturn(ingredientsExecuteSpec);
        when(ingredientsExecuteSpec.bind(anyString(), anyLong())).thenReturn(ingredientsExecuteSpec);
        when(ingredientsExecuteSpec.fetch()).thenReturn(ingredientsFetchSpec);
        when(ingredientsFetchSpec.all()).thenReturn(ingredientsMapFlux);
        when(client.sql(eq(RecipeService.INSTRUCTIONS_MATCHING_QUERY))).thenReturn(instructionsExecuteSpec);
        when(instructionsExecuteSpec.bind(anyString(), anyLong())).thenReturn(instructionsExecuteSpec);
        when(instructionsExecuteSpec.fetch()).thenReturn(instructionsFetchSpec);
        when(instructionsFetchSpec.all()).thenReturn(instructionsMapFlux);

        var response = recipeService.getRecipeById(NON_EXISTENT_RECIPE_ID);

        StepVerifier.create(response).verifyComplete();

        verify(recipeRepository, times(1)).findById(anyLong());
        verify(client, times(1)).sql(eq(RecipeService.INGREDIENTS_MATCHING_QUERY));
        verify(ingredientsExecuteSpec, times(1)).bind(anyString(), anyLong());
        verify(ingredientsExecuteSpec, times(1)).fetch();
        verify(ingredientsFetchSpec, times(1)).all();
        verify(client, times(1)).sql(eq(RecipeService.INSTRUCTIONS_MATCHING_QUERY));
        verify(instructionsExecuteSpec, times(1)).bind(anyString(), anyLong());
        verify(instructionsExecuteSpec, times(1)).fetch();
        verify(instructionsFetchSpec, times(1)).all();
        verifyNoMoreInteractions(elasticsearchOperations, elasticsearchClient, recipeRepository,
            ingredientRepository, instructionRepository, recipeSearchRepository, client);
    }

    /**
     * Test getRecipeById where recipe id exists.
     */
    @Test
    void testGetRecipesByIdExists() {

        var recipe = Instancio.create(Recipe.class);

        Mono<Recipe> recipeMono = Mono.just(getRecipeWithoutIngredientsOrInstructions(recipe));
        Flux<Map<String, Object>> ingredientsMapFlux = Flux.fromIterable(getIngredientMaps(recipe.getIngredients()));
        Flux<Map<String, Object>> instructionsMapFlux = Flux.fromIterable(getInstructionMaps(recipe.getInstructions()));

        when(recipeRepository.findById(anyLong())).thenReturn(recipeMono);
        when(client.sql(eq(RecipeService.INGREDIENTS_MATCHING_QUERY))).thenReturn(ingredientsExecuteSpec);
        when(ingredientsExecuteSpec.bind(anyString(), anyLong())).thenReturn(ingredientsExecuteSpec);
        when(ingredientsExecuteSpec.fetch()).thenReturn(ingredientsFetchSpec);
        when(ingredientsFetchSpec.all()).thenReturn(ingredientsMapFlux);
        when(client.sql(eq(RecipeService.INSTRUCTIONS_MATCHING_QUERY))).thenReturn(instructionsExecuteSpec);
        when(instructionsExecuteSpec.bind(anyString(), anyLong())).thenReturn(instructionsExecuteSpec);
        when(instructionsExecuteSpec.fetch()).thenReturn(instructionsFetchSpec);
        when(instructionsFetchSpec.all()).thenReturn(instructionsMapFlux);

        var response = recipeService.getRecipeById(NON_EXISTENT_RECIPE_ID);

        StepVerifier.create(response)
            .expectNextMatches(result -> {
                return result.equals(recipe);
            })
            .verifyComplete();

        verify(recipeRepository, times(1)).findById(anyLong());
        verify(client, times(1)).sql(eq(RecipeService.INGREDIENTS_MATCHING_QUERY));
        verify(ingredientsExecuteSpec, times(1)).bind(anyString(), anyLong());
        verify(ingredientsExecuteSpec, times(1)).fetch();
        verify(ingredientsFetchSpec, times(1)).all();
        verify(client, times(1)).sql(eq(RecipeService.INSTRUCTIONS_MATCHING_QUERY));
        verify(instructionsExecuteSpec, times(1)).bind(anyString(), anyLong());
        verify(instructionsExecuteSpec, times(1)).fetch();
        verify(instructionsFetchSpec, times(1)).all();
        verifyNoMoreInteractions(elasticsearchOperations, elasticsearchClient, recipeRepository,
            ingredientRepository, instructionRepository, recipeSearchRepository, client);
    }

    /**
     * Test addRecipe.
     */
    @Test
    void testAddRecipe() {

        var recipe = Instancio.create(Recipe.class);

        Flux<Recipe> existingRecipesFlux = Flux.empty();
        Flux<Ingredient> ingredientsFlux = Flux.fromIterable(recipe.getIngredients());
        Flux<Instruction> instructionsFlux = Flux.fromIterable(recipe.getInstructions());

        when(recipeRepository.findAllByName(anyString())).thenReturn(existingRecipesFlux);
        when(recipeRepository.save(any(Recipe.class))).thenReturn(Mono.just(recipe));
        when(ingredientRepository.saveAll(any(List.class))).thenReturn(ingredientsFlux);
        when(instructionRepository.saveAll(any(List.class))).thenReturn(instructionsFlux);
        when(client.sql(anyString()))
            .thenReturn(instructionsExecuteSpec)
            .thenReturn(ingredientsExecuteSpec);
        when(instructionsExecuteSpec.fetch()).thenReturn(instructionsFetchSpec);
        when(ingredientsExecuteSpec.fetch()).thenReturn(ingredientsFetchSpec);
        when(instructionsFetchSpec.all()).thenReturn(Flux.empty());
        when(ingredientsFetchSpec.all()).thenReturn(Flux.empty());
        when(recipeSearchRepository.save(any(RecipeDoc.class))).thenReturn(Mono.empty());

        var response = recipeService.addRecipe(recipe);

        StepVerifier.create(response)
            .expectNextMatches(result -> {
                return result.equals(recipe);
            })
            .verifyComplete();

        verify(recipeRepository, times(1)).findAllByName(anyString());
        verify(recipeRepository, times(1)).save(any(Recipe.class));
        verify(ingredientRepository, times(1)).saveAll(any(List.class));
        verify(instructionRepository, times(1)).saveAll(any(List.class));
        verify(client, times(2)).sql(anyString());
        verify(instructionsExecuteSpec, times(1)).fetch();
        verify(ingredientsExecuteSpec, times(1)).fetch();
        verify(instructionsFetchSpec, times(1)).all();
        verify(ingredientsFetchSpec, times(1)).all();
        verify(recipeSearchRepository, times(1)).save(any(RecipeDoc.class));
        verifyNoMoreInteractions(elasticsearchOperations, elasticsearchClient, recipeRepository,
            ingredientRepository, instructionRepository, recipeSearchRepository, client);
    }

    /**
     * Test updateRecipe update ingredients and instructions.
     */
    @Test
    void testUpdateRecipeUpdateIngredientsAndInstructions() {

        var existingRecipe = Instancio.create(Recipe.class);
        var updatedRecipe = Recipe.builder()
            .recipeId(existingRecipe.getRecipeId())
            .name(existingRecipe.getName())
            .variation(existingRecipe.getVariation())
            .description(existingRecipe.getDescription())
            .version(existingRecipe.getVersion())
            .creationDateTime(existingRecipe.getCreationDateTime())
            .lastModifiedDateTime(existingRecipe.getLastModifiedDateTime())
            .ingredients(existingRecipe.getIngredients())
            .instructions(existingRecipe.getInstructions())
            .build();

        Mono<Recipe> existingRecipeMono = Mono.just(existingRecipe);

        when(recipeRepository.findById(anyLong())).thenReturn(existingRecipeMono);
        when(client.sql(anyString()))
            .thenReturn(ingredientsExecuteSpec)
            .thenReturn(instructionsExecuteSpec);
        when(ingredientsExecuteSpec.bind(anyString(), anyLong())).thenReturn(ingredientsExecuteSpec);
        when(ingredientsExecuteSpec.fetch()).thenReturn(ingredientsFetchSpec);
        when(ingredientsFetchSpec.all()).thenReturn(Flux.fromIterable(getIngredientMaps(existingRecipe.getIngredients())));
        when(instructionsExecuteSpec.bind(anyString(), anyLong())).thenReturn(instructionsExecuteSpec);
        when(instructionsExecuteSpec.fetch()).thenReturn(instructionsFetchSpec);
        when(instructionsFetchSpec.all()).thenReturn(Flux.fromIterable(getInstructionMaps(existingRecipe.getInstructions())));
        when(recipeRepository.update(any(Recipe.class))).thenReturn(Mono.just(LONG_RETURN_VALUE));
        when(ingredientRepository.updateAll(any(List.class))).thenReturn(Flux.fromIterable(updatedRecipe.getIngredients()));
        when(instructionRepository.updateAll(any(List.class))).thenReturn(Flux.fromIterable(updatedRecipe.getInstructions()));
        when(recipeSearchRepository.save(any(RecipeDoc.class))).thenReturn(Mono.empty());

        var response = recipeService.updateRecipe(updatedRecipe);

        StepVerifier.create(response)
            .expectNextMatches(result -> {
                return result.equals(updatedRecipe);
            })
            .verifyComplete();

        verify(recipeRepository, times(1)).findById(anyLong());
        verify(client, times(2)).sql(anyString());
        verify(ingredientsExecuteSpec, times(1)).bind(anyString(), anyLong());
        verify(ingredientsExecuteSpec, times(1)).fetch();
        verify(ingredientsFetchSpec, times(1)).all();
        verify(instructionsExecuteSpec, times(1)).bind(anyString(), anyLong());
        verify(instructionsExecuteSpec, times(1)).fetch();
        verify(instructionsFetchSpec, times(1)).all();
        verify(recipeRepository, times(1)).update(any(Recipe.class));
        verify(ingredientRepository, times(1)).updateAll(any(List.class));
        verify(instructionRepository, times(1)).updateAll(any(List.class));
        verify(recipeSearchRepository, times(1)).save(any(RecipeDoc.class));
        verify(recipeSearchRepository, times(1)).save(any(RecipeDoc.class));
        verifyNoMoreInteractions(elasticsearchOperations, elasticsearchClient, recipeRepository,
            ingredientRepository, instructionRepository, recipeSearchRepository, client);
    }

    /**
     * Test updateRecipe save ingredients and instructions.
     */
    @Test
    void testUpdateRecipeSaveIngredientsAndInstructions() {

        var updatedRecipe = Instancio.create(Recipe.class);
        var existingRecipe = getRecipeWithoutIngredientsOrInstructions(updatedRecipe);

        Mono<Recipe> existingRecipeMono = Mono.just(existingRecipe);

        when(recipeRepository.findById(anyLong())).thenReturn(existingRecipeMono);
        when(client.sql(anyString()))
            .thenReturn(ingredientsExecuteSpec)
            .thenReturn(instructionsExecuteSpec);
        when(ingredientsExecuteSpec.bind(anyString(), anyLong())).thenReturn(ingredientsExecuteSpec);
        when(ingredientsExecuteSpec.fetch()).thenReturn(ingredientsFetchSpec);
        when(ingredientsFetchSpec.all()).thenReturn(Flux.empty());
        when(instructionsExecuteSpec.bind(anyString(), anyLong())).thenReturn(instructionsExecuteSpec);
        when(instructionsExecuteSpec.fetch()).thenReturn(instructionsFetchSpec);
        when(instructionsFetchSpec.all()).thenReturn(Flux.empty());
        when(recipeRepository.update(any(Recipe.class))).thenReturn(Mono.just(LONG_RETURN_VALUE));
        when(ingredientRepository.saveAll(any(List.class))).thenReturn(Flux.fromIterable(updatedRecipe.getIngredients()));
        when(instructionRepository.saveAll(any(List.class))).thenReturn(Flux.fromIterable(updatedRecipe.getInstructions()));
        when(recipeSearchRepository.save(any(RecipeDoc.class))).thenReturn(Mono.empty());

        var response = recipeService.updateRecipe(updatedRecipe);

        StepVerifier.create(response)
            .expectNextMatches(result -> {
                return result.equals(updatedRecipe);
            })
            .verifyComplete();

        verify(recipeRepository, times(1)).findById(anyLong());
        verify(client, atLeastOnce()).sql(anyString());
        verify(ingredientsExecuteSpec, times(1)).bind(anyString(), anyLong());
        verify(ingredientsExecuteSpec, atLeastOnce()).fetch();
        verify(ingredientsFetchSpec, atLeastOnce()).all();
        verify(instructionsExecuteSpec, times(1)).bind(anyString(), anyLong());
        verify(instructionsExecuteSpec, atLeastOnce()).fetch();
        verify(instructionsFetchSpec, atLeastOnce()).all();
        verify(recipeRepository, times(1)).update(any(Recipe.class));
        verify(ingredientRepository, times(1)).saveAll(any(List.class));
        verify(instructionRepository, times(1)).saveAll(any(List.class));
        verify(recipeSearchRepository, times(1)).save(any(RecipeDoc.class));
        verifyNoMoreInteractions(elasticsearchOperations, elasticsearchClient, recipeRepository,
            ingredientRepository, instructionRepository, recipeSearchRepository, client);
    }

    /**
     * Test updateRecipe delete ingredients and instructions.
     */
    @Test
    void testUpdateRecipeDeleteIngredientsAndInstructions() {

        var existingRecipe = Instancio.create(Recipe.class);
        var updatedRecipe = getRecipeWithoutIngredientsOrInstructions(existingRecipe);

        Mono<Recipe> existingRecipeMono = Mono.just(existingRecipe);

        when(recipeRepository.findById(anyLong())).thenReturn(existingRecipeMono);
        when(client.sql(anyString()))
            .thenReturn(ingredientsExecuteSpec)
            .thenReturn(instructionsExecuteSpec);
        when(ingredientsExecuteSpec.bind(anyString(), anyLong())).thenReturn(ingredientsExecuteSpec);
        when(ingredientsExecuteSpec.fetch()).thenReturn(ingredientsFetchSpec);
        when(ingredientsFetchSpec.all()).thenReturn(Flux.fromIterable(getIngredientMaps(existingRecipe.getIngredients())));
        when(instructionsExecuteSpec.bind(anyString(), anyLong())).thenReturn(instructionsExecuteSpec);
        when(instructionsExecuteSpec.fetch()).thenReturn(instructionsFetchSpec);
        when(instructionsFetchSpec.all()).thenReturn(Flux.fromIterable(getInstructionMaps(existingRecipe.getInstructions())));
        when(recipeRepository.update(any(Recipe.class))).thenReturn(Mono.just(LONG_RETURN_VALUE));
        when(ingredientRepository.deleteAllByIds(any(List.class))).thenReturn(Flux.empty());
        when(instructionRepository.deleteAllByIds(any(List.class))).thenReturn(Flux.empty());
        when(recipeSearchRepository.save(any(RecipeDoc.class))).thenReturn(Mono.empty());

        var response = recipeService.updateRecipe(updatedRecipe);

        StepVerifier.create(response)
            .expectNextMatches(result -> {
                return result.equals(updatedRecipe);
            })
            .verifyComplete();

        verify(recipeRepository, times(1)).findById(anyLong());
        verify(client, atLeastOnce()).sql(anyString());
        verify(ingredientsExecuteSpec, times(1)).bind(anyString(), anyLong());
        verify(ingredientsExecuteSpec, times(1)).fetch();
        verify(ingredientsFetchSpec, times(1)).all();
        verify(instructionsExecuteSpec, times(1)).bind(anyString(), anyLong());
        verify(instructionsExecuteSpec, atLeastOnce()).fetch();
        verify(instructionsFetchSpec, atLeastOnce()).all();
        verify(recipeRepository, times(1)).update(any(Recipe.class));
        verify(ingredientRepository, times(1)).deleteAllByIds(any(List.class));
        verify(instructionRepository, times(1)).deleteAllByIds(any(List.class));
        verify(recipeSearchRepository, times(1)).save(any(RecipeDoc.class));
        verifyNoMoreInteractions(elasticsearchOperations, elasticsearchClient, recipeRepository,
            ingredientRepository, instructionRepository, recipeSearchRepository, client);
    }

    /**
     * Test deleteRecipeById where a recipe for the id doesn't exist.
     */
    @Test
    void testDeleteRecipeByIdDoesNotExist() {

        when(client.sql(anyString()))
            .thenReturn(ingredientsExecuteSpec)
            .thenReturn(instructionsExecuteSpec);
        when(ingredientsExecuteSpec.bind(anyString(), anyLong())).thenReturn(ingredientsExecuteSpec);
        when(ingredientsExecuteSpec.fetch()).thenReturn(ingredientsFetchSpec);
        when(ingredientsFetchSpec.all()).thenReturn(Flux.empty());
        when(instructionsExecuteSpec.bind(anyString(), anyLong())).thenReturn(instructionsExecuteSpec);
        when(instructionsExecuteSpec.fetch()).thenReturn(instructionsFetchSpec);
        when(instructionsFetchSpec.all()).thenReturn(Flux.empty());
        when(recipeRepository.deleteById(anyLong())).thenReturn(Mono.just(LONG_RETURN_VALUE));
        when(ingredientRepository.deleteAllByIds(any(List.class))).thenReturn(Flux.empty());
        when(instructionRepository.deleteAllByIds(any(List.class))).thenReturn(Flux.empty());
        when(recipeSearchRepository.deleteById(anyLong())).thenReturn(Mono.empty());

        var response = recipeService.deleteRecipeById(NON_EXISTENT_RECIPE_ID);

        StepVerifier.create(response)
                .verifyComplete();

        verify(client, atLeastOnce()).sql(anyString());
        verify(ingredientsExecuteSpec, times(1)).bind(anyString(), anyLong());
        verify(ingredientsExecuteSpec, times(1)).fetch();
        verify(ingredientsFetchSpec, times(1)).all();
        verify(instructionsExecuteSpec, times(1)).bind(anyString(), anyLong());
        verify(instructionsExecuteSpec, atLeastOnce()).fetch();
        verify(instructionsFetchSpec, atLeastOnce()).all();
        verify(recipeRepository, times(1)).deleteById(anyLong());
        verify(ingredientRepository, times(1)).deleteAllByIds(any(List.class));
        verify(instructionRepository, times(1)).deleteAllByIds(any(List.class));
        verify(recipeSearchRepository, times(1)).deleteById(anyLong());
        verifyNoMoreInteractions(elasticsearchOperations, elasticsearchClient, recipeRepository,
            ingredientRepository, instructionRepository, recipeSearchRepository, client);
    }

    /**
     * Test deleteRecipeById where a recipe exists for the id.
     */
    @Test
    void testDeleteRecipeByIdExists() {

        var recipe = Instancio.create(Recipe.class);

        when(client.sql(anyString()))
                .thenReturn(ingredientsExecuteSpec)
                .thenReturn(instructionsExecuteSpec);
        when(ingredientsExecuteSpec.bind(anyString(), anyLong())).thenReturn(ingredientsExecuteSpec);
        when(ingredientsExecuteSpec.fetch()).thenReturn(ingredientsFetchSpec);
        when(ingredientsFetchSpec.all()).thenReturn(Flux.fromIterable(getIngredientMaps(recipe.getIngredients())));
        when(instructionsExecuteSpec.bind(anyString(), anyLong())).thenReturn(instructionsExecuteSpec);
        when(instructionsExecuteSpec.fetch()).thenReturn(instructionsFetchSpec);
        when(instructionsFetchSpec.all()).thenReturn(Flux.fromIterable(getInstructionMaps(recipe.getInstructions())));
        when(recipeRepository.deleteById(anyLong())).thenReturn(Mono.just(LONG_RETURN_VALUE));
        when(ingredientRepository.deleteAllByIds(any(List.class)))
            .thenReturn(Flux.fromIterable(recipe.getIngredients().stream()
                .map(Ingredient::getIngredientId).collect(Collectors.toList())));
        when(instructionRepository.deleteAllByIds(any(List.class)))
            .thenReturn(Flux.fromIterable(recipe.getInstructions().stream()
                .map(Instruction::getInstructionId).collect(Collectors.toList())));
        when(recipeSearchRepository.deleteById(anyLong())).thenReturn(Mono.empty());

        var response = recipeService.deleteRecipeById(NON_EXISTENT_RECIPE_ID);

        StepVerifier.create(response).verifyComplete();

        verify(client, atLeastOnce()).sql(anyString());
        verify(ingredientsExecuteSpec, times(1)).bind(anyString(), anyLong());
        verify(ingredientsExecuteSpec, times(1)).fetch();
        verify(ingredientsFetchSpec, times(1)).all();
        verify(instructionsExecuteSpec, times(1)).bind(anyString(), anyLong());
        verify(instructionsExecuteSpec, atLeastOnce()).fetch();
        verify(instructionsFetchSpec, atLeastOnce()).all();
        verify(recipeRepository, times(1)).deleteById(anyLong());
        verify(ingredientRepository, times(1)).deleteAllByIds(any(List.class));
        verify(instructionRepository, times(1)).deleteAllByIds(any(List.class));
        verify(recipeSearchRepository, times(1)).deleteById(anyLong());
        verifyNoMoreInteractions(elasticsearchOperations, elasticsearchClient, recipeRepository,
            ingredientRepository, instructionRepository, recipeSearchRepository, client);
    }

    @Test
    void testSearchRecipes() {

        SearchResponse<RecipeDoc> searchResponse = new SearchResponse.Builder<RecipeDoc>()
                .shards(new ShardStatistics.Builder().successful(0).failed(0).total(0).build())
                .took(TOOK_ELASTICSEARCH)
                .timedOut(false)
                .hits(new HitsMetadata.Builder<RecipeDoc>().hits(Collections.emptyList()).build())
                .build();

        when(elasticsearchClient.search(any(SearchRequest.class), any(Class.class)))
                .thenReturn(Mono.just(searchResponse));

        var response = recipeService.searchRecipes(SEARCH_TEXT);

        StepVerifier.create(response)
                .expectNextMatches(results -> {
                    return true;
                })
                .verifyComplete();

        verify(elasticsearchClient, times(1)).search(any(SearchRequest.class), any(Class.class));
        verifyNoMoreInteractions(elasticsearchOperations, elasticsearchClient, recipeRepository,
                ingredientRepository, instructionRepository, recipeSearchRepository, client);
    }

    @NotNull
    private List<Map<String, Object>> getIngredientMaps(final List<Ingredient> ingredients) {
        return ingredients.stream().map(ingredient -> {
            try {
                var map = objectMapper.readValue(objectMapper.writeValueAsString(ingredient), HashMap.class);
                Map<String, Object> ingredientMap = new HashMap<>();
                for (var key : map.keySet()) {
                    ingredientMap.put(INGREDIENT_FIELD_NAME_MAPPING.get(key.toString()), map.get(key));
                }
                return ingredientMap;
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toList());
    }

    @NotNull
    private List<Map<String, Object>> getInstructionMaps(final List<Instruction> instructions) {
        return instructions.stream().map(instruction -> {
            try {
                var map = objectMapper.readValue(objectMapper.writeValueAsString(instruction), HashMap.class);
                Map<String, Object> instructionMap = new HashMap<>();
                for (var key : map.keySet()) {
                    instructionMap.put(INSTRUCTION_FIELD_NAME_MAPPING.get(key.toString()), map.get(key));
                }
                return instructionMap;
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toList());
    }

    private static Recipe getRecipeWithoutIngredientsOrInstructions(final Recipe recipe) {
        return Recipe.builder()
                .recipeId(recipe.getRecipeId())
                .name(recipe.getName())
                .variation(recipe.getVariation())
                .description(recipe.getDescription())
                .version(recipe.getVersion())
                .creationDateTime(recipe.getCreationDateTime())
                .lastModifiedDateTime(recipe.getLastModifiedDateTime())
                .ingredients(Collections.emptyList())
                .instructions(Collections.emptyList())
                .build();
    }
}
