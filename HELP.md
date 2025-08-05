# Getting Started

### File Structure
src/main/java/com/proximashare/
├── app/
│   ├── config/
|   │   └── CorsConfig.java
|   │   └── SecurityConfig.java
│   ├── ProximaShareApplication.java
│   └── GlobalExceptionHandler.java
├── entity/
│   └── FileMetadata.java
├── repository/
│   └── FileMetadataRepository.java
├── service/
│   ├── FileService.java
│   └── FileCleanupScheduler.java
├── controller/
│   └── FileController.java
├── dto/
│   └── ErrorDetails.java
src/main/resources/
├── application.properties
└── build.gradle.kts


### Reference Documentation
For further reference, please consider the following sections:

* [Official Gradle documentation](https://docs.gradle.org)
* [Spring Boot Gradle Plugin Reference Guide](https://docs.spring.io/spring-boot/3.3.13/gradle-plugin)
* [Create an OCI image](https://docs.spring.io/spring-boot/3.3.13/gradle-plugin/packaging-oci-image.html)
* [Spring Web](https://docs.spring.io/spring-boot/3.3.13/reference/web/servlet.html)
* [Spring Boot DevTools](https://docs.spring.io/spring-boot/3.3.13/reference/using/devtools.html)
* [Spring Data JPA](https://docs.spring.io/spring-boot/3.3.13/reference/data/sql.html#data.sql.jpa-and-spring-data)
* [Spring Boot Actuator](https://docs.spring.io/spring-boot/3.3.13/reference/actuator/index.html)
* [Validation](https://docs.spring.io/spring-boot/3.3.13/reference/io/validation.html)

### Guides
The following guides illustrate how to use some features concretely:

* [Building a RESTful Web Service](https://spring.io/guides/gs/rest-service/)
* [Serving Web Content with Spring MVC](https://spring.io/guides/gs/serving-web-content/)
* [Building REST services with Spring](https://spring.io/guides/tutorials/rest/)
* [Accessing Data with JPA](https://spring.io/guides/gs/accessing-data-jpa/)
* [Building a RESTful Web Service with Spring Boot Actuator](https://spring.io/guides/gs/actuator-service/)
* [Validation](https://spring.io/guides/gs/validating-form-input/)

### Additional Links
These additional references should also help you:

* [Gradle Build Scans – insights for your project's build](https://scans.gradle.com#gradle)

