package pe.edu.unmsm.servicios.satisfaction.infrastructure.persistence;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import pe.edu.unmsm.servicios.satisfaction.application.ResolvedRequestStore;

@Repository
public class JdbcResolvedRequestStore implements ResolvedRequestStore {

	private final JdbcTemplate jdbc;

	public JdbcResolvedRequestStore(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public List<Long> findResolvedWithoutRatingOlderThan(int hours, Instant now) {
		Instant cutoff = now.minus(hours, ChronoUnit.HOURS);
		String sql = """
				SELECT r.id FROM service_requests r
				LEFT JOIN ratings rt ON rt.request_id = r.id
				WHERE r.status = 'RESUELTA'
				  AND r.updated_at <= ?
				  AND rt.id IS NULL
				""";
		return jdbc.queryForList(sql, Long.class, Timestamp.from(cutoff));
	}

	@Override
	public void closeWithoutRating(long requestId, Instant now) {
		Timestamp ts = Timestamp.from(now);
		jdbc.update("""
				UPDATE service_requests
				SET status = 'CERRADA', closure_reason = 'CONFIRMADA', updated_at = ?
				WHERE id = ? AND status = 'RESUELTA'
				""", ts, requestId);
		jdbc.update("""
				INSERT INTO request_history (request_id, from_status, to_status, actor_id, reason, created_at)
				VALUES (?, 'RESUELTA', 'CERRADA', NULL, 'Cierre automático por inactividad', ?)
				""", requestId, ts);
	}

}
