package pe.edu.unmsm.servicios.requests.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URLEncoder;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import pe.edu.unmsm.servicios.TestcontainersConfiguration;
import pe.edu.unmsm.servicios.testsupport.ApiClient;

/** Server-side filters, order, paging and counters of {@code GET /requests} (docs/api/contrato-v1.md). */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT,
		properties = "spring.flyway.locations=classpath:db/migration,classpath:db/seed-dev")
@Import(TestcontainersConfiguration.class)
class RequestListFiltersApiTests {

	// Seeded by seed-dev.
	private static final String STUDENT = "estudiante@unmsm.edu.pe";
	private static final String SUPERVISOR = "supervisor.mantenimiento@unmsm.edu.pe"; // area 1
	private static final String TECHNICIAN = "tecnico.mantenimiento@unmsm.edu.pe"; // id 3, area 1
	private static final String ADMIN = "admin@unmsm.edu.pe";
	private static final long TECHNICIAN_ID = 3;
	// Inserted by these tests.
	private static final String OTHER_STUDENT = "filtros.solicitante@unmsm.edu.pe";
	private static final String DEMO_HASH = "$2b$10$hOCLn2iSSfARxF.PH/fQoeUJGY3u2dOY7aug6hnxrdiqia2rKKXvy";

	// Catalog ids from the seed.
	private static final long ELECTRICITY = 1; // area 1
	private static final long PLUMBING = 2; // area 1
	private static final long NETWORK = 6; // area 3
	private static final long ROOM_101 = 3;
	private static final long LIBRARY = 8;
	private static final long CRITICAL = 1;
	private static final long HIGH = 2;
	private static final long LOW = 4;

	@Value("${local.server.port}")
	int port;

	@Autowired
	JdbcTemplate jdbc;

	private ApiClient api;

	private final Map<String, String> tokens = new java.util.HashMap<>();

	@BeforeEach
	void setUp() {
		api = new ApiClient(port);
		jdbc.update("delete from request_history");
		jdbc.update("delete from service_requests");
		jdbc.update("delete from request_code_sequences");
		jdbc.update("insert ignore into users (id, full_name, email, password_hash, role, requester_type, area_id, "
				+ "created_at) values (95, 'Rosa Filtros', ?, ?, 'SOLICITANTE', 'DOCENTE', null, utc_timestamp(6))",
				OTHER_STUDENT, DEMO_HASH);
	}

	private String token(String user) throws Exception {
		String token = tokens.get(user);
		if (token == null) {
			token = api.login(user);
			tokens.put(user, token);
		}
		return token;
	}

	private String create(String user, long category, long location, String description, long priority)
			throws Exception {
		String body = "{\"categoryId\":" + category + ",\"locationId\":" + location + ",\"description\":\""
				+ description + "\",\"priorityId\":" + priority + "}";
		HttpResponse<String> response = api.post("/api/v1/requests", body, token(user));
		assertThat(response.statusCode()).as(response.body()).isEqualTo(201);
		return ApiClient.read(response, "$.code", String.class);
	}

	private String create(long category, long priority) throws Exception {
		return create(STUDENT, category, ROOM_101, "El tomacorriente del aula no funciona", priority);
	}

	private HttpResponse<String> list(String user, String query) throws Exception {
		return api.get("/api/v1/requests" + query, token(user));
	}

	private List<String> codes(String user, String query) throws Exception {
		HttpResponse<String> response = list(user, query);
		assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
		return ApiClient.read(response, "$.items[*].code");
	}

	private void set(String code, String assignment, Object value) {
		jdbc.update("update service_requests set " + assignment + " where code = ?", value, code);
	}

	private static LocalDateTime utc(Instant instant) {
		return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
	}

	private static String encode(String value) {
		return URLEncoder.encode(value, StandardCharsets.UTF_8);
	}

	// ───── Single filters ─────

