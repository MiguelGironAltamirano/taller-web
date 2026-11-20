package pe.edu.unmsm.servicios.requests.infrastructure.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import pe.edu.unmsm.servicios.requests.application.HistoryView;
import pe.edu.unmsm.servicios.requests.application.PageView;
import pe.edu.unmsm.servicios.requests.application.RequestCountsView;
import pe.edu.unmsm.servicios.requests.application.RequestDetailView;
import pe.edu.unmsm.servicios.requests.application.RequestListCriteria;
import pe.edu.unmsm.servicios.requests.application.RequestQueries;
import pe.edu.unmsm.servicios.requests.application.RequestScope;
import pe.edu.unmsm.servicios.requests.application.RequestSort;
import pe.edu.unmsm.servicios.requests.application.RequestSummaryView;
import pe.edu.unmsm.servicios.requests.application.TechnicianWorkload;
import pe.edu.unmsm.servicios.requests.domain.ClosureReason;
import pe.edu.unmsm.servicios.requests.domain.RequestStatus;
import pe.edu.unmsm.servicios.requests.domain.Sla;

/** Read model: one joined query per list page or detail, instead of loading entities and their relations. */
@Repository
class JdbcRequestQueries implements RequestQueries, TechnicianWorkload {

	private static final String COLUMNS = """
			select r.id, r.code, r.status, r.created_at, r.due_at,
			       coalesce(r.requested_due_at, r.due_at) as requested_due_at, r.description, r.requester_id, r.area_id,
			       r.technician_id, r.closure_reason, r.solution,
			       c.name as category_name, p.name as priority_name, rp.name as requested_priority_name,
			       room.name as room_name, pav.name as pavilion_name, fac.name as faculty_name,
			       u.full_name as requester_name, a.name as area_name, t.full_name as technician_name
			""";

	/**
	 * Every join is on a mandatory foreign key except the technician, so none of them changes the row count. The
	 * requested priority and estimated date fall back to the current ones for rows not yet backfilled (see
	 * V20261119_1000 and V20261119_1100).
	 */
	private static final String FROM = """
			  from service_requests r
			  join categories c on c.id = r.category_id
			  join priorities p on p.id = r.priority_id
			  join priorities rp on rp.id = coalesce(r.requested_priority_id, r.priority_id)
			  join locations room on room.id = r.location_id
			  join locations pav on pav.id = room.parent_id
			  join locations fac on fac.id = pav.parent_id
			  join users u on u.id = r.requester_id
			  join areas a on a.id = r.area_id
			  left join users t on t.id = r.technician_id
			""";

	private static final String SELECT = COLUMNS + FROM;

	/** Statuses that no longer count against the SLA, as a SQL list built from the domain rule. */
	private static final String FINISHED = Arrays.stream(RequestStatus.values()).filter(RequestStatus::isFinished)
			.map(status -> "'" + status.name() + "'").collect(Collectors.joining(", ", "(", ")"));

	/** SQL equivalent of {@link Sla#isOverdue}; its only parameter is the current instant. */
	private static final String OVERDUE = "(r.status not in " + FINISHED + " and r.due_at < ?)";

	/** Allow-listed orders; each ends with the id so consecutive pages never overlap. */
	private static final Map<RequestSort, String> ORDER = Map.of(
			RequestSort.NEWEST, "r.created_at desc, r.id desc",
			RequestSort.OLDEST, "r.created_at asc, r.id asc",
			RequestSort.DUE, "case when r.status in " + FINISHED + " then 1 else 0 end, r.due_at asc, r.id asc",
			RequestSort.PRIORITY, "p.sort_order asc, r.created_at desc, r.id desc");

	/** The requester sorts by the priority they chose and by the estimated date instead of the due date. */
	private static final Map<RequestSort, String> REQUESTER_ORDER = Map.of(
			RequestSort.NEWEST, ORDER.get(RequestSort.NEWEST),
			RequestSort.OLDEST, ORDER.get(RequestSort.OLDEST),
			RequestSort.DUE, "case when r.status in " + FINISHED + " then 1 else 0 end, "
					+ "coalesce(r.requested_due_at, r.due_at) asc, r.id asc",
			RequestSort.PRIORITY, "rp.sort_order asc, r.created_at desc, r.id desc");

	private final JdbcTemplate jdbc;

