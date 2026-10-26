package pe.edu.unmsm.servicios;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.transaction.support.TransactionTemplate;

import pe.edu.unmsm.servicios.requests.application.RequestCodeGenerator;

/**
 * The opt-in demo data (db/seed-demo) loads after every schema migration and tells a coherent story: each request
 * follows the state machine of the workflow module, deadlines follow the SLA, codes continue the sequence and the
 * dashboards always have recent activity to show.
 */
@SpringBootTest(properties = "spring.flyway.locations=classpath:db/migration,classpath:db/seed-dev,classpath:db/seed-demo")
@Import(TestcontainersConfiguration.class)
class DemoSeedTests {

	private static final String DEMO_SCRIPT = "db/seed-demo/R__demo_data.sql";

	/** Status changes of the spec table (section 6); ASIGNADA → ASIGNADA is a reassignment. */
	private static final Set<String> TRANSITIONS = Set.of("REGISTRADA>EN_EVALUACION", "REGISTRADA>CERRADA",
			"EN_EVALUACION>ASIGNADA", "EN_EVALUACION>CERRADA", "ASIGNADA>ASIGNADA", "ASIGNADA>EN_ATENCION",
			"EN_ATENCION>RESUELTA", "RESUELTA>CERRADA", "RESUELTA>EN_ATENCION");

	@Autowired
	JdbcTemplate jdbc;

	@Autowired
	DataSource dataSource;

	@Autowired
	RequestCodeGenerator codes;

	@Autowired
	TransactionTemplate transaction;

	@Test
	void seedsRequestsInEveryStatusAndArea() {
		assertThat(count("select count(*) from service_requests")).isBetween(30, 60);
		assertThat(jdbc.queryForList("select distinct status from service_requests", String.class))
				.containsExactlyInAnyOrder("REGISTRADA", "EN_EVALUACION", "ASIGNADA", "EN_ATENCION", "RESUELTA",
						"CERRADA");
		assertThat(jdbc.queryForList("select distinct closure_reason from service_requests where status = 'CERRADA'",
				String.class)).containsExactlyInAnyOrder("CONFIRMADA", "NO_PROCEDE", "DUPLICADA", "AUTOMATICA");
		assertThat(count("select count(distinct area_id) from service_requests")).isEqualTo(4);
		assertThat(count("select count(distinct requester_id) from service_requests")).isGreaterThanOrEqualTo(4);
	}

	@Test
	void everyHistoryFollowsTheStateMachineAndEndsInTheCurrentStatus() {
		Map<Long, List<Map<String, Object>>> byRequest = new LinkedHashMap<>();
		jdbc.queryForList("select request_id, from_status, to_status, note, created_at from request_history"
				+ " order by request_id, created_at, id")
			.forEach(row -> byRequest.computeIfAbsent(((Number) row.get("request_id")).longValue(),
					id -> new ArrayList<>()).add(row));
		Map<Long, String> statusById = new LinkedHashMap<>();
		jdbc.queryForList("select id, status from service_requests")
			.forEach(row -> statusById.put(((Number) row.get("id")).longValue(), (String) row.get("status")));
		LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

		assertThat(byRequest.keySet()).containsExactlyInAnyOrderElementsOf(statusById.keySet());
		byRequest.forEach((id, entries) -> {
			assertThat(entries.getFirst().get("from_status")).as("registration of %d", id).isNull();
			assertThat(entries.getFirst().get("to_status")).isEqualTo("REGISTRADA");
			for (int i = 1; i < entries.size(); i++) {
				Map<String, Object> previous = entries.get(i - 1);
				Map<String, Object> entry = entries.get(i);
				assertThat(entry.get("from_status")).as("chain of %d", id).isEqualTo(previous.get("to_status"));
				String change = entry.get("from_status") + ">" + entry.get("to_status");
				if (entry.get("note") == null) {
					assertThat(TRANSITIONS).as("transition of %d", id).contains(change);
				}
				assertThat((LocalDateTime) entry.get("created_at")).isAfter((LocalDateTime) previous.get("created_at"));
			}
			assertThat(entries.getLast().get("to_status")).as("status of %d", id).isEqualTo(statusById.get(id));
			assertThat((LocalDateTime) entries.getLast().get("created_at")).isBefore(now);
		});
	}

