package pe.edu.unmsm.servicios.satisfaction.infrastructure.persistence;

import java.sql.Timestamp;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;

import pe.edu.unmsm.servicios.satisfaction.application.RatingStore;
import pe.edu.unmsm.servicios.satisfaction.domain.Rating;

@Repository
public class JdbcRatingStore implements RatingStore {

	private final JdbcTemplate jdbc;
	private final SimpleJdbcInsert insert;

	public JdbcRatingStore(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
		this.insert = new SimpleJdbcInsert(jdbc).withTableName("ratings").usingGeneratedKeyColumns("id");
	}

	@Override
	public Rating save(Rating rating) {
		java.util.HashMap<String, Object> params = new java.util.HashMap<>();
		params.put("request_id", rating.requestId());
		params.put("score", rating.score());
		params.put("comment", rating.comment());
		params.put("created_at", Timestamp.from(rating.createdAt()));
		long id = insert.executeAndReturnKey(params).longValue();
		return new Rating(id, rating.requestId(), rating.score(), rating.comment(), rating.createdAt());
	}

	@Override
	public boolean existsByRequestId(long requestId) {
		Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM ratings WHERE request_id = ?", Integer.class, requestId);
		return count != null && count > 0;
	}

}