	JdbcRequestQueries(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public PageView<RequestSummaryView> search(RequestScope scope, RequestListCriteria criteria, Instant now) {
		List<Object> args = new ArrayList<>();
		String where = scopeClause(scope, args) + filterClause(criteria, scope.isRequester(), now, args);
		Long total = jdbc.queryForObject("select count(*)" + FROM + " where " + where, Long.class, args.toArray());

		List<Object> pageArgs = new ArrayList<>(args);
		pageArgs.add(criteria.size());
		pageArgs.add((long) criteria.page() * criteria.size());
		String order = (scope.isRequester() ? REQUESTER_ORDER : ORDER).get(criteria.sort());
		List<RequestSummaryView> items = jdbc.query(
				SELECT + " where " + where + " order by " + order + " limit ? offset ?",
				(rs, row) -> summary(rs, now), pageArgs.toArray());
		return new PageView<>(items, criteria.page(), criteria.size(), total == null ? 0 : total);
	}

	@Override
	public RequestCountsView count(RequestScope scope, Instant now) {
		List<Object> args = new ArrayList<>();
		args.add(utc(now));
		String where = scopeClause(scope, args);
		Map<RequestStatus, Long> byStatus = new EnumMap<>(RequestStatus.class);
		long[] overdue = { 0 };
		jdbc.query("select r.status, count(*) as total, sum(case when " + OVERDUE
				+ " then 1 else 0 end) as overdue from service_requests r where " + where + " group by r.status",
				rs -> {
					byStatus.put(RequestStatus.valueOf(rs.getString("status")), rs.getLong("total"));
					overdue[0] += rs.getLong("overdue");
				}, args.toArray());
		long total = byStatus.values().stream().mapToLong(Long::longValue).sum();
		return new RequestCountsView(total, overdue[0], byStatus);
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

	/**
	 * The optional filters, each one a fixed fragment with {@code ?} placeholders: user input only ever travels as
	 * a bound parameter. They narrow the scope clause, never widen it.
	 */
	private static String filterClause(RequestListCriteria criteria, boolean requester, Instant now,
			List<Object> args) {
		StringBuilder where = new StringBuilder();
		if (!criteria.statuses().isEmpty()) {
			List<RequestStatus> statuses = criteria.statuses().stream().sorted().toList();
			where.append(" and r.status in (").append(String.join(", ", statuses.stream().map(s -> "?").toList()))
					.append(')');
			statuses.forEach(status -> args.add(status.name()));
		}
		equal(where, args, "r.area_id", criteria.areaId());
		equal(where, args, "r.category_id", criteria.categoryId());
		equal(where, args, requester ? "rp.id" : "r.priority_id", criteria.priorityId());
		equal(where, args, "r.technician_id", criteria.technicianId());
		if (criteria.unassigned()) {
			where.append(" and r.technician_id is null");
		}
		if (criteria.overdue() != null) {
			where.append(criteria.overdue() ? " and " : " and not ").append(OVERDUE);
			args.add(utc(now));
		}
		if (criteria.createdFrom() != null) {
			where.append(" and r.created_at >= ?");
			args.add(utc(criteria.createdFrom()));
		}
		if (criteria.createdBefore() != null) {
			where.append(" and r.created_at < ?");
			args.add(utc(criteria.createdBefore()));
		}
		if (criteria.hasText()) {
			String pattern = "%" + escapeLike(criteria.text()) + "%";
			where.append(" and (r.code like ? escape '!' or r.description like ? escape '!'"
					+ " or u.full_name like ? escape '!' or c.name like ? escape '!' or room.name like ? escape '!')");
			for (int i = 0; i < 5; i++) {
				args.add(pattern);
			}
		}
		return where.toString();
	}

	private static void equal(StringBuilder where, List<Object> args, String column, Long value) {
		if (value != null) {
			where.append(" and ").append(column).append(" = ?");
			args.add(value);
		}
	}

	/** {@code %} and {@code _} typed by the user are literal characters, not wildcards. */
	private static String escapeLike(String text) {
		return text.replace("!", "!!").replace("%", "!%").replace("_", "!_");
	}

	private RequestDetailView detail(ResultSet rs, Instant now) throws SQLException {
		long technicianId = rs.getLong("technician_id");
		Long technician = rs.wasNull() ? null : technicianId;
		String closure = rs.getString("closure_reason");
		long requestId = rs.getLong("id");
		return new RequestDetailView(requestId, summary(rs, now), rs.getLong("requester_id"), rs.getLong("area_id"),
				technician, closure == null ? null : ClosureReason.valueOf(closure), rs.getString("solution"),
				history(requestId));
	}

	private List<HistoryView> history(long requestId) {
		return jdbc.query("""
				select h.from_status, h.to_status, coalesce(actor.full_name, 'Sistema') as actor_name, h.reason, h.note,
				       h.internal, h.created_at
				  from request_history h
				  left join users actor on actor.id = h.actor_id
				 where h.request_id = ?
				 order by h.created_at, h.id
				""", (rs, row) -> {
			String from = rs.getString("from_status");
			return new HistoryView(from == null ? null : RequestStatus.valueOf(from),
					RequestStatus.valueOf(rs.getString("to_status")), rs.getString("actor_name"),
					rs.getString("reason"), instant(rs, "created_at"), rs.getString("note"), rs.getBoolean("internal"));
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
				pavilion, room, rs.getString("requested_priority_name"), instant(rs, "requested_due_at"), false);
	}

	/** Timestamps are stored as UTC wall-clock values (see hibernate.jdbc.time_zone). */
	private static Instant instant(ResultSet rs, String column) throws SQLException {
		return rs.getObject(column, LocalDateTime.class).toInstant(ZoneOffset.UTC);
	}

	private static LocalDateTime utc(Instant instant) {
		return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
	}

}
