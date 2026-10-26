package pe.edu.unmsm.servicios.triage.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static pe.edu.unmsm.servicios.testsupport.ApiClient.assertProblem;
import static pe.edu.unmsm.servicios.testsupport.ApiClient.read;

import java.net.http.HttpResponse;
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

/**
 * Creates rules and deactivates a category, so its context and database are discarded afterwards. Each test uses
 * its own keywords because the rules created by the other tests stay in the database.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT,
		properties = "spring.flyway.locations=classpath:db/migration,classpath:db/seed-dev")
@Import(TestcontainersConfiguration.class)
@DirtiesContext
class TriageApiTests {

	private static final String ADMIN = "admin@unmsm.edu.pe";
	private static final String REQUESTER = "estudiante@unmsm.edu.pe";
	private static final String RULES = "/api/v1/admin/triage-rules";
	private static final String SUGGESTIONS = "/api/v1/triage/suggestions";

	@Value("${local.server.port}")
	int port;

	private ApiClient api;
	private String admin;
	private String requester;

	@BeforeEach
	void setUp() throws Exception {
		api = new ApiClient(port);
		admin = api.login(ADMIN);
		requester = api.login(REQUESTER);
	}

	private static String body(String keywords, long categoryId, long priorityId, Boolean active) {
		return "{\"keywords\":[" + keywords + "],\"categoryId\":" + categoryId + ",\"priorityId\":" + priorityId
				+ (active == null ? "" : ",\"active\":" + active) + "}";
	}

	private long createRule(String keywords, long categoryId, long priorityId) throws Exception {
		HttpResponse<String> response = api.post(RULES, body(keywords, categoryId, priorityId, null), admin);
		assertThat(response.statusCode()).as(response.body()).isEqualTo(201);
		return read(response, "$.id", Long.class);
	}

	private HttpResponse<String> suggest(String description) throws Exception {
		return api.post(SUGGESTIONS, "{\"description\":\"" + description + "\"}", requester);
	}

	private static void assertNoSuggestion(HttpResponse<String> response) {
		assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
		assertThat(response.headers().firstValue("Content-Type")).hasValueSatisfying(
				type -> assertThat(type).startsWith("application/json"));
		assertThat(response.body()).isEqualTo("null");
	}

	@Test
	void onlyAdministratorsManageRulesAndOnlyRequestersAskForSuggestions() throws Exception {
		assertProblem(api.get(RULES, requester), 403);
		assertProblem(api.post(RULES, body("\"humo\"", 1, 1, null), api.login("supervisor.mantenimiento@unmsm.edu.pe")),
				403);
		assertProblem(api.send("DELETE", RULES + "/1", null, api.login("tecnico.mantenimiento@unmsm.edu.pe")), 403);
		assertThat(api.get(RULES, null).statusCode()).isEqualTo(401);

		assertProblem(api.post(SUGGESTIONS, "{\"description\":\"No hay luz en el aula\"}", admin), 403);
		assertThat(api.post(SUGGESTIONS, "{\"description\":\"No hay luz en el aula\"}", null).statusCode())
				.isEqualTo(401);
	}

	@Test
	void aRuleIsCreatedListedUpdatedAndDeleted() throws Exception {
		HttpResponse<String> created = api.post(RULES, body("\"  Chispa \",\"HUMO\",\"humo\",\"Corto   Circuito\"", 1, 1,
				null), admin);

		assertThat(created.statusCode()).as(created.body()).isEqualTo(201);
		long id = read(created, "$.id", Long.class);
		assertThat(read(created, "$.keywords", List.class)).containsExactly("chispa", "humo", "corto circuito");
		assertThat(read(created, "$.categoryId", Long.class)).isEqualTo(1L);
		assertThat(read(created, "$.categoryName", String.class)).isEqualTo("Electricidad");
		assertThat(read(created, "$.priorityName", String.class)).isEqualTo("Crítica");
		assertThat(read(created, "$.active", Boolean.class)).isTrue();

		long second = createRule("\"tomacorriente\"", 1, 2);
		List<Integer> ids = read(api.get(RULES, admin), "$[*].id", List.class);
		assertThat(ids).containsSubsequence((int) id, (int) second);
		assertThat(ids).isSorted();

		HttpResponse<String> updated = api.send("PUT", RULES + "/" + id, body("\"chispa\"", 2, 3, false), admin);
		assertThat(updated.statusCode()).as(updated.body()).isEqualTo(200);
		assertThat(read(updated, "$.keywords", List.class)).containsExactly("chispa");
		assertThat(read(updated, "$.categoryName", String.class)).isEqualTo("Gasfitería");
		assertThat(read(updated, "$.priorityName", String.class)).isEqualTo("Media");
		assertThat(read(updated, "$.active", Boolean.class)).isFalse();
		assertThat(read(api.get(RULES, admin), "$[?(@.id == " + id + ")].active", List.class)).containsExactly(false);

		assertThat(api.send("DELETE", RULES + "/" + id, null, admin).statusCode()).isEqualTo(204);
		assertThat(read(api.get(RULES, admin), "$[*].id", List.class)).doesNotContain((int) id);
		assertProblem(api.send("DELETE", RULES + "/" + id, null, admin), 404);
		assertProblem(api.send("PUT", RULES + "/" + id, body("\"chispa\"", 1, 1, null), admin), 404);
	}

	@Test
	void ruleBodiesAreValidated() throws Exception {
		HttpResponse<String> noKeywords = api.post(RULES, body("", 1, 1, null), admin);
		assertProblem(noKeywords, 400);
		assertThat(read(noKeywords, "$.errors.keywords", String.class)).isNotBlank();

		assertProblem(api.post(RULES, body("\"x\"", 1, 1, null), admin), 400);

		HttpResponse<String> unknown = api.post(RULES, body("\"zumbido\"", 999, 99, null), admin);
		assertProblem(unknown, 400);
		assertThat(read(unknown, "$.errors.categoryId", String.class)).isNotBlank();
		assertThat(read(unknown, "$.errors.priorityId", String.class)).isNotBlank();

		HttpResponse<String> missing = api.post(RULES, "{\"keywords\":[\"zumbido\"]}", admin);
		assertProblem(missing, 400);
		assertThat(read(missing, "$.errors.categoryId", String.class)).isNotBlank();
		assertThat(read(missing, "$.errors.priorityId", String.class)).isNotBlank();
	}

	@Test
	void aRuleNeedsAnActiveCategory() throws Exception {
		long category = createCategory("Climatización");
		deactivateCategory(category);

		HttpResponse<String> inactive = api.post(RULES, body("\"aire acondicionado\"", category, 3, null), admin);

		assertProblem(inactive, 400);
		assertThat(read(inactive, "$.errors.categoryId", String.class)).isNotBlank();
	}

	@Test
	void theSuggestionComesFromTheRuleWithMostMatches() throws Exception {
		createRule("\"goteo\",\"caño\"", 2, 3);
		createRule("\"goteo\",\"inundación\",\"caño\"", 2, 1);

		HttpResponse<String> response = suggest("Hay goteo en el CANO y una inundacion en el baño");

		assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
		assertThat(read(response, "$.categoryId", Long.class)).isEqualTo(2L);
		assertThat(read(response, "$.priorityId", Long.class)).isEqualTo(1L);
	}

	@Test
	void aTieIsWonByTheOldestRule() throws Exception {
		createRule("\"pizarra\"", 4, 4);
		createRule("\"plumón\"", 3, 2);

		HttpResponse<String> response = suggest("Falta un plumon para la pizarra");

		assertThat(read(response, "$.categoryId", Long.class)).isEqualTo(4L);
		assertThat(read(response, "$.priorityId", Long.class)).isEqualTo(4L);
	}

	@Test
	void withoutMatchesTheSuggestionIsNull() throws Exception {
		createRule("\"teclado\"", 5, 3);

		assertNoSuggestion(suggest("Los teclados no son el problema, es otra cosa"));
	}

	@Test
	void inactiveRulesAndRulesOfInactiveCategoriesAreIgnored() throws Exception {
		HttpResponse<String> inactiveRule = api.post(RULES, body("\"impresora\"", 5, 2, false), admin);
		assertThat(inactiveRule.statusCode()).as(inactiveRule.body()).isEqualTo(201);
		long category = createCategory("Ascensores");
		createRule("\"ascensor\"", category, 1);
		assertThat(read(suggest("El ascensor está detenido"), "$.categoryId", Long.class)).isEqualTo(category);

		deactivateCategory(category);

		assertNoSuggestion(suggest("La impresora y el ascensor fallan"));
	}

	@Test
	void nothingIsSuggestedWhileTheAdministratorHasSuggestionsTurnedOff() throws Exception {
		createRule("\"proyector\"", 5, 2);
		assertThat(read(suggest("El proyector no enciende"), "$.categoryId", Long.class)).isEqualTo(5L);

		setTriageSuggestions(false);
		try {
			assertNoSuggestion(suggest("El proyector no enciende"));
		}
		finally {
			setTriageSuggestions(true);
		}

		assertThat(read(suggest("El proyector no enciende"), "$.categoryId", Long.class)).isEqualTo(5L);
	}

	@Test
	void theDescriptionIsValidated() throws Exception {
		HttpResponse<String> blank = suggest("   ");
		assertProblem(blank, 400);
		assertThat(read(blank, "$.errors.description", String.class)).isNotBlank();

		assertProblem(suggest("x".repeat(2001)), 400);
		assertThat(suggest("x".repeat(2000)).statusCode()).isEqualTo(200);
	}

	private long createCategory(String name) throws Exception {
		HttpResponse<String> response = api.post("/api/v1/admin/categories",
				"{\"name\":\"" + name + "\",\"areaId\":1}", admin);
		assertThat(response.statusCode()).as(response.body()).isEqualTo(201);
		return read(response, "$.id", Long.class);
	}

	private void setTriageSuggestions(boolean enabled) throws Exception {
		HttpResponse<String> response = api.send("PUT", "/api/v1/admin/config",
				"{\"autoCloseHours\":72,\"emailNotifications\":false,\"triageSuggestions\":" + enabled + "}", admin);
		assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
	}

	private void deactivateCategory(long id) throws Exception {
		assertThat(api.send("DELETE", "/api/v1/admin/categories/" + id, null, admin).statusCode()).isEqualTo(204);
	}

}
