package org.grego.springboot.recipeservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.grego.springboot.recipeservice.model.Recipe;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.SimpleRepresentationModelAssembler;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.core.DummyInvocationUtils.methodOn;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.afford;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

@Component
public class RecipeResourceAssembler implements SimpleRepresentationModelAssembler<Recipe> {
    @Autowired
    private ObjectMapper objectMapper;

    @SneakyThrows
    @Override
    public void addLinks(EntityModel<Recipe> resource) {
        resource.add(linkTo(
            methodOn(RecipeController.class).getRecipe(resource.getContent().getRecipeId(), false)).withSelfRel()
                .andAffordance(afford(
                    methodOn(RecipeController.class).deleteRecipe(resource.getContent().getRecipeId())))
                .andAffordance(afford(
                    methodOn(RecipeController.class).updateRecipe(objectMapper.writeValueAsString(resource.getContent()), false))));
    }

    @Override
    public void addLinks(CollectionModel<EntityModel<Recipe>> resources) {
    }
}
