package pe.edu.unmsm.servicios.notifications.domain;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class NotificationRecipients {

	private NotificationRecipients() {
	}

	public static List<Long> forTransition(long requesterId, Long technicianId, List<Long> areaSupervisorIds, long actorId) {
		Set<Long> recipients = new LinkedHashSet<>();
		recipients.add(requesterId);
		if (technicianId != null) {
			recipients.add(technicianId);
		}
		if (areaSupervisorIds != null) {
			recipients.addAll(areaSupervisorIds);
		}
		recipients.remove(actorId);
		return new ArrayList<>(recipients);
	}

	public static List<Long> forAssignment(long newTechnicianId, long actorId) {
		Set<Long> recipients = new LinkedHashSet<>();
		recipients.add(newTechnicianId);
		recipients.remove(actorId);
		return new ArrayList<>(recipients);
	}

	public static List<Long> forComment(long requesterId, Long technicianId, List<Long> areaSupervisorIds, long authorId) {
		return forTransition(requesterId, technicianId, areaSupervisorIds, authorId);
	}

}
