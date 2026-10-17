package pe.edu.unmsm.servicios.requests.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import pe.edu.unmsm.servicios.TestcontainersConfiguration;
import pe.edu.unmsm.servicios.requests.application.RequestCodeGenerator;
import pe.edu.unmsm.servicios.testsupport.ApiClient;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT,
		properties = "spring.flyway.locations=classpath:db/migration,classpath:db/seed-dev")
@Import(TestcontainersConfiguration.class)
class RequestsApiTests {

	// Seeded by seed-dev.
	private static final String STUDENT = "estudiante@unmsm.edu.pe";
	private static final String SUPERVISOR = "supervisor.mantenimiento@unmsm.edu.pe"; // area 1
	private static final String TECHNICIAN = "tecnico.mantenimiento@unmsm.edu.pe"; // id 3, area 1
	private static final String ADMIN = "admin@unmsm.edu.pe";
	// Inserted by these tests.
	private static final String OTHER_STUDENT = "otra.solicitante@unmsm.edu.pe";
	private static final String SUPPORT_SUPERVISOR = "supervisor.soporte@unmsm.edu.pe"; // area 3
	private static final String SUPPORT_TECHNICIAN = "tecnico.soporte@unmsm.edu.pe"; // area 3
	private static final String DEMO_HASH = "$2b$10$hOCLn2iSSfARxF.PH/fQoeUJGY3u2dOY7aug6hnxrdiqia2rKKXvy";

	private static final Pattern CODE = Pattern.compile("SOL-\\d{4}-\\d{6}");
	private static final String VALID = "El tomacorriente del aula no funciona desde ayer";

	// Catalog ids from the seed.
	private static final long ELECTRICITY = 1; // area 1
	private static final long NETWORK = 6; // area 3
	private static final long ROOM = 3; // Aula 101
	private static final long FACULTY = 1;
	private static final long CRITICAL = 1;
	private static final long HIGH = 2;

	@Value("${local.server.port}")
	int port;

	@Autowired
	JdbcTemplate jdbc;

	@Autowired
	RequestCodeGenerator codes;

	@Autowired
	PlatformTransactionManager transactionManager;

	private ApiClient api;

	@BeforeEach
	void setUp() {
		api = new ApiClient(port);
		jdbc.update("delete from request_history");
		jdbc.update("delete from service_requests");
		jdbc.update("delete from request_code_sequences");
		insertUser(90, "Otra Solicitante", OTHER_STUDENT, "SOLICITANTE", "DOCENTE", null);
		insertUser(91, "Supervisor Soporte", SUPPORT_SUPERVISOR, "SUPERVISOR", null, 3);
		insertUser(92, "Técnico Soporte", SUPPORT_TECHNICIAN, "TECNICO", null, 3);
	}

	private void insertUser(long id, String name, String email, String role, String requesterType, Integer areaId) {
		jdbc.update("insert ignore into users (id, full_name, email, password_hash, role, requester_type, area_id, "
				+ "created_at) values (?, ?, ?, ?, ?, ?, ?, utc_timestamp(6))", id, name, email, DEMO_HASH, role,
				requesterType, areaId);
	}

	private static String body(long category, long location, String description, long priority) {
		return "{\"categoryId\":" + category + ",\"locationId\":" + location + ",\"description\":\"" + description
				+ "\",\"priorityId\":" + priority + "}";
	}

	private HttpResponse<String> create(String user, long category, String description, long priority) throws Exception {
		return api.post("/api/v1/requests", body(category, ROOM, description, priority), api.login(user));
	}

	private String createCode(String user, long category) throws Exception {
		HttpResponse<String> response = create(user, category, VALID, HIGH);
		assertThat(response.statusCode()).as(response.body()).isEqualTo(201);
		return ApiClient.read(response, "$.code", String.class);
	}

	private List<String> listedCodes(String user, String query) throws Exception {
		HttpResponse<String> response = api.get("/api/v1/requests" + query, api.login(user));
		assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
		return ApiClient.read(response, "$.items[*].code");
	}

	private static LocalDateTime utc(Instant instant) {
		return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
	}

	// ───── Creation ─────

