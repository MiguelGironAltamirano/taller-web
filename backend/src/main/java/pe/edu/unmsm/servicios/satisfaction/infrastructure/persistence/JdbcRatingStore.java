package pe.edu.unmsm.servicios.satisfaction.infrastructure.persistence;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import pe.edu.unmsm.servicios.satisfaction.application.RatingStore;
import pe.edu.unmsm.servicios.satisfaction.domain.Rating;

@Repository
class JdbcRatingStore implements RatingStore {

	private final JdbcTemplate jdbc;

	JdbcRatingStore(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	/** Timestamps are stored as UTC wall-clock values, like the rest of the schema. */
	@Override
	public Rating save(Rating rating) {
		KeyHolder keys = new GeneratedKeyHolder();
		jdbc.update(connection -> {
			PreparedStatement ps = connection.prepareStatement(
					"insert into ratings (request_id, score, comment, created_at) values (?, ?, ?, ?)",
					Statement.RETURN_GENERATED_KEYS);
			ps.setLong(1, rating.requestId());
			ps.setInt(2, rating.score());
			ps.setString(3, rating.comment());
			ps.setObject(4, LocalDateTime.ofInstant(rating.createdAt(), ZoneOffset.UTC));
			return ps;
		}, keys);
		return new Rating(keys.getKey().longValue(), rating.requestId(), rating.score(), rating.comment(),
				rating.createdAt());
	}

	@Override
	public boolean existsForRequest(long requestId) {
		return Boolean.TRUE.equals(jdbc.queryForObject(
				"select exists(select 1 from ratings where request_id = ?)", Boolean.class, requestId));
	}

}
