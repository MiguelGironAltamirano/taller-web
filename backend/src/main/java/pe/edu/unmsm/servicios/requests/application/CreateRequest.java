package pe.edu.unmsm.servicios.requests.application;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.transaction.annotation.Transactional;

import pe.edu.unmsm.servicios.catalog.application.CatalogLookup;
import pe.edu.unmsm.servicios.catalog.domain.Category;
import pe.edu.unmsm.servicios.catalog.domain.Location;
import pe.edu.unmsm.servicios.catalog.domain.LocationKind;
import pe.edu.unmsm.servicios.catalog.domain.PriorityLevel;
import pe.edu.unmsm.servicios.requests.domain.InvalidRequestException;
import pe.edu.unmsm.servicios.requests.domain.RequestCode;
import pe.edu.unmsm.servicios.requests.domain.RequestHistoryEntry;
import pe.edu.unmsm.servicios.requests.domain.RequestStatus;
import pe.edu.unmsm.servicios.requests.domain.ServiceRequest;
import pe.edu.unmsm.servicios.shared.domain.AuthenticatedUser;

public class CreateRequest {

	/** The year of the code follows the institution's calendar (Lima), not UTC. */
	static final ZoneId CODE_ZONE = ZoneId.of("America/Lima");

	public record Command(long categoryId, long locationId, String description, long priorityId) {
	}

	private final CatalogLookup catalog;
	private final RequestCodeGenerator codes;
	private final RequestRepository requests;
	private final Clock clock;

	public CreateRequest(CatalogLookup catalog, RequestCodeGenerator codes, RequestRepository requests, Clock clock) {
		this.catalog = catalog;
		this.codes = codes;
		this.requests = requests;
		this.clock = clock;
	}

	@Transactional
	public ServiceRequest execute(AuthenticatedUser requester, Command command) {
		Map<String, String> errors = new LinkedHashMap<>();
		ServiceRequest.descriptionError(command.description()).ifPresent(message -> errors.put("description", message));
		Optional<Category> category = catalog.findActiveCategory(command.categoryId());
		if (category.isEmpty()) {
			errors.put("categoryId", "Selecciona una categoría válida");
		}
		Optional<Location> room = catalog.findLocation(command.locationId())
				.filter(location -> location.kind() == LocationKind.AMBIENTE);
		if (room.isEmpty()) {
			errors.put("locationId", "Selecciona un ambiente válido");
		}
		Optional<PriorityLevel> priority = catalog.findPriority(command.priorityId());
		if (priority.isEmpty()) {
			errors.put("priorityId", "Selecciona una prioridad válida");
		}
		if (!errors.isEmpty()) {
			throw new InvalidRequestException(errors);
		}

		Instant now = clock.instant();
		RequestCode code = codes.next(now.atZone(CODE_ZONE).getYear());
		// The area is always the default area of the category (spec 5.1).
		ServiceRequest request = ServiceRequest.register(code.value(), requester.id(), category.get().id(),
				category.get().areaId(), room.get().id(), command.description(), priority.get().id(),
				priority.get().slaHours(), now);
		RequestHistoryEntry registered = new RequestHistoryEntry(null, RequestStatus.REGISTRADA, requester.id(), null,
				"Solicitud creada", now);
		return requests.save(request, registered);
	}

}
