package pe.edu.unmsm.servicios.requests.infrastructure.persistence;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import pe.edu.unmsm.servicios.requests.application.RequestHistoryCriteria;
import pe.edu.unmsm.servicios.requests.application.RequestHistoryQuery;
import pe.edu.unmsm.servicios.requests.application.RequestHistoryRecord;
import pe.edu.unmsm.servicios.requests.domain.RequestStatus;

/** Read model over {@code request_history}: one joined, filtered and limited query. */
@Repository
class JdbcRequestHistoryQuery implements RequestHistoryQuery {

	/** Entries made by the system (automatic closure) have no actor and are shown as {@code Sistema}. */
	private static final String ACTOR_NAME = "coalesce(actor.full_name, 'Sistema')";

	private static final String SELECT = """
			select h.id, h.created_at, %s as actor_name, r.code, h.from_status, h.to_status, h.reason
			  from request_history h
			  join service_requests r on r.id = h.request_id
			  left join users actor on actor.id = h.actor_id
			""".formatted(ACTOR_NAME);

	private final JdbcTemplate jdbc;

	JdbcRequestHistoryQuery(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public List<RequestHistoryRecord> latest(RequestHistoryCriteria criteria) {
		StringBuilder sql = new StringBuilder(SELECT);
		List<Object> args = new ArrayList<>();
		if (!criteria.matchesEverything()) {
			List<String> conditions = new ArrayList<>();
			if (!criteria.text().isEmpty()) {
				String pattern = "%" + escapeLike(criteria.text()) + "%";
				conditions.add(ACTOR_NAME + " like ? escape '!'");
				conditions.add("r.code like ? escape '!'");
				conditions.add("h.reason like ? escape '!'");
				args.add(pattern);
				args.add(pattern);
				args.add(pattern);
			}
			if (!criteria.statuses().isEmpty()) {
				String placeholders = String.join(", ", criteria.statuses().stream().map(s -> "?").toList());
				conditions.add("h.from_status in (" + placeholders + ")");
				conditions.add("h.to_status in (" + placeholders + ")");
				List<String> names = criteria.statuses().stream().map(Enum::name).toList();
				args.addAll(names);
				args.addAll(names);
			}
			sql.append(" where ").append(String.join(" or ", conditions));
		}
		sql.append(" order by h.created_at desc, h.id desc limit ?");
		args.add(criteria.limit());
		return jdbc.query(sql.toString(), (rs, row) -> {
			String from = rs.getString("from_status");
			return new RequestHistoryRecord(rs.getLong("id"),
					rs.getObject("created_at", LocalDateTime.class).toInstant(ZoneOffset.UTC),
					rs.getString("actor_name"), rs.getString("code"),
					from == null ? null : RequestStatus.valueOf(from), RequestStatus.valueOf(rs.getString("to_status")),
					rs.getString("reason"));
		}, args.toArray());
	}

	/** Treats {@code %} and {@code _} typed by the user as literal characters. */
	private static String escapeLike(String text) {
		return text.replace("!", "!!").replace("%", "!%").replace("_", "!_");
	}

}
