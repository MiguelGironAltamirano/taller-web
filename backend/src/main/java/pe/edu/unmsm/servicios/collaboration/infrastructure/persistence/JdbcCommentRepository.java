package pe.edu.unmsm.servicios.collaboration.infrastructure.persistence;

import java.sql.Timestamp;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import pe.edu.unmsm.servicios.collaboration.application.CommentRepository;
import pe.edu.unmsm.servicios.collaboration.domain.Comment;

@Repository
public class JdbcCommentRepository implements CommentRepository {

	private final JdbcTemplate jdbc;

	public JdbcCommentRepository(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	private static final RowMapper<Comment> ROW_MAPPER = (rs, rowNum) -> new Comment(
			rs.getLong("id"),
			rs.getLong("request_id"),
			rs.getLong("author_id"),
			rs.getString("author_name"),
			rs.getString("text"),
			rs.getTimestamp("created_at").toInstant()
	);

	@Override
	public Comment save(Comment comment) {
		String sql = "INSERT INTO comments (request_id, author_id, text, created_at) VALUES (?, ?, ?, ?)";
		KeyHolder keyHolder = new GeneratedKeyHolder();
		jdbc.update(con -> {
			var ps = con.prepareStatement(sql, new String[] { "id" });
			ps.setLong(1, comment.requestId());
			ps.setLong(2, comment.authorId());
			ps.setString(3, comment.text());
			ps.setTimestamp(4, Timestamp.from(comment.createdAt()));
			return ps;
		}, keyHolder);

		long id = keyHolder.getKey().longValue();
		return new Comment(id, comment.requestId(), comment.authorId(), comment.authorName(), comment.text(),
				comment.createdAt());
	}

	@Override
	public List<Comment> findByRequestId(long requestId) {
		String sql = """
				SELECT c.id, c.request_id, c.author_id, u.full_name AS author_name, c.text, c.created_at
				FROM comments c
				JOIN users u ON c.author_id = u.id
				WHERE c.request_id = ?
				ORDER BY c.created_at ASC, c.id ASC
				""";
		return jdbc.query(sql, ROW_MAPPER, requestId);
	}

}
