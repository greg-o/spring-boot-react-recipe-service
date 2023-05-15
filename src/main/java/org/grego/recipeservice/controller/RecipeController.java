/**
 * Recipe Service.
 * Copyright: none
 *
 * @author Greg-O
 */
package org.grego.recipeservice.controller;

import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.annotation.Timed;
import jakarta.json.stream.JsonGenerator;
import org.grego.recipeservice.model.Ingredient;
import org.grego.recipeservice.model.Instruction;
import org.grego.recipeservice.model.QuantitySpecifier;
import org.grego.recipeservice.service.IRecipeService;
import org.grego.recipeservice.model.Recipe;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import reactor.core.publisher.Mono;

import java.io.StringWriter;
import java.util.Collections;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.afford;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * RecipeController defines the http methods supported by the Recipe Service.
 */
@RestController
@RequestMapping(
    path = "/recipes",
    produces = {
        de.ingogriebsch.spring.hateoas.siren.MediaTypes.SIREN_JSON_VALUE,
        org.springframework.http.MediaType.APPLICATION_JSON_VALUE
    }
)
public class RecipeController {

    /**
     * Sample recipe for HATEOAS.
     */
    public static final Recipe SAMPLE_RECIPE = Recipe.builder()
        .name("Sample Recipe")
        .description("Sample description")
        .ingredients(Collections.singletonList(Ingredient.builder()
            .ingredient("Sample Ingredient")
            .quantitySpecifier(QuantitySpecifier.Unspecified)
            .quantity(1.0d)
            .build()))
        .instructions(Collections.singletonList(Instruction.builder()
            .instruction("Sample Instruction")
            .build()))
        .build();
    /**
     * IRecipeService is for performing recipe service operations.
     */
    @Autowired
    private IRecipeService recipeService;

    /**
     * RecipeResourceAssembler adds hyper-links to Recipe results.
     */
    @Autowired
    private RecipeResourceAssembler recipeResourceAssembler;

    /**
     * ObjectMapper maps objects to JSON and JSON to objects.
     */
    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Constructor to build RecipeController to set class variables.
     * @param service
     * @param resourceAssembler
     * @param mapper
     */
    public RecipeController(final IRecipeService service, final RecipeResourceAssembler resourceAssembler,
                            final ObjectMapper mapper) {
        this.recipeService = service;
        this.recipeResourceAssembler = resourceAssembler;
        this.objectMapper = mapper;
    }

    /**
     * Retrieve a list of recipes.
     * @param pageNumber
     * @param pageSize
     * @param includeHyperLinks
     * @return the list of Recipes.
     */
    @Timed
    @GetMapping(path = "/list",
            produces = {
                de.ingogriebsch.spring.hateoas.siren.MediaTypes.SIREN_JSON_VALUE,
                org.springframework.http.MediaType.APPLICATION_JSON_VALUE
            }
    )
    public  Mono<ResponseEntity<?>> listRecipes(
            @RequestParam(value = "page-number", required = false, defaultValue = "1")
            final long pageNumber,
            @RequestParam(value = "page-size", required = false, defaultValue = "${service.default_page_size:20}")
            final int pageSize,
            @RequestParam(name = "include-hyper-links", required = false, defaultValue = "false")
            final Boolean includeHyperLinks) {

        if (pageNumber < 1) {
            return Mono.just(ResponseEntity.badRequest()
                .contentType(MediaType.TEXT_PLAIN)
                .body(String.format("Pages begin at 1:  page-number = %d", pageNumber)));
        }

        if (includeHyperLinks) {
            return listRecipesWithHyperLinks(pageNumber, pageSize);
        } else {
            return listRecipesWithoutHyperLinks(pageNumber, pageSize);
        }
    }

    /**
     * Get a recipe by the recipe id.
     * @param id
     * @param includeHyperLinks
     * @return A recipe for the recipe id
     */
    @Timed
    @GetMapping(path = "/get/{id}",
            produces = {
                    de.ingogriebsch.spring.hateoas.siren.MediaTypes.SIREN_JSON_VALUE,
                    org.springframework.http.MediaType.APPLICATION_JSON_VALUE
            }
    )
    public Mono<ResponseEntity<?>> getRecipe(
            @PathVariable("id") final long id,
            @RequestParam(name = "include-hyper-links", required = false, defaultValue = "false")
            final Boolean includeHyperLinks) {
        Mono<ResponseEntity<?>> response = recipeService.getRecipeById(id)
            .map(recipe -> getRecipeResponse(includeHyperLinks, recipe));

        return response.switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }

    /**
     * Add a recipe.
     * @param recipe
     * @param includeHyperLinks
     * @return The added recipe.
     */
    @Timed
    @PutMapping(path = "/add",
            consumes = org.springframework.http.MediaType.ALL_VALUE,
            produces = {
                    de.ingogriebsch.spring.hateoas.siren.MediaTypes.SIREN_JSON_VALUE,
                    org.springframework.http.MediaType.APPLICATION_JSON_VALUE
            }
    )
    public Mono<ResponseEntity<?>> addRecipe(
            @RequestBody final String recipe,
            @RequestParam(name = "include-hyper-links", required = false, defaultValue = "false")
            final Boolean includeHyperLinks) {

        try {
            return recipeService.addRecipe(objectMapper.readValue(recipe, Recipe.class))
                .map(savedRecipe -> getRecipeResponse(includeHyperLinks, savedRecipe));
        } catch (JsonProcessingException e) {
            return Mono.just(ResponseEntity.internalServerError().build());
        }
    }

