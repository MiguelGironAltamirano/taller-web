package pe.edu.unmsm.servicios.requests.application;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import org.springframework.transaction.annotation.Transactional;

import pe.edu.unmsm.servicios.catalog.application.CatalogLookup;
import pe.edu.unmsm.servicios.catalog.domain.PriorityLevel;
import pe.edu.unmsm.servicios.requests.domain.InvalidRequestException;
import pe.edu.unmsm.servicios.requests.domain.RequestChangeForbiddenException;
import pe.edu.unmsm.servicios.requests.domain.RequestHistoryEntry;
import pe.edu.unmsm.servicios.requests.domain.RequestNotFoundException;
import pe.edu.unmsm.servicios.requests.domain.RequestStatus;
import pe.edu.unmsm.servicios.requests.domain.ServiceRequest;
import pe.edu.unmsm.servicios.shared.domain.AccessPolicy;
import pe.edu.unmsm.servicios.shared.domain.AuthenticatedUser;

/**
 * The supervisor of the area changes the priority of a request that is still being handled. The due date is
 * recalculated with the SLA of the new priority and the change is recorded in the history with a note, without
 * changing the status. The requester never learns about it: they keep the priority they chose and the entry is
 * internal.
 */
public class ChangePriority {

	/** Dates in the history text follow the institution's calendar (Lima), like the request codes. */
	private static final DateTimeFormatter DUE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
			.withZone(ZoneId.of("America/Lima"));

	private final RequestStateStore requests;
	private final CatalogLookup catalog;
	private final Clock clock;

	public ChangePriority(RequestStateStore requests, CatalogLookup catalog, Clock clock) {
		this.requests = requests;
		this.catalog = catalog;
		this.clock = clock;
	}

	/** Whether the user may change the priority of a request of this area in this status. */
	public static boolean mayChange(AuthenticatedUser user, long areaId, RequestStatus status) {
		return AccessPolicy.isSupervisorOfArea(user, areaId) && status.allowsPriorityChange();
	}

	/**
	 * Checks run in the order of the transitions: not visible (404), resolved or closed (409), not the supervisor
	 * of the area (403), invalid priority (400). The request stays locked until the change commits.
	 */
	@Transactional
	public ServiceRequest execute(AuthenticatedUser user, String code, long priorityId) {
		ServiceRequest request = requests.findByCodeForUpdate(code)
				.filter(r -> AccessPolicy.canViewRequest(user, r.requesterId(), r.areaId(), r.technicianId()))
				.orElseThrow(() -> new RequestNotFoundException(code));
		request.requirePriorityChangeAllowed();
		if (!AccessPolicy.isSupervisorOfArea(user, request.areaId())) {
			throw new RequestChangeForbiddenException("Solo el supervisor del área puede cambiar la prioridad");
		}
		PriorityLevel priority = catalog.findPriority(priorityId).orElseThrow(
				() -> new InvalidRequestException(Map.of("priorityId", "Selecciona una prioridad válida")));
		String previousName = catalog.findPriority(request.priorityId()).map(PriorityLevel::name).orElse("—");

		Instant now = clock.instant();
		ServiceRequest changed = request.withPriority(priority.id(), priority.slaHours(), now);
		RequestHistoryEntry entry = new RequestHistoryEntry(request.status(), request.status(), user.id(), null,
				"Nuevo vencimiento: " + DUE_FORMAT.format(changed.dueAt()), now,
				"Prioridad " + previousName + " → Prioridad " + priority.name(), true);
		return requests.update(changed, entry);
	}

}
