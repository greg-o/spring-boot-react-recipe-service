package org.grego.recipeservice.repository;

import org.grego.recipeservice.model.Ingredient;
import org.instancio.Instancio;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.OngoingStubbing;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.r2dbc.core.ReactiveDeleteOperation;
import org.springframework.data.r2dbc.core.ReactiveInsertOperation;
import org.springframework.data.r2dbc.core.ReactiveUpdateOperation;
import org.springframework.data.relational.core.query.Query;
import org.springframework.data.relational.core.query.Update;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Random;
import java.util.stream.LongStream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Test the IngredientRepository.
 */
@ExtendWith(MockitoExtension.class)
@RunWith(PowerMockRunner.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag("UnitTests")
public class IngredientRepositoryTest {
    /**
     * At least create this many ingredients.
     */
    public static final int AT_LEAST_INGREDIENTS = 2;

    /**
     * Range for the number of additional ingredients.
     */
    public static final int NUMBER_OF_ADDITIONAL_INGREDIENTS_RANGE = 3;
    /**
     * The Rddbc entity template used to perform reactive database operations.
     */
    @Mock
    private R2dbcEntityTemplate template;

    /**
     * Reactive insert.
     */
    @Mock
    private ReactiveInsertOperation.ReactiveInsert<Ingredient> reactiveInsert;

    /**
     * Terminating insert.
     */
    @Mock
    private ReactiveInsertOperation.TerminatingInsert<Ingredient> terminatingInsert;

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
    private ReactiveUpdateOperation.UpdateWithQuery terminatingUpdate;

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
     * Test save.
     */
    @Test
    void testSave() {

        IngredientRepository ingredientRepository = new IngredientRepository(template);
        Ingredient ingredient = Instancio.create(Ingredient.class);

        when(template.insert(eq(Ingredient.class))).thenReturn(reactiveInsert);
        when(reactiveInsert.into(eq(Ingredient.INGREDIENTS_TABLE_NAME))).thenReturn(terminatingInsert);
        when(terminatingInsert.using(any(Ingredient.class))).thenReturn(Mono.just(ingredient));

        var response = ingredientRepository.save(ingredient);

        StepVerifier.create(response)
                .expectNextMatches(result -> result.equals(ingredient))
                .verifyComplete();

        verify(template, times(1)).insert(eq(Ingredient.class));
        verify(reactiveInsert, times(1)).into(eq(Ingredient.INGREDIENTS_TABLE_NAME));
        verify(terminatingInsert, times(1)).using(any(Ingredient.class));
        verifyNoMoreInteractions(template);
    }

    /**
     * Test saveAll.
     */
    @Test
    void testSaveAll() {

        IngredientRepository ingredientRepository = new IngredientRepository(template);
        List<Ingredient> ingredients =
            Instancio.ofList(Ingredient.class)
                .size(AT_LEAST_INGREDIENTS + new Random().nextInt(NUMBER_OF_ADDITIONAL_INGREDIENTS_RANGE))
                .create();

        when(template.insert(eq(Ingredient.class))).thenReturn(reactiveInsert);
        when(reactiveInsert.into(eq(Ingredient.INGREDIENTS_TABLE_NAME))).thenReturn(terminatingInsert);
        OngoingStubbing<Mono<Ingredient>> when = when(terminatingInsert.using(any(Ingredient.class)));
        for (var ingredient : ingredients) {
            when = when.thenReturn(Mono.just(ingredient));
        }

        var response = ingredientRepository.saveAll(ingredients);

        StepVerifier.FirstStep<Ingredient> verifier = StepVerifier.create(response);

        for (var ingredient : ingredients) {
            verifier.expectNextMatches(result -> result.equals(ingredient));
        }
        verifier.verifyComplete();

        verify(template, times(ingredients.size())).insert(eq(Ingredient.class));
        verify(reactiveInsert, times(ingredients.size())).into(eq(Ingredient.INGREDIENTS_TABLE_NAME));
        verify(terminatingInsert, times(ingredients.size())).using(any(Ingredient.class));
        verifyNoMoreInteractions(template);
    }

    /**
     * Test update.
     */
    @Test
    void testUpdate() {

        IngredientRepository ingredientRepository = new IngredientRepository(template);
        Ingredient ingredient = Instancio.create(Ingredient.class);

        when(template.update(eq(ReactiveUpdateOperation.UpdateWithTable.class))).thenReturn(reactiveUpdate);
        when(reactiveUpdate.inTable(eq(Ingredient.INGREDIENTS_TABLE_NAME))).thenReturn(updateWithQuery);
        when(updateWithQuery.matching(any(Query.class))).thenReturn(terminatingUpdate);
        when(terminatingUpdate.apply(any(Update.class))).thenReturn(Mono.just(ingredient.getIngredientId()));

        var response = ingredientRepository.update(ingredient);

        StepVerifier.create(response)
                .expectNextMatches(result -> result == ingredient.getIngredientId())
                .verifyComplete();

        verify(template, times(1)).update(eq(ReactiveUpdateOperation.UpdateWithTable.class));
        verify(reactiveUpdate, times(1)).inTable(eq(Ingredient.INGREDIENTS_TABLE_NAME));
        verify(updateWithQuery, times(1)).matching(any(Query.class));
        verify(terminatingUpdate, times(1)).apply(any(Update.class));
        verifyNoMoreInteractions(template);
    }

    /**
     * Test updateAll.
     */
    @Test
    void testUpdateAll() {

        IngredientRepository ingredientRepository = new IngredientRepository(template);
        List<Ingredient> ingredients =
            Instancio.ofList(Ingredient.class)
                .size(AT_LEAST_INGREDIENTS + new Random().nextInt(NUMBER_OF_ADDITIONAL_INGREDIENTS_RANGE))
                .create();

        when(template.update(eq(ReactiveUpdateOperation.UpdateWithTable.class))).thenReturn(reactiveUpdate);
        when(reactiveUpdate.inTable(eq(Ingredient.INGREDIENTS_TABLE_NAME))).thenReturn(updateWithQuery);
        when(updateWithQuery.matching(any(Query.class))).thenReturn(terminatingUpdate);
        OngoingStubbing<Mono<Long>> when = when(terminatingUpdate.apply(any(Update.class)));
        for (var ingredient : ingredients) {
            when = when.thenReturn(Mono.just(ingredient.getIngredientId()));
        }

        var response = ingredientRepository.updateAll(ingredients);

        StepVerifier.FirstStep<Long> verifier = StepVerifier.create(response);
        for (var ingredient : ingredients) {
            verifier.expectNextMatches(result -> result == ingredient.getIngredientId());
        }
        verifier.verifyComplete();

        verify(template, times(ingredients.size())).update(eq(ReactiveUpdateOperation.UpdateWithTable.class));
        verify(reactiveUpdate, times(ingredients.size())).inTable(eq(Ingredient.INGREDIENTS_TABLE_NAME));
        verify(updateWithQuery, times(ingredients.size())).matching(any(Query.class));
        verify(terminatingUpdate, times(ingredients.size())).apply(any(Update.class));
        verifyNoMoreInteractions(template);
    }

    /**
     * Test deleteById.
     */
    @Test
    void testDeleteById() {

        IngredientRepository ingredientRepository = new IngredientRepository(template);
        var instructionId = new Random().nextLong();

        when(template.delete(eq(Ingredient.class))).thenReturn(reactiveDelete);
        when(reactiveDelete.from(eq(Ingredient.INGREDIENTS_TABLE_NAME))).thenReturn(deleteWithQuery);
        when(deleteWithQuery.matching(any(Query.class))).thenReturn(terminatingDelete);
        when(terminatingDelete.all()).thenReturn(Mono.just(instructionId));

        var response = ingredientRepository.deleteById(instructionId);

        StepVerifier.create(response)
                .expectNextMatches(result -> result == instructionId)
                .verifyComplete();

        verify(template, times(1)).delete(eq(Ingredient.class));
        verify(reactiveDelete, times(1)).from(eq(Ingredient.INGREDIENTS_TABLE_NAME));
        verify(deleteWithQuery, times(1)).matching(any(Query.class));
        verify(terminatingDelete, times(1)).all();
        verifyNoMoreInteractions(template);
    }

    /**
     * Test deleteAllByIds.
     */
    @Test
    void testDeleteAllByIds() {

        IngredientRepository ingredientRepository = new IngredientRepository(template);
        var ingredientIds =
            LongStream
                .range(0, AT_LEAST_INGREDIENTS + new Random().nextInt(NUMBER_OF_ADDITIONAL_INGREDIENTS_RANGE))
                .boxed()
                .toList();
        when(template.delete(eq(Ingredient.class))).thenReturn(reactiveDelete);
        when(reactiveDelete.from(eq(Ingredient.INGREDIENTS_TABLE_NAME))).thenReturn(deleteWithQuery);
        when(deleteWithQuery.matching(any(Query.class))).thenReturn(terminatingDelete);
        OngoingStubbing<Mono<Long>> when = when(terminatingDelete.all());
        for (var ingredientId : ingredientIds) {
            when = when.thenReturn(Mono.just(ingredientId));
        }

        var response = ingredientRepository.deleteAllByIds(ingredientIds);

        StepVerifier.FirstStep<Long> verifier = StepVerifier.create(response);
        for (var ingredientId : ingredientIds) {
            verifier.expectNextMatches(result -> result == ingredientId);
        }
        verifier.verifyComplete();

        verify(template, times(ingredientIds.size())).delete(eq(Ingredient.class));
        verify(reactiveDelete, times(ingredientIds.size())).from(eq(Ingredient.INGREDIENTS_TABLE_NAME));
        verify(deleteWithQuery, times(ingredientIds.size())).matching(any(Query.class));
        verify(terminatingDelete, times(ingredientIds.size())).all();
        verifyNoMoreInteractions(template);
    }
}