	@Test
	void deadlinesFollowThePrioritySla() {
		assertThat(count("""
				select count(*) from service_requests r join priorities p on p.id = r.priority_id
				 where r.due_at <> r.created_at + interval p.sla_hours hour""")).isZero();
		assertThat(count("select count(*) from service_requests where status not in ('RESUELTA', 'CERRADA')"
				+ " and due_at < utc_timestamp(6)")).as("overdue").isPositive();
		assertThat(count("select count(*) from service_requests where status not in ('RESUELTA', 'CERRADA')"
				+ " and due_at between utc_timestamp(6) and utc_timestamp(6) + interval 24 hour")).as("due soon")
			.isPositive();
	}

	@Test
	void closuresSolutionsAndRatingsAreConsistent() {
		assertThat(count("select count(*) from service_requests where (status = 'CERRADA') <> (closure_reason is not null)"))
			.isZero();
		assertThat(count("""
				select count(*) from service_requests
				 where (status = 'RESUELTA' or closure_reason in ('CONFIRMADA', 'AUTOMATICA')) and solution is null"""))
			.isZero();
		assertThat(count("select count(*) from service_requests where status <> 'CERRADA' and status <> 'RESUELTA'"
				+ " and solution is not null")).as("solution only once resolved").isZero();
		assertThat(count("""
				select count(*) from request_ratings rt join service_requests r on r.id = rt.request_id
				 where r.closure_reason <> 'CONFIRMADA' or rt.requester_id <> r.requester_id"""))
			.isZero();
		assertThat(count("select count(*) from request_ratings")).isPositive();
	}

	@Test
	void resolvedRequestsAreStillInsideTheConfirmationWindow() {
		assertThat(count("""
				select count(*) from service_requests r join system_settings s on s.id = 1
				 where r.status = 'RESUELTA'
				   and (select max(h.created_at) from request_history h
				         where h.request_id = r.id and h.to_status = 'RESUELTA')
				       < utc_timestamp(6) - interval s.auto_close_hours hour""")).isZero();
	}

	@Test
	void codesContinueTheSequenceSoNewRequestsNeverCollide() {
		List<String> all = jdbc.queryForList("select code from service_requests", String.class);
		assertThat(all).allMatch(code -> code.matches("SOL-\\d{4}-\\d{6}")).doesNotHaveDuplicates();

		String next = transaction.execute(status -> codes.next(LocalDateTime.now(ZoneOffset.ofHours(-5)).getYear())
			.value());

		assertThat(all).doesNotContain(next);
	}

	@Test
	void dashboardsNotificationsAndTriageHaveData() {
		assertThat(count("select count(*) from service_requests where created_at >= utc_timestamp(6) - interval 7 day"))
			.isGreaterThanOrEqualTo(10);
		assertThat(count("select count(*) from service_requests where created_at < utc_timestamp(6) - interval 30 day"))
			.isGreaterThanOrEqualTo(5);
		assertThat(count("select count(*) from notifications where recipient_id = 4 and read_at is null")).isPositive();
		assertThat(count("select count(*) from notifications where recipient_id = 4 and read_at is not null"))
			.isPositive();
		assertThat(count("select count(*) from notifications where text like '%DEMO-%'")).isZero();
		assertThat(count("select count(*) from triage_rules where active")).isPositive();
		assertThat(count("select count(*) from triage_rule_keywords")).isPositive();
		assertThat(count("select count(*) from comments")).isPositive();
	}

	@Test
	void runningTheScriptAgainChangesNothing() throws Exception {
		List<Map<String, Object>> before = snapshot();

		try (Connection connection = dataSource.getConnection()) {
			ScriptUtils.executeSqlScript(connection, new ClassPathResource(DEMO_SCRIPT));
		}

		assertThat(snapshot()).isEqualTo(before);
	}

	private List<Map<String, Object>> snapshot() {
		return jdbc.queryForList("""
				select (select count(*) from users) as users, (select count(*) from service_requests) as requests,
				       (select count(*) from request_history) as history, (select count(*) from comments) as comments,
				       (select count(*) from notifications) as notifications,
				       (select count(*) from request_ratings) as ratings,
				       (select count(*) from triage_rule_keywords) as keywords,
				       (select group_concat(code order by id) from service_requests) as codes""");
	}

	private int count(String sql) {
		Integer value = jdbc.queryForObject(sql, Integer.class);
		return value == null ? 0 : value;
	}

}