	@Test
	void statusAcceptsSeveralValuesRepeatedOrCommaSeparated() throws Exception {
		String registered = create(ELECTRICITY, HIGH);
		String assigned = create(ELECTRICITY, HIGH);
		String resolved = create(ELECTRICITY, HIGH);
		set(assigned, "status = ?", "ASIGNADA");
		set(resolved, "status = ?", "RESUELTA");

		assertThat(codes(STUDENT, "?status=REGISTRADA&status=ASIGNADA")).containsExactlyInAnyOrder(registered,
				assigned);
		assertThat(codes(STUDENT, "?status=ASIGNADA,RESUELTA")).containsExactlyInAnyOrder(assigned, resolved);
		assertThat(codes(STUDENT, "?status=CERRADA")).isEmpty();
	}

	@Test
	void categoryPriorityAndAreaFilterByIdentifier() throws Exception {
		String electricity = create(ELECTRICITY, HIGH);
		String plumbing = create(PLUMBING, CRITICAL);
		String network = create(NETWORK, HIGH); // area 3

		assertThat(codes(ADMIN, "?categoryId=" + PLUMBING)).containsExactly(plumbing);
		assertThat(codes(ADMIN, "?priorityId=" + HIGH)).containsExactlyInAnyOrder(electricity, network);
		assertThat(codes(ADMIN, "?areaId=3")).containsExactly(network);
		assertThat(codes(ADMIN, "?areaId=1")).containsExactlyInAnyOrder(electricity, plumbing);
		assertThat(codes(ADMIN, "?categoryId=999")).isEmpty();
	}

	@Test
	void technicianAndUnassignedFilters() throws Exception {
		String assigned = create(ELECTRICITY, HIGH);
		String unassigned = create(ELECTRICITY, HIGH);
		set(assigned, "technician_id = ?", TECHNICIAN_ID);

		assertThat(codes(SUPERVISOR, "?technicianId=" + TECHNICIAN_ID)).containsExactly(assigned);
		assertThat(codes(SUPERVISOR, "?unassigned=true")).containsExactly(unassigned);
		assertThat(codes(SUPERVISOR, "?unassigned=false")).containsExactlyInAnyOrder(assigned, unassigned);
	}

	@Test
	void overdueFollowsTheSlaRule() throws Exception {
		String late = create(ELECTRICITY, HIGH);
		String resolvedLate = create(ELECTRICITY, HIGH);
		String onTime = create(ELECTRICITY, HIGH);
		LocalDateTime past = utc(Instant.now().minus(Duration.ofHours(3)));
		set(late, "due_at = ?", past);
		set(resolvedLate, "due_at = ?", past);
		set(resolvedLate, "status = ?", "RESUELTA");

		assertThat(codes(STUDENT, "?overdue=true")).containsExactly(late);
		assertThat(codes(STUDENT, "?overdue=false")).containsExactlyInAnyOrder(resolvedLate, onTime);
	}

	@Test
	void textSearchCoversCodeDescriptionRequesterCategoryAndRoom() throws Exception {
		String outlet = create(STUDENT, ELECTRICITY, ROOM_101, "El tomacorriente del aula no funciona", HIGH);
		String leak = create(STUDENT, PLUMBING, LIBRARY, "Hay una fuga de agua junto a la ventana", HIGH);
		String other = create(OTHER_STUDENT, NETWORK, ROOM_101, "No hay conexion a internet en el aula", LOW);

		assertThat(codes(ADMIN, "?q=" + encode(leak))).containsExactly(leak);
		assertThat(codes(ADMIN, "?q=TOMACORRIENTE")).as("case-insensitive").containsExactly(outlet);
		assertThat(codes(ADMIN, "?q=" + encode("Rosa Filtros"))).as("requester").containsExactly(other);
		assertThat(codes(ADMIN, "?q=" + encode("gasfiteria"))).as("category, accent-insensitive")
				.containsExactly(leak);
		assertThat(codes(ADMIN, "?q=Biblioteca")).as("room").containsExactly(leak);
		assertThat(codes(ADMIN, "?q=" + encode("  aula 101  "))).as("trimmed").containsExactlyInAnyOrder(outlet,
				other);
	}

