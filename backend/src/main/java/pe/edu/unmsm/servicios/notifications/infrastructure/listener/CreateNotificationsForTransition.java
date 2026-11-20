package pe.edu.unmsm.servicios.notifications.infrastructure.listener;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import pe.edu.unmsm.servicios.notifications.application.NotificationService;
import pe.edu.unmsm.servicios.notifications.domain.NotificationRecipients;
import pe.edu.unmsm.servicios.notifications.domain.NotificationText;
import pe.edu.unmsm.servicios.notifications.infrastructure.adapter.AreaSupervisorLookup;
import pe.edu.unmsm.servicios.shared.domain.event.RequestTransitionedEvent;

@Component
public class CreateNotificationsForTransition {

	private static final Logger log = LoggerFactory.getLogger(CreateNotificationsForTransition.class);

	private final NotificationService notificationService;
	private final AreaSupervisorLookup areaSupervisorLookup;

	public CreateNotificationsForTransition(NotificationService notificationService,
			AreaSupervisorLookup areaSupervisorLookup) {
		this.notificationService = notificationService;
		this.areaSupervisorLookup = areaSupervisorLookup;
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void onTransition(RequestTransitionedEvent event) {
		try {
			List<Long> supervisorIds = areaSupervisorLookup.findSupervisorIdsOfArea(event.areaId());

			boolean assignment = event.isAssignment() && event.technicianId() != null;
			if (assignment) {
				// Notificación específica de asignación (o reasignación) al técnico
				List<Long> assignmentRecipients = NotificationRecipients.forAssignment(event.technicianId(), event.actorId());
				String assignmentText = event.isReassignment()
						? NotificationText.forReassignment(event.code())
						: NotificationText.forAssignment(event.code());
				notificationService.notify(assignmentRecipients, assignmentText, event.requestId());
			}

			// Notificación general de cambio de estado (el técnico asignado ya recibió la suya)
			List<Long> recipients = assignment
					? NotificationRecipients.forAssignmentTransition(
							event.requesterId(), event.technicianId(), supervisorIds, event.actorId())
					: NotificationRecipients.forTransition(
							event.requesterId(), event.technicianId(), supervisorIds, event.actorId());
			String text = event.bySystem() && "CERRADA".equals(event.toStatus())
					? NotificationText.forAutomaticClosure(event.code())
					: NotificationText.forTransition(event.code(), statusToLabel(event.toStatus()));
			notificationService.notify(recipients, text, event.requestId());
		} catch (Exception e) {
			log.error("Failed to generate notifications for transition event on {}: {}", event.code(), e.getMessage(), e);
		}
	}

	private String statusToLabel(String status) {
		return switch (status) {
			case "REGISTRADA" -> "Registrada";
			case "EN_EVALUACION" -> "En evaluación";
			case "ASIGNADA" -> "Asignada";
			case "EN_ATENCION" -> "En atención";
			case "RESUELTA" -> "Resuelta";
			case "CERRADA" -> "Cerrada";
			case "CANCELADA" -> "Cancelada";
			default -> status;
		};
	}

}
