package pe.edu.unmsm.servicios.collaboration.infrastructure.persistence;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import pe.edu.unmsm.servicios.collaboration.application.EvidenceRepository;
import pe.edu.unmsm.servicios.collaboration.domain.Evidence;
import pe.edu.unmsm.servicios.collaboration.domain.EvidenceKind;

@Repository
public class JdbcEvidenceRepository implements EvidenceRepository {

	private final JdbcTemplate jdbc;

	public JdbcEvidenceRepository(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	private static final RowMapper<Evidence> ROW_MAPPER = (rs, rowNum) -> new Evidence(
			rs.getLong("id"),
			rs.getLong("request_id"),
			rs.getLong("uploader_id"),
			rs.getString("file_name"),
			rs.getString("stored_name"),
			EvidenceKind.valueOf(rs.getString("kind")),
			rs.getLong("size_bytes"),
			rs.getTimestamp("created_at").toInstant()
	);

	@Override
	public Evidence save(Evidence evidence) {
		String sql = """
				INSERT INTO evidences (request_id, uploader_id, file_name, stored_name, kind, size_bytes, created_at)
				VALUES (?, ?, ?, ?, ?, ?, ?)
				""";
		KeyHolder keyHolder = new GeneratedKeyHolder();
		jdbc.update(con -> {
			var ps = con.prepareStatement(sql, new String[] { "id" });
			ps.setLong(1, evidence.requestId());
			ps.setLong(2, evidence.uploaderId());
			ps.setString(3, evidence.fileName());
			ps.setString(4, evidence.storedName());
			ps.setString(5, evidence.kind().name());
			ps.setLong(6, evidence.sizeBytes());
			ps.setTimestamp(7, Timestamp.from(evidence.createdAt()));
			return ps;
		}, keyHolder);

		long id = keyHolder.getKey().longValue();
		return new Evidence(id, evidence.requestId(), evidence.uploaderId(), evidence.fileName(),
				evidence.storedName(), evidence.kind(), evidence.sizeBytes(), evidence.createdAt());
	}

	@Override
	public List<Evidence> findByRequestId(long requestId) {
		String sql = "SELECT * FROM evidences WHERE request_id = ? ORDER BY created_at ASC, id ASC";
		return jdbc.query(sql, ROW_MAPPER, requestId);
	}

	@Override
	public Optional<Evidence> findByIdAndRequestId(long id, long requestId) {
		String sql = "SELECT * FROM evidences WHERE id = ? AND request_id = ?";
		try {
			return Optional.ofNullable(jdbc.queryForObject(sql, ROW_MAPPER, id, requestId));
		}
		catch (EmptyResultDataAccessException e) {
			return Optional.empty();
		}
	}

	@Override
	public long countByRequestId(long requestId) {
		String sql = "SELECT COUNT(*) FROM evidences WHERE request_id = ?";
		Long count = jdbc.queryForObject(sql, Long.class, requestId);
		return count == null ? 0 : count;
	}

}
