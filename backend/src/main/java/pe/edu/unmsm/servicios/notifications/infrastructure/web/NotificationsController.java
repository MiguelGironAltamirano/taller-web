package pe.edu.unmsm.servicios.notifications.infrastructure.web;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import pe.edu.unmsm.servicios.notifications.application.NotificationUseCases;
import pe.edu.unmsm.servicios.notifications.domain.Notification;
import pe.edu.unmsm.servicios.shared.domain.AuthenticatedUser;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationsController {

	private final NotificationUseCases useCases;

	public NotificationsController(NotificationUseCases useCases) {
		this.useCases = useCases;
	}

	@GetMapping
	public ResponseEntity<List<NotificationDto>> listNotifications(@AuthenticationPrincipal AuthenticatedUser user) {
		List<Notification> items = useCases.listNotifications(user);
		List<NotificationDto> dtos = items.stream()
				.map(NotificationDto::from)
				.toList();
		return ResponseEntity.ok(dtos);
	}

	@PatchMapping("/{id}/read")
	public ResponseEntity<Void> markAsRead(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable long id) {
		useCases.markRead(user, id);
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/read-all")
	public ResponseEntity<Void> markAllAsRead(@AuthenticationPrincipal AuthenticatedUser user) {
		useCases.markAllRead(user);
		return ResponseEntity.noContent().build();
	}

	public record NotificationDto(
			long id,
			String text,
			String createdAt,
			boolean read,
			String requestCode
	) {
		public static NotificationDto from(Notification n) {
			return new NotificationDto(
					n.id(),
					n.text(),
					n.createdAt().toString(),
					n.read(),
					n.requestCode()
			);
		}
	}

}
