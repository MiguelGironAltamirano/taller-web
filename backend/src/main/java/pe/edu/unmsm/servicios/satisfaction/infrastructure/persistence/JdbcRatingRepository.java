package pe.edu.unmsm.servicios.satisfaction.infrastructure.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import pe.edu.unmsm.servicios.satisfaction.application.RatingRepository;
import pe.edu.unmsm.servicios.satisfaction.domain.Rating;

/** Ratings in {@code request_ratings}; timestamps are stored as UTC wall-clock values, like the rest of the schema. */
@Repository
class JdbcRatingRepository implements RatingRepository {

	private static final String SELECT = "select request_id, requester_id, score, comment, created_at "
			+ "from request_ratings";

	private final JdbcTemplate jdbc;

	JdbcRatingRepository(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public void save(Rating rating) {
		jdbc.update("insert into request_ratings (request_id, requester_id, score, comment, created_at) "
				+ "values (?, ?, ?, ?, ?)", rating.requestId(), rating.requesterId(), rating.score(), rating.comment(),
				utc(rating.createdAt()));
	}

	@Override
	public Optional<Rating> findByRequestId(long requestId) {
		return jdbc.query(SELECT + " where request_id = ?", JdbcRatingRepository::toRating, requestId).stream()
				.findFirst();
	}

	@Override
	public List<Rating> findCreatedBetween(Instant from, Instant to) {
		return jdbc.query(SELECT + " where created_at >= ? and created_at < ? order by created_at, request_id",
				JdbcRatingRepository::toRating, utc(from), utc(to));
	}

	private static Rating toRating(ResultSet rs, int row) throws SQLException {
		return new Rating(rs.getLong("request_id"), rs.getLong("requester_id"), rs.getInt("score"),
				rs.getString("comment"), rs.getObject("created_at", LocalDateTime.class).toInstant(ZoneOffset.UTC));
	}

	private static LocalDateTime utc(Instant instant) {
		return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
	}

}