	@Test
	void likeWildcardsTypedByTheUserAreLiteral() throws Exception {
		String percent = create(STUDENT, ELECTRICITY, ROOM_101, "El voltaje cae al 50% en el aula 101", HIGH);
		create(STUDENT, ELECTRICITY, ROOM_101, "El voltaje cae a la mitad en el aula", HIGH);

		assertThat(codes(STUDENT, "?q=" + encode("%"))).containsExactly(percent);
		assertThat(codes(STUDENT, "?q=" + encode("_"))).isEmpty();
		assertThat(codes(STUDENT, "?q=" + encode("' or 1=1 --"))).isEmpty();
	}

	@Test
	void dateRangeCoversWholeLimaDaysInclusive() throws Exception {
		String old = create(ELECTRICITY, HIGH);
		String recent = create(ELECTRICITY, HIGH);
		// Stored as UTC: 2026-09-30 23:59:59 and 2026-10-01 00:00 in Lima.
		set(old, "created_at = ?", LocalDateTime.parse("2026-10-01T04:59:59"));
		set(recent, "created_at = ?", LocalDateTime.parse("2026-10-01T05:00:00"));

		assertThat(codes(STUDENT, "?from=2026-10-01")).containsExactly(recent);
		assertThat(codes(STUDENT, "?to=2026-09-30")).containsExactly(old);
		assertThat(codes(STUDENT, "?from=2026-09-30&to=2026-09-30")).containsExactly(old);
		assertThat(codes(STUDENT, "?from=2026-09-01&to=2026-12-01")).hasSize(2);
	}

	// ───── Combined filters, visibility and order ─────

	@Test
	void filtersCombineWithAnd() throws Exception {
		String match = create(ELECTRICITY, CRITICAL);
		String wrongStatus = create(ELECTRICITY, CRITICAL);
		String wrongTechnician = create(ELECTRICITY, CRITICAL);
		create(ELECTRICITY, HIGH); // wrong priority
		set(wrongStatus, "status = ?", "RESUELTA");
		set(wrongTechnician, "technician_id = ?", TECHNICIAN_ID);

		assertThat(codes(SUPERVISOR, "?status=REGISTRADA&priorityId=" + CRITICAL + "&unassigned=true&q=aula"))
				.containsExactly(match);
	}

	@Test
	void filtersNeverWidenWhatTheRoleCanSee() throws Exception {
		String mine = create(ELECTRICITY, HIGH);
		String othersInArea = create(OTHER_STUDENT, ELECTRICITY, ROOM_101, "El tomacorriente del aula no funciona",
				HIGH);
		String otherArea = create(NETWORK, HIGH);
		set(othersInArea, "technician_id = ?", TECHNICIAN_ID);

		assertThat(codes(STUDENT, "?technicianId=" + TECHNICIAN_ID)).isEmpty();
		assertThat(codes(STUDENT, "?areaId=1")).containsExactly(mine);
		assertThat(codes(SUPERVISOR, "?areaId=3")).as("another area").isEmpty();
		assertThat(codes(SUPERVISOR, "?categoryId=" + NETWORK)).isEmpty();
		assertThat(codes(TECHNICIAN, "")).containsExactly(othersInArea);
		assertThat(codes(TECHNICIAN, "?unassigned=true")).isEmpty();
		assertThat(codes(ADMIN, "?areaId=3")).containsExactly(otherArea);
	}

	@Test
	void sortIsAnAllowListWithStableTieBreakers() throws Exception {
		String low = create(ELECTRICITY, LOW);
		String critical = create(ELECTRICITY, CRITICAL);
		String high = create(ELECTRICITY, HIGH);
		String resolvedSoon = create(ELECTRICITY, CRITICAL);
		set(resolvedSoon, "status = ?", "RESUELTA");

		assertThat(codes(STUDENT, "")).containsExactly(resolvedSoon, high, critical, low);
		assertThat(codes(STUDENT, "?sort=newest")).containsExactly(resolvedSoon, high, critical, low);
		assertThat(codes(STUDENT, "?sort=oldest")).containsExactly(low, critical, high, resolvedSoon);
		assertThat(codes(STUDENT, "?sort=priority")).containsExactly(resolvedSoon, critical, high, low);
		assertThat(codes(STUDENT, "?sort=due")).as("pending first, by due date").containsExactly(critical, high,
				low, resolvedSoon);
		ApiClient.assertProblem(list(STUDENT, "?sort=" + encode("created_at; drop table users")), 400);
	}