    /**
     * Update a recipe.
     * @param recipe
     * @param includeHyperLinks
     * @return The recipe that was updated
     */
    @Timed
    @PatchMapping(path = "/update",
            consumes = org.springframework.http.MediaType.ALL_VALUE,
            produces = {
                    de.ingogriebsch.spring.hateoas.siren.MediaTypes.SIREN_JSON_VALUE,
                    org.springframework.http.MediaType.APPLICATION_JSON_VALUE
            }
    )
    public Mono<ResponseEntity<?>> updateRecipe(
            @RequestBody() final String recipe,
            @RequestParam(name = "include-hyper-links", required = false, defaultValue = "false")
            final Boolean includeHyperLinks) {
        try {
            Mono<ResponseEntity<?>> response = recipeService.updateRecipe(objectMapper.readValue(recipe, Recipe.class))
                    .map(updatedRecipe -> getRecipeResponse(includeHyperLinks, updatedRecipe));

            return response.switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
        } catch (JsonProcessingException ex) {
            return Mono.just(ResponseEntity.internalServerError().build());
        }
    }

    /**
     * Delete a recipe by recipe id.
     * @param id
     * @return The id of the recipe that was deleted
     */
    @Timed
    @DeleteMapping(path = "/delete/{id}",
            produces = MediaType.TEXT_PLAIN_VALUE
    )
    public Mono<ResponseEntity<?>> deleteRecipe(@PathVariable("id") final long id) {
        Mono<ResponseEntity<?>> response =  recipeService.deleteRecipeById(id)
            .map(recipeId -> ResponseEntity.ok(String.format("Deleted recipe %d", recipeId)));

        return response.switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }

    /**
     * Search for recipes.
     * @param searchString
     * @return The search results for the search string
     */
    @Timed
    @GetMapping(path = "/search",
        produces = {
            org.springframework.http.MediaType.APPLICATION_JSON_VALUE
        }
    )
    public Mono<ResponseEntity<?>> searchRecipes(
            @RequestParam(value = "search-string", required = true)
            final String searchString) {

        return recipeService.searchRecipes(searchString).map(results -> {
            StringWriter writer = new StringWriter();
            JacksonJsonpMapper jacksonJsonpMapper = new JacksonJsonpMapper(objectMapper);

            try (JsonGenerator generator = jacksonJsonpMapper.jsonProvider().createGenerator(writer)) {
                results.serialize(generator, jacksonJsonpMapper);
            }

            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(writer.toString());
        });
    }

    private Mono<ResponseEntity<?>> listRecipesWithHyperLinks(final Long pageNumber, final Integer pageSize) {
        return Mono.zip(recipeService.getAllRecipes(pageNumber, pageSize).collectList(),
            recipeService.getRecipeCount()
        ).map(tuple -> {
            try {
                var recipeCollectionModel = recipeResourceAssembler.toCollectionModel(tuple.getT1());

                var metadata = new PagedModel.PageMetadata(tuple.getT1().size(), pageNumber, tuple.getT2(),
                    (tuple.getT2() / pageNumber));
                Link link = linkTo(
                    methodOn(RecipeController.class).listRecipes(pageNumber, pageSize, true))
                    .withSelfRel()
                    .andAffordance(afford(methodOn(RecipeController.class)
                        .addRecipe(objectMapper.writeValueAsString(SAMPLE_RECIPE), false)));
                var pagedModel = PagedModel.of(recipeCollectionModel.getContent(), metadata, link);

                return ResponseEntity.ok()
                    .contentType(de.ingogriebsch.spring.hateoas.siren.MediaTypes.SIREN_JSON)
                    .body(objectMapper.writeValueAsString(pagedModel));
            } catch (JsonProcessingException ex) {
                return ResponseEntity.internalServerError().build();
            }
        });
    }

    private Mono<ResponseEntity<?>> listRecipesWithoutHyperLinks(final Long pageNumber, final Integer pageSize) {
        return recipeService.getAllRecipes(pageNumber, pageSize).collectList().map(recipes -> {
            try {
                return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(objectMapper.writeValueAsString(recipes));
            } catch (JsonProcessingException ex) {
                return ResponseEntity.internalServerError().build();
            }
        });
    }

    private ResponseEntity<?> getRecipeResponse(final Boolean includeHyperLinks, final Recipe recipe) {
        try {
            if (includeHyperLinks) {
                return ResponseEntity.ok()
                        .contentType(de.ingogriebsch.spring.hateoas.siren.MediaTypes.SIREN_JSON)
                        .body(objectMapper.writeValueAsString(recipeResourceAssembler.toModel(recipe)));
            } else {
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(objectMapper.writeValueAsString(recipe));
            }
        } catch (JsonProcessingException ex) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
