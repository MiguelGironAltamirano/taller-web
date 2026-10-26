package pe.edu.unmsm.servicios.requests.infrastructure.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import pe.edu.unmsm.servicios.requests.application.RequestAnalyticsQuery;
import pe.edu.unmsm.servicios.requests.application.RequestAnalyticsRow;
import pe.edu.unmsm.servicios.requests.domain.RequestStatus;

/** One joined query per call; the first resolution is read from the history with a correlated subquery. */
@Repository
class JdbcRequestAnalyticsQuery implements RequestAnalyticsQuery {

	private static final String SELECT = """
			select r.code, r.status, r.area_id, r.technician_id, r.created_at, r.due_at,
			       c.name as category_name, p.name as priority_name, a.name as area_name,
			       t.full_name as technician_name,
			       room.name as room_name, pav.name as pavilion_name, fac.name as faculty_name,
			       (select min(h.created_at) from request_history h
			         where h.request_id = r.id and h.to_status = 'RESUELTA') as first_resolved_at
			  from service_requests r
			  join categories c on c.id = r.category_id
			  join priorities p on p.id = r.priority_id
			  join areas a on a.id = r.area_id
			  join locations room on room.id = r.location_id
			  join locations pav on pav.id = room.parent_id
			  join locations fac on fac.id = pav.parent_id
			  left join users t on t.id = r.technician_id
			""";

	private final JdbcTemplate jdbc;

	JdbcRequestAnalyticsQuery(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public List<RequestAnalyticsRow> registeredBetween(Long areaId, Instant from, Instant to) {
		List<Object> args = new ArrayList<>(List.of(utc(from), utc(to)));
		String where = " where r.created_at >= ? and r.created_at < ?" + areaClause(areaId, args);
		return jdbc.query(SELECT + where + " order by r.created_at, r.id", (rs, row) -> row(rs), args.toArray());
	}

	@Override
	public List<RequestAnalyticsRow> openRequests(Long areaId) {
		List<Object> args = new ArrayList<>();
		String where = " where r.status not in ('RESUELTA', 'CERRADA')" + areaClause(areaId, args);
		return jdbc.query(SELECT + where + " order by r.due_at, r.id", (rs, row) -> row(rs), args.toArray());
	}

	private static String areaClause(Long areaId, List<Object> args) {
		if (areaId == null) {
			return "";
		}
		args.add(areaId);
		return " and r.area_id = ?";
	}

	private static RequestAnalyticsRow row(ResultSet rs) throws SQLException {
		long technicianId = rs.getLong("technician_id");
		Long technician = rs.wasNull() ? null : technicianId;
		return new RequestAnalyticsRow(rs.getString("code"), RequestStatus.valueOf(rs.getString("status")),
				rs.getString("category_name"), rs.getString("priority_name"), rs.getLong("area_id"),
				rs.getString("area_name"), technician, rs.getString("technician_name"),
				rs.getString("faculty_name") + " → " + rs.getString("pavilion_name") + " → "
						+ rs.getString("room_name"),
				instant(rs, "created_at"), instant(rs, "due_at"), instant(rs, "first_resolved_at"));
	}

	/** Timestamps are stored as UTC wall-clock values (see hibernate.jdbc.time_zone). */
	private static Instant instant(ResultSet rs, String column) throws SQLException {
		LocalDateTime value = rs.getObject(column, LocalDateTime.class);
		return value == null ? null : value.toInstant(ZoneOffset.UTC);
	}

	private static LocalDateTime utc(Instant instant) {
		return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
	}

}
