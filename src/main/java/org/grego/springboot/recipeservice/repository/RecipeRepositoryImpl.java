package org.grego.springboot.recipeservice.repository;

import lombok.RequiredArgsConstructor;
import org.grego.springboot.recipeservice.model.Ingredient;
import org.grego.springboot.recipeservice.model.Instruction;
import org.grego.springboot.recipeservice.model.Recipe;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class RecipeRepositoryImpl implements RecipeRepository {
//    private static final String SELECT_QUERY = """
//            SELECT r.recipe_id, r.name, r.variation, r.creation_date_time, r.last_modified_date_time,
//            i.ingredient_id, i.ingredient, i.ingredient_number, i.quantity, i.quantity_specifier,
//            i2.instruction_id, i2.instruction, i2.instruction_number FROM recipes r
//            LEFT JOIN recipes_ingredients ri ON r.recipe_id = ri.recipe_recipe_id
//            LEFT JOIN ingredients i ON ri.ingredients_ingredient_id = i.ingredient_id
//            LEFT JOIN recipes_instructions ri2 ON r.recipe_id  = ri2.recipe_recipe_id
//            LEFT JOIN instructions i2 ON ri2.instructions_instruction_id = i2.instruction_id
//            """;
private static final String SELECT_QUERY = """
            SELECT r.recipe_id, r.name, r.variation, r.creation_date_time, r.last_modified_date_time
            FROM recipes r
            """;

    private final IngredientRepository ingredientRepository;
    private final InstructionRepository instructionRepository;
    private final DatabaseClient client;

    @Override
    public Flux<Recipe> findAllByName(String name) {
        String query = String.format("%s WHERE r.name = '%s'", SELECT_QUERY, name);

        return client.sql(query)
                .fetch()
                .all()
                .bufferUntilChanged(result -> result.get("r_recipeId"))
                .flatMap(Recipe::fromRows);
    }

    @Override
    public Flux<Recipe> findAll() {
        return client.sql(SELECT_QUERY)
                .fetch()
                .all()
                .bufferUntilChanged(result -> result.get("recipe_id"))
                .flatMap(Recipe::fromRows);

    }

    @Override
    public Mono<Boolean> existsById(long recipeId) {
        String query = String.format("select exists (SELECT recipe_id  FROM recipe where recipe_id = :recipeId)");

        var results = client.sql(query)
                .bind("recipe_id", recipeId)
                .fetch()
                .first();

        return Mono.just(false);
    }

    @Override
    public Mono<Recipe> findById(long recipeId) {
        String query = String.format("%s WHERE r.recipeId = :recipeId", SELECT_QUERY);

        return client.sql(query)
                .bind("recipeId", recipeId)
                .fetch()
                .all()
                .bufferUntilChanged(result -> result.get("d_id"))
                .flatMap(Recipe::fromRows)
                .singleOrEmpty();
    }

    @Override
    @Transactional
    public Mono<Recipe> save(Recipe recipe) {
        return this.saveRecipe(recipe)
                .flatMap(this::deleteIngredients)
                .flatMap(this::saveIngredients)
                .flatMap(this::deleteInstructions)
                .flatMap(this::saveInstructions);
    }

    @Override
    @Transactional
    public Mono<Void> deleteById(long recipeId) {
        return this.deleteRecipe(recipeId)
                .flatMap(this::deleteIngredients)
                .flatMap(this::deleteInstructions)
                .then();
    }

    private Mono<Recipe> saveRecipe(Recipe recipe) {
        LocalDateTime currentTime = LocalDateTime.now();

        if (recipe.getRecipeId() == null) {
            var addRecipesWithSameName = this.findAllByName(recipe.getName());
            var nextVariation = addRecipesWithSameName.toStream().mapToLong(r -> r.getVariation()).max().orElse(0) + 1;

            return client.sql("INSERT INTO recipe(name, variation, description, creation_date_time, Last_modified_date_time) VALUES(:name, :variation, :description, :creationDateTime, :LastModifiedDateTime)")
                    .bind("name", recipe.getName())
                    .bind("variation", nextVariation)
                    .bind("description", recipe.getDescription())
                    .bind("creation_date_time", currentTime)
                    .bind("Last_modified_date_time", currentTime)
                    .filter((statement, executeFunction) -> statement.returnGeneratedValues("recipe_id").execute())
                    .fetch().first()
                    .doOnNext(result -> recipe.setRecipeId(Long.parseLong(result.get("id").toString())))
                    .thenReturn(recipe);
        } else {
            return this.client.sql("UPDATE recipe SET name = :name, description = :description, Last_modified_date_time = :LastModifiedDateTime WHERE recipe_id = :recipeIdd")
                    .bind("name", recipe.getName())
                    .bind("description", recipe.getDescription())
                    .bind("Last_modified_date_time", currentTime)
                    .bind("recipeId", recipe.getRecipeId())
                    .fetch().first()
                    .thenReturn(recipe);
        }
    }

    private Mono<Recipe> saveIngredients(Recipe recipe) {
        return Flux.fromIterable(recipe.getIngredients())
                .flatMap(this.ingredientRepository::save)
                .collectList()
                .doOnNext(recipe::setIngredients)
                .thenReturn(recipe);
    }

    private Mono<Recipe> saveInstructions(Recipe recipe) {
        return Flux.fromIterable(recipe.getInstructions())
                .flatMap(this.instructionRepository::save)
                .collectList()
                .doOnNext(recipe::setInstructions)
                .thenReturn(recipe);
    }

    private Mono<Recipe> deleteRecipe(long recipeId) {
        var recipe = findById(recipeId);
        client.sql("DELETE FROM recipe WHERE recipe_id = :recipeId")
                .bind("recipeId", recipeId)
                .fetch();

        return recipe;
    }

    private Mono<Recipe> deleteIngredients(Recipe recipe) {
        String query = "DELETE FROM ingredient WHERE recipe_id = :id OR employee_id in (:ids)";

        List<Long> ingredientIds = recipe.getIngredients().stream().map(Ingredient::getIngredientId).toList();

        return Mono.just(recipe)
                .flatMap(dep -> client.sql(query)
                        .bind("id", recipe.getRecipeId())
                        .bind("ids", ingredientIds.isEmpty() ? List.of(0) : ingredientIds)
                        .fetch().rowsUpdated())
                .thenReturn(recipe);
    }

    private Mono<Recipe> deleteInstructions(Recipe recipe) {
        String query = "DELETE FROM instruction WHERE recipe_id = :id OR employee_id in (:ids)";

        List<Long> instructionIds = recipe.getInstructions().stream().map(Instruction::getInstructionId).toList();

        return Mono.just(recipe)
                .flatMap(dep -> client.sql(query)
                        .bind("id", recipe.getRecipeId())
                        .bind("ids", instructionIds.isEmpty() ? List.of(0) : instructionIds)
                        .fetch().rowsUpdated())
                .thenReturn(recipe);
    }
}