	@Test
	void pagingAppliesAfterFiltering() throws Exception {
		for (int i = 0; i < 5; i++) {
			create(ELECTRICITY, HIGH);
		}
		create(PLUMBING, HIGH);

		HttpResponse<String> second = list(STUDENT, "?categoryId=" + ELECTRICITY + "&page=1&size=2");

		assertThat(ApiClient.<Integer>read(second, "$.total")).isEqualTo(5);
		assertThat(ApiClient.<Integer>read(second, "$.totalPages")).isEqualTo(3);
		assertThat(ApiClient.<List<String>>read(second, "$.items[*].code")).hasSize(2);
		assertThat(codes(STUDENT, "?categoryId=" + ELECTRICITY + "&page=2&size=2")).hasSize(1);
		assertThat(codes(STUDENT, "?categoryId=" + ELECTRICITY + "&page=3&size=2")).isEmpty();
	}

	// ───── Validation ─────

	@Test
	void invalidParametersAreReportedTogetherAsProblemDetails() throws Exception {
		HttpResponse<String> response = list(STUDENT,
				"?status=ABIERTA&categoryId=abc&overdue=yes&from=ayer&sort=random&page=-1&size=1000");

		ApiClient.assertProblem(response, 400);
		Map<String, String> errors = ApiClient.read(response, "$.errors");
		assertThat(errors).containsOnlyKeys("status", "categoryId", "overdue", "from", "sort", "page", "size");
		ApiClient.assertProblem(list(STUDENT, "?technicianId=3&unassigned=true"), 400);
		ApiClient.assertProblem(list(STUDENT, "?from=2026-10-02&to=2026-10-01"), 400);
		ApiClient.assertProblem(list(STUDENT, "?q=" + "a".repeat(101)), 400);
	}

	// ───── Counters ─────

	@Test
	void countersCoverEveryStatusWithinTheScopeOfTheRole() throws Exception {
		String late = create(ELECTRICITY, HIGH);
		String resolved = create(ELECTRICITY, HIGH);
		create(NETWORK, HIGH); // area 3
		create(OTHER_STUDENT, ELECTRICITY, ROOM_101, "El tomacorriente del aula no funciona", HIGH);
		set(late, "due_at = ?", utc(Instant.now().minus(Duration.ofHours(1))));
		set(resolved, "status = ?", "RESUELTA");

		HttpResponse<String> student = api.get("/api/v1/requests/counts", token(STUDENT));
		assertThat(student.statusCode()).as(student.body()).isEqualTo(200);
		assertThat(ApiClient.<Integer>read(student, "$.total")).isEqualTo(3);
		assertThat(ApiClient.<Integer>read(student, "$.overdue")).isEqualTo(1);
		assertThat(ApiClient.<Integer>read(student, "$.byStatus.REGISTRADA")).isEqualTo(2);
		assertThat(ApiClient.<Integer>read(student, "$.byStatus.RESUELTA")).isEqualTo(1);
		assertThat(ApiClient.<Integer>read(student, "$.byStatus.CERRADA")).isZero();

		HttpResponse<String> supervisor = api.get("/api/v1/requests/counts", token(SUPERVISOR));
		assertThat(ApiClient.<Integer>read(supervisor, "$.total")).isEqualTo(3);
		HttpResponse<String> technician = api.get("/api/v1/requests/counts", token(TECHNICIAN));
		assertThat(ApiClient.<Integer>read(technician, "$.total")).isZero();
		assertThat(ApiClient.<Map<String, Integer>>read(technician, "$.byStatus")).hasSize(6);
		HttpResponse<String> admin = api.get("/api/v1/requests/counts", token(ADMIN));
		assertThat(ApiClient.<Integer>read(admin, "$.total")).isEqualTo(4);
		ApiClient.assertProblem(api.get("/api/v1/requests/counts", null), 401);
	}

}
