package pe.edu.unmsm.servicios.requests.infrastructure.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import pe.edu.unmsm.servicios.requests.application.HistoryView;
import pe.edu.unmsm.servicios.requests.application.PageView;
import pe.edu.unmsm.servicios.requests.application.RequestDetailView;
import pe.edu.unmsm.servicios.requests.application.RequestQueries;
import pe.edu.unmsm.servicios.requests.application.RequestScope;
import pe.edu.unmsm.servicios.requests.application.RequestSummaryView;
import pe.edu.unmsm.servicios.requests.application.TechnicianWorkload;
import pe.edu.unmsm.servicios.requests.domain.ClosureReason;
import pe.edu.unmsm.servicios.requests.domain.RequestStatus;
import pe.edu.unmsm.servicios.requests.domain.Sla;

/** Read model: one joined query per list page or detail, instead of loading entities and their relations. */
@Repository
class JdbcRequestQueries implements RequestQueries, TechnicianWorkload {

	private static final String SELECT = """
			select r.id, r.code, r.status, r.created_at, r.due_at, r.description, r.requester_id, r.area_id,
			       r.technician_id, r.closure_reason,
			       c.name as category_name, p.name as priority_name,
			       room.name as room_name, pav.name as pavilion_name, fac.name as faculty_name,
			       u.full_name as requester_name, a.name as area_name, t.full_name as technician_name
			  from service_requests r
			  join categories c on c.id = r.category_id
			  join priorities p on p.id = r.priority_id
			  join locations room on room.id = r.location_id
			  join locations pav on pav.id = room.parent_id
			  join locations fac on fac.id = pav.parent_id
			  join users u on u.id = r.requester_id
			  join areas a on a.id = r.area_id
			  left join users t on t.id = r.technician_id
			""";

	private final JdbcTemplate jdbc;

	JdbcRequestQueries(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public PageView<RequestSummaryView> search(RequestScope scope, RequestStatus status, int page, int size,
			Instant now) {
		List<Object> args = new ArrayList<>();
		StringBuilder where = new StringBuilder(scopeClause(scope, args));
		if (status != null) {
			where.append(" and r.status = ?");
			args.add(status.name());
		}
		Long total = jdbc.queryForObject("select count(*) from service_requests r where " + where, Long.class,
				args.toArray());

		List<Object> pageArgs = new ArrayList<>(args);
		pageArgs.add(size);
		pageArgs.add((long) page * size);
		List<RequestSummaryView> items = jdbc.query(
				SELECT + " where " + where + " order by r.created_at desc, r.id desc limit ? offset ?",
				(rs, row) -> summary(rs, now), pageArgs.toArray());
		return new PageView<>(items, page, size, total == null ? 0 : total);
	}

	@Override
	public Optional<RequestDetailView> findDetail(String code, Instant now) {
		return jdbc.query(SELECT + " where r.code = ?", (rs, row) -> detail(rs, now), code).stream().findFirst();
	}

	/** Requests assigned or in attention count as load; resolved and closed ones do not. */
	@Override
	public Map<Long, Long> activeRequestsByTechnician(Collection<Long> technicianIds) {
		if (technicianIds.isEmpty()) {
			return Map.of();
		}
		String placeholders = String.join(",", technicianIds.stream().map(id -> "?").toList());
		Map<Long, Long> counts = new HashMap<>();
		jdbc.query("select technician_id, count(*) as active from service_requests where technician_id in ("
				+ placeholders + ") and status in ('ASIGNADA', 'EN_ATENCION') group by technician_id",
				rs -> {
					counts.put(rs.getLong("technician_id"), rs.getLong("active"));
				}, technicianIds.toArray());
		return counts;
	}

	/** SQL equivalent of AccessPolicy.canViewRequest; a role without the data it needs sees nothing. */
	private static String scopeClause(RequestScope scope, List<Object> args) {
		switch (scope.role()) {
			case "ADMINISTRADOR":
				return "1 = 1";
			case "SOLICITANTE":
				args.add(scope.userId());
				return "r.requester_id = ?";
			case "TECNICO":
				args.add(scope.userId());
				return "r.technician_id = ?";
			case "SUPERVISOR":
				if (scope.areaId() == null) {
					return "1 = 0";
				}
				args.add(scope.areaId());
				return "r.area_id = ?";
			default:
				return "1 = 0";
		}
	}

	private RequestDetailView detail(ResultSet rs, Instant now) throws SQLException {
		long technicianId = rs.getLong("technician_id");
		Long technician = rs.wasNull() ? null : technicianId;
		String closure = rs.getString("closure_reason");
		long requestId = rs.getLong("id");
		return new RequestDetailView(summary(rs, now), rs.getLong("requester_id"), rs.getLong("area_id"), technician,
				closure == null ? null : ClosureReason.valueOf(closure), history(requestId), List.of());
	}

	private List<HistoryView> history(long requestId) {
		return jdbc.query("""
				select h.from_status, h.to_status, actor.full_name as actor_name, h.reason, h.created_at
				  from request_history h
				  join users actor on actor.id = h.actor_id
				 where h.request_id = ?
				 order by h.created_at, h.id
				""", (rs, row) -> {
			String from = rs.getString("from_status");
			return new HistoryView(from == null ? null : RequestStatus.valueOf(from),
					RequestStatus.valueOf(rs.getString("to_status")), rs.getString("actor_name"),
					rs.getString("reason"), instant(rs, "created_at"));
		}, requestId);
	}

	private static RequestSummaryView summary(ResultSet rs, Instant now) throws SQLException {
		RequestStatus status = RequestStatus.valueOf(rs.getString("status"));
		Instant dueAt = instant(rs, "due_at");
		String faculty = rs.getString("faculty_name");
		String pavilion = rs.getString("pavilion_name");
		String room = rs.getString("room_name");
		return new RequestSummaryView(rs.getString("code"), rs.getString("category_name"),
				faculty + " → " + pavilion + " → " + room, rs.getString("priority_name"), status,
				instant(rs, "created_at"), dueAt, Sla.isOverdue(status, dueAt, now), rs.getString("description"),
				rs.getString("requester_name"), rs.getString("area_name"), rs.getString("technician_name"), faculty,
				pavilion, room);
	}

	/** Timestamps are stored as UTC wall-clock values (see hibernate.jdbc.time_zone). */
	private static Instant instant(ResultSet rs, String column) throws SQLException {
		return rs.getObject(column, LocalDateTime.class).toInstant(ZoneOffset.UTC);
	}

}