	@Test
	void createReturnsTheDetailWithCodeStatusAndFirstHistoryEntry() throws Exception {
		HttpResponse<String> response = create(STUDENT, ELECTRICITY, VALID, HIGH);

		assertThat(response.statusCode()).as(response.body()).isEqualTo(201);
		assertThat(response.headers().firstValue("Content-Type")).hasValueSatisfying(
				type -> assertThat(type).startsWith("application/json"));
		String code = ApiClient.read(response, "$.code", String.class);
		assertThat(code).matches("SOL-\\d{4}-000001");
		assertThat(response.headers().firstValue("Location")).hasValueSatisfying(
				location -> assertThat(location).endsWith("/api/v1/requests/" + code));
		assertThat(ApiClient.<String>read(response, "$.status")).isEqualTo("REGISTRADA");
		assertThat(ApiClient.<String>read(response, "$.categoryName")).isEqualTo("Electricidad");
		assertThat(ApiClient.<String>read(response, "$.priorityName")).isEqualTo("Alta");
		assertThat(ApiClient.<String>read(response, "$.locationName")).contains("Facultad de Ingeniería")
				.contains("Pabellón Principal").contains("Aula 101");
		assertThat(ApiClient.<String>read(response, "$.requester.fullName")).isEqualTo("Estudiante Demo");
		assertThat(ApiClient.<Object>read(response, "$.technician")).isNull();
		assertThat(ApiClient.<Boolean>read(response, "$.overdue")).isFalse();
		assertThat(ApiClient.<List<Object>>read(response, "$.history")).hasSize(1);
		assertThat(ApiClient.<Object>read(response, "$.history[0].fromStatus")).isNull();
		assertThat(ApiClient.<String>read(response, "$.history[0].toStatus")).isEqualTo("REGISTRADA");
		assertThat(ApiClient.<String>read(response, "$.history[0].actorName")).isEqualTo("Estudiante Demo");
		assertThat(ApiClient.<List<Object>>read(response, "$.allowedTransitions")).isEmpty();
	}

	@Test
	void createExposesTheSummaryFieldsTheBandejasNeed() throws Exception {
		HttpResponse<String> response = create(STUDENT, ELECTRICITY, VALID, HIGH);

		assertThat(ApiClient.<String>read(response, "$.description")).isEqualTo(VALID);
		assertThat(ApiClient.<String>read(response, "$.requesterName")).isEqualTo("Estudiante Demo");
		assertThat(ApiClient.<String>read(response, "$.areaName")).isEqualTo("Mantenimiento");
		assertThat(ApiClient.<Object>read(response, "$.technicianName")).isNull();
		assertThat(ApiClient.<String>read(response, "$.facultyName")).startsWith("Facultad de Ingeniería");
		assertThat(ApiClient.<String>read(response, "$.pavilionName")).isEqualTo("Pabellón Principal");
		assertThat(ApiClient.<String>read(response, "$.roomName")).isEqualTo("Aula 101");
	}

	@Test
	void dueDateIsRegistrationPlusTheSlaHoursOfThePriority() throws Exception {
		long[][] priorityAndHours = { { 1, 8 }, { 2, 24 }, { 3, 72 }, { 4, 120 } };
		for (long[] entry : priorityAndHours) {
			HttpResponse<String> response = create(STUDENT, ELECTRICITY, VALID, entry[0]);
			Instant created = Instant.parse(ApiClient.read(response, "$.createdAt", String.class));
			Instant due = Instant.parse(ApiClient.read(response, "$.dueAt", String.class));
			assertThat(Duration.between(created, due)).as("priority " + entry[0]).isEqualTo(Duration.ofHours(entry[1]));
		}
	}

	@Test
	void theAreaIsAlwaysTheDefaultAreaOfTheCategory() throws Exception {
		assertThat(ApiClient.<Integer>read(create(STUDENT, ELECTRICITY, VALID, HIGH), "$.area.id")).isEqualTo(1);
		assertThat(ApiClient.<Integer>read(create(STUDENT, NETWORK, VALID, HIGH), "$.area.id")).isEqualTo(3);
		assertThat(ApiClient.<String>read(create(STUDENT, NETWORK, VALID, HIGH), "$.area.name"))
				.isEqualTo("Soporte Informático");
	}

	@Test
	void descriptionLengthIsValidated() throws Exception {
		HttpResponse<String> tooShort = create(STUDENT, ELECTRICITY, "x".repeat(19), HIGH);
		ApiClient.assertProblem(tooShort, 400);
		assertThat(ApiClient.<String>read(tooShort, "$.errors.description")).contains("20").contains("2000");

		assertThat(create(STUDENT, ELECTRICITY, "x".repeat(20), HIGH).statusCode()).isEqualTo(201);
		assertThat(create(STUDENT, ELECTRICITY, "x".repeat(2000), HIGH).statusCode()).isEqualTo(201);

		ApiClient.assertProblem(create(STUDENT, ELECTRICITY, "x".repeat(2001), HIGH), 400);
	}

