package pe.edu.unmsm.servicios.requests.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import pe.edu.unmsm.servicios.TestcontainersConfiguration;
import pe.edu.unmsm.servicios.requests.application.RequestAnalyticsQuery;
import pe.edu.unmsm.servicios.requests.application.RequestAnalyticsRow;
import pe.edu.unmsm.servicios.requests.domain.RequestStatus;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT,
		properties = "spring.flyway.locations=classpath:db/migration,classpath:db/seed-dev")
@Import(TestcontainersConfiguration.class)
class RequestAnalyticsQueryTests {

	// Seeded by seed-dev: requester 4, technician 3 (area 1), actor 2 (supervisor of area 1).
	private static final Instant T0 = Instant.parse("2026-10-01T12:00:00Z");

	@Autowired
	RequestAnalyticsQuery analytics;

	@Autowired
	JdbcTemplate jdbc;

	@BeforeEach
	void cleanRequests() {
		jdbc.update("delete from request_history");
		jdbc.update("delete from service_requests");
	}

	@Test
	void returnsTheRequestsRegisteredInTheWindowWithTheirNames() {
		long id = insert("SOL-2026-000001", 1, 1, 2, "ASIGNADA", 3L, T0, T0.plusSeconds(24 * 3600));
		insert("SOL-2026-000002", 1, 1, 2, "REGISTRADA", null, T0.minusSeconds(1), T0.plusSeconds(3600));
		insert("SOL-2026-000003", 1, 1, 2, "REGISTRADA", null, T0.plusSeconds(3600), T0.plusSeconds(7200));
		history(id, "REGISTRADA", T0);

		List<RequestAnalyticsRow> rows = analytics.registeredBetween(null, T0, T0.plusSeconds(3600));

		assertThat(rows).hasSize(1);
		RequestAnalyticsRow row = rows.getFirst();
		assertThat(row.code()).isEqualTo("SOL-2026-000001");
		assertThat(row.status()).isEqualTo(RequestStatus.ASIGNADA);
		assertThat(row.categoryName()).isEqualTo("Electricidad");
		assertThat(row.priorityName()).isEqualTo("Alta");
		assertThat(row.areaId()).isEqualTo(1);
		assertThat(row.technicianId()).isEqualTo(3L);
		assertThat(row.technicianName()).isEqualTo("Técnico Mantenimiento");
		assertThat(row.locationName()).isEqualTo(
				"Facultad de Ingeniería de Sistemas e Informática → Pabellón Principal → Aula 101");
		assertThat(row.createdAt()).isEqualTo(T0);
		assertThat(row.dueAt()).isEqualTo(T0.plusSeconds(24 * 3600));
		assertThat(row.firstResolvedAt()).isNull();
	}

	@Test
	void filtersByAreaWhenOneIsGiven() {
		insert("SOL-2026-000001", 1, 1, 2, "REGISTRADA", null, T0, T0.plusSeconds(3600));
		insert("SOL-2026-000002", 5, 3, 2, "REGISTRADA", null, T0, T0.plusSeconds(3600));

		assertThat(analytics.registeredBetween(3L, T0, T0.plusSeconds(60))).extracting(RequestAnalyticsRow::code)
				.containsExactly("SOL-2026-000002");
		assertThat(analytics.registeredBetween(null, T0, T0.plusSeconds(60))).hasSize(2);
	}

	@Test
	void theFirstResolutionComesFromTheHistoryEvenAfterAReopening() {
		long id = insert("SOL-2026-000001", 1, 1, 2, "CERRADA", 3L, T0, T0.plusSeconds(24 * 3600));
		history(id, "REGISTRADA", T0);
		history(id, "RESUELTA", T0.plusSeconds(5 * 3600));
		history(id, "EN_ATENCION", T0.plusSeconds(6 * 3600));
		history(id, "RESUELTA", T0.plusSeconds(9 * 3600));
		history(id, "CERRADA", T0.plusSeconds(10 * 3600));

		RequestAnalyticsRow row = analytics.registeredBetween(null, T0, T0.plusSeconds(1)).getFirst();

		assertThat(row.firstResolvedAt()).isEqualTo(T0.plusSeconds(5 * 3600));
		assertThat(row.wasAttended()).isTrue();
		assertThat(row.isOpen()).isFalse();
	}

	@Test
	void openRequestsExcludeResolvedAndClosedOnesAndKnowWhenTheyAreOverdue() {
		insert("SOL-2026-000001", 1, 1, 2, "EN_ATENCION", 3L, T0, T0.plusSeconds(3600));
		insert("SOL-2026-000002", 1, 1, 2, "RESUELTA", 3L, T0, T0.plusSeconds(3600));
		insert("SOL-2026-000003", 1, 1, 2, "CERRADA", null, T0, T0.plusSeconds(3600));
		insert("SOL-2026-000004", 5, 3, 2, "ASIGNADA", null, T0, T0.plusSeconds(3600));

		List<RequestAnalyticsRow> open = analytics.openRequests(1L);

		assertThat(open).extracting(RequestAnalyticsRow::code).containsExactly("SOL-2026-000001");
		assertThat(open.getFirst().isOverdue(T0.plusSeconds(7200))).isTrue();
		assertThat(open.getFirst().isOverdue(T0)).isFalse();
		assertThat(analytics.openRequests(null)).hasSize(2);
	}

	private long insert(String code, long categoryId, long areaId, long priorityId, String status, Long technicianId,
			Instant createdAt, Instant dueAt) {
		jdbc.update("""
				insert into service_requests (code, requester_id, category_id, location_id, description, priority_id,
				  status, area_id, technician_id, created_at, due_at, updated_at)
				values (?, 4, ?, 3, 'Descripción de prueba', ?, ?, ?, ?, ?, ?, ?)
				""", code, categoryId, priorityId, status, areaId, technicianId, utc(createdAt), utc(dueAt),
				utc(createdAt));
		return jdbc.queryForObject("select id from service_requests where code = ?", Long.class, code);
	}

	private void history(long requestId, String toStatus, Instant at) {
		jdbc.update("insert into request_history (request_id, to_status, actor_id, created_at) values (?, ?, 2, ?)",
				requestId, toStatus, utc(at));
	}

	/** Timestamps are stored as UTC wall-clock values. */
	private static LocalDateTime utc(Instant instant) {
		return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
	}

}
