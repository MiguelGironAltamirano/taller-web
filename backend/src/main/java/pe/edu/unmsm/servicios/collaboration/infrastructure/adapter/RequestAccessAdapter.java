package pe.edu.unmsm.servicios.collaboration.infrastructure.adapter;

import java.time.Clock;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import pe.edu.unmsm.servicios.collaboration.application.RequestAccess;
import pe.edu.unmsm.servicios.collaboration.application.RequestSnapshot;
import pe.edu.unmsm.servicios.requests.application.RequestDetailView;
import pe.edu.unmsm.servicios.requests.application.RequestQueries;
import pe.edu.unmsm.servicios.requests.domain.RequestNotFoundException;
import pe.edu.unmsm.servicios.shared.domain.AccessPolicy;
import pe.edu.unmsm.servicios.shared.domain.AuthenticatedUser;

@Component
public class RequestAccessAdapter implements RequestAccess {

	private final JdbcTemplate jdbc;

	public RequestAccessAdapter(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public RequestSnapshot findVisibleOrThrow(AuthenticatedUser user, String code) {
		String sql = "SELECT id, code, requester_id, area_id, technician_id FROM service_requests WHERE code = ?";
		List<RequestSnapshot> list = jdbc.query(sql, (rs, rowNum) -> new RequestSnapshot(
				rs.getLong("id"),
				rs.getString("code"),
				rs.getLong("requester_id"),
				rs.getLong("area_id"),
				rs.getObject("technician_id", Long.class)
		), code);

		if (list.isEmpty()) {
			throw new RequestNotFoundException(code);
		}

		RequestSnapshot snapshot = list.get(0);
		if (!AccessPolicy.canViewRequest(user, snapshot.requesterId(), snapshot.areaId(), snapshot.technicianId())) {
			throw new RequestNotFoundException(code);
		}
		return snapshot;
	}

	@Override
	public List<Long> findSupervisorIds(long areaId) {
		String sql = "SELECT id FROM users WHERE role = 'SUPERVISOR' AND area_id = ? AND active = true";
		return jdbc.queryForList(sql, Long.class, areaId);
	}

}
