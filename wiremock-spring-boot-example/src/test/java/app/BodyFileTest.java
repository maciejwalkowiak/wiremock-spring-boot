package app;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.wiremock.spring.ConfigureWireMock;
import org.wiremock.spring.EnableWireMock;
import org.wiremock.spring.InjectWireMock;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;

import io.restassured.RestAssured;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestPropertySource(properties = {
		"application.client.first-server.token.uri=${wiremock.server.first-server.baseUrl}/auth/realms/test-realm/protocol/openid-connect/token",
		"application.client.second-server.uri=${wiremock.server.second-server.baseUrl}" })
@EnableWireMock({
		@ConfigureWireMock(name = "first-server", baseUrlProperties = { "wiremock.server.first-server.baseUrl" }),
		@ConfigureWireMock(name = "second-server", baseUrlProperties = {
				"wiremock.server.second-server.baseUrl" }, filesUnderDirectory = "src/test/body-file") })
public class BodyFileTest {

	@InjectWireMock("second-server")
	private WireMockServer secondServer;

	@Test
	public void testCorrectResponseFromServer() {
		this.secondServer.stubFor(WireMock.post("/consent")
				.willReturn(WireMock.ok().withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
						.withBodyFile("second-server/200_response.json")));

		RestAssured.baseURI = "http://localhost:" + this.secondServer.port();
		final String actual = RestAssured.when().post("/consent").then().log().all().statusCode(200).extract()
				.asPrettyString();

		assertThat(actual).isEqualToIgnoringWhitespace("""
				{
				    "response": "consent response"
				}
								""");
	}

}
