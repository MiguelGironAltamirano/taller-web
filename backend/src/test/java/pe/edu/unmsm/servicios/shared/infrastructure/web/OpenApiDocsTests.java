package pe.edu.unmsm.servicios.shared.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;

import com.jayway.jsonpath.JsonPath;

import pe.edu.unmsm.servicios.TestcontainersConfiguration;

/**
 * The OpenAPI document Swagger UI reads (docs/api/panorama-api.md): it describes the API, declares the Bearer JWT
 * scheme so «Authorize» works, and points «Try it out» to the same origin it was loaded from, whatever proxy sits in
 * front. Production switches it off with APP_API_DOCS_ENABLED=false (springdoc.api-docs/swagger-ui.enabled).
 */
class OpenApiDocsTests {

	private static final HttpClient HTTP = HttpClient.newHttpClient();

	private static HttpResponse<String> get(int port, String path) throws Exception {
		HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build();
		return HTTP.send(request, HttpResponse.BodyHandlers.ofString());
	}

	@Nested
	@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
	@Import(TestcontainersConfiguration.class)
	class Enabled {

		@Value("${local.server.port}")
		int port;

		@Test
		void describesTheApi() throws Exception {
			HttpResponse<String> response = get(port, "/v3/api-docs");

			assertThat(response.statusCode()).isEqualTo(200);
			String json = response.body();
			assertThat(JsonPath.<String>read(json, "$.info.title")).isEqualTo("API de servicios universitarios");
			assertThat(JsonPath.<String>read(json, "$.info.version")).isNotBlank();
			assertThat(JsonPath.<Map<String, Object>>read(json, "$.paths")).containsKey("/api/v1/requests");
		}

		@Test
		void declaresTheBearerJwtSchemeForEveryOperation() throws Exception {
			String json = get(port, "/v3/api-docs").body();

			assertThat(JsonPath.<String>read(json, "$.components.securitySchemes.bearerAuth.type")).isEqualTo("http");
			assertThat(JsonPath.<String>read(json, "$.components.securitySchemes.bearerAuth.scheme")).isEqualTo("bearer");
			assertThat(JsonPath.<String>read(json, "$.components.securitySchemes.bearerAuth.bearerFormat"))
					.isEqualTo("JWT");
			assertThat(JsonPath.<List<Map<String, Object>>>read(json, "$.security")).anySatisfy(
					requirement -> assertThat(requirement).containsKey("bearerAuth"));
		}

		@Test
		void tryItOutCallsTheOriginSwaggerWasLoadedFrom() throws Exception {
			String json = get(port, "/v3/api-docs").body();

			assertThat(JsonPath.<List<String>>read(json, "$.servers[*].url")).containsExactly("/");
		}
	}

	@Nested
	@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT,
			properties = "APP_API_DOCS_ENABLED=false")
	@Import(TestcontainersConfiguration.class)
	class DisabledForProduction {

		@Value("${local.server.port}")
		int port;

		@Test
		void apiDocsAndSwaggerUiAreNotServed() throws Exception {
			assertThat(get(port, "/v3/api-docs").statusCode()).isEqualTo(404);
			assertThat(get(port, "/swagger-ui/index.html").statusCode()).isEqualTo(404);
		}
	}
}
