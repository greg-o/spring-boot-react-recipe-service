package org.grego.recipeservice.repository;

import org.grego.recipeservice.model.Recipe;
import org.instancio.Instancio;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.r2dbc.core.ReactiveDeleteOperation;
import org.springframework.data.r2dbc.core.ReactiveSelectOperation;
import org.springframework.data.r2dbc.core.ReactiveInsertOperation;
import org.springframework.data.r2dbc.core.ReactiveUpdateOperation;
import org.springframework.data.relational.core.query.Query;
import org.springframework.data.relational.core.query.Update;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Random;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Test the RecipeRepository.
 */
@ExtendWith(MockitoExtension.class)
@RunWith(PowerMockRunner.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag("UnitTests")
public class RecipeRepositoryTest {
    /**
     * Page size of 10.
     */
    public static final int PAGE_SIZE_10 = 10;

    /**
     * First page.
     */
    public static final long FIRST_PAGE = 1L;

    /**
     * Number of recipes.
     */
    public static final long NUMBER_OF_RECIPES = 7L;

    /**
     * Recipe id.
     */
    public static final long RECIPE_ID = 1L;

    /**
     * The Rddbc entity template used to perform reactive database operations.
     */
    @Mock
    private R2dbcEntityTemplate template;

    /**
     * Reactive select.
     */
    @Mock
    private ReactiveSelectOperation.ReactiveSelect<Recipe> reactiveSelect;

    /**
     * Select with projection.
     */
    @Mock
    private ReactiveSelectOperation.SelectWithProjection<Recipe> selectWithProjection;

    /**
     * Terminating select.
     */
    @Mock
    private ReactiveSelectOperation.TerminatingSelect<Recipe> terminatingSelect;

    /**
     * Reactive insert.
     */
    @Mock
    private ReactiveInsertOperation.ReactiveInsert<Recipe> reactiveInsert;

    /**
     * Termination insert.
     */
    @Mock
    private ReactiveInsertOperation.TerminatingInsert<Recipe> terminatingInsert;

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
    private ReactiveUpdateOperation.TerminatingUpdate terminatingUpdate;

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
     * Test findAllByName with no recipes.
     */
    @Test
    void testFindAllByNameNoRecipesFound() {

        RecipeRepository recipeRepository = new RecipeRepository(template);

        when(template.select(Recipe.class)).thenReturn(reactiveSelect);
        when(reactiveSelect.from(eq(Recipe.RECIPES_TABLE_NAME))).thenReturn(selectWithProjection);
        when(selectWithProjection.matching(any(Query.class))).thenReturn(terminatingSelect);
        when(terminatingSelect.all()).thenReturn(Flux.empty());

        var response = recipeRepository.findAllByName("recipe");

        StepVerifier.create(response).verifyComplete();

        verify(template, times(1)).select(Recipe.class);
        verify(reactiveSelect, times(1)).from(eq(Recipe.RECIPES_TABLE_NAME));
        verify(selectWithProjection, times(1)).matching(any(Query.class));
        verify(terminatingSelect, times(1)).all();
        verifyNoMoreInteractions(template);
    }

    /**
     * Test findAllByName with one recipe.
     */
    @Test
    void testFindAllByNameRecipeFoundOne() {

        RecipeRepository recipeRepository = new RecipeRepository(template);
        var recipe = Instancio.create(Recipe.class);

        when(template.select(Recipe.class)).thenReturn(reactiveSelect);
        when(reactiveSelect.from(eq(Recipe.RECIPES_TABLE_NAME))).thenReturn(selectWithProjection);
        when(selectWithProjection.matching(any(Query.class))).thenReturn(terminatingSelect);
        when(terminatingSelect.all()).thenReturn(Flux.just(recipe));

        var response = recipeRepository.findAllByName("recipe");

        StepVerifier.create(response)
            .expectNextMatches(result -> result.equals(recipe))
            .verifyComplete();

        verify(template, times(1)).select(Recipe.class);
        verify(reactiveSelect, times(1)).from(eq(Recipe.RECIPES_TABLE_NAME));
        verify(selectWithProjection, times(1)).matching(any(Query.class));
        verify(terminatingSelect, times(1)).all();
        verifyNoMoreInteractions(template);
    }

    /**
     * Test findAllByName with more than one recipe.
     */
    @Test
    void testFindAllByNameRecipeFoundMoreThanOne() {

        RecipeRepository recipeRepository = new RecipeRepository(template);
        var recipes = Instancio.ofList(Recipe.class).size(new Random().nextInt(PAGE_SIZE_10 - 2) + 2).create();

        when(template.select(Recipe.class)).thenReturn(reactiveSelect);
        when(reactiveSelect.from(eq(Recipe.RECIPES_TABLE_NAME))).thenReturn(selectWithProjection);
        when(selectWithProjection.matching(any(Query.class))).thenReturn(terminatingSelect);
        when(terminatingSelect.all()).thenReturn(Flux.fromIterable(recipes));

        var response = recipeRepository.findAllByName("recipe");

        StepVerifier.FirstStep<Recipe> recipeVerifier = StepVerifier.create(response);

        for (var recipe : recipes) {
            recipeVerifier.expectNextMatches(result -> result.equals(recipe));
        }
        recipeVerifier.verifyComplete();

        verify(template, times(1)).select(Recipe.class);
        verify(reactiveSelect, times(1)).from(eq(Recipe.RECIPES_TABLE_NAME));
        verify(selectWithProjection, times(1)).matching(any(Query.class));
        verify(terminatingSelect, times(1)).all();
        verifyNoMoreInteractions(template);
    }

    /**
     * Test finaAll with no recipes found.
     * @param startPage
     * @param pageSize
     */
    @ParameterizedTest
    @CsvSource({"0, 0", "0, 10", "1, 0", "1, 10"})
    void testFindAllNoRecipesFound(final long startPage, final int pageSize) {

        RecipeRepository recipeRepository = new RecipeRepository(template);

        when(template.select(Recipe.class)).thenReturn(reactiveSelect);
        when(reactiveSelect.from(eq(Recipe.RECIPES_TABLE_NAME))).thenReturn(selectWithProjection);
        when(selectWithProjection.matching(any(Query.class))).thenReturn(terminatingSelect);
        when(terminatingSelect.all()).thenReturn(Flux.empty());

        var response = recipeRepository.findAll(startPage, pageSize);

        StepVerifier.create(response).verifyComplete();

        verify(template, times(1)).select(Recipe.class);
        verify(reactiveSelect, times(1)).from(eq(Recipe.RECIPES_TABLE_NAME));
        verify(selectWithProjection, times(1)).matching(any(Query.class));
        verify(terminatingSelect, times(1)).all();
        verifyNoMoreInteractions(template);
    }

    /**
     * Test finaAll with one recipe found.
     * @param startPage
     * @param pageSize
     */
    @ParameterizedTest
    @CsvSource({"0, 0", "0, 10", "1, 0", "1, 10"})
    void testFindAllRecipeFoundOne(final long startPage, final int pageSize) {

        RecipeRepository recipeRepository = new RecipeRepository(template);
        var recipe = Instancio.create(Recipe.class);

        when(template.select(Recipe.class)).thenReturn(reactiveSelect);
        when(reactiveSelect.from(eq(Recipe.RECIPES_TABLE_NAME))).thenReturn(selectWithProjection);
        when(selectWithProjection.matching(any(Query.class))).thenReturn(terminatingSelect);
        when(terminatingSelect.all()).thenReturn(Flux.just(recipe));

        var response = recipeRepository.findAll(FIRST_PAGE, PAGE_SIZE_10);

        StepVerifier.create(response)
                .expectNextMatches(result -> result.equals(recipe))
                .verifyComplete();

        verify(template, times(1)).select(Recipe.class);
        verify(reactiveSelect, times(1)).from(eq(Recipe.RECIPES_TABLE_NAME));
        verify(selectWithProjection, times(1)).matching(any(Query.class));
        verify(terminatingSelect, times(1)).all();
        verifyNoMoreInteractions(template);
    }

    /**
     * Test finaAll with more than one recipe found.
     * @param startPage
     * @param pageSize
     */
    @ParameterizedTest
    @CsvSource({"0, 0", "0, 10", "1, 0", "1, 10"})
    void testFindAllRecipeFoundMoreThanOne(final long startPage, final int pageSize) {

        RecipeRepository recipeRepository = new RecipeRepository(template);
        var recipes = Instancio.ofList(Recipe.class).size(new Random().nextInt(PAGE_SIZE_10 - 2) + 2).create();

        when(template.select(Recipe.class)).thenReturn(reactiveSelect);
        when(reactiveSelect.from(eq(Recipe.RECIPES_TABLE_NAME))).thenReturn(selectWithProjection);
        when(selectWithProjection.matching(any(Query.class))).thenReturn(terminatingSelect);
        when(terminatingSelect.all()).thenReturn(Flux.fromIterable(recipes));

        var response = recipeRepository.findAll(FIRST_PAGE, PAGE_SIZE_10);

        StepVerifier.FirstStep<Recipe> recipeVerifier = StepVerifier.create(response);

        for (var recipe : recipes) {
            recipeVerifier.expectNextMatches(result -> result.equals(recipe));
        }
        recipeVerifier.verifyComplete();

        verify(template, times(1)).select(Recipe.class);
        verify(reactiveSelect, times(1)).from(eq(Recipe.RECIPES_TABLE_NAME));
        verify(selectWithProjection, times(1)).matching(any(Query.class));
        verify(terminatingSelect, times(1)).all();
        verifyNoMoreInteractions(template);
    }

    /**
     * Test countAll.
     */
    @Test
    void testCountAll() {

        RecipeRepository recipeRepository = new RecipeRepository(template);

        when(template.select(Recipe.class)).thenReturn(reactiveSelect);
        when(reactiveSelect.from(eq(Recipe.RECIPES_TABLE_NAME))).thenReturn(selectWithProjection);
        when(selectWithProjection.count()).thenReturn(Mono.just(NUMBER_OF_RECIPES));

        var response = recipeRepository.countAll();

        StepVerifier.create(response)
                .expectNextMatches(result -> result == NUMBER_OF_RECIPES)
                .verifyComplete();

        verify(template, times(1)).select(Recipe.class);
        verify(reactiveSelect, times(1)).from(eq(Recipe.RECIPES_TABLE_NAME));
        verify(selectWithProjection, times(1)).count();
        verifyNoMoreInteractions(template);
    }

    /**
     * Test existsById where it doesn't exist.
     */
    @Test
    void testExistByIDDoesNotExist() {

        RecipeRepository recipeRepository = new RecipeRepository(template);

        when(template.select(Recipe.class)).thenReturn(reactiveSelect);
        when(reactiveSelect.from(eq(Recipe.RECIPES_TABLE_NAME))).thenReturn(selectWithProjection);
        when(selectWithProjection.matching(any(Query.class))).thenReturn(terminatingSelect);
        when(terminatingSelect.exists()).thenReturn(Mono.just(false));

        var response = recipeRepository.existsById(RECIPE_ID);

        StepVerifier.create(response)
                .expectNextMatches(result -> !result)
                .verifyComplete();

        verify(template, times(1)).select(Recipe.class);
        verify(reactiveSelect, times(1)).from(eq(Recipe.RECIPES_TABLE_NAME));
        verify(selectWithProjection, times(1)).matching(any(Query.class));
        verify(terminatingSelect, times(1)).exists();
        verifyNoMoreInteractions(template);
    }

    /**
     * Test existsById where it exists.
     */
    @Test
    void testeExistsByIdDoesExist() {

        RecipeRepository recipeRepository = new RecipeRepository(template);

        when(template.select(Recipe.class)).thenReturn(reactiveSelect);
        when(reactiveSelect.from(eq(Recipe.RECIPES_TABLE_NAME))).thenReturn(selectWithProjection);
        when(selectWithProjection.matching(any(Query.class))).thenReturn(terminatingSelect);
        when(terminatingSelect.exists()).thenReturn(Mono.just(true));

        var response = recipeRepository.existsById(RECIPE_ID);

        StepVerifier.create(response)
                .expectNextMatches(result -> result)
                .verifyComplete();

        verify(template, times(1)).select(Recipe.class);
        verify(reactiveSelect, times(1)).from(eq(Recipe.RECIPES_TABLE_NAME));
        verify(selectWithProjection, times(1)).matching(any(Query.class));
        verify(terminatingSelect, times(1)).exists();
        verifyNoMoreInteractions(template);
    }

    /**
     * Test findById where it doesn't exist.
     */
    @Test
    void testFindByIDDoesNotExist() {

        RecipeRepository recipeRepository = new RecipeRepository(template);

        when(template.select(Recipe.class)).thenReturn(reactiveSelect);
        when(reactiveSelect.from(eq(Recipe.RECIPES_TABLE_NAME))).thenReturn(selectWithProjection);
        when(selectWithProjection.matching(any(Query.class))).thenReturn(terminatingSelect);
        when(terminatingSelect.one()).thenReturn(Mono.empty());

        var response = recipeRepository.findById(RECIPE_ID);

        StepVerifier.create(response)
                .verifyComplete();

        verify(template, times(1)).select(Recipe.class);
        verify(reactiveSelect, times(1)).from(eq(Recipe.RECIPES_TABLE_NAME));
        verify(selectWithProjection, times(1)).matching(any(Query.class));
        verify(terminatingSelect, times(1)).one();
        verifyNoMoreInteractions(template);
    }

    /**
     * Test findById where it exists.
     */
    @Test
    void testFindByIdDoesExist() {

        RecipeRepository recipeRepository = new RecipeRepository(template);
        var recipe = Instancio.create(Recipe.class);

        when(template.select(Recipe.class)).thenReturn(reactiveSelect);
        when(reactiveSelect.from(eq(Recipe.RECIPES_TABLE_NAME))).thenReturn(selectWithProjection);
        when(selectWithProjection.matching(any(Query.class))).thenReturn(terminatingSelect);
        when(terminatingSelect.one()).thenReturn(Mono.just(recipe));

        var response = recipeRepository.findById(RECIPE_ID);

        StepVerifier.create(response)
                .expectNextMatches(result -> result.equals(recipe))
                .verifyComplete();

        verify(template, times(1)).select(Recipe.class);
        verify(reactiveSelect, times(1)).from(eq(Recipe.RECIPES_TABLE_NAME));
        verify(selectWithProjection, times(1)).matching(any(Query.class));
        verify(terminatingSelect, times(1)).one();
        verifyNoMoreInteractions(template);
    }

    /**
     * Test save.
     */
    @Test
    void testSave() {

        RecipeRepository recipeRepository = new RecipeRepository(template);
        var recipe = Instancio.create(Recipe.class);

        when(template.insert(eq(Recipe.class))).thenReturn(reactiveInsert);
        when(reactiveInsert.into(eq(Recipe.RECIPES_TABLE_NAME))).thenReturn(terminatingInsert);
        when(terminatingInsert.using(any(Recipe.class))).thenReturn(Mono.just(recipe));

        var response = recipeRepository.save(recipe);

        StepVerifier.create(response)
                .expectNextMatches(result -> result.equals(recipe))
                .verifyComplete();

        verify(template, times(1)).insert(eq(Recipe.class));
        verify(reactiveInsert, times(1)).into(eq(Recipe.RECIPES_TABLE_NAME));
        verify(terminatingInsert, times(1)).using(any(Recipe.class));
        verifyNoMoreInteractions(template);
    }

    /**
     * Test update.
     */
    @Test
    void testUpdate() {

        RecipeRepository recipeRepository = new RecipeRepository(template);
        var recipe = Instancio.create(Recipe.class);

        when(template.update(eq(ReactiveUpdateOperation.UpdateWithTable.class))).thenReturn(reactiveUpdate);
        when(reactiveUpdate.inTable(eq(Recipe.RECIPES_TABLE_NAME))).thenReturn(updateWithQuery);
        when(updateWithQuery.matching(any(Query.class))).thenReturn(terminatingUpdate);
        when(terminatingUpdate.apply(any(Update.class))).thenReturn(Mono.just(RECIPE_ID));

        var response = recipeRepository.update(recipe);

        StepVerifier.create(response)
                .expectNextMatches(result -> result == RECIPE_ID)
                .verifyComplete();

        verify(template, times(1)).update(eq(ReactiveUpdateOperation.UpdateWithTable.class));
        verify(reactiveUpdate, times(1)).inTable(eq(Recipe.RECIPES_TABLE_NAME));
        verify(updateWithQuery, times(1)).matching(any(Query.class));
        verify(terminatingUpdate, times(1)).apply(any(Update.class));
        verifyNoMoreInteractions(template);
    }

    /**
     * Test deleteById.
     */
    @Test
    void testDeleteById() {

        RecipeRepository recipeRepository = new RecipeRepository(template);

        when(template.delete(eq(Recipe.class))).thenReturn(reactiveDelete);
        when(reactiveDelete.from(eq(Recipe.RECIPES_TABLE_NAME))).thenReturn(deleteWithQuery);
        when(deleteWithQuery.matching(any(Query.class))).thenReturn(terminatingDelete);
        when(terminatingDelete.all()).thenReturn(Mono.just(RECIPE_ID));

        var response = recipeRepository.deleteById(RECIPE_ID);

        StepVerifier.create(response)
                .expectNextMatches(result -> result == RECIPE_ID)
                .verifyComplete();

        verify(template, times(1)).delete(eq(Recipe.class));
        verify(reactiveDelete, times(1)).from(eq(Recipe.RECIPES_TABLE_NAME));
        verify(deleteWithQuery, times(1)).matching(any(Query.class));
        verify(terminatingDelete, times(1)).all();
        verifyNoMoreInteractions(template);
    }
}