	@Test
	void surroundingWhitespaceDoesNotCountAsDescription() throws Exception {
		ApiClient.assertProblem(create(STUDENT, ELECTRICITY, "x".repeat(10) + " ".repeat(30), HIGH), 400);
	}

	@Test
	void invalidReferencesAndMissingFieldsAreRejectedWithFieldErrors() throws Exception {
		HttpResponse<String> badCategory = create(STUDENT, 999, VALID, HIGH);
		ApiClient.assertProblem(badCategory, 400);
		assertThat(ApiClient.<String>read(badCategory, "$.errors.categoryId")).isNotBlank();

		HttpResponse<String> facultyAsLocation = api.post("/api/v1/requests",
				body(ELECTRICITY, FACULTY, VALID, HIGH), api.login(STUDENT));
		ApiClient.assertProblem(facultyAsLocation, 400);
		assertThat(ApiClient.<String>read(facultyAsLocation, "$.errors.locationId")).isNotBlank();

		HttpResponse<String> badPriority = create(STUDENT, ELECTRICITY, VALID, 999);
		assertThat(ApiClient.<String>read(badPriority, "$.errors.priorityId")).isNotBlank();

		HttpResponse<String> empty = api.post("/api/v1/requests", "{}", api.login(STUDENT));
		ApiClient.assertProblem(empty, 400);
		assertThat(ApiClient.<String>read(empty, "$.errors.categoryId")).isNotBlank();
		assertThat(ApiClient.<String>read(empty, "$.errors.description")).isNotBlank();
		assertThat(jdbc.queryForObject("select count(*) from service_requests", Integer.class)).isZero();
	}

	@Test
	void createIgnoresFieldsOfOtherModulesTheClientAlreadySends() throws Exception {
		// The UI sends the attachment metadata too; storing evidences belongs to the collaboration module.
		String json = "{\"categoryId\":1,\"locationId\":3,\"description\":\"" + VALID + "\",\"priorityId\":2,"
				+ "\"evidences\":[{\"fileName\":\"foto.jpg\",\"sizeBytes\":1024}]}";

		HttpResponse<String> response = api.post("/api/v1/requests", json, api.login(STUDENT));

		assertThat(response.statusCode()).as(response.body()).isEqualTo(201);
	}

	@Test
	void onlyRequestersCanCreateRequests() throws Exception {
		ApiClient.assertProblem(create(SUPERVISOR, ELECTRICITY, VALID, HIGH), 403);
		ApiClient.assertProblem(create(TECHNICIAN, ELECTRICITY, VALID, HIGH), 403);
		ApiClient.assertProblem(create(ADMIN, ELECTRICITY, VALID, HIGH), 403);
		ApiClient.assertProblem(api.post("/api/v1/requests", body(ELECTRICITY, ROOM, VALID, HIGH), null), 401);
	}

	@Test
	void persistsTheRequestAndItsHistory() throws Exception {
		String code = createCode(STUDENT, ELECTRICITY);

		assertThat(jdbc.queryForObject("select status from service_requests where code = ?", String.class, code))
				.isEqualTo("REGISTRADA");
		assertThat(jdbc.queryForObject("select count(*) from request_history h join service_requests r "
				+ "on r.id = h.request_id where r.code = ? and h.from_status is null and h.to_status = 'REGISTRADA'",
				Integer.class, code)).isEqualTo(1);
	}

	// ───── Code sequence ─────

	@Test
	void tenParallelCreationsGetTenDistinctConsecutiveCodes() throws Exception {
		String token = api.login(STUDENT);
		int total = 10;
		ExecutorService pool = Executors.newFixedThreadPool(total);
		CountDownLatch start = new CountDownLatch(1);
		List<Future<String>> futures = new ArrayList<>();
		try {
			for (int i = 0; i < total; i++) {
				Callable<String> task = () -> {
					start.await();
					HttpResponse<String> response = api.post("/api/v1/requests",
							body(ELECTRICITY, ROOM, VALID, HIGH), token);
					assertThat(response.statusCode()).as(response.body()).isEqualTo(201);
					return ApiClient.read(response, "$.code", String.class);
				};
				futures.add(pool.submit(task));
			}
			start.countDown();
			List<String> created = new ArrayList<>();
			for (Future<String> future : futures) {
				created.add(future.get());
			}

			assertThat(created).doesNotHaveDuplicates().allMatch(code -> CODE.matcher(code).matches());
			List<Integer> sequences = created.stream().map(code -> Integer.parseInt(code.substring(code.length() - 6)))
					.sorted().toList();
			assertThat(sequences).containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
		}
		finally {
			pool.shutdownNow();
		}
	}

