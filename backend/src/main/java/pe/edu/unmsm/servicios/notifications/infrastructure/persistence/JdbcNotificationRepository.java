package pe.edu.unmsm.servicios.notifications.infrastructure.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import pe.edu.unmsm.servicios.notifications.application.NotificationRepository;
import pe.edu.unmsm.servicios.notifications.domain.Notification;

@Repository
public class JdbcNotificationRepository implements NotificationRepository {

	private final JdbcTemplate jdbc;

	public JdbcNotificationRepository(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public void saveAll(List<Long> recipientIds, long requestId, String text, Instant createdAt) {
		if (recipientIds == null || recipientIds.isEmpty()) {
			return;
		}
		String sql = "INSERT INTO notifications (recipient_id, request_id, text, created_at) VALUES (?, ?, ?, ?)";
		Timestamp ts = Timestamp.from(createdAt);
		List<Object[]> batchArgs = recipientIds.stream()
				.map(rId -> new Object[] { rId, requestId, text, ts })
				.toList();
		jdbc.batchUpdate(sql, batchArgs);
	}

	@Override
	public List<Notification> findByRecipient(long recipientId) {
		String sql = """
				SELECT n.id, n.recipient_id, n.request_id, r.code AS request_code, n.text, n.created_at, n.read_at
				FROM notifications n
				JOIN service_requests r ON r.id = n.request_id
				WHERE n.recipient_id = ?
				ORDER BY n.created_at DESC
				""";
		return jdbc.query(sql, new NotificationRowMapper(), recipientId);
	}

	@Override
	public boolean markAsRead(long notificationId, long recipientId, Instant readAt) {
		String sql = "UPDATE notifications SET read_at = ? WHERE id = ? AND recipient_id = ?";
		int updated = jdbc.update(sql, Timestamp.from(readAt), notificationId, recipientId);
		return updated > 0;
	}

	@Override
	public int markAllAsRead(long recipientId, Instant readAt) {
		String sql = "UPDATE notifications SET read_at = ? WHERE recipient_id = ? AND read_at IS NULL";
		return jdbc.update(sql, Timestamp.from(readAt), recipientId);
	}

	private static class NotificationRowMapper implements RowMapper<Notification> {

		@Override
		public Notification mapRow(ResultSet rs, int rowNum) throws SQLException {
			long id = rs.getLong("id");
			long recipientId = rs.getLong("recipient_id");
			long requestId = rs.getLong("request_id");
			String requestCode = rs.getString("request_code");
			String text = rs.getString("text");
			Instant createdAt = rs.getTimestamp("created_at").toInstant();
			Timestamp readAtTs = rs.getTimestamp("read_at");
			boolean read = readAtTs != null;
			return new Notification(id, recipientId, requestId, requestCode, text, createdAt, read);
		}

	}

}
