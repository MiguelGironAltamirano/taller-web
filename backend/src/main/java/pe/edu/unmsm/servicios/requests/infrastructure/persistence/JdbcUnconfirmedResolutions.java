package pe.edu.unmsm.servicios.requests.infrastructure.persistence;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import pe.edu.unmsm.servicios.requests.application.UnconfirmedResolutions;

/**
 * Reads the moment each request entered {@code RESUELTA} from {@code request_history}. Entries that keep the status
 * (for example a priority change while resolved) do not restart the period.
 */
@Repository
class JdbcUnconfirmedResolutions implements UnconfirmedResolutions {

	private static final String ENTERED_RESOLVED = "h.to_status = 'RESUELTA' "
			+ "and (h.from_status is null or h.from_status <> 'RESUELTA')";

	private final JdbcTemplate jdbc;

	JdbcUnconfirmedResolutions(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public List<String> resolvedAtOrBefore(Instant cutoff, int limit) {
		return jdbc.queryForList("""
				select r.code
				  from service_requests r
				  join request_history h on h.request_id = r.id and %s
				 where r.status = 'RESUELTA'
				 group by r.id, r.code
				having max(h.created_at) <= ?
				 order by max(h.created_at), r.id
				 limit ?
				""".formatted(ENTERED_RESOLVED), String.class, utc(cutoff), limit);
	}

	@Override
	public Optional<Instant> resolvedAt(String code) {
		LocalDateTime value = jdbc.queryForObject("""
				select max(h.created_at)
				  from request_history h
				  join service_requests r on r.id = h.request_id
				 where r.code = ? and %s
				""".formatted(ENTERED_RESOLVED), LocalDateTime.class, code);
		return Optional.ofNullable(value).map(time -> time.toInstant(ZoneOffset.UTC));
	}

	/** Timestamps are stored as UTC wall-clock values (see hibernate.jdbc.time_zone). */
	private static LocalDateTime utc(Instant instant) {
		return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
	}

}
