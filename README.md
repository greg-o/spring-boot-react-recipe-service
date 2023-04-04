# Spring Boot R2DBC + PostgreSQL example: CRUD Application

Spring Boot R2DBC + PostgreSQL example - CRUD application that uses Spring Data Reactive (R2DBC) to interact with PostgreSQL database and Spring WebFlux for Reactive Rest API. You'll know:
- How to configure Spring Data Reactive, R2DBC to work with PostgreSQL Database
- How to define Data Models and Repository interfaces
- Way to create Spring Rest Controller to process HTTP requests
- Way to use Spring Data R2DBC to interact with PostgreSQL Database

What's In the Box?

* Java (18.0.2)
* Spring Boot (3.0.2)
* Maven build (12.3.1)
* Undertow web container

## Building
Ensure you have Java 8, Docker, and Make installed.

Set up Postgres Database
CREATE USER grego WITH PASSWORD 'springboot';
GRANT admins TO joe;
CREATE DATABASE recipe WITH ENCODING 'UTF8' LC_COLLATE='English_United States' LC_CTYPE='English_United States' OWNER grego;
GRANT ALL PRIVILEGES ON recipe TO grego;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO grego;
GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA public TO grego;
GRANT USAGE ON ALL SEQUENCES IN SCHEMA public TO grego;

## Run Spring Boot application
```
mvn spring-boot:run
```

Get list of recipes:
```bash
curl "http://localhost:8080/recipes/list?start=0&count=20"
```

Add recipe:
```bash
curl -X PUT "http://localhost:8080/recipes/add" -d '{"name":"Tea","description":"cup of tea","ingredients":[{"quantitySpecifier":"Cup","quantity":1.0,"ingredient":"water"}, {"quantitySpecifier":"Teaspoon","quantity":1.0,"ingredient":"tea"}],"instructions":[{"instruction":"add tea to hot water"}]}' -H "Content-Type: application/json"
```

Get recipe:
```bash
curl "http://localhost:8080/recipes/get/1"
```

Delete recipe:

```bash
curl -X DELETE "http://localhost:8080/recipes/delete/1"
```

Update recipe:

```bash
curl -X PATCH http://localhost:8080/recipes/update -d '{"recipeId":1,"name":"chili","description":"homemade","ingredients":[{"recipeId":1,"ingredientNumber":1,"quantitySpecifier":"Cup","quantity":1.0,"ingredient":"beer"}],"instructions":[{"recipeId":1,"instructionNumber":1,"instruction":"add beer"}]}' -H "Content-Type: application/json"
```

Search for recipes
```bash
curl "http://localhost:8080/recipes/search?search-string=tea"
```
Elasticsearch:  http://localhost:9200/