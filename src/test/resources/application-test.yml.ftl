application:
  name: "spring-boot-scala-recipe-service"
  description: "An example Spring Boot app using Scala"

server:
  port: 8080

management:
  endpoints:
    enabled-by-default: true
    web:
      exposure:
        include: "*"
  endpoint:
    metrics:
      enabled: true
    health:
      enabled: true
      show-details: always

spring:
  r2dbc:
    driverClassName: "org.h2.Driver"
    url: "r2dbc:h2:mem:///testdb"
    jetty:
      max-wait: 10000
      max-active: 50
      test-on-borrow: true
  jdbc:
    template:
      max-rows: 500
  data:
    elasticsearch:
      client:
        reactive:
          endpoints: localhost:${elasticsearch.port}
          username: elastic
          password: ${elasticsearch.password}
      repositories:
        enables: true
    jdbc:
      repositories:
        enabled=false:
  elasticsearch:
    rest:
      uris: https://localhost:${elasticsearch.port}
logging:
  file:
    name: logs/spring-boot-scala-recipeservice-test.log
  level:
    org:
      hibernate:
        SQL: DEBUG
        type:
          descriptor:
            sql:
              BasicBinder: TRACE
      springframework:
        jdbc:
          core:
            JdbcTemplate: DEBUG
            StatementCreatorUtils: TRACE
