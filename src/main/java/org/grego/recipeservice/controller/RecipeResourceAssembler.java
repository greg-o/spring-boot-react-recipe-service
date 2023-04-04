/**
 * Recipe Service.
 * Copyright: none
 *
 * @author Greg-O
 */
package org.grego.recipeservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.grego.recipeservice.model.Recipe;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.SimpleRepresentationModelAssembler;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.core.DummyInvocationUtils.methodOn;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.afford;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

/**
 * RecipeResourceAssembler adds the hyper links for a Recipe.
 */
@Component
public class RecipeResourceAssembler implements SimpleRepresentationModelAssembler<Recipe> {
    /**
     * The ObjectMapper converts objects into JSON and JSON into objects.
     */
    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Add hyper links for a Recipe.
     * @param resource
     */
    @SneakyThrows
    @Override
    public void addLinks(final EntityModel<Recipe> resource) {
        resource.add(linkTo(
                methodOn(RecipeController.class)
                        .getRecipe(resource.getContent().getRecipeId(), false)).withSelfRel()
                .andAffordance(afford(
                        methodOn(RecipeController.class).deleteRecipe(resource.getContent().getRecipeId())))
                .andAffordance(afford(
                        methodOn(RecipeController.class)
                                .updateRecipe(objectMapper.writeValueAsString(resource.getContent()), false))));
    }

    /**
     * Add hyper links for a list of recipes.
     * @param resources
     */
    @Override
    public void addLinks(final CollectionModel<EntityModel<Recipe>> resources) {
    }
}
