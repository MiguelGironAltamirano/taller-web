package pe.edu.unmsm.servicios.requests.infrastructure.web;

import java.net.URI;
import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import pe.edu.unmsm.servicios.requests.application.ChangePriority;
import pe.edu.unmsm.servicios.requests.application.CreateRequest;
import pe.edu.unmsm.servicios.requests.application.GetRequest;
import pe.edu.unmsm.servicios.requests.application.ListRequests;
import pe.edu.unmsm.servicios.requests.application.RequestListCriteria;
import pe.edu.unmsm.servicios.requests.domain.ServiceRequest;
import pe.edu.unmsm.servicios.requests.infrastructure.web.RequestDtos.ChangePriorityBody;
import pe.edu.unmsm.servicios.requests.infrastructure.web.RequestDtos.CountsResponse;
import pe.edu.unmsm.servicios.requests.infrastructure.web.RequestDtos.CreateRequestBody;
import pe.edu.unmsm.servicios.requests.infrastructure.web.RequestDtos.DetailResponse;
import pe.edu.unmsm.servicios.requests.infrastructure.web.RequestDtos.PageResponse;
import pe.edu.unmsm.servicios.shared.domain.AuthenticatedUser;
import pe.edu.unmsm.servicios.shared.infrastructure.security.CurrentUser;

@RestController
@RequestMapping("/api/v1/requests")
class RequestsController {

	private final CreateRequest createRequest;
	private final GetRequest getRequest;
	private final ListRequests listRequests;

	private final ChangePriority changePriority;

	RequestsController(CreateRequest createRequest, GetRequest getRequest, ListRequests listRequests,
			ChangePriority changePriority) {
		this.createRequest = createRequest;
		this.getRequest = getRequest;
		this.listRequests = listRequests;
		this.changePriority = changePriority;
	}

	@PostMapping
	@PreAuthorize("hasRole('SOLICITANTE')")
	ResponseEntity<DetailResponse> create(@Valid @RequestBody CreateRequestBody body, @AuthenticationPrincipal Jwt jwt) {
		AuthenticatedUser user = CurrentUser.from(jwt);
		ServiceRequest created = createRequest.execute(user, new CreateRequest.Command(body.categoryId(),
				body.locationId(), body.description(), body.priorityId()));
		DetailResponse detail = DetailResponse.from(getRequest.execute(user, created.code()));
		return ResponseEntity.created(URI.create("/api/v1/requests/" + created.code())).body(detail);
	}

	/**
	 * Filtered, ordered and paged inbox. Parameters arrive as text and are validated together by
	 * {@link RequestListCriteria#parse}, so every invalid one is reported in a single 400.
	 */
	@GetMapping
	PageResponse list(@RequestParam(required = false) List<String> status,
			@RequestParam(required = false) String areaId, @RequestParam(required = false) String categoryId,
			@RequestParam(required = false) String priorityId, @RequestParam(required = false) String technicianId,
			@RequestParam(required = false) String unassigned, @RequestParam(required = false) String overdue,
			@RequestParam(required = false) String q, @RequestParam(required = false) String from,
			@RequestParam(required = false) String to, @RequestParam(required = false) String sort,
			@RequestParam(required = false) String page, @RequestParam(required = false) String size,
			@AuthenticationPrincipal Jwt jwt) {
		RequestListCriteria criteria = RequestListCriteria.parse(new RequestListCriteria.Params(status, areaId,
				categoryId, priorityId, technicianId, unassigned, overdue, q, from, to, sort, page, size));
		return PageResponse.from(listRequests.execute(CurrentUser.from(jwt), criteria));
	}

	/** Counters of the inbox over everything the user can see (see docs/api/ui-extensions.md). */
	@GetMapping("/counts")
	CountsResponse counts(@AuthenticationPrincipal Jwt jwt) {
		return CountsResponse.from(listRequests.counts(CurrentUser.from(jwt)));
	}

	@GetMapping("/{code}")
	DetailResponse detail(@PathVariable String code, @AuthenticationPrincipal Jwt jwt) {
		return DetailResponse.from(getRequest.execute(CurrentUser.from(jwt), code));
	}

	/** Supervisor of the area only; the due date is recalculated and the detail is returned (see ui-extensions). */
	@PatchMapping("/{code}/priority")
	@PreAuthorize("hasRole('SUPERVISOR')")
	DetailResponse changePriority(@PathVariable String code, @Valid @RequestBody ChangePriorityBody body,
			@AuthenticationPrincipal Jwt jwt) {
		AuthenticatedUser user = CurrentUser.from(jwt);
		changePriority.execute(user, code, body.priorityId());
		return DetailResponse.from(getRequest.execute(user, code));
	}

}
