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
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.text.StringSubstitutor;
import org.apache.http.client.utils.URIBuilder;
import org.grego.recipeservice.model.Ingredient;
import org.grego.recipeservice.model.Instruction;
import org.grego.recipeservice.model.QuantitySpecifier;
import org.grego.recipeservice.model.Recipe;
import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.shaded.org.apache.commons.io.IOUtils;
import org.testcontainers.utility.DockerImageName;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javafx.util.Pair;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@ExtendWith(SpringExtension.class)
@SpringBootTest(properties = "spring.main.web-application-type=reactive",
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(RecipeServiceAppTest.PROFILE)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag("IntegrationTests")
class RecipeServiceAppTest {
    /**
     * The profile for testing.
     */
    public static final String PROFILE = "test";

    /**
     * The configuration .yml file.
     */
    private static final String CONFIGURATION_YML_FILE = String.format("application-%s.yml", PROFILE);

    /**
     * The password for the keystore.
     */
    private static final String KEYSTORE_PASSWORD = RandomStringUtils.random(10, true, true);

    /**
     * Elasticsearch default username, when secured.
     */
    private static final String ELASTICSEARCH_USERNAME = "elastic";

    /**
     * Elasticsearch password.
     */
    private static final String ELASTICSEARCH_PASSWORD = RandomStringUtils.random(10, true, true);

    /**
     * Elasticsearch port.
     */
    private static final int ELASTICSEARCH_PORT = 9200;

    /**
     * Parameter used to specify whether to include hyper-links.
     */
    private static final String INCLUDE_HYPER_LINKS_PARAM = "include-hyper-links";

    /**
     * Parameter use to specify the search string.
     */
    private static final String SEARCH_STRING_PARAM = "search-string";

    /**
     * The version of Elasticsearch.
     */
    private static final String ELASTICSEARCH_VERSION = "8.6.2";

    /**
     * The docker image for Elasticsearch.
     */
    private static final String ELASTICSEARCH_IMAGE =
            String.format("docker.elastic.co/elasticsearch/elasticsearch:%s-%s", ELASTICSEARCH_VERSION,
                    SystemUtils.OS_ARCH.equals("aarch64") ? "arm64" : "amd64");

    /**
     * The port that the RecipeServiceApp is using during tests.
     */
    @LocalServerPort
    private int recipeServicePort = 0;

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
     * Object mapper for converting Java objects to JSON and JSON to Java objects.
     */
    @Autowired
    private ObjectMapper objectMapper;

    /**
     * JsonProvider is used to execute JSON path queries.
     */
    private JsonProvider jsonPath = Configuration.defaultConfiguration().jsonProvider();

    /**
     * The name of the recipe.
     */
    private static final String RECIPE_NAME = "Test";

    /**
     * Recipe used for testing.
     */
    private final Recipe recipe = Recipe.builder()
        .name(RECIPE_NAME)
        .description("Test recipe")
        .ingredients(Arrays.asList(Ingredient.builder().
            ingredient("something")
            .quantitySpecifier(QuantitySpecifier.Unspecified).quantity(0.0).build()))
        .instructions(Arrays.asList(Instruction.builder().instruction("Do something").build()))
        .build();

    /**
     * Container to run test instance of Elasticsearch.
     */
    private ElasticsearchContainer elasticsearchContainer;

    RecipeServiceAppTest() throws IOException, URISyntaxException, KeyStoreException,
            CertificateException, NoSuchAlgorithmException {
        elasticsearchContainer = new ElasticsearchContainer(new DockerImageName(ELASTICSEARCH_IMAGE))
            .withEnv("cluster.name", "integration-test-cluster")
            .withPassword(ELASTICSEARCH_PASSWORD);

        elasticsearchContainer.start();
        var configurationMapping = Map.of(
            "elasticsearch.port", elasticsearchContainer.getMappedPort(ELASTICSEARCH_PORT),
            "elasticsearch.password", ELASTICSEARCH_PASSWORD
        );

        writeConfigurationYmlFile(configurationMapping);

        List<Pair<String, Certificate>> certificates = new ArrayList<Pair<String, Certificate>>();

        addElasticsearchCertificate(certificates, elasticsearchContainer);
        createKeyStore(certificates);
    }

    @Test
    public void testListRecipes() throws JSONException, URISyntaxException, MalformedURLException {
        Integer recipeId = -1;
        try {
            var listRecipes = new URIBuilder("http://localhost")
                .setPort(recipeServicePort)
                .setPath("recipes/list")
                .addParameter(INCLUDE_HYPER_LINKS_PARAM, "true")
                .build().toString();
            var addRecipe = new URIBuilder("http://localhost")
                .setPort(recipeServicePort)
                .setPath("recipes/add")
                .build().toString();

            verifyListRecipesSize(0);

            var listRecipesWithHyperLinksResponse =
                restTemplate.exchange(RequestEntity.get(listRecipes).build(), String.class);
            var addRecipeResponse =
                restTemplate.exchange(RequestEntity
                    .put(addRecipe)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(recipe), String.class);

            assertEquals(HttpStatus.OK, addRecipeResponse.getStatusCode());
            verifyListRecipesSize(1);

            var addRecipeJson = jsonPath.parse(addRecipeResponse.getBody());
            recipeId = (Integer) JsonPath.read(addRecipeJson, "$.recipeId");
            assertNotNull(recipeId);
        } finally {
            if (recipeId != -1) {
                restTemplate.exchange(RequestEntity.delete(String.format("http://localhost:%d/recipes/delete/%d",
                    recipeServicePort, recipeId)).build(), String.class);

                verifyListRecipesSize(0);
            }
        }
    }

    @Test
    public void testAddRecipeWithoutHyperlinks() throws JSONException, URISyntaxException, JsonProcessingException {
        Integer recipeId = -1;

        try {
            var addRecipe = new URIBuilder("http://localhost")
                .setPort(recipeServicePort)
                .setPath("recipes/add")
                .build().toString();
            var searchForRecipe = new URIBuilder("http://localhost")
                .setPort(recipeServicePort)
                .setPath("recipes/search")
                .addParameter(SEARCH_STRING_PARAM, RECIPE_NAME)
                .build().toString();

            verifyListRecipesSize(0);

            var addRecipeResponse =
                restTemplate.exchange(RequestEntity
                    .put(addRecipe)
                    .accept(APPLICATION_JSON)
                    .body(objectMapper.writeValueAsString(recipe)), String.class);

            assertEquals(HttpStatus.OK, addRecipeResponse.getStatusCode());

            var addRecipeJson = (LinkedHashMap<String, Object>) jsonPath.parse(addRecipeResponse.getBody());

            verifyRecipe(recipe, addRecipeJson);

            recipeId = JsonPath.read(addRecipeJson, "$.recipeId");

            assertNotNull(recipeId);

            var getRecipeResponse =
                restTemplate.exchange(RequestEntity.get(String.format("http://localhost:%d/recipes/get/%d",
                    recipeServicePort, recipeId)).build(), String.class);

            assertEquals(HttpStatus.OK, getRecipeResponse.getStatusCode());
            verifyRecipe(recipe, (LinkedHashMap<String, Object>) jsonPath.parse(getRecipeResponse.getBody()));

            var searchForRecipeResponse =
                restTemplate.exchange(RequestEntity.get(searchForRecipe).build(), String.class);

            assertEquals(HttpStatus.OK, searchForRecipeResponse.getStatusCode());
        } finally {
            if (recipeId != -1) {
                restTemplate.exchange(RequestEntity.delete(String.format("http://localhost:%d/recipes/delete/%d",
                    recipeServicePort, recipeId)).build(), String.class);

                verifyListRecipesSize(0);
            }
        }
    }

    @Test
    public void testAddRecipeWithHyperlinks()
            throws URISyntaxException, JSONException, JsonProcessingException, MalformedURLException {
        Integer recipeId = -1;

        try {
            var addRecipe = new URIBuilder("http://localhost")
                .setPort(recipeServicePort)
                .setPath("recipes/add")
                .addParameter(INCLUDE_HYPER_LINKS_PARAM, "true")
                .build().toString();
            var searchForRecipe = new URIBuilder("http://localhost")
                .setPort(recipeServicePort)
                .setPath("recipes/search")
                .addParameter(SEARCH_STRING_PARAM, RECIPE_NAME)
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

            recipeId = (Integer) JsonPath.read(responseJson, "$.recipeId");

            assertNotNull(recipeId);

            var getRecipe = new URIBuilder("http://localhost")
                .setPort(recipeServicePort)
                .setPath(String.format("recipes/get/%d", recipeId))
                .build();

            var getRecipeResponse = restTemplate.exchange(RequestEntity.get(getRecipe.toString()).build(), String.class);

            assertEquals(HttpStatus.OK, getRecipeResponse.getStatusCode());
            verifyRecipe(recipe, (LinkedHashMap<String, Object>) jsonPath.parse(getRecipeResponse.getBody()));

            var self = (String) ((JSONArray) JsonPath.read(responseJson, "$.links[?(@.rel == 'self')].href")).get(0);
            assertTrue(self.contains(getRecipe.getPath()));

            var searchForRecipeResponse =
                restTemplate.exchange(RequestEntity.get(searchForRecipe).build(), String.class);

            assertEquals(HttpStatus.OK, searchForRecipeResponse.getStatusCode());
        } finally {
            restTemplate.exchange(RequestEntity.delete(String.format("http://localhost:%d/recipes/delete/%d",
                recipeServicePort, recipeId)).build(), String.class);

            verifyListRecipesSize(0);
        }
    }

    @Test
    public void testGetRecipeWithoutHyperlinks() throws JSONException, URISyntaxException, JsonProcessingException {
        Integer recipeId = -1;

        try {
            verifyListRecipesSize(0);

            var addRecipeResponse =
                restTemplate.exchange(RequestEntity
                    .put(String.format("http://localhost:%d/recipes/add", recipeServicePort))
                    .accept(APPLICATION_JSON)
                    .body(objectMapper.writeValueAsString(recipe)), String.class);

            assertEquals(HttpStatus.OK, addRecipeResponse.getStatusCode());

            var addRecipeJson = (LinkedHashMap<String, Object>) jsonPath.parse(addRecipeResponse.getBody());
            recipeId = (Integer) JsonPath.read(addRecipeJson, "$.recipeId");

            var getRecipe = String.format("http://localhost:%d/recipes/get/%d", recipeServicePort, recipeId);
            var getRecipeResponse = restTemplate.exchange(RequestEntity.get(getRecipe).build(), String.class);

            assertEquals(HttpStatus.OK, getRecipeResponse.getStatusCode());
            verifyRecipe(recipe, (LinkedHashMap<String, Object>) jsonPath.parse(getRecipeResponse.getBody()));
        } finally {
            if (recipeId != -1) {
                restTemplate.exchange(RequestEntity.delete(String.format("http://localhost:%d/recipes/delete/%d",
                    recipeServicePort, recipeId)).build(), String.class);

                verifyListRecipesSize(0);
            }
        }
    }

    @Test
    public void testGetRecipeWithHyperlinks()
            throws JSONException, URISyntaxException, MalformedURLException, JsonProcessingException {
        Integer recipeId = -1;

        try {
            verifyListRecipesSize(0);

            var addRecipeResponse =
                restTemplate.exchange(RequestEntity
                    .put(String.format("http://localhost:%d/recipes/add", recipeServicePort))
                    .accept(APPLICATION_JSON)
                    .body(objectMapper.writeValueAsString(recipe)), String.class);

            assertEquals(HttpStatus.OK, addRecipeResponse.getStatusCode());

            var addRecipeJson = (LinkedHashMap<String, Object>) jsonPath.parse(addRecipeResponse.getBody());
            recipeId = (Integer) JsonPath.read(addRecipeJson, "$.recipeId");

            var getRecipe = new URIBuilder("http://localhost")
                .setPort(recipeServicePort)
                .setPath(String.format("recipes/get/%d", recipeId))
                .addParameter(INCLUDE_HYPER_LINKS_PARAM, "true").build().toString();
            var getRecipeResponse = restTemplate.exchange(RequestEntity.get(getRecipe).build(), String.class);
            var getRecipeJson = (LinkedHashMap<String, Object>) jsonPath.parse(getRecipeResponse.getBody());

            assertEquals(HttpStatus.OK, getRecipeResponse.getStatusCode());
            verifyRecipe(recipe, getRecipeJson);

            assertEquals(String.format("/recipes/get/%d?include-hyper-links=false", recipeId),
                (String) ((JSONArray) JsonPath.read(getRecipeJson, "$.links[*].href")).get(0));

        } finally {
            if (recipeId != -1) {
                restTemplate.exchange(RequestEntity.delete(String.format("http://localhost:%d/recipes/delete/%d",
                    recipeServicePort, recipeId)).build(), String.class);

                verifyListRecipesSize(0);
            }
        }
    }

    @Test
    public void testUpdateRecipeWithoutHyperlinks() throws JsonProcessingException, JSONException, URISyntaxException {
        Integer recipeId = -1;

        try {
            var updateRecipe = String.format("http://localhost:%d/recipes/update", recipeServicePort);

            verifyListRecipesSize(0);

            var addRecipeResponse =
                restTemplate.exchange(RequestEntity
                    .put(String.format("http://localhost:%d/recipes/add", recipeServicePort))
                    .accept(APPLICATION_JSON)
                    .body(objectMapper.writeValueAsString(recipe)), String.class);

            assertEquals(HttpStatus.OK, addRecipeResponse.getStatusCode());

            var addRecipeJson = (LinkedHashMap<String, Object>) jsonPath.parse(addRecipeResponse.getBody());
            recipeId = (Integer) JsonPath.read(addRecipeJson, "$.recipeId");

            var updatedRecipe = Recipe.builder()
                .recipeId(recipeId.longValue())
                .name("Another test")
                .description("Another test recipe")
                .ingredients(Arrays.asList(Ingredient.builder()
                    .ingredientId(((Integer) JsonPath.read(addRecipeJson,
                "$.ingredients[0].ingredientId")).longValue())
                    .ingredient("something else")
                    .quantitySpecifier(QuantitySpecifier.Unspecified)
                    .quantity(0.0)
                    .build()))
                .instructions(Arrays.asList(Instruction.builder()
                    .instructionId(((Integer) JsonPath.read(addRecipeJson,
                "$.instructions[0].instructionId")).longValue())
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

            var getRecipe = String.format("http://localhost:%d/recipes/get/%d", recipeServicePort, recipeId);
            var getRecipeResponse = restTemplate.exchange(RequestEntity.get(getRecipe).build(), String.class);

            assertEquals(HttpStatus.OK, getRecipeResponse.getStatusCode());
            verifyRecipe(updatedRecipe, (LinkedHashMap<String, Object>) jsonPath.parse(getRecipeResponse.getBody()));
        } finally {
            if (recipeId != -1) {
                restTemplate.exchange(RequestEntity.delete(String.format("http://localhost:%d/recipes/delete/%d",
                    recipeServicePort, recipeId)).build(), String.class);

                verifyListRecipesSize(0);
            }
        }
    }

    @Test
    public void testUpdateRecipeWithHyperlinks()
            throws JSONException, URISyntaxException, MalformedURLException, JsonProcessingException {
        Integer recipeId = -1;

        try {
            var updateRecipe = new URIBuilder("http://localhost")
                .setPort(recipeServicePort)
                .setPath("/recipes/update")
                .addParameter(INCLUDE_HYPER_LINKS_PARAM, "true")
                .build().toString();

            verifyListRecipesSize(0);

            var addRecipeResponse = restTemplate.exchange(RequestEntity
                .put(String.format("http://localhost:%d/recipes/add", recipeServicePort))
                .accept(APPLICATION_JSON)
                .body(objectMapper.writeValueAsString(recipe)), String.class);

            assertEquals(HttpStatus.OK, addRecipeResponse.getStatusCode());

            var addRecipeJson = (LinkedHashMap<String, Object>) jsonPath.parse(addRecipeResponse.getBody());
            recipeId = (Integer) JsonPath.read(addRecipeJson, "$.recipeId");

            var updatedRecipe = Recipe.builder()
                .recipeId(recipeId.longValue())
                .name("Another test")
                .description("Another test recipe")
                .ingredients(Arrays.asList(Ingredient.builder()
                    .ingredientId(((Integer) JsonPath.read(addRecipeJson,
                "$.ingredients[0].ingredientId")).longValue())
                    .ingredient("something else")
                    .quantitySpecifier(QuantitySpecifier.Unspecified)
                    .quantity(0.0)
                    .build()))
                .instructions(Arrays.asList(Instruction.builder()
                    .instructionId(((Integer) JsonPath.read(addRecipeJson,
                        "$.instructions[0].instructionId")).longValue())
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

            var getRecipe = String.format("http://localhost:%d/recipes/get/%d", recipeServicePort, recipeId);
            var getRecipeResponse = restTemplate.exchange(RequestEntity.get(getRecipe).build(), String.class);

            assertEquals(HttpStatus.OK, getRecipeResponse.getStatusCode());
            verifyRecipe(updatedRecipe, (LinkedHashMap<String, Object>) jsonPath.parse(getRecipeResponse.getBody()));
        } finally {
            if (recipeId != -1) {
                restTemplate.exchange(RequestEntity.delete(String.format("http://localhost:%d/recipes/delete/%d",
                    recipeServicePort, recipeId)).build(), String.class);

                verifyListRecipesSize(0);
            }
        }
    }

    private void verifyListRecipesSize(final int expectedNumberOfElements) throws JSONException, URISyntaxException {
        var listRecipes = String.format("http://localhost:%d/recipes/list", recipeServicePort);

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
                    .addParameter(INCLUDE_HYPER_LINKS_PARAM, "true")
                    .build().toString()).build(), String.class);

        assertEquals(HttpStatus.OK, listRecipesWithHyperLinksResponse.getStatusCode());
        var listRecipesWithHyperLinksJson = jsonPath.parse(listRecipesWithHyperLinksResponse.getBody());
        assertEquals(expectedNumberOfElements, (Integer) JsonPath.read(listRecipesWithHyperLinksJson,
    "$.page.totalElements"));
        if (expectedNumberOfElements > 0) {
            assertEquals(expectedNumberOfElements,
                ((JSONArray) JsonPath.read(listRecipesWithHyperLinksJson,  "$.content")).size());
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

    @NotNull
    private String getResourcePath() throws URISyntaxException {
        URI classResourceUri = this.getClass().getResource("").toURI();
        for (int i = 0; i < this.getClass().toString().split("\\.").length - 1; i++) {
            classResourceUri = Paths.get(classResourceUri).getParent().toUri();
        }

        return classResourceUri.getPath();
    }

    private void writeConfigurationYmlFile(final Map<String, ?> configurationMapping)
            throws IOException, URISyntaxException {

        try (InputStream inputStream = this.getClass().getClassLoader()
                .getResourceAsStream(CONFIGURATION_YML_FILE + ".ftl")) {
            String templateString = IOUtils.toString(inputStream);
            StringSubstitutor substitutor = new StringSubstitutor(configurationMapping);
            String resolvedString = substitutor.replace(templateString);
            Files.write(Paths.get(getResourcePath(), CONFIGURATION_YML_FILE), resolvedString.getBytes());
        }
    }

    private void addElasticsearchCertificate(final List<Pair<String, Certificate>> certificates,
                                             final ElasticsearchContainer container)
            throws CertificateException, IOException {
        var caCerts = container.caCertAsBytes();
        if (caCerts.isPresent()) {

            InputStream fis = new ByteArrayInputStream(caCerts.get());
            BufferedInputStream bis = new BufferedInputStream(fis);
            CertificateFactory cf = CertificateFactory.getInstance("X.509");

            while (bis.available() > 0) {
                Certificate cert = cf.generateCertificate(bis);
                String alias = "Elasticsearch" + bis.available();
                certificates.add(new Pair(alias, cert));
            }
        }
    }

    private void createKeyStore(final List<Pair<String, Certificate>> certificates)
            throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException, URISyntaxException {
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null); //Make an empty store
        var keyStoreFile = Paths.get(getResourcePath().toString(), "keystore.jks").toFile();

        for (var certificate : certificates) {
            keyStore.setCertificateEntry(certificate.getKey(), certificate.getValue());
        }

        try (FileOutputStream stream = new FileOutputStream(keyStoreFile)) {
            keyStore.store(stream, KEYSTORE_PASSWORD.toCharArray());
        }

        System.setProperty("javax.net.ssl.trustStore", keyStoreFile.getAbsolutePath());
        System.setProperty("javax.net.ssl.trustStorePassword", KEYSTORE_PASSWORD);
    }
}