	@Test
	void theSequenceRestartsEachYear() {
		assertThat(nextCode(2031)).isEqualTo("SOL-2031-000001");
		assertThat(nextCode(2031)).isEqualTo("SOL-2031-000002");
		assertThat(nextCode(2032)).isEqualTo("SOL-2032-000001");
		assertThat(nextCode(2031)).isEqualTo("SOL-2031-000003");
	}

	/** The generator must run inside a transaction, as it does when a request is created. */
	private String nextCode(int year) {
		String code = new TransactionTemplate(transactionManager).execute(status -> codes.next(year).value());
		return code;
	}

	// ───── Visibility ─────

	@Test
	void detailRespectsVisibilityByRole() throws Exception {
		String code = createCode(STUDENT, ELECTRICITY); // area 1
		jdbc.update("update service_requests set technician_id = 3 where code = ?", code);
		String url = "/api/v1/requests/" + code;

		assertThat(api.get(url, api.login(STUDENT)).statusCode()).as("owner").isEqualTo(200);
		assertThat(api.get(url, api.login(SUPERVISOR)).statusCode()).as("supervisor of the area").isEqualTo(200);
		assertThat(api.get(url, api.login(TECHNICIAN)).statusCode()).as("assigned technician").isEqualTo(200);
		assertThat(api.get(url, api.login(ADMIN)).statusCode()).as("administrator").isEqualTo(200);

		ApiClient.assertProblem(api.get(url, api.login(OTHER_STUDENT)), 404);
		ApiClient.assertProblem(api.get(url, api.login(SUPPORT_SUPERVISOR)), 404);
		ApiClient.assertProblem(api.get(url, api.login(SUPPORT_TECHNICIAN)), 404);
	}

	@Test
	void aTechnicianOfTheAreaThatIsNotAssignedCannotSeeTheRequest() throws Exception {
		String code = createCode(STUDENT, ELECTRICITY);

		ApiClient.assertProblem(api.get("/api/v1/requests/" + code, api.login(TECHNICIAN)), 404);
	}

	@Test
	void unknownCodeAndMissingTokenAreHandled() throws Exception {
		ApiClient.assertProblem(api.get("/api/v1/requests/SOL-2026-999999", api.login(ADMIN)), 404);
		ApiClient.assertProblem(api.get("/api/v1/requests/SOL-2026-000001", null), 401);
		ApiClient.assertProblem(api.get("/api/v1/requests", null), 401);
	}

	@Test
	void listShowsOnlyWhatEachRoleCanSee() throws Exception {
		String r1 = createCode(STUDENT, ELECTRICITY); // area 1, will be assigned to technician 3
		String r2 = createCode(STUDENT, NETWORK); // area 3
		String r3 = createCode(OTHER_STUDENT, ELECTRICITY); // area 1
		jdbc.update("update service_requests set technician_id = 3 where code = ?", r1);

		assertThat(listedCodes(STUDENT, "")).containsExactlyInAnyOrder(r1, r2);
		assertThat(listedCodes(OTHER_STUDENT, "")).containsExactly(r3);
		assertThat(listedCodes(SUPERVISOR, "")).containsExactlyInAnyOrder(r1, r3);
		assertThat(listedCodes(SUPPORT_SUPERVISOR, "")).containsExactly(r2);
		assertThat(listedCodes(TECHNICIAN, "")).containsExactly(r1);
		assertThat(listedCodes(SUPPORT_TECHNICIAN, "")).isEmpty();
		assertThat(listedCodes(ADMIN, "")).containsExactlyInAnyOrder(r1, r2, r3);
	}

	@Test
	void listItemsFollowTheContractSummaryShape() throws Exception {
		createCode(STUDENT, ELECTRICITY);

		HttpResponse<String> response = api.get("/api/v1/requests", api.login(STUDENT));

		assertThat(ApiClient.<Integer>read(response, "$.page")).isZero();
		assertThat(ApiClient.<Integer>read(response, "$.size")).isEqualTo(20);
		assertThat(ApiClient.<Integer>read(response, "$.total")).isEqualTo(1);
		assertThat(ApiClient.<String>read(response, "$.items[0].categoryName")).isEqualTo("Electricidad");
		assertThat(ApiClient.<String>read(response, "$.items[0].status")).isEqualTo("REGISTRADA");
		assertThat(ApiClient.<Boolean>read(response, "$.items[0].overdue")).isFalse();
		assertThat(ApiClient.<String>read(response, "$.items[0].locationName")).contains("Aula 101");
	}

	// ───── Overdue ─────

