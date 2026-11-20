package pe.edu.unmsm.servicios.requests.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpResponse;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;

import pe.edu.unmsm.servicios.TestcontainersConfiguration;
import pe.edu.unmsm.servicios.testsupport.ApiClient;

/** Draft of a new request, docs/api/contrato-v1.md «Borrador de nueva solicitud». */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT,
		properties = "spring.flyway.locations=classpath:db/migration,classpath:db/seed-dev")
@Import(TestcontainersConfiguration.class)
class RequestDraftApiTests {

	private static final String PATH = "/api/v1/me/request-draft";
	private static final String PASSWORD = "Clave12345";
	private static final String DRAFT = """
			{"category":"Electricidad","description":"El foco del pasadizo parpadea","priority":"Alta",
			 "faculty":"Facultad de Ingeniería de Sistemas e Informática","pavilion":"","room":""}""";

	@Value("${local.server.port}")
	int port;

	private ApiClient api;
	private String token;

	@BeforeEach
	void setUp() throws Exception {
		api = new ApiClient(port);
		token = newRequester();
	}

	/** A fresh requester per test, so no other test sees or changes its draft. */
	private String newRequester() throws Exception {
		String email = "d" + UUID.randomUUID().toString().replace("-", "").substring(0, 12) + "@unmsm.edu.pe";
		HttpResponse<String> registered = api.post("/api/v1/auth/register", "{\"fullName\":\"Ana Borrador\",\"email\":\""
				+ email + "\",\"password\":\"" + PASSWORD + "\",\"requesterType\":\"ESTUDIANTE\"}", null);
		assertThat(registered.statusCode()).as(registered.body()).isEqualTo(201);
		HttpResponse<String> login = api.post("/api/v1/auth/login",
				"{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}", null);
		return ApiClient.read(login, "$.accessToken", String.class);
	}

	private HttpResponse<String> save(String json) throws Exception {
		return api.send("PUT", PATH, json, token);
	}

	@Test
	void withoutDraftTheAnswerIsEmpty() throws Exception {
		assertThat(api.get(PATH, token).statusCode()).isEqualTo(204);
	}

	@Test
	void aSavedDraftComesBackAsItWasLeft() throws Exception {
		HttpResponse<String> saved = save(DRAFT);

		assertThat(saved.statusCode()).as(saved.body()).isEqualTo(200);
		HttpResponse<String> read = api.get(PATH, token);
		assertThat(read.statusCode()).isEqualTo(200);
		assertThat(ApiClient.<String>read(read, "$.category")).isEqualTo("Electricidad");
		assertThat(ApiClient.<String>read(read, "$.description")).isEqualTo("El foco del pasadizo parpadea");
		assertThat(ApiClient.<String>read(read, "$.priority")).isEqualTo("Alta");
		assertThat(ApiClient.<String>read(read, "$.faculty")).isEqualTo("Facultad de Ingeniería de Sistemas e Informática");
		assertThat(ApiClient.<String>read(read, "$.room")).isEmpty();
		assertThat(ApiClient.<String>read(read, "$.updatedAt")).isNotBlank();
	}

	@Test
	void savingAgainReplacesTheDraft() throws Exception {
		save(DRAFT);
		save("{\"description\":\"Otra descripción\"}");

		HttpResponse<String> read = api.get(PATH, token);
		assertThat(ApiClient.<String>read(read, "$.description")).isEqualTo("Otra descripción");
		assertThat(ApiClient.<String>read(read, "$.category")).isEmpty();
	}

	@Test
	void aFieldLongerThanAllowedIsRejected() throws Exception {
		HttpResponse<String> tooLong = save("{\"description\":\"" + "x".repeat(2001) + "\",\"room\":\"" + "y".repeat(151)
				+ "\"}");

		ApiClient.assertProblem(tooLong, 400);
		assertThat(ApiClient.<String>read(tooLong, "$.errors.description")).isNotBlank();
		assertThat(ApiClient.<String>read(tooLong, "$.errors.room")).isNotBlank();
	}

	@Test
	void anEmptyDraftIsNotKept() throws Exception {
		save(DRAFT);

		HttpResponse<String> emptied = save("{\"category\":\"\",\"description\":\"   \"}");

		assertThat(emptied.statusCode()).isEqualTo(204);
		assertThat(api.get(PATH, token).statusCode()).isEqualTo(204);
	}

	@Test
	void discardingRemovesItAndCanBeRepeated() throws Exception {
		save(DRAFT);

		assertThat(api.send("DELETE", PATH, null, token).statusCode()).isEqualTo(204);
		assertThat(api.send("DELETE", PATH, null, token).statusCode()).isEqualTo(204);
		assertThat(api.get(PATH, token).statusCode()).isEqualTo(204);
	}

	@Test
	void eachRequesterHasTheirOwnDraft() throws Exception {
		save(DRAFT);

		String someoneElse = newRequester();
		assertThat(api.get(PATH, someoneElse).statusCode()).isEqualTo(204);
	}

	@Test
	void onlyRequestersHaveDrafts() throws Exception {
		String supervisor = api.login("supervisor.mantenimiento@unmsm.edu.pe");

		ApiClient.assertProblem(api.get(PATH, supervisor), 403);
		ApiClient.assertProblem(api.send("PUT", PATH, DRAFT, supervisor), 403);
		ApiClient.assertProblem(api.get(PATH, null), 401);
	}

	@Test
	void registeringTheRequestDiscardsTheDraft() throws Exception {
		save(DRAFT);

		HttpResponse<String> created = api.post("/api/v1/requests", "{\"categoryId\":1,\"locationId\":3,"
				+ "\"description\":\"El foco del pasadizo parpadea desde ayer en la noche\",\"priorityId\":2}", token);

		assertThat(created.statusCode()).as(created.body()).isEqualTo(201);
		assertThat(api.get(PATH, token).statusCode()).isEqualTo(204);
	}

}
