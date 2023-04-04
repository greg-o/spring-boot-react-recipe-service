/**
 * Recipe Service.
 * Copyright: none
 *
 * @author Greg-O
 */
package org.grego.recipeservice;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.spi.json.JsonProvider;
import net.minidev.json.JSONArray;
import org.apache.http.client.utils.URIBuilder;
import org.grego.recipeservice.controller.RecipeController;
import org.grego.recipeservice.model.Instruction;
import org.grego.recipeservice.model.QuantitySpecifier;
import org.grego.recipeservice.model.Recipe;
import org.grego.recipeservice.model.Ingredient;
import org.json.JSONException;
import org.junit.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.LinkedHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@ExtendWith(SpringExtension.class)
@SpringBootTest(properties = "spring.main.web-application-type=reactive",
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(locations = {"classpath:application-test.yml"})
class RecipeServiceAppTest {

    /**
     * The port that the RecipeServiceApp is using during tests.
     */
    @LocalServerPort
    private int port = 0;

    /**
     * The server properties that the RecipeServiceApp is using during tests.
     */
    @Autowired
    private ServerProperties serverProperties;

    /**
     * The rest client used to issue requests against the RecipeServiceApp.
     * Note:  TestRestTemplate does not support the patch command.
     */
    @Autowired
    private TestRestTemplate restTemplate;

    /**
     * The web client used to issue requests against the RecipeServiceApp.
     * Note:  WebTestClient supports the patch command.
     */
    @Autowired
    private WebTestClient webClient;

    /**
     * Direct access to the RecipeController.
     */
    @Autowired
    private RecipeController recipeController;

    /**
     * Object mapper for converting Java objects to JSON and JSON to Java objects.
     */
    @Autowired
    private ObjectMapper objectMapper;

    /**
     * JsonProvider is used to execute JSON path queries.
     */
    private JsonProvider jsonPath = Configuration.defaultConfiguration().jsonProvider();

    /**
     * Recipe used for testing.
     */
    private final Recipe recipe = Recipe.builder()
            .name("Test")
            .description("Test recipe")
            .ingredients(Arrays.asList(Ingredient.builder().
                    ingredient("something")
                    .quantitySpecifier(QuantitySpecifier.Unspecified).quantity(0.0).build()))
            .instructions(Arrays.asList(Instruction.builder().instruction("Do something").build()))
            .build();

    /**
     * Parameter used to specify whether to include hyper-links.
     */
    private final String includeHyperLinksParam = "include-hyper-links";

    /**
     * Setup test dependencies.
     */
    @BeforeEach
    public void setup() {
    }

    /**
     * Tear down test dependencies.
     * @throws Exception
     */
    @AfterEach
    public void tearDown() throws Exception {
    }

    @Test
    void testListRecipes() throws JSONException, URISyntaxException, MalformedURLException {
        var listRecipes = new URIBuilder("http://localhost")
            .setPort(port)
            .setPath("recipes/list")
            .addParameter(includeHyperLinksParam, "true")
            .build().toString();

        verifyListRecipesSize(0);

//        var listRecipesWithHyperLinksResponse =
//                restTemplate.exchange(RequestEntity.get(listRecipes).build(), String.class);
//        var recipeListJson = jsonPath.parse(listRecipesWithHyperLinksResponse.getBody());
//        var addRecipeUrl =
//                (String) ((JSONArray) JsonPath.read(recipeListJson, "$.actions[?(@.method == 'PUT')].href")).get(0);
//
//        verifyUrlsMatch(listRecipes, (String) ((JSONArray)JsonPath.read(recipeListJson,
//                "$.links[*].href")).get(0));
//        assertTrue(addRecipeUrl.contains(includeHyperLinksParam + "=false"));
//
//        var addRecipeResponse =
//                restTemplate.exchange(RequestEntity.put(addRecipeUrl).accept(MediaType.APPLICATION_JSON).body(recipe),
//                        String.class);
//
//        assertEquals(HttpStatus.OK, addRecipeResponse.getStatusCode());
//        verifyListRecipesSize(1);
//
//        var addRecipeJson = jsonPath.parse(addRecipeResponse.getBody());
//        var recipeId = (Integer) JsonPath.read(addRecipeJson, "$.recipeId");
//        assertNotNull(recipeId);
//
//        recipeController.deleteRecipe(recipeId.longValue());
//
//        verifyListRecipesSize(0);
    }

    @Test
    void testAddRecipeWithoutHyperlinks() throws JSONException, URISyntaxException, JsonProcessingException {
        var addRecipe = new URIBuilder("http://localhost")
                .setPort(port)
                .setPath("recipes/add")
                .build().toString();

        verifyListRecipesSize(0);

        var addRecipeResponse =
                restTemplate.exchange(RequestEntity
                                .put(addRecipe)
                                .accept(APPLICATION_JSON)
                                .body(objectMapper.writeValueAsString(recipe)),
                        String.class);

        assertEquals(HttpStatus.OK, addRecipeResponse.getStatusCode());

        var addRecipeJson = (LinkedHashMap<String, Object>) jsonPath.parse(addRecipeResponse.getBody());

        verifyRecipe(recipe, addRecipeJson);

        Integer recipeId = JsonPath.read(addRecipeJson, "$.recipeId");

        assertNotNull(recipeId);

        var getRecipeResponse =
                restTemplate.exchange(RequestEntity.get(String.format("http://localhost:%d/recipes/get/%d",
                                port, recipeId)).build(), String.class);

        assertEquals(HttpStatus.OK, getRecipeResponse.getStatusCode());
        verifyRecipe(recipe, (LinkedHashMap<String, Object>) jsonPath.parse(getRecipeResponse.getBody()));

        var deleteRecipeResponse =
                restTemplate.exchange(RequestEntity.delete(String.format("http://localhost:%d/recipes/delete/%d",
                                port, recipeId)).build(), String.class);

        assertEquals(HttpStatus.OK, deleteRecipeResponse.getStatusCode());

        verifyListRecipesSize(0);
    }

    @Test
    void testAddRecipeWithHyperlinks() throws URISyntaxException, JSONException, JsonProcessingException, MalformedURLException {
        var addRecipe = new URIBuilder("http://localhost")
                .setPort(port)
                .setPath("recipes/add")
                .addParameter(includeHyperLinksParam, "true")
                .build().toString();

        verifyListRecipesSize(0);

        var addRecipeResponse =
                restTemplate.exchange(RequestEntity
                        .put(addRecipe)
                        .accept(APPLICATION_JSON)
                        .body(objectMapper.writeValueAsString(recipe)), String.class);

        assertEquals(HttpStatus.OK, addRecipeResponse.getStatusCode());

        var responseJson = (LinkedHashMap<String, Object>) jsonPath.parse(addRecipeResponse.getBody());

        verifyRecipe(recipe, responseJson);

        var recipeId = (Integer) JsonPath.read(responseJson, "$.recipeId");

        assertNotNull(recipeId);

        var getRecipe = new URIBuilder("http://localhost")
                .setPort(port)
                .setPath(String.format("recipes/get/%d", recipeId))
                .build();

        var getRecipeResponse = restTemplate.exchange(RequestEntity.get(getRecipe.toString()).build(), String.class);

        assertEquals(HttpStatus.OK, getRecipeResponse.getStatusCode());
        verifyRecipe(recipe, (LinkedHashMap<String, Object>) jsonPath.parse(getRecipeResponse.getBody()));

        var self = (String) ((JSONArray) JsonPath.read(responseJson, "$.links[?(@.rel == 'self')].href")).get(0);
        assertTrue(self.contains(getRecipe.getPath()));

//        var updateRecipe = (String) ((JSONArray) JsonPath.read(responseJson,
//          "$.actions[?(@.method == 'PATCH')].href")).get(0);
//        var updatedRecipe = Recipe.builder()
//                .name("Another test")
//                .recipeId(recipeId.longValue())
//                .description("Another test recipe")
//                .ingredients(Arrays.asList(Ingredient.builder()
//                  .ingredientId(((Integer) JsonPath.read(responseJson, "$.ingredients[0].ingredientId")).longValue())
//                  .ingredient("something else")
//                  .quantitySpecifier(QuantitySpecifier.Unspecified)
//                  .quantity(0.0).build()))
//                .instructions(Arrays.asList(Instruction.builder()
//                  .instructionId(((Integer) JsonPath.read(responseJson,
//                      "$.instructions[0].instructionId")).longValue())
//                  .instruction("Do something else").build()))
//                .build();
//
//        var updateRecipeResponse =
//            restTemplate.exchange(RequestEntity
//                        .patch(updateRecipe)
//                        .accept(MediaType.APPLICATION_JSON)
//                        .body(objectMapper.writeValueAsString(updatedRecipe)),
//                    String.class);
//
//        assertEquals(HttpStatus.OK, updateRecipeResponse.getStatusCode());
//
//        var updatedRecipeJson = (LinkedHashMap<String, Object>)
//        jsonPath.parse(updateRecipeResponse.getBody());
//
//        verifyRecipe(updatedRecipe, updatedRecipeJson);
//
//        var deleteRecipe =
//                (String) ((JSONArray) JsonPath.read(responseJson, "$.actions[?(@.method == 'DELETE')].href"))
//                        .get(0);
//
//        var deleteRecipeResponse = restTemplate.exchange(RequestEntity.delete(deleteRecipe).build(), String.class);
//
//        assertEquals(HttpStatus.OK, deleteRecipeResponse.getStatusCode());

        var deleteRecipeResponse =
            restTemplate.exchange(RequestEntity.delete(String.format("http://localhost:%d/recipes/delete/%d",
                port, recipeId)).build(), String.class);

        assertEquals(HttpStatus.OK, deleteRecipeResponse.getStatusCode());

        verifyListRecipesSize(0);
    }

    @Test
    void testGetRecipeWithoutHyperlinks() throws JSONException, URISyntaxException, JsonProcessingException {
        verifyListRecipesSize(0);

        var addRecipeResponse =
                restTemplate.exchange(RequestEntity
                        .put(String.format("http://localhost:%d/recipes/add", port))
                        .accept(APPLICATION_JSON)
                        .body(objectMapper.writeValueAsString(recipe)), String.class);

        assertEquals(HttpStatus.OK, addRecipeResponse.getStatusCode());

        var addRecipeJson = (LinkedHashMap<String, Object>) jsonPath.parse(addRecipeResponse.getBody());
        var recipeId = (Integer) JsonPath.read(addRecipeJson, "$.recipeId");

        var getRecipe = String.format("http://localhost:%d/recipes/get/%d", port, recipeId);
        var getRecipeResponse = restTemplate.exchange(RequestEntity.get(getRecipe).build(), String.class);

        assertEquals(HttpStatus.OK, getRecipeResponse.getStatusCode());
        verifyRecipe(recipe, (LinkedHashMap<String, Object>) jsonPath.parse(getRecipeResponse.getBody()));

        var deleteRecipeResponse =
            restTemplate.exchange(RequestEntity.delete(String.format("http://localhost:%d/recipes/delete/%d",
                port, recipeId)).build(), String.class);

        assertEquals(HttpStatus.OK, deleteRecipeResponse.getStatusCode());

        verifyListRecipesSize(0);
    }

    @Test
    void testGetRecipeWithHyperlinks()
            throws JSONException, URISyntaxException, MalformedURLException, JsonProcessingException {
        verifyListRecipesSize(0);

        var addRecipeResponse =
                restTemplate.exchange(RequestEntity
                        .put(String.format("http://localhost:%d/recipes/add", port))
                        .accept(APPLICATION_JSON)
                        .body(objectMapper.writeValueAsString(recipe)), String.class);

        assertEquals(HttpStatus.OK, addRecipeResponse.getStatusCode());

        var addRecipeJson = (LinkedHashMap<String, Object>) jsonPath.parse(addRecipeResponse.getBody());
        var recipeId = (Integer) JsonPath.read(addRecipeJson, "$.recipeId");

        var getRecipe = new URIBuilder("http://localhost")
                .setPort(port)
                .setPath(String.format("recipes/get/%d", recipeId))
                .addParameter(includeHyperLinksParam, "true").build().toString();
        var getRecipeResponse = restTemplate.exchange(RequestEntity.get(getRecipe).build(), String.class);
        var getRecipeJson = (LinkedHashMap<String, Object>) jsonPath.parse(getRecipeResponse.getBody());

        assertEquals(HttpStatus.OK, getRecipeResponse.getStatusCode());
        verifyRecipe(recipe, getRecipeJson);

        assertEquals(String.format("/recipes/get/%d?include-hyper-links=false", recipeId),
            (String) ((JSONArray) JsonPath.read(getRecipeJson, "$.links[*].href")).get(0));

//        var updatedRecipe = Recipe.builder()
//          .recipeId(recipeId.longValue())
//          .name("Another test")
//          .description("Another test recipe")
//          .ingredients(Arrays.asList(Ingredient.builder()
//              .ingredientId(((Integer) JsonPath.read(getRecipeJson, "$.ingredients[0].ingredientId").longValue())
//              .ingredient("something else")
//              .quantitySpecifier(QuantitySpecifier.Unspecified)
//              .quantity(0.0)
//              .build())))
//          .instructions(Arrays.asList(Instruction.builder()
//              .instructionId(((Integer) JsonPath.read(getRecipeJson, "$.instructions[0].instructionId")).longValue())
//              .instruction("Do something else")
//              .build()))
//          .build();
//
//        var updateRecipe = (String) ((JSONArray) JsonPath.read(responseJson,
//          "$.actions[?(@.method == 'PATCH')].href")).get(0);
//        var updateRecipeResponse =
//                restTemplate.exchange(RequestEntity.patch(updateRecipe)
//                        .accept(MediaType.APPLICATION_JSON)
//                        .body(objectMapper.writeValueAsString(updatedRecipe)), String.class);
//
//        assertEquals(HttpStatus.OK, updateRecipeResponse.getStatusCode());
//
//        var updatedRecipeJson =
//                (LinkedHashMap<String, Object>) jsonPath.parse(updateRecipeResponse.getBody());
//
//        verifyRecipe(updatedRecipe, updatedRecipeJson);

        var deleteRecipeResponse =
            restTemplate.exchange(RequestEntity.delete(String.format("http://localhost:%d/recipes/delete/%d",
                port, recipeId)).build(), String.class);

        assertEquals(HttpStatus.OK, deleteRecipeResponse.getStatusCode());

        verifyListRecipesSize(0);
    }

    @Test
    void testUpdateRecipeWithoutHyperlinks() throws JsonProcessingException, JSONException, URISyntaxException {
        var updateRecipe = String.format("http://localhost:%d/recipes/update", port);

        verifyListRecipesSize(0);

        var addRecipeResponse =
                restTemplate.exchange(RequestEntity
                        .put(String.format("http://localhost:%d/recipes/add", port))
                        .accept(APPLICATION_JSON)
                        .body(objectMapper.writeValueAsString(recipe)), String.class);

        assertEquals(HttpStatus.OK, addRecipeResponse.getStatusCode());

        var addRecipeJson = (LinkedHashMap<String, Object>) jsonPath.parse(addRecipeResponse.getBody());
        var recipeId = (Integer) JsonPath.read(addRecipeJson, "$.recipeId");

        var updatedRecipe = Recipe.builder()
                .recipeId(recipeId.longValue())
                .name("Another test")
                .description("Another test recipe")
                .ingredients(Arrays.asList(Ingredient.builder()
                    .ingredientId(((Integer) JsonPath.read(addRecipeJson, "$.ingredients[0].ingredientId")).longValue())
                    .ingredient("something else")
                    .quantitySpecifier(QuantitySpecifier.Unspecified)
                    .quantity(0.0)
                    .build()))
                .instructions(Arrays.asList(Instruction.builder()
                        .instructionId(((Integer) JsonPath.read(addRecipeJson, "$.instructions[0].instructionId")).longValue())
                        .instruction("Do something else")
                        .build()))
                .build();

        var updateRecipeResponse = webClient
                .patch()
                .uri(updateRecipe)
                .accept(APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(updatedRecipe))
                .exchange()
                .expectStatus()
                .is2xxSuccessful()
                .expectHeader()
                .contentType(APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.recipeId")
                    .isEqualTo(recipeId)
                .jsonPath("$.name")
                    .isEqualTo(updatedRecipe.getName())
                .jsonPath("$.description")
                    .isEqualTo(updatedRecipe.getDescription())
                .jsonPath("$.ingredients[0].ingredient")
                    .isEqualTo(updatedRecipe.getIngredients().get(0).getIngredient())
                .jsonPath("$.ingredients[0].quantitySpecifier")
                    .isEqualTo(updatedRecipe.getIngredients().get(0).getQuantitySpecifier().toString())
                .jsonPath("$.ingredients[0].quantity")
                    .isEqualTo(updatedRecipe.getIngredients().get(0).getQuantity())
                .jsonPath("$.instructions[0].instruction")
                    .isEqualTo(updatedRecipe.getInstructions().get(0).getInstruction());

        var getRecipe = String.format("http://localhost:%d/recipes/get/%d", port, recipeId);
        var getRecipeResponse = restTemplate.exchange(RequestEntity.get(getRecipe).build(), String.class);

        assertEquals(HttpStatus.OK, getRecipeResponse.getStatusCode());
        verifyRecipe(updatedRecipe, (LinkedHashMap<String, Object>) jsonPath.parse(getRecipeResponse.getBody()));

        var deleteRecipeResponse =
                restTemplate.exchange(RequestEntity.delete(String.format("http://localhost:%d/recipes/delete/%d",
                    port, recipeId)).build(), String.class);

        assertEquals(HttpStatus.OK, deleteRecipeResponse.getStatusCode());

        verifyListRecipesSize(0);
    }

    @Test
    void testUpdateRecipeWithHyperlinks()
            throws JSONException, URISyntaxException, MalformedURLException, JsonProcessingException {
        var updateRecipe = new URIBuilder("http://localhost")
                .setPort(port)
                .setPath("/recipes/update")
                .addParameter(includeHyperLinksParam, "true")
                .build().toString();

        verifyListRecipesSize(0);

        var addRecipeResponse = restTemplate.exchange(RequestEntity
                .put(String.format("http://localhost:%d/recipes/add", port))
                .accept(APPLICATION_JSON)
                .body(objectMapper.writeValueAsString(recipe)), String.class);

        assertEquals(HttpStatus.OK, addRecipeResponse.getStatusCode());

        var addRecipeJson = (LinkedHashMap<String, Object>) jsonPath.parse(addRecipeResponse.getBody());
        var recipeId = (Integer) JsonPath.read(addRecipeJson, "$.recipeId");

        var updatedRecipe = Recipe.builder()
                .recipeId(recipeId.longValue())
                .name("Another test")
                .description("Another test recipe")
                .ingredients(Arrays.asList(Ingredient.builder()
                    .ingredientId(((Integer) JsonPath.read(addRecipeJson, "$.ingredients[0].ingredientId")).longValue())
                    .ingredient("something else")
                    .quantitySpecifier(QuantitySpecifier.Unspecified)
                    .quantity(0.0)
                    .build()))
                .instructions(Arrays.asList(Instruction.builder()
                    .instructionId(((Integer) JsonPath.read(addRecipeJson, "$.instructions[0].instructionId")).longValue())
                    .instruction("Do something else")
                    .build()))
                .build();

        var updateRecipeResponse = webClient
                .patch()
                .uri(updateRecipe)
                .accept(APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(updatedRecipe))
                .exchange()
                .expectStatus()
                .is2xxSuccessful()
                .expectHeader()
                .contentType(de.ingogriebsch.spring.hateoas.siren.MediaTypes.SIREN_JSON_VALUE)
                .expectBody()
                .jsonPath("$.recipeId")
                    .isEqualTo(recipeId)
                .jsonPath("$.name")
                    .isEqualTo(updatedRecipe.getName())
                .jsonPath("$.description")
                    .isEqualTo(updatedRecipe.getDescription())
                .jsonPath("$.ingredients[0].ingredient")
                    .isEqualTo(updatedRecipe.getIngredients().get(0).getIngredient())
                .jsonPath("$.ingredients[0].quantitySpecifier")
                    .isEqualTo(updatedRecipe.getIngredients().get(0).getQuantitySpecifier().toString())
                .jsonPath("$.ingredients[0].quantity")
                    .isEqualTo(updatedRecipe.getIngredients().get(0).getQuantity())
                .jsonPath("$.instructions[0].instruction")
                    .isEqualTo(updatedRecipe.getInstructions().get(0).getInstruction());

        var getRecipe = String.format("http://localhost:%d/recipes/get/%d", port, recipeId);
        var getRecipeResponse = restTemplate.exchange(RequestEntity.get(getRecipe).build(), String.class);

        assertEquals(HttpStatus.OK, getRecipeResponse.getStatusCode());
        verifyRecipe(updatedRecipe, (LinkedHashMap<String, Object>) jsonPath.parse(getRecipeResponse.getBody()));
//        verifyUrlsMatch(String.format("/recipes/get/%d", port, recipeId),
//                (String) ((JSONArray) JsonPath.read(responseJson, "$.links[*].href")).get(0));
//
//        var updatedRecipe2 = Recipe.builder()
//                .recipeId(recipeId.longValue())
//                .name("Yet another test")
//                .description("Yet another test recipe")
//                .ingredients(Arrays.asList(Ingredient.builder()
//                        .ingredientId(((Integer) JsonPath.read(addRecipeJson, "$.ingredients[0].ingredientId")).longValue())
//                        .ingredient("something else again")
//                        .quantitySpecifier(QuantitySpecifier.Unspecified)
//                        .quantity(0.0)
//                        .build()))
//                .instructions(Arrays.asList(Instruction.builder()
//                        .instructionId(((Integer) JsonPath.read(addRecipeJson, "$.instructions[0].instructionId")).longValue())
//                        .instruction("Do something else again")
//                        .build()))
//                .build();
//
//        var updateRecipe2 =
//                (String) ((JSONArray) JsonPath.read(responseJson, "$.actions[?(@.method == 'PATCH')].href")).get(0);
//        var updateRecipeResponse2 = restTemplate.exchange(RequestEntity
//                .patch(updateRecipe2)
//                .accept(APPLICATION_JSON)
//                .body(updatedRecipe2), String.class);
//
//        assertEquals(HttpStatus.OK, updateRecipeResponse2.getStatusCode());
//
//        var updatedRecipeJson2 = (LinkedHashMap<String, Object>) jsonPath.parse(updateRecipeResponse2.getBody());
//
//        verifyRecipe(updatedRecipe2, updatedRecipeJson2);
//
//        var deleteRecipe =
//            (String) ((JSONArray) JsonPath.read(responseJson, "$.actions[?(@.method == 'DELETE')].href")).get(0);
//        var deleteRecipeResponse = restTemplate.exchange(RequestEntity.delete(deleteRecipe).build(), String.class);

        var deleteRecipeResponse =
                restTemplate.exchange(RequestEntity.delete(String.format("http://localhost:%d/recipes/delete/%d",
                        port, recipeId)).build(), String.class);

        assertEquals(HttpStatus.OK, deleteRecipeResponse.getStatusCode());

        verifyListRecipesSize(0);
    }

    private void verifyListRecipesSize(final int expectedNumberOfElements) throws JSONException, URISyntaxException {
        var listRecipes = String.format("http://localhost:%d/recipes/list", port);

        // Check for list recipes without hyper-links
        var listRecipesWithoutHyperLinksResponse =
                restTemplate.exchange(RequestEntity.get(listRecipes).build(), String.class);

        assertEquals(HttpStatus.OK, listRecipesWithoutHyperLinksResponse.getStatusCode());
        assertEquals(expectedNumberOfElements,
                new org.json.JSONArray(listRecipesWithoutHyperLinksResponse.getBody()).length());

        // Check for list recipes with hyper-links
        var listRecipesWithHyperLinksResponse =
                restTemplate.exchange(RequestEntity
                        .get(new URIBuilder(listRecipes)
                                .addParameter(includeHyperLinksParam, "true")
                                .build().toString()).build(), String.class);

        assertEquals(HttpStatus.OK, listRecipesWithHyperLinksResponse.getStatusCode());
        var listRecipesWithHyperLinksJson = jsonPath.parse(listRecipesWithHyperLinksResponse.getBody());
        assertEquals(expectedNumberOfElements, (Integer) JsonPath.read(listRecipesWithHyperLinksJson,
                "$.page.totalElements"));
        if (expectedNumberOfElements > 0) {
            assertEquals(expectedNumberOfElements,
                ((JSONArray) JsonPath.read(listRecipesWithHyperLinksJson,  "$.entities")).size());
        }
    }

    private void verifyUrlsMatch(final String urlStr1, final String urlStr2) throws MalformedURLException {
        var url1 = new URL(urlStr1);
        var url2 = new URL(urlStr2);

        assertEquals(url1.getProtocol(), url2.getProtocol());
        assertEquals(url1.getHost(), url2.getHost());
        assertEquals(url1.getPort(), url2.getPort());
        assertEquals(url1.getPath(), url2.getPath());
    }

    private void verifyRecipe(final Recipe expectedRecipe, final LinkedHashMap<String, Object> actualRecipeJson) {

        assertEquals(expectedRecipe.getName(), (String) JsonPath.read(actualRecipeJson, "$.name"));
        assertEquals(expectedRecipe.getDescription(),
                (String) JsonPath.read(actualRecipeJson, "$.description"));

        var getIngredientsJson = (JSONArray) JsonPath.read(actualRecipeJson, "$.ingredients");

        assertEquals(expectedRecipe.getIngredients().size(), getIngredientsJson.size());
        for (int i = 0; i < expectedRecipe.getIngredients().size(); i++) {
            assertEquals(expectedRecipe.getIngredients().get(i).getQuantitySpecifier().toString(),
                    (String) JsonPath.read(getIngredientsJson, String.format("$[%d].quantitySpecifier", i)));
            assertEquals(expectedRecipe.getIngredients().get(i).getQuantity(),
                    (Double) JsonPath.read(getIngredientsJson, String.format("$[%d].quantity", i)));
            assertEquals(expectedRecipe.getIngredients().get(i).getIngredient(),
                    (String) JsonPath.read(getIngredientsJson, String.format("$[%d].ingredient", i)));
        }

        var getInstructionsJson = (JSONArray) JsonPath.read(actualRecipeJson, "$.instructions");

        assertEquals(expectedRecipe.getInstructions().size(), getInstructionsJson.size());
        for (int i = 0; i < expectedRecipe.getInstructions().size(); i++) {
            assertEquals(expectedRecipe.getInstructions().get(i).getInstruction(),
                (String) JsonPath.read(getInstructionsJson, String.format("$[%d].instruction", i)));
        }
    }
}
