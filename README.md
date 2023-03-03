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
CREATE DATABASE recipe WITH ENCODING 'UTF8' LC_COLLATE='English_United States' LC_CTYPE='English_United States' OWNER rolename;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO userName;
GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA public TO userName;
GRANT USAGE ON ALL SEQUENCES IN SCHEMA public TO userName;

## Run Spring Boot application
```
mvn spring-boot:run
```
