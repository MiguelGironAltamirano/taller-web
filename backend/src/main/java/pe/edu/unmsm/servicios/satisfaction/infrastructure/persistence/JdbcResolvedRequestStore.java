package pe.edu.unmsm.servicios.satisfaction.infrastructure.persistence;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.unmsm.servicios.satisfaction.application.ResolvedRequestStore;

@Repository
class JdbcResolvedRequestStore implements ResolvedRequestStore {

	static final String HISTORY_REASON = "Cierre automático: sin respuesta del solicitante en 72 horas";

	private final JdbcTemplate jdbc;

	JdbcResolvedRequestStore(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	/**
	 * The clock starts at the latest move to Resuelta, not at updated_at: a solution that was rejected and resolved
	 * again gets its full 72 hours.
	 */
	@Override
	public List<ResolvedRequest> findResolvedBefore(Instant resolvedBefore) {
		return jdbc.query("""
				select r.id, r.code, r.requester_id
				  from service_requests r
				 where r.status = 'RESUELTA'
				   and (select max(h.created_at) from request_history h
				         where h.request_id = r.id and h.to_status = 'RESUELTA') <= ?
				 order by r.id
				""", (rs, row) -> new ResolvedRequest(rs.getLong("id"), rs.getString("code"),
				rs.getLong("requester_id")), utc(resolvedBefore));
	}

	/**
	 * The status guard makes the update a no-op if the requester answered after the request was selected; the
	 * version bump keeps the optimistic lock of the JPA entity consistent.
	 */
	@Override
	@Transactional
	public boolean closeAutomatically(long requestId, Instant now) {
		LocalDateTime at = utc(now);
		int updated = jdbc.update("""
				update service_requests
				   set status = 'CERRADA', closure_reason = 'AUTOMATICA', updated_at = ?, version = version + 1
				 where id = ? and status = 'RESUELTA'
				""", at, requestId);
		if (updated == 0) {
			return false;
		}
		jdbc.update("""
				insert into request_history (request_id, from_status, to_status, actor_id, technician_id, reason,
				                             created_at)
				values (?, 'RESUELTA', 'CERRADA', null, null, ?, ?)
				""", requestId, HISTORY_REASON, at);
		return true;
	}

	private static LocalDateTime utc(Instant instant) {
		return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
	}

}
