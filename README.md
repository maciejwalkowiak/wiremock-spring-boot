# WireMock Spring Boot

WireMock Spring Boot drastically simplifies testing HTTP clients in **Spring Boot** & **Junit 5** based integration tests.

> **Warning**
> Project is in development stage, artifacts are not yet published to Maven Central.

## 🤩 Highlights

- fully declarative [WireMock](https://wiremock.org/) setup
- support for multiple `WireMockServer` instances - one per HTTP client as recommended in the WireMock documentation
- automatically sets Spring environment properties
- does not pollute Spring application context with extra beans

## 🤔 How to install

Temporarily, until the package is published to Maven Central, either check out this project locally or include the dependency using [jitpack.io](https://jitpack.io).

### Local build

1. Build project locally:

```
$ git clone https://github.com/maciejwalkowiak/wiremock-spring-boot
$ cd wiremock-spring-boot
$ ./mvnw install
```

2. Add the dependency:

```xml
<dependency>
    <groupId>com.maciejwalkowiak</groupId>
    <artifactId>wiremock-spring-boot</artifactId>
    <version>{version}</version>
    <scope>test</scope>
</dependency>
```

### With Jitpack

1. Add Jitpack repository:

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
```

2. Add the dependency to `wiremock-spring-boot`:

```xml
<dependency>
    <groupId>com.github.maciejwalkowiak.wiremock-spring-boot</groupId>
    <artifactId>wiremock-spring-boot</artifactId>
    <version>main-SNAPSHOT</version>
    <scope>test</scope>
</dependency>
```

> **Note**
> First build on Jitpack may take a minute or two so be patient.

## ✨ How to use

Use `@EnableWireMock` with `@ConfigureWireMock` with tests annotated that use `SpringExtension`, like `@SpringBootTest`:

```java
@SpringBootTest
@EnableWireMock({
        @ConfigureWireMock(name = "user-service", property = "user-client.url")
})
class TodoControllerTests {

    @WireMock("user-service")
    private WireMockServer wiremock;

    @Test
    void aTest() {
        wiremock.stubFor(...);
    }
}
```

- `@EnableWireMock` adds test context customizer and enables `WireMockSpringExtension` 
- `@ConfigureWireMock` creates a `WireMockServer` and passes the `WireMockServer#baseUrl` to a Spring environment property with a name given by `property`.
- `@WireMock` injects `WireMockServer` instance to a test

Note that `WireMockServer` instances are not added as beans to Spring application context to avoid polluting it with test-related infrastructure. Instead, instances are kept in a separate store associated with an application context.

Sounds good? Consider [❤️ Sponsoring](https://github.com/sponsors/maciejwalkowiak) the project! Thank you!

## 🙏 Credits

I looked into and learned few concepts from following projects and resources during the development of this project: 

- [Spring Cloud Contract WireMock](https://github.com/spring-cloud/spring-cloud-contract/blob/main/spring-cloud-contract-wiremock)
- [Spring Boot WireMock](https://github.com/skuzzle/spring-boot-wiremock)
- [Spring Boot Integration Tests With WireMock and JUnit 5](https://rieckpil.de/spring-boot-integration-tests-with-wiremock-and-junit-5/) by [Philip Riecks](https://twitter.com/rieckpil)
