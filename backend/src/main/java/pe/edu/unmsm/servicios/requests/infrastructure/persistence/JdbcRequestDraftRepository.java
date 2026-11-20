package pe.edu.unmsm.servicios.requests.infrastructure.persistence;

import java.sql.Timestamp;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import pe.edu.unmsm.servicios.requests.application.RequestDraftRepository;
import pe.edu.unmsm.servicios.requests.domain.RequestDraft;

@Repository
class JdbcRequestDraftRepository implements RequestDraftRepository {

	private final JdbcTemplate jdbc;

	JdbcRequestDraftRepository(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public Optional<RequestDraft> find(long userId) {
		return jdbc.query("""
				select category, description, priority, faculty, pavilion, room, updated_at
				  from request_drafts where user_id = ?""",
				(rs, row) -> new RequestDraft(rs.getString("category"), rs.getString("description"),
						rs.getString("priority"), rs.getString("faculty"), rs.getString("pavilion"), rs.getString("room"),
						rs.getTimestamp("updated_at").toInstant()),
				userId).stream().findFirst();
	}

	@Override
	public void save(long userId, RequestDraft draft) {
		jdbc.update("""
				insert into request_drafts (user_id, category, description, priority, faculty, pavilion, room, updated_at)
				values (?, ?, ?, ?, ?, ?, ?, ?)
				on duplicate key update category = values(category), description = values(description),
				  priority = values(priority), faculty = values(faculty), pavilion = values(pavilion),
				  room = values(room), updated_at = values(updated_at)""",
				userId, draft.category(), draft.description(), draft.priority(), draft.faculty(), draft.pavilion(),
				draft.room(), Timestamp.from(draft.updatedAt()));
	}

	@Override
	public void delete(long userId) {
		jdbc.update("delete from request_drafts where user_id = ?", userId);
	}

}
