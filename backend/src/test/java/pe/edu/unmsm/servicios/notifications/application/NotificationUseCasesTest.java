package pe.edu.unmsm.servicios.notifications.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import pe.edu.unmsm.servicios.notifications.domain.Notification;
import pe.edu.unmsm.servicios.shared.domain.AuthenticatedUser;

class NotificationUseCasesTest {

	private static final Instant NOW = Instant.parse("2026-10-17T12:00:00Z");
	private static final AuthenticatedUser USER_A = new AuthenticatedUser(10L, "a@unmsm.edu.pe", "User A", "SOLICITANTE");
	private static final AuthenticatedUser USER_B = new AuthenticatedUser(20L, "b@unmsm.edu.pe", "User B", "TECNICO");

	private InMemoryNotificationRepository repository;
	private NotificationUseCases useCases;

	@BeforeEach
	void setUp() {
		repository = new InMemoryNotificationRepository();
		useCases = new NotificationUseCases(repository, Clock.fixed(NOW, ZoneOffset.UTC));
	}

	@Test
	void notifySavesToRepository() {
		useCases.notify(List.of(10L, 20L), "Mensaje de prueba", 100L);

		List<Notification> notifsA = useCases.listNotifications(USER_A);
		List<Notification> notifsB = useCases.listNotifications(USER_B);

		assertThat(notifsA).hasSize(1);
		assertThat(notifsA.getFirst().text()).isEqualTo("Mensaje de prueba");
		assertThat(notifsA.getFirst().read()).isFalse();

		assertThat(notifsB).hasSize(1);
		assertThat(notifsB.getFirst().text()).isEqualTo("Mensaje de prueba");
	}

	@Test
	void markReadUpdatesReadStatus() {
		useCases.notify(List.of(10L), "Mensaje 1", 100L);
		long notifId = useCases.listNotifications(USER_A).getFirst().id();

		useCases.markRead(USER_A, notifId);

		Notification updated = useCases.listNotifications(USER_A).getFirst();
		assertThat(updated.read()).isTrue();
	}

	@Test
	void markReadThrowsNotFoundIfNotificationBelongsToAnotherUser() {
		useCases.notify(List.of(20L), "Mensaje para B", 100L);
		long notifId = useCases.listNotifications(USER_B).getFirst().id();

		assertThatThrownBy(() -> useCases.markRead(USER_A, notifId))
				.isInstanceOf(NotificationNotFoundException.class);
	}

	@Test
	void markAllReadUpdatesAllForRecipient() {
		useCases.notify(List.of(10L), "Mensaje 1", 100L);
		useCases.notify(List.of(10L), "Mensaje 2", 100L);
		useCases.notify(List.of(20L), "Mensaje para B", 100L);

		useCases.markAllRead(USER_A);

		List<Notification> notifsA = useCases.listNotifications(USER_A);
		assertThat(notifsA).allMatch(Notification::read);

		List<Notification> notifsB = useCases.listNotifications(USER_B);
		assertThat(notifsB).allMatch(n -> !n.read());
	}

	private static class InMemoryNotificationRepository implements NotificationRepository {
		private long nextId = 1L;
		private final List<StoredNotification> list = new ArrayList<>();

		private static class StoredNotification {
			long id;
			long recipientId;
			long requestId;
			String text;
			Instant createdAt;
			Instant readAt;
		}

		@Override
		public void saveAll(List<Long> recipientIds, long requestId, String text, Instant createdAt) {
			for (Long rId : recipientIds) {
				StoredNotification sn = new StoredNotification();
				sn.id = nextId++;
				sn.recipientId = rId;
				sn.requestId = requestId;
				sn.text = text;
				sn.createdAt = createdAt;
				list.add(sn);
			}
		}

		@Override
		public List<Notification> findByRecipient(long recipientId) {
			return list.stream()
					.filter(n -> n.recipientId == recipientId)
					.map(n -> new Notification(n.id, n.recipientId, n.requestId, "SOL-2026-000001", n.text, n.createdAt, n.readAt != null))
					.toList();
		}

		@Override
		public boolean markAsRead(long notificationId, long recipientId, Instant readAt) {
			for (StoredNotification n : list) {
				if (n.id == notificationId && n.recipientId == recipientId) {
					n.readAt = readAt;
					return true;
				}
			}
			return false;
		}

		@Override
		public int markAllAsRead(long recipientId, Instant readAt) {
			int count = 0;
			for (StoredNotification n : list) {
				if (n.recipientId == recipientId && n.readAt == null) {
					n.readAt = readAt;
					count++;
				}
			}
			return count;
		}
	}

}
