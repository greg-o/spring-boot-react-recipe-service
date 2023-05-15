package org.grego.recipeservice.controller;

import co.elastic.clients.elasticsearch._types.ShardStatistics;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import co.elastic.clients.elasticsearch.core.search.ResponseBody;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.spi.json.JsonProvider;
import net.minidev.json.JSONArray;
import org.grego.recipeservice.document.RecipeDoc;
import org.grego.recipeservice.model.Recipe;
import org.grego.recipeservice.service.IRecipeService;
import org.instancio.Instancio;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Collections;
import java.util.Random;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Test RecipeController with mock IRecipeService and RecipeResourceAssembler.
 */
@ExtendWith(MockitoExtension.class)
@RunWith(PowerMockRunner.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag("UnitTests")
public class RecipeControllerTest {
    /**
     * Invalid page number (pages start at 1).
     */
    public static final long INVALID_PAGE_NUMBER = 0L;

    /**
     * First page.
     */
    private static final long PAGE_NUMBER_1 = 1L;

    /**
     * Page size of 10.
     */
    private static final int PAGE_SIZE_10 = 10;

    /**
     * Include hyper-links.
     */
    private static final boolean INCLUDE_HYPER_LINKS = true;

    /**
     * Don't include hyper-links.
     */
    private static final boolean DO_NOT_INCLUDE_HYPER_LINKS = false;

    /**
     * The time it took Elasticsearch.
     */
    public static final long TOOK_ELASTICSEARCH = 3L;

    /**
     * Instance RecipeController to test against.
     */
    @InjectMocks
    private RecipeController recipeController;

    /**
     * Mock IRecipeService.
     */
    @Mock
    private IRecipeService recipeService;

    /**
     * Mock RecipeResourceAssembler.
     */
    @Mock
    private RecipeResourceAssembler recipeResourceAssembler;

    /**
     * Instance of objectMapper.
     */
    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    /**
     * JsonProvider is used to execute JSON path queries.
     */
    private JsonProvider jsonPath = Configuration.defaultConfiguration().jsonProvider();

    /**
     * Before running tests set up objectMapper.
     */
    @BeforeAll
    void init() {
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    /**
     * Test list recipes with hyper-links and an invalid page number.
     * @throws Exception
     */
    @Test
    void testListRecipesWithHyperLinksInvalidPage() throws Exception {

        var response = recipeController.listRecipes(INVALID_PAGE_NUMBER, PAGE_SIZE_10, INCLUDE_HYPER_LINKS);

        StepVerifier.create(response)
            .expectNextMatches(responseEntity -> {
                return
                    statusCodeAndContentTypeAreExpected(HttpStatus.BAD_REQUEST,
                        MediaType.TEXT_PLAIN_VALUE, responseEntity)
                        && responseEntity.getBody().toString().startsWith("Pages begin at 1:  page-number = ");
            })
            .verifyComplete();

        verifyNoInteractions(recipeService, recipeResourceAssembler);
    }

    /**
     * Test list recipes without hyper-links and an invalid page number.
     * @throws Exception
     */
    @Test
    void testListRecipesWithoutHyperLinksInvalidPage() throws Exception {

        var response = recipeController.listRecipes(INVALID_PAGE_NUMBER, PAGE_SIZE_10, DO_NOT_INCLUDE_HYPER_LINKS);

        StepVerifier.create(response)
                .expectNextMatches(responseEntity -> {
                    return
                            statusCodeAndContentTypeAreExpected(HttpStatus.BAD_REQUEST,
                                    MediaType.TEXT_PLAIN_VALUE, responseEntity)
                                    && responseEntity.getBody().toString().startsWith("Pages begin at 1:  page-number = ");
                })
                .verifyComplete();

        verifyNoInteractions(recipeService, recipeResourceAssembler);
    }

    /**
     * Test list recipes with hyper-links and no recipes.
     * @throws Exception
     */
    @Test
    void testListRecipesWithHyperLinksEmpty() throws Exception {

        Flux<Recipe> recipeFlux = Flux.empty();
        RecipeResourceAssembler assembler = new RecipeResourceAssembler(objectMapper);

        when(recipeService.getAllRecipes(anyLong(), anyInt())).thenReturn(recipeFlux);
        when(recipeService.getRecipeCount()).thenReturn(Mono.just(0L));
        when(recipeResourceAssembler.toCollectionModel(any(Iterable.class)))
                .thenAnswer(invocation -> assembler.toCollectionModel(invocation.getArgument(0)));

        var response = recipeController.listRecipes(PAGE_NUMBER_1, PAGE_SIZE_10, INCLUDE_HYPER_LINKS);

        StepVerifier.create(response)
                .expectNextMatches(responseEntity -> {
                    if (!statusCodeAndContentTypeAreExpected(HttpStatus.OK,
                            de.ingogriebsch.spring.hateoas.siren.MediaTypes.SIREN_JSON_VALUE, responseEntity)) {
                        return false;
                    }

                    var json = jsonPath.parse(responseEntity.getBody().toString());
                    return (0 == (Integer) JsonPath.read(json, "$.page.size")
                            && ((JSONArray) JsonPath.read(json, "$.content")).isEmpty());
                })
                .verifyComplete();

        verify(recipeService, times(1)).getAllRecipes(anyLong(), anyInt());
        verify(recipeService, times(1)).getRecipeCount();
        verify(recipeResourceAssembler, times(1)).toCollectionModel(any(Iterable.class));
        verifyNoMoreInteractions(recipeService, recipeResourceAssembler);
    }

    /**
     * Test list recipes without hyper-links and no recipes.
     * @throws Exception
     */

    @Test
    void testListRecipesWithoutHyperLinksEmpty() throws Exception {

        Flux<Recipe> recipeFlux = Flux.empty();

        when(recipeService.getAllRecipes(anyLong(), anyInt())).thenReturn(recipeFlux);

        var response = recipeController.listRecipes(PAGE_NUMBER_1, PAGE_SIZE_10, DO_NOT_INCLUDE_HYPER_LINKS);

        StepVerifier.create(response)
                .expectNextMatches(responseEntity -> {
                    if (!statusCodeAndContentTypeAreExpected(HttpStatus.OK,
                            MediaType.APPLICATION_JSON_VALUE, responseEntity)) {
                        return false;
                    }

                    return ((JSONArray) jsonPath.parse(responseEntity.getBody().toString())).isEmpty();
                })
                .verifyComplete();

        verify(recipeService, times(1)).getAllRecipes(anyLong(), anyInt());
        verifyNoMoreInteractions(recipeService, recipeResourceAssembler);
    }

    /**
     * Test list recipes with hyper-links and one recipe.
     * @throws Exception
     */
    @Test
    void testListRecipesWithHyperLinksWithOneRecipe() throws Exception {

        var recipes = Instancio.ofList(Recipe.class).size(1).create();
        Flux<Recipe> recipeFlux = Flux.fromIterable(recipes);
        RecipeResourceAssembler assembler = new RecipeResourceAssembler(objectMapper);

        when(recipeService.getAllRecipes(anyLong(), anyInt())).thenReturn(recipeFlux);
        when(recipeService.getRecipeCount()).thenReturn(Mono.just((long) recipes.size()));
        when(recipeResourceAssembler.toCollectionModel(any(Iterable.class)))
                .thenAnswer(invocation -> assembler.toCollectionModel(invocation.getArgument(0)));

        var response = recipeController.listRecipes(PAGE_NUMBER_1, PAGE_SIZE_10, INCLUDE_HYPER_LINKS);

        StepVerifier.create(response)
                .expectNextMatches(responseEntity -> {
                    if (!statusCodeAndContentTypeAreExpected(HttpStatus.OK,
                            de.ingogriebsch.spring.hateoas.siren.MediaTypes.SIREN_JSON_VALUE, responseEntity)) {
                        return false;
                    }

                    var json = jsonPath.parse(responseEntity.getBody().toString());
                    return (recipes.size() == (Integer) JsonPath.read(json, "$.page.size")
                            && recipes.size() == ((JSONArray) JsonPath.read(json, "$.content")).size());
                })
                .verifyComplete();

        verify(recipeService, times(1)).getAllRecipes(anyLong(), anyInt());
        verify(recipeService, times(1)).getRecipeCount();
        verify(recipeResourceAssembler, times(1)).toCollectionModel(any(Iterable.class));
        verifyNoMoreInteractions(recipeService, recipeResourceAssembler);
    }

    /**
     * Test list recipes without hyper-links and one recipe.
     * @throws Exception
     */
    @Test
    void testListRecipesWithoutHyperLinksWithOneRecipe() throws Exception {

        var recipes = Instancio.ofList(Recipe.class).size(1).create();
        Flux<Recipe> recipeFlux = Flux.fromIterable(recipes);
        RecipeResourceAssembler assembler = new RecipeResourceAssembler(objectMapper);

        when(recipeService.getAllRecipes(anyLong(), anyInt())).thenReturn(recipeFlux);

        var response = recipeController.listRecipes(PAGE_NUMBER_1, PAGE_SIZE_10, DO_NOT_INCLUDE_HYPER_LINKS);

        StepVerifier.create(response)
                .expectNextMatches(responseEntity -> {
                    if (!statusCodeAndContentTypeAreExpected(HttpStatus.OK,
                            MediaType.APPLICATION_JSON_VALUE, responseEntity)) {
                        return false;
                    }

                    return (recipes.size() == ((JSONArray) jsonPath.parse(responseEntity.getBody().toString())).size());
                })
                .verifyComplete();

        verify(recipeService, times(1)).getAllRecipes(anyLong(), anyInt());
        verifyNoMoreInteractions(recipeService, recipeResourceAssembler);
    }

    /**
     * Test list recipes with hyper-links and more than one recipe.
     * @throws Exception
     */
    @Test
    void testListRecipesWithHyperLinksWithMoreThanOneRecipe() throws Exception {

        var recipes = Instancio.ofList(Recipe.class).size(new Random().nextInt(PAGE_SIZE_10 - 2) + 2).create();
        Flux<Recipe> recipeFlux = Flux.fromIterable(recipes);
        RecipeResourceAssembler assembler = new RecipeResourceAssembler(objectMapper);

        when(recipeService.getAllRecipes(anyLong(), anyInt())).thenReturn(recipeFlux);
        when(recipeService.getRecipeCount()).thenReturn(Mono.just((long) recipes.size()));
        when(recipeResourceAssembler.toCollectionModel(any(Iterable.class)))
                .thenAnswer(invocation -> assembler.toCollectionModel(invocation.getArgument(0)));

        var response = recipeController.listRecipes(PAGE_NUMBER_1, PAGE_SIZE_10, INCLUDE_HYPER_LINKS);

        StepVerifier.create(response)
                .expectNextMatches(responseEntity -> {
                    if (!statusCodeAndContentTypeAreExpected(HttpStatus.OK,
                            de.ingogriebsch.spring.hateoas.siren.MediaTypes.SIREN_JSON_VALUE, responseEntity)) {
                        return false;
                    }

                    var json = jsonPath.parse(responseEntity.getBody().toString());
                    return (recipes.size() == (Integer) JsonPath.read(json, "$.page.size")
                            && recipes.size() == ((JSONArray) JsonPath.read(json, "$.content")).size());
                })
                .verifyComplete();

        verify(recipeService, times(1)).getAllRecipes(anyLong(), anyInt());
        verify(recipeService, times(1)).getRecipeCount();
        verify(recipeResourceAssembler, times(1)).toCollectionModel(any(Iterable.class));
        verifyNoMoreInteractions(recipeService, recipeResourceAssembler);
    }

    /**
     * Test list recipes without hyper-links and more than one recipe.
     * @throws Exception
     */
    @Test
    void testListRecipesWithoutHyperLinksWithMoreThanOneRecipe() throws Exception {

        var recipes = Instancio.ofList(Recipe.class).size(new Random().nextInt(PAGE_SIZE_10 - 2) + 2).create();
        Flux<Recipe> recipeFlux = Flux.fromIterable(recipes);
        RecipeResourceAssembler assembler = new RecipeResourceAssembler(objectMapper);

        when(recipeService.getAllRecipes(anyLong(), anyInt())).thenReturn(recipeFlux);

        var response = recipeController.listRecipes(PAGE_NUMBER_1, PAGE_SIZE_10, DO_NOT_INCLUDE_HYPER_LINKS);

        StepVerifier.create(response)
                .expectNextMatches(responseEntity -> {
                    if (!statusCodeAndContentTypeAreExpected(HttpStatus.OK,
                            MediaType.APPLICATION_JSON_VALUE, responseEntity)) {
                        return false;
                    }

                    return (recipes.size() == ((JSONArray) jsonPath.parse(responseEntity.getBody().toString())).size());
                })
                .verifyComplete();

        verify(recipeService, times(1)).getAllRecipes(anyLong(), anyInt());
        verifyNoMoreInteractions(recipeService, recipeResourceAssembler);
    }

    /**
     * Test list recipes with hyper-links throws JsonProcessingException.
     * @throws Exception
     */
    @Test
    void testListRecipesWithHyperLinksJsonProcessiongException() throws Exception {

        var recipes = Instancio.ofList(Recipe.class).size(1).create();
        Flux<Recipe> recipeFlux = Flux.fromIterable(recipes);
        RecipeResourceAssembler assembler = new RecipeResourceAssembler(objectMapper);

        when(recipeService.getAllRecipes(anyLong(), anyInt())).thenReturn(recipeFlux);
        when(recipeService.getRecipeCount()).thenReturn(Mono.just((long) recipes.size()));
        when(recipeResourceAssembler.toCollectionModel(any(Iterable.class)))
                .thenAnswer(invocation -> assembler.toCollectionModel(invocation.getArgument(0)));
        doThrow(JsonProcessingException.class).when(objectMapper).writeValueAsString(any());

        var response = recipeController.listRecipes(PAGE_NUMBER_1, PAGE_SIZE_10, INCLUDE_HYPER_LINKS);

        StepVerifier.create(response)
                .expectNextMatches(responseEntity -> {
                    return responseEntity.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR;
                })
                .verifyComplete();

        verify(recipeService, times(1)).getAllRecipes(anyLong(), anyInt());
        verify(recipeService, times(1)).getRecipeCount();
        verify(recipeResourceAssembler, times(1)).toCollectionModel(any(Iterable.class));
        verifyNoMoreInteractions(recipeService, recipeResourceAssembler);
    }

    /**
     * Test list recipes without hyper-links throw JsonProcessingException.
     * @throws Exception
     */
    @Test
    void testListRecipesWithoutHyperLinksJsonProcessingException() throws Exception {

        var recipes = Instancio.ofList(Recipe.class).size(1).create();
        Flux<Recipe> recipeFlux = Flux.fromIterable(recipes);
        RecipeResourceAssembler assembler = new RecipeResourceAssembler(objectMapper);

        when(recipeService.getAllRecipes(anyLong(), anyInt())).thenReturn(recipeFlux);
        doThrow(JsonProcessingException.class).when(objectMapper).writeValueAsString(any());

        var response = recipeController.listRecipes(PAGE_NUMBER_1, PAGE_SIZE_10, DO_NOT_INCLUDE_HYPER_LINKS);

        StepVerifier.create(response)
                .expectNextMatches(responseEntity -> {
                    return responseEntity.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR;
                })
                .verifyComplete();

        verify(recipeService, times(1)).getAllRecipes(anyLong(), anyInt());
        verifyNoMoreInteractions(recipeService, recipeResourceAssembler);
    }

    /**
     * Test get recipe with hyper-links where recipe doesn't exist.
     * @throws Exception
     */
    @Test
    void testGetRecipeWithHyperLinksDoesNotExist() throws Exception {

        Mono<Recipe> recipeMono = Mono.empty();

        when(recipeService.getRecipeById(anyLong())).thenReturn(recipeMono);

        var response = recipeController.getRecipe(-1L, INCLUDE_HYPER_LINKS);

        StepVerifier.create(response)
                .expectNextMatches(responseEntity -> {
                    return responseEntity.getStatusCode() == HttpStatus.NOT_FOUND;
                })
                .verifyComplete();

        verify(recipeService, times(1)).getRecipeById(anyLong());
        verifyNoMoreInteractions(recipeService, recipeResourceAssembler);
    }

    /**
     * Test get recipe without hyper-links where recipe doesn't exist.
     * @throws Exception
     */
    @Test
    void testGetRecipeWithoutHyperLinksDoesNotExist() throws Exception {

        Mono<Recipe> recipeMono = Mono.empty();

        when(recipeService.getRecipeById(anyLong())).thenReturn(recipeMono);

        var response = recipeController.getRecipe(-1L, DO_NOT_INCLUDE_HYPER_LINKS);

        StepVerifier.create(response)
                .expectNextMatches(responseEntity -> {
                    return responseEntity.getStatusCode() == HttpStatus.NOT_FOUND;
                })
                .verifyComplete();

        verify(recipeService, times(1)).getRecipeById(anyLong());
        verifyNoMoreInteractions(recipeService, recipeResourceAssembler);
    }

    /**
     * Test get recipe with hyper-links where recipe exists.
     * @throws Exception
     */
    @Test
    void testGetRecipeWithHyperLinksExists() throws Exception {

        var recipe = Instancio.create(Recipe.class);
        Mono<Recipe> recipeMono = Mono.just(recipe);
        RecipeResourceAssembler assembler = new RecipeResourceAssembler(objectMapper);

        when(recipeService.getRecipeById(anyLong())).thenReturn(recipeMono);
        when(recipeResourceAssembler.toModel(any(Recipe.class)))
                .thenAnswer(invocation -> assembler.toModel(invocation.getArgument(0)));

        var response = recipeController.getRecipe(-1L, INCLUDE_HYPER_LINKS);

        StepVerifier.create(response)
                .expectNextMatches(responseEntity -> {
                    if (!statusCodeAndContentTypeAreExpected(HttpStatus.OK,
                            de.ingogriebsch.spring.hateoas.siren.MediaTypes.SIREN_JSON_VALUE, responseEntity)) {
                        return false;
                    }

                    var json = jsonPath.parse(responseEntity.getBody().toString());
                    return recipe.getName().equals(JsonPath.read(json, "$.name"));
                })
                .verifyComplete();

        verify(recipeService, times(1)).getRecipeById(anyLong());
        verify(recipeResourceAssembler, times(1)).toModel(any(Recipe.class));
        verifyNoMoreInteractions(recipeService, recipeResourceAssembler);
    }

    /**
     * Test get recipe without hyper-links where recipe exists.
     * @throws Exception
     */
    @Test
    void testGetRecipeWithoutHyperLinksExists() throws Exception {

        var recipe = Instancio.create(Recipe.class);
        Mono<Recipe> recipeMono = Mono.just(recipe);
        RecipeResourceAssembler assembler = new RecipeResourceAssembler(objectMapper);

        when(recipeService.getRecipeById(anyLong())).thenReturn(recipeMono);

        var response = recipeController.getRecipe(-1L, DO_NOT_INCLUDE_HYPER_LINKS);

        StepVerifier.create(response)
                .expectNextMatches(responseEntity -> {
                    if (!statusCodeAndContentTypeAreExpected(HttpStatus.OK,
                            MediaType.APPLICATION_JSON_VALUE, responseEntity)) {
                        return false;
                    }

                    var json = jsonPath.parse(responseEntity.getBody().toString());
                    return recipe.getName().equals(JsonPath.read(json, "$.name"));
                })
                .verifyComplete();

        verify(recipeService, times(1)).getRecipeById(anyLong());
        verifyNoMoreInteractions(recipeService, recipeResourceAssembler);
    }

    /**
     * Test get recipe with hyper-links throws JsonProcessingException.
     * @throws Exception
     */
    @Test
    void testGetRecipeWithHyperLinksJsonProcessingException() throws Exception {

        var recipe = Instancio.create(Recipe.class);
        Mono<Recipe> recipeMono = Mono.just(recipe);
        RecipeResourceAssembler assembler = new RecipeResourceAssembler(objectMapper);

        when(recipeService.getRecipeById(anyLong())).thenReturn(recipeMono);
        when(recipeResourceAssembler.toModel(any(Recipe.class)))
                .thenAnswer(invocation -> assembler.toModel(invocation.getArgument(0)));
        doThrow(JsonProcessingException.class).when(objectMapper).writeValueAsString(any());

        var response = recipeController.getRecipe(-1L, INCLUDE_HYPER_LINKS);

        StepVerifier.create(response)
                .expectNextMatches(responseEntity -> {
                    return responseEntity.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR;
                })
                .verifyComplete();

        verify(recipeService, times(1)).getRecipeById(anyLong());
        verify(recipeResourceAssembler, times(1)).toModel(any(Recipe.class));
        verifyNoMoreInteractions(recipeService, recipeResourceAssembler);
    }

    /**
     * Test get recipe without hyper-links throw JsonProcessingException.
     * @throws Exception
     */
    @Test
    void testGetRecipeWithoutHyperLinksJsonProcessingException() throws Exception {

        var recipe = Instancio.create(Recipe.class);
        Mono<Recipe> recipeMono = Mono.just(recipe);

        when(recipeService.getRecipeById(anyLong())).thenReturn(recipeMono);
        doThrow(JsonProcessingException.class).when(objectMapper).writeValueAsString(any());

        var response = recipeController.getRecipe(-1L, DO_NOT_INCLUDE_HYPER_LINKS);

        StepVerifier.create(response)
                .expectNextMatches(responseEntity -> {
                    return responseEntity.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR;
                })
                .verifyComplete();

        verify(recipeService, times(1)).getRecipeById(anyLong());
        verifyNoMoreInteractions(recipeService, recipeResourceAssembler);
    }

    /**
     * Test add recipe with hyper-links.
     * @throws Exception
     */
    @Test
    void testAddRecipeWithHyperLinks() throws Exception {
        var recipe = Instancio.create(Recipe.class);
        RecipeResourceAssembler assembler = new RecipeResourceAssembler(objectMapper);

        when(recipeService.addRecipe(any(Recipe.class))).thenReturn(Mono.just(recipe));
        when(recipeResourceAssembler.toModel(any(Recipe.class)))
                .thenAnswer(invocation -> assembler.toModel(invocation.getArgument(0)));

        var response = recipeController.addRecipe(objectMapper.writeValueAsString(recipe), INCLUDE_HYPER_LINKS);

        StepVerifier.create(response)
                .expectNextMatches(responseEntity -> {
                    if (!statusCodeAndContentTypeAreExpected(HttpStatus.OK,
                            de.ingogriebsch.spring.hateoas.siren.MediaTypes.SIREN_JSON_VALUE, responseEntity)) {
                        return false;
                    }

                    var json = jsonPath.parse(responseEntity.getBody().toString());
                    return (recipe.getName().equals((String) JsonPath.read(json, "$.name")));
                })
                .verifyComplete();

        verify(recipeService, times(1)).addRecipe(any(Recipe.class));
        verify(recipeResourceAssembler, times(1)).toModel(any(Recipe.class));
        verifyNoMoreInteractions(recipeService, recipeResourceAssembler);
    }

    /**
     * Test add recipe without hyper-links.
     * @throws Exception
     */
    @Test
    void testAddRecipeWithoutHyperLinks() throws Exception {
        var recipe = Instancio.create(Recipe.class);
        RecipeResourceAssembler assembler = new RecipeResourceAssembler(objectMapper);

        when(recipeService.addRecipe(any(Recipe.class))).thenReturn(Mono.just(recipe));

        var response = recipeController.addRecipe(objectMapper.writeValueAsString(recipe), DO_NOT_INCLUDE_HYPER_LINKS);

        StepVerifier.create(response)
                .expectNextMatches(responseEntity -> {
                    if (!statusCodeAndContentTypeAreExpected(HttpStatus.OK,
                            MediaType.APPLICATION_JSON_VALUE, responseEntity)) {
                        return false;
                    }

                    var json = jsonPath.parse(responseEntity.getBody().toString());
                    return (recipe.getName().equals((String) JsonPath.read(json, "$.name")));
                })
                .verifyComplete();

        verify(recipeService, times(1)).addRecipe(any(Recipe.class));
        verifyNoMoreInteractions(recipeService, recipeResourceAssembler);
    }

    /**
     * Test add recipe with hyper-links throws JsonProcessingException.
     * @throws Exception
     */
    @Test
    void testAddRecipeWithHyperLinksJsonProcessingException() throws Exception {
        var recipe = Instancio.create(Recipe.class);

        doThrow(JsonProcessingException.class).when(objectMapper).readValue(anyString(), any(Class.class));

        var response = recipeController.addRecipe(objectMapper.writeValueAsString(recipe), INCLUDE_HYPER_LINKS);

        StepVerifier.create(response)
                .expectNextMatches(responseEntity -> {
                    return responseEntity.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR;
                })
                .verifyComplete();

        verifyNoInteractions(recipeService, recipeResourceAssembler);
    }

    /**
     * Test add recipe without hyper-links throws JsonProcessingException.
     * @throws Exception
     */
    @Test
    void testAddRecipeWithoutHyperLinksJsonProcessingException() throws Exception {
        var recipe = Instancio.create(Recipe.class);

        doThrow(JsonProcessingException.class).when(objectMapper).readValue(anyString(), any(Class.class));

        var response = recipeController.addRecipe(objectMapper.writeValueAsString(recipe), DO_NOT_INCLUDE_HYPER_LINKS);

        StepVerifier.create(response)
                .expectNextMatches(responseEntity -> {
                    return responseEntity.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR;
                })
                .verifyComplete();

        verifyNoInteractions(recipeService, recipeResourceAssembler);
    }

    /**
     * Test update recipe with hyper-links where recipe doesn't exist.
     * @throws Exception
     */
    @Test
    void testUpdateRecipeWithHyperLinksDoesNotExist() throws Exception {

        var recipe = Instancio.create(Recipe.class);
        Mono<Recipe> recipeMono = Mono.empty();

        when(recipeService.updateRecipe(any(Recipe.class))).thenReturn(recipeMono);

        var response = recipeController.updateRecipe(objectMapper.writeValueAsString(recipe), INCLUDE_HYPER_LINKS);

        StepVerifier.create(response)
                .expectNextMatches(responseEntity -> {
                    return responseEntity.getStatusCode() == HttpStatus.NOT_FOUND;
                })
                .verifyComplete();

        verify(recipeService, times(1)).updateRecipe(any(Recipe.class));
        verifyNoMoreInteractions(recipeService, recipeResourceAssembler);
    }

    /**
     * Test update recipe with hyper-links where recipe doesn't exist.
     * @throws Exception
     */
    @Test
    void testUpdateRecipeWithoutHyperLinksDoesNotExist() throws Exception {

        var recipe = Instancio.create(Recipe.class);
        Mono<Recipe> recipeMono = Mono.empty();

        when(recipeService.updateRecipe(any(Recipe.class))).thenReturn(recipeMono);

        var response = recipeController.updateRecipe(objectMapper.writeValueAsString(recipe), DO_NOT_INCLUDE_HYPER_LINKS);

        StepVerifier.create(response)
                .expectNextMatches(responseEntity -> {
                    return responseEntity.getStatusCode() == HttpStatus.NOT_FOUND;
                })
                .verifyComplete();

        verify(recipeService, times(1)).updateRecipe(any(Recipe.class));
        verifyNoMoreInteractions(recipeService, recipeResourceAssembler);
    }

    /**
     * Test update recipe with hyper-links where recipe exists.
     * @throws Exception
     */
    @Test
    void testUpdateRecipeWithHyperLinksExists() throws Exception {

        var recipe = Instancio.create(Recipe.class);

        Mono<Recipe> recipeMono = Mono.just(recipe);
        RecipeResourceAssembler assembler = new RecipeResourceAssembler(objectMapper);

        when(recipeService.updateRecipe(any(Recipe.class))).thenReturn(recipeMono);
        when(recipeResourceAssembler.toModel(any(Recipe.class)))
                .thenAnswer(invocation -> assembler.toModel(invocation.getArgument(0)));

        var response = recipeController.updateRecipe(objectMapper.writeValueAsString(recipe), INCLUDE_HYPER_LINKS);

        StepVerifier.create(response)
                .expectNextMatches(responseEntity -> {
                    if (!statusCodeAndContentTypeAreExpected(HttpStatus.OK,
                            de.ingogriebsch.spring.hateoas.siren.MediaTypes.SIREN_JSON_VALUE, responseEntity)) {
                        return false;
                    }

                    var json = jsonPath.parse(responseEntity.getBody().toString());
                    return (recipe.getName().equals((String) JsonPath.read(json, "$.name"))
                            && recipe.getDescription().equals((String) JsonPath.read(json, "$.description")));
                })
                .verifyComplete();

        verify(recipeService, times(1)).updateRecipe(any(Recipe.class));
        verify(recipeResourceAssembler, times(1)).toModel(any(Recipe.class));
        verifyNoMoreInteractions(recipeService, recipeResourceAssembler);
    }

    /**
     * Test update recipe without hyper-links where recipe exists.
     * @throws Exception
     */
    @Test
    void testUpdateRecipeWithoutHyperLinksExists() throws Exception {

        var recipe = Instancio.create(Recipe.class);

        Mono<Recipe> recipeMono = Mono.just(recipe);

        when(recipeService.updateRecipe(any(Recipe.class))).thenReturn(recipeMono);

        var response = recipeController.updateRecipe(objectMapper.writeValueAsString(recipe), DO_NOT_INCLUDE_HYPER_LINKS);

        StepVerifier.create(response)
                .expectNextMatches(responseEntity -> {
                    if (!statusCodeAndContentTypeAreExpected(HttpStatus.OK,
                            MediaType.APPLICATION_JSON_VALUE, responseEntity)) {
                        return false;
                    }

                    var json = jsonPath.parse(responseEntity.getBody().toString());
                    return (recipe.getName().equals((String) JsonPath.read(json, "$.name"))
                            && recipe.getDescription().equals((String) JsonPath.read(json, "$.description")));
                })
                .verifyComplete();

        verify(recipeService, times(1)).updateRecipe(any(Recipe.class));
        verifyNoMoreInteractions(recipeService, recipeResourceAssembler);
    }

    /**
     * Test update recipe with hyper-links throws JsonProcessingException.
     * @throws Exception
     */
    @Test
    void testUpdateRecipeWithHyperLinksJsonProcessingException() throws Exception {
        var recipe = Instancio.create(Recipe.class);

        doThrow(JsonProcessingException.class).when(objectMapper).readValue(anyString(), any(Class.class));

        var response = recipeController.updateRecipe(objectMapper.writeValueAsString(recipe), INCLUDE_HYPER_LINKS);

        StepVerifier.create(response)
                .expectNextMatches(responseEntity -> {
                    return responseEntity.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR;
                })
                .verifyComplete();

        verifyNoInteractions(recipeService, recipeResourceAssembler);
    }

    /**
     * Test update recipe without hyper-links throws JsonProcessingException.
     * @throws Exception
     */
    @Test
    void testUpdateRecipeWithoutHyperLinksJsonProcessingException() throws Exception {
        var recipe = Instancio.create(Recipe.class);

        doThrow(JsonProcessingException.class).when(objectMapper).readValue(anyString(), any(Class.class));

        var response = recipeController.updateRecipe(objectMapper.writeValueAsString(recipe), DO_NOT_INCLUDE_HYPER_LINKS);

        StepVerifier.create(response)
                .expectNextMatches(responseEntity -> {
                    return responseEntity.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR;
                })
                .verifyComplete();

        verifyNoInteractions(recipeService, recipeResourceAssembler);
    }

    /**
     * Test delete recipe where recipe doesn't exist.
     * @throws Exception
     */
    @Test
    void testDeleteRecipeDoesNotExist() throws Exception {

        Mono<Long> mono = Mono.empty();

        when(recipeService.deleteRecipeById(anyLong())).thenReturn(mono);

        var response = recipeController.deleteRecipe(0L);

        StepVerifier.create(response)
                .expectNextMatches(responseEntity -> {
                    return responseEntity.getStatusCode() == HttpStatus.NOT_FOUND;
                })
                .verifyComplete();

        verify(recipeService, times(1)).deleteRecipeById(anyLong());
        verifyNoMoreInteractions(recipeService, recipeResourceAssembler);
    }

    /**
     * Test delete recipe where recipe exists.
     * @throws Exception
     */
    @Test
    void testDeleteRecipeExists() throws Exception {

        Mono<Long> mono = Mono.just(1L);

        when(recipeService.deleteRecipeById(anyLong())).thenReturn(mono);

        var response = recipeController.deleteRecipe(1L);

        StepVerifier.create(response)
                .expectNextMatches(responseEntity -> {
                    return responseEntity.getStatusCode() == HttpStatus.OK;
                })
                .verifyComplete();

        verify(recipeService, times(1)).deleteRecipeById(anyLong());
        verifyNoMoreInteractions(recipeService, recipeResourceAssembler);
    }

    /**
     * Test delete recipe where recipe doesn't exist.
     * @throws Exception
     */
    @Test
    void testSearchRecipesNoneFound() throws Exception {

        SearchResponse<RecipeDoc> searchResponse = new SearchResponse.Builder<RecipeDoc>()
                .shards(new ShardStatistics.Builder().successful(0).failed(0).total(0).build())
                .took(TOOK_ELASTICSEARCH)
                .timedOut(false)
                .hits(new HitsMetadata.Builder<RecipeDoc>().hits(Collections.emptyList()).build())
                .build();

        Mono<ResponseBody<RecipeDoc>> mono = Mono.just(searchResponse);

        when(recipeService.searchRecipes(anyString())).thenReturn(mono);

        var response = recipeController.searchRecipes("search string");

        StepVerifier.create(response)
                .expectNextMatches(responseEntity -> {
                    return responseEntity.getStatusCode() == HttpStatus.OK;
                })
                .verifyComplete();

        verify(recipeService, times(1)).searchRecipes(anyString());
        verifyNoMoreInteractions(recipeService, recipeResourceAssembler);
    }

    private static boolean statusCodeAndContentTypeAreExpected(final HttpStatus expectedStatus,
               final String expectedContentType, final ResponseEntity<?> responseEntity) {
        return (expectedStatus == responseEntity.getStatusCode()
                && expectedContentType.equals(responseEntity.getHeaders().getContentType().toString()));
    }
}
