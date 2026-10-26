package pe.edu.unmsm.servicios.admin.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static pe.edu.unmsm.servicios.testsupport.ApiClient.assertProblem;
import static pe.edu.unmsm.servicios.testsupport.ApiClient.read;

import java.net.URLEncoder;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;

import pe.edu.unmsm.servicios.TestcontainersConfiguration;
import pe.edu.unmsm.servicios.testsupport.ApiClient;

/** Registers requests to produce history, so its context and database are discarded afterwards. */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT,
		properties = "spring.flyway.locations=classpath:db/migration,classpath:db/seed-dev")
@Import(TestcontainersConfiguration.class)
@DirtiesContext
class AdminAuditApiTests {

	private static final String ADMIN = "admin@unmsm.edu.pe";
	private static final String REQUESTER = "estudiante@unmsm.edu.pe";
	private static final String BODY = "{\"categoryId\":1,\"locationId\":3,\"description\":"
			+ "\"El proyector del aula no enciende desde ayer por la tarde\",\"priorityId\":3}";

	@Value("${local.server.port}")
	int port;

	private ApiClient api;
	private String admin;

	@BeforeEach
	void setUp() throws Exception {
		api = new ApiClient(port);
		admin = api.login(ADMIN);
	}

	private String register() throws Exception {
		HttpResponse<String> created = api.post("/api/v1/requests", BODY, api.login(REQUESTER));
		assertThat(created.statusCode()).as(created.body()).isEqualTo(201);
		return read(created, "$.code", String.class);
	}

	private HttpResponse<String> audit(String q) throws Exception {
		String query = q == null ? "" : "?q=" + URLEncoder.encode(q, StandardCharsets.UTF_8);
		return api.get("/api/v1/admin/audit" + query, admin);
	}

	@Test
	void onlyAdministratorsCanReadTheAudit() throws Exception {
		assertProblem(api.get("/api/v1/admin/audit", api.login("supervisor.mantenimiento@unmsm.edu.pe")), 403);
		assertThat(api.get("/api/v1/admin/audit", null).statusCode()).isEqualTo(401);
	}

	@Test
	void listsHistoryChangesNewestFirst() throws Exception {
		String first = register();
		String second = register();

		HttpResponse<String> response = audit(null);

		assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
		List<String> codes = read(response, "$[*].requestCode", List.class);
		assertThat(codes.indexOf(second)).isLessThan(codes.indexOf(first));
		assertThat(read(response, "$[0].detail", String.class)).isEqualTo("— → Registrada · Solicitud creada");
		assertThat(read(response, "$[0].actorName", String.class)).isNotBlank();
		assertThat(read(response, "$[0].createdAt", String.class)).endsWith("Z");
		assertThat(read(response, "$[0].id", Long.class)).isPositive();
	}

	@Test
	void filtersByRequestCodeAndStateLabelAndCapsAtForty() throws Exception {
		String code = register();
		for (int i = 0; i < 41; i++) {
			register();
		}

		assertThat(read(audit(null), "$.length()", Integer.class)).isEqualTo(40);
		assertThat(read(audit(code), "$[*].requestCode", List.class)).containsExactly(code);
		assertThat(read(audit("registrada"), "$.length()", Integer.class)).isEqualTo(40);
		assertThat(read(audit("en atención"), "$", List.class)).isEmpty();
		assertThat(read(audit("100%_inexistente"), "$", List.class)).isEmpty();
	}

}
