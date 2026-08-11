# Technology Stack
- Java 17; Spring Boot parent 3.3.5; Maven reactor with three modules.
- MyBatis Spring Boot starter 3.0.3; MySQL Connector/J; Druid in interface module.
- Spring AMQP/RabbitMQ and Redisson 3.34.0; Resilience4j Spring Boot 3 integration; Actuator/Micrometer.
- Testcontainers 1.20.3 dependencies are test-scoped in `intern-base-service`: core, JUnit Jupiter, MySQL, RabbitMQ.
- `integration-test` Maven profile enables Failsafe `**/*IT.java` and passes `integration.requireDocker=true`; ordinary `mvn test` intentionally excludes ITs.
- Production SQL/resources are under `intern-base-intf/src/main/resources`; MyBatis mapper XML is loaded by application `@MapperScan` and by integration tests directly.
