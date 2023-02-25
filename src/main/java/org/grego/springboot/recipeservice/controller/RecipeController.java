package org.grego.springboot.recipeservice.controller;

import io.micrometer.core.annotation.Timed;

import org.grego.springboot.recipeservice.model.Recipe;
import org.grego.springboot.recipeservice.service.IRecipeService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(path = "/recipes")//, produces = Array(MediaTypes.SIREN_JSON_VALUE))
public class RecipeController {

    @Autowired
    private IRecipeService recipeService;

//    @Autowired
//    private RecipeResourceAssembler recipeResourceAssembler;

    @Timed
    @GetMapping("/list")
    public Flux<Recipe> listRecipes() {
        return recipeService.getAllRecipes();
    }

    @Timed
    @GetMapping("/get/{id}")
    public Mono<Recipe> getRecipe(@PathVariable("id") long id) {
        return  recipeService.getRecipeById(id);
    }

    @Timed
    @PutMapping("/add")
    public Mono<Recipe> addRecipe(@RequestBody() Recipe recipe) {
        return recipeService.addRecipe(recipe);
    }

    @Timed
    @PatchMapping("/update")
    public Mono<Recipe> updateRecipe(@RequestBody() Recipe recipe) {
        return recipeService.updateRecipe(recipe);
    }

    @Timed
    @DeleteMapping("/delete/{id}")
    public Mono<Void> deleteRecipe(@PathVariable("id") long id){
        return recipeService.deleteRecipeById(id);
    }
}
