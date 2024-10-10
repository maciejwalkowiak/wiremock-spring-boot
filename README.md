# WireMock Spring Boot

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.wiremock.integrations/wiremock-spring-boot/badge.svg)](https://search.maven.org/artifact/org.wiremock.integrations/wiremock-spring-boot)

**WireMock Spring Boot** library drastically simplifies [WireMock](https://wiremock.org) configuration in a **Spring Boot** and **JUnit 5** application.

## Highlights

* Fully declarative [WireMock](https://wiremock.org/) setup
* Support for **multiple** `WireMockServer` **instances** - one per HTTP client as recommended in the WireMock documentation
* Automatically sets Spring environment properties
* Does not pollute Spring application context with extra beans

It is originally forked from [WireMock Spring Boot](https://github.com/maciejwalkowiak/wiremock-spring-boot).

## How to install

Add the dependency to `wiremock-spring-boot`:

```xml
<dependency>
    <groupId>org.wiremock.integrations</groupId>
    <artifactId>wiremock-spring-boot</artifactId>
    <version>X</version>
    <scope>test</scope>
</dependency>
```

```groovy
testImplementation "org.wiremock.integrations:wiremock-spring-boot:X"
```

## Example

Enable it with the `@EnableWireMock` annotation, like:

```java
@SpringBootTest
@EnableWireMock
class DefaultPropertiesTest {

  @Value("${wiremock.server.baseUrl}")
  private String wiremockUrl;

  @Value("${wiremock.server.port}")
  private String wiremockPort;

  @BeforeEach
  public void before() {
    WireMock.stubFor(get("/the_default_prop_mock").willReturn(aResponse().withStatus(202)));
  }

  @Test
  void test() {
    RestAssured.baseURI = this.wiremockUrl;
    RestAssured.when().get("/the_default_prop_mock").then().statusCode(202);
  }
}
```

There are more running examples in [the repo](/wiremock-spring-boot-example/src/test/java/app).

## Annotations

- `@EnableWireMock` adds test context customizer and enables `WireMockSpringExtension`.
- `@ConfigureWireMock` creates a `WireMockServer`.
- `@InjectWireMock` injects `WireMockServer` instance to a test.

## Properties

By default these will be provided:

- `wiremock.server.baseUrl` - Base URL of WireMock server.
- `wiremock.server.port` - HTTP port of WireMock server.

These can be changed with:

```java
@EnableWireMock(
    @ConfigureWireMock(
        baseUrlProperties = { "customUrl", "sameCustomUrl" },
        portProperties = "customPort"))
class CustomPropertiesTest {

 @Value("${customUrl}")
 private String customUrl;

 @Value("${sameCustomUrl}")
 private String sameCustomUrl;

 @Value("${customPort}")
 private String customPort;
```

## Customizing mappings directory

By default, each `WireMockServer` is configured to load WireMock root from:

1. Classpath *if specified*
   1. `{name-of-mock}/{server-name}`
   2. `{name-of-mock}`
2. Directory
   1. `{CWD}/wiremock/{server-name}`
   2. `{CWD}/stubs/{server-name}`
   3. `{CWD}/mappings/{server-name}`
   4. `{CWD}/wiremock`
   5. `{CWD}/stubs`
   6. `{CWD}/mappings`

It can be changed:

```java
@EnableWireMock({
  @ConfigureWireMock(
      name = "fs-client",
      filesUnderClasspath = "some/classpath/resource",
      filesUnderDirectory = "or/a/directory")
})
```

## Registering WireMock extensions

WireMock extensions can be registered independently with each `@ConfigureWireMock`:

```java
@EnableWireMock({
    @ConfigureWireMock(extensions = { ... })
})
```

## Credits

* [Maciej Walkowiak](https://github.com/maciejwalkowiak) - This was originally his project and later moved to WireMock organization
* [Spring Cloud Contract WireMock](https://github.com/spring-cloud/spring-cloud-contract/blob/main/spring-cloud-contract-wiremock)
* [Spring Boot WireMock](https://github.com/skuzzle/spring-boot-wiremock)
* [Spring Boot Integration Tests With WireMock and JUnit 5](https://rieckpil.de/spring-boot-integration-tests-with-wiremock-and-junit-5/) by [Philip Riecks](https://twitter.com/rieckpil)