	@Test
	void anOpenRequestPastItsDueDateIsOverdueButAResolvedOneIsNot() throws Exception {
		String pending = createCode(STUDENT, ELECTRICITY);
		String resolved = createCode(STUDENT, ELECTRICITY);
		String closed = createCode(STUDENT, ELECTRICITY);
		String onTime = createCode(STUDENT, ELECTRICITY);
		LocalDateTime past = utc(Instant.now().minus(Duration.ofHours(5)));
		jdbc.update("update service_requests set due_at = ? where code in (?, ?, ?)", past, pending, resolved, closed);
		jdbc.update("update service_requests set status = 'RESUELTA' where code = ?", resolved);
		jdbc.update("update service_requests set status = 'CERRADA', closure_reason = 'CONFIRMADA' where code = ?",
				closed);
		String token = api.login(STUDENT);

		assertThat(ApiClient.<Boolean>read(api.get("/api/v1/requests/" + pending, token), "$.overdue")).isTrue();
		assertThat(ApiClient.<Boolean>read(api.get("/api/v1/requests/" + resolved, token), "$.overdue")).isFalse();
		assertThat(ApiClient.<Boolean>read(api.get("/api/v1/requests/" + closed, token), "$.overdue")).isFalse();
		assertThat(ApiClient.<Boolean>read(api.get("/api/v1/requests/" + onTime, token), "$.overdue")).isFalse();

		HttpResponse<String> list = api.get("/api/v1/requests", token);
		assertThat(ApiClient.<List<String>>read(list, "$.items[?(@.overdue==true)].code")).containsExactly(pending);
	}

	// ───── Filters, paging and order ─────

	@Test
	void listCanBeFilteredByStatus() throws Exception {
		String a = createCode(STUDENT, ELECTRICITY);
		createCode(STUDENT, ELECTRICITY);
		jdbc.update("update service_requests set status = 'EN_EVALUACION' where code = ?", a);

		assertThat(listedCodes(STUDENT, "?status=EN_EVALUACION")).containsExactly(a);
		assertThat(listedCodes(STUDENT, "?status=ASIGNADA")).isEmpty();
		ApiClient.assertProblem(api.get("/api/v1/requests?status=INEXISTENTE", api.login(STUDENT)), 400);
	}

	@Test
	void listIsPagedAndNewestFirst() throws Exception {
		String first = createCode(STUDENT, ELECTRICITY);
		String second = createCode(STUDENT, ELECTRICITY);
		String third = createCode(STUDENT, ELECTRICITY);

		assertThat(listedCodes(STUDENT, "")).containsExactly(third, second, first);
		assertThat(listedCodes(STUDENT, "?page=0&size=2")).containsExactly(third, second);
		assertThat(listedCodes(STUDENT, "?page=1&size=2")).containsExactly(first);
		assertThat(listedCodes(STUDENT, "?page=2&size=2")).isEmpty();
		HttpResponse<String> paged = api.get("/api/v1/requests?page=1&size=2", api.login(STUDENT));
		assertThat(ApiClient.<Integer>read(paged, "$.total")).isEqualTo(3);
		assertThat(ApiClient.<Integer>read(paged, "$.size")).isEqualTo(2);
	}

	@Test
	void sizeIsCappedAndNegativePageIsTreatedAsFirst() throws Exception {
		createCode(STUDENT, ELECTRICITY);

		HttpResponse<String> response = api.get("/api/v1/requests?page=-4&size=100000", api.login(STUDENT));

		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(ApiClient.<Integer>read(response, "$.page")).isZero();
		assertThat(ApiClient.<Integer>read(response, "$.size")).isEqualTo(500);
	}

	@Test
	void historyOfTheDetailIsOrderedByTime() throws Exception {
		String code = createCode(STUDENT, ELECTRICITY);
		Long id = jdbc.queryForObject("select id from service_requests where code = ?", Long.class, code);
		jdbc.update("insert into request_history (request_id, from_status, to_status, actor_id, reason, created_at) "
				+ "values (?, 'REGISTRADA', 'EN_EVALUACION', 2, 'Evaluando', utc_timestamp(6) + interval 1 minute)", id);

		HttpResponse<String> response = api.get("/api/v1/requests/" + code, api.login(SUPERVISOR));

		assertThat(ApiClient.<List<String>>read(response, "$.history[*].toStatus"))
				.containsExactly("REGISTRADA", "EN_EVALUACION");
		assertThat(ApiClient.<String>read(response, "$.history[1].actorName")).isEqualTo("Supervisora Mantenimiento");
		assertThat(ApiClient.<String>read(response, "$.history[1].reason")).isEqualTo("Evaluando");
	}

}
