package pe.edu.unmsm.servicios.shared.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import pe.edu.unmsm.servicios.TestcontainersConfiguration;
import pe.edu.unmsm.servicios.testsupport.ApiClient;

/**
 * Errors no module handles on its own still answer in Spanish, with a message a person can act on and nothing
 * internal (docs/api/contrato-v1.md «Errores»).
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT,
		properties = "spring.flyway.locations=classpath:db/migration,classpath:db/seed-dev")
@Import({ TestcontainersConfiguration.class, GeneralErrorsApiTests.FailingEndpoint.class })
class GeneralErrorsApiTests {

	/** Test-only endpoints: a bug that escapes every handler, and a required number. */
	@TestConfiguration(proxyBeanMethods = false)
	@RestController
	static class FailingEndpoint {

		@GetMapping("/api/v1/test/boom")
		String boom() {
			throw new IllegalStateException("password=secreto en la tabla users");
		}

		@GetMapping("/api/v1/test/number")
		String number(@RequestParam int value) {
			return Integer.toString(value);
		}

	}

	@Value("${local.server.port}")
	int port;

	private ApiClient api;
	private String token;

	@BeforeEach
	void setUp() throws Exception {
		api = new ApiClient(port);
		token = api.login("estudiante@unmsm.edu.pe");
	}

	private static void assertSpanishProblem(HttpResponse<String> response, int status, String detail) {
		ApiClient.assertProblem(response, status);
		assertThat(ApiClient.<String>read(response, "$.detail")).isEqualTo(detail);
	}

	@Test
	void withoutSessionTheProblemIsInSpanish() throws Exception {
		assertSpanishProblem(api.get("/api/v1/me", null), 401, "Tu sesión no es válida o expiró. Vuelve a iniciar sesión");
	}

	@Test
	void withoutPermissionTheProblemIsInSpanish() throws Exception {
		assertSpanishProblem(api.get("/api/v1/admin/users", token), 403, "No tienes permiso para realizar esta acción");
	}

	@Test
	void anUnexpectedErrorHidesItsCause() throws Exception {
		HttpResponse<String> response = api.get("/api/v1/test/boom", token);

		assertSpanishProblem(response, 500, "Ocurrió un error inesperado. Inténtalo de nuevo en unos minutos");
		assertThat(response.body()).doesNotContain("secreto").doesNotContain("IllegalStateException");
	}

	@Test
	void aBodyThatIsNotValidJsonIsExplained() throws Exception {
		assertSpanishProblem(api.post("/api/v1/requests", "{malo", token), 400,
				"Los datos enviados no tienen el formato esperado");
	}

	@Test
	void aMissingOrWrongParameterIsExplained() throws Exception {
		assertSpanishProblem(api.get("/api/v1/test/number", token), 400, "Falta el dato «value»");
		assertSpanishProblem(api.get("/api/v1/test/number?value=abc", token), 400, "El dato «value» no es válido");
	}

	@Test
	void anUnknownRouteIsExplained() throws Exception {
		HttpResponse<String> response = api.get("/api/v1/no-existe", token);

		assertSpanishProblem(response, 404, "La dirección solicitada no existe");
		assertThat(response.body()).doesNotContain("static resource");
	}

	@Test
	void anUnsupportedMethodOrContentTypeIsExplained() throws Exception {
		assertSpanishProblem(api.send("DELETE", "/api/v1/catalogs", null, token), 405,
				"Esta operación no está disponible");
		HttpResponse<String> plain = api.send("POST", "/api/v1/requests", null, token);
		assertThat(plain.statusCode()).isIn(400, 415);
		assertThat(plain.body()).doesNotContain("Content-Type").doesNotContain("Failed to read");
	}

}
