package org.grego.springboot.recipeservice.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.annotation.Timed;
import org.grego.springboot.recipeservice.model.Recipe;
import org.grego.springboot.recipeservice.service.IRecipeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

@RestController
@RequestMapping(path = "/recipes")
public class RecipeController {

    @Autowired
    private IRecipeService recipeService;

    @Autowired
    private RecipeResourceAssembler recipeResourceAssembler;

    @Autowired
    private ObjectMapper objectMapper;


    @Timed
    @GetMapping(path = "/list",
        produces = {
            de.ingogriebsch.spring.hateoas.siren.MediaTypes.SIREN_JSON_VALUE,
            org.springframework.http.MediaType.APPLICATION_JSON_VALUE
        }
    )
    public Mono<ResponseEntity<?>> listRecipes(
            @RequestParam(value = "page-number", required = false, defaultValue = "1")
                Long pageNumber,
            @RequestParam(value = "page-size", required = false, defaultValue = "${service.default_page_size:20}")
                Integer pageSize,
            @RequestParam(name = "include-hyper-links", required = false, defaultValue = "false")
                Boolean includeHyperLinks) {

        if (includeHyperLinks) {
            return listRecipesWithHyperLinks(pageNumber, pageSize);
        } else {
            return listRecipesWithoutHyperLinks(pageNumber, pageSize);
        }
    }

    @Timed
    @GetMapping(path = "/get/{id}",
        produces = {
            de.ingogriebsch.spring.hateoas.siren.MediaTypes.SIREN_JSON_VALUE,
            org.springframework.http.MediaType.APPLICATION_JSON_VALUE
        }
    )
    public Mono<ResponseEntity<?>> getRecipe(
            @PathVariable("id") long id,
            @RequestParam(name = "include-hyper-links", required = false, defaultValue = "false")
                Boolean includeHyperLinks) {
        return recipeService.getRecipeById(id)
            .map(recipe -> getRecipeResponse(includeHyperLinks, recipe));
    }

    @Timed
    @PutMapping(path = "/add",
        consumes = org.springframework.http.MediaType.ALL_VALUE,
        produces = {
            de.ingogriebsch.spring.hateoas.siren.MediaTypes.SIREN_JSON_VALUE,
            org.springframework.http.MediaType.APPLICATION_JSON_VALUE
        }
    )
    public Mono<ResponseEntity<?>> addRecipe(
            @RequestBody() String recipe,
            @RequestParam(name = "include-hyper-links", required = false, defaultValue = "false")
                Boolean includeHyperLinks) {

        try {
            return recipeService.addRecipe(objectMapper.readValue(recipe, Recipe.class))
                .map(savedRecipe -> getRecipeResponse(includeHyperLinks, savedRecipe));
        } catch (JsonProcessingException e) {
            return Mono.just(ResponseEntity.internalServerError().build());
        }
    }

    @Timed
    @PatchMapping(path = "/update",
        consumes = org.springframework.http.MediaType.ALL_VALUE,
        produces = {
            de.ingogriebsch.spring.hateoas.siren.MediaTypes.SIREN_JSON_VALUE,
            org.springframework.http.MediaType.APPLICATION_JSON_VALUE
        }
    )
    public Mono<ResponseEntity<?>> updateRecipe(
            @RequestBody() String recipe,
            @RequestParam(name = "include-hyper-links", required = false, defaultValue = "false")
                Boolean includeHyperLinks) {
        try {
            return recipeService.updateRecipe(objectMapper.readValue(recipe, Recipe.class))
                .map(updatedRecipe -> getRecipeResponse(includeHyperLinks, updatedRecipe));
        } catch (JsonProcessingException ex) {
            return Mono.just(ResponseEntity.internalServerError().build());
        }
    }

    @Timed
    @DeleteMapping(path = "/delete/{id}",
        produces = MediaType.TEXT_PLAIN_VALUE
    )
    public Mono<ResponseEntity<?>> deleteRecipe(@PathVariable("id") long id) {
        return recipeService.deleteRecipeById(id)
            .map(recipeId -> ResponseEntity.ok(String.format("Deleted recipe %d", recipeId)));
    }

    private Mono<ResponseEntity<?>> listRecipesWithHyperLinks(Long pageNumber, Integer pageSize) {
        return Mono.zip(recipeService.getAllRecipes(pageNumber, pageSize).collectList(),
            recipeService.getRecipeCount()
        ).map(tuple -> {
            var recipeCollectionModel = recipeResourceAssembler.toCollectionModel(tuple.getT1());

            var metadata = new PagedModel.PageMetadata(tuple.getT1().size(), pageNumber, tuple.getT2(),
                    (tuple.getT2() / pageNumber));
            var link = linkTo(
                methodOn(RecipeController.class).listRecipes(pageNumber, pageSize, true)).withSelfRel()
                .andAffordance(afford(methodOn(RecipeController.class).addRecipe(null, false)));
            var pagedModel = PagedModel.of(recipeCollectionModel.getContent(), metadata, link);

            try {
                return ResponseEntity.ok()
                    .contentType(de.ingogriebsch.spring.hateoas.siren.MediaTypes.SIREN_JSON)
                    .body(objectMapper.writeValueAsString(pagedModel));
            } catch (JsonProcessingException ex) {
                return ResponseEntity.internalServerError().build();
            }
        });
    }

    private Mono<ResponseEntity<?>> listRecipesWithoutHyperLinks(Long pageNumber, Integer pageSize) {
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

    private ResponseEntity<?> getRecipeResponse(Boolean includeHyperLinks, Recipe recipe) {
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
