package pe.edu.unmsm.servicios.requests.infrastructure.web;

import java.net.URI;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import pe.edu.unmsm.servicios.requests.application.CreateRequest;
import pe.edu.unmsm.servicios.requests.application.GetRequest;
import pe.edu.unmsm.servicios.requests.application.ListRequests;
import pe.edu.unmsm.servicios.requests.domain.RequestStatus;
import pe.edu.unmsm.servicios.requests.domain.ServiceRequest;
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

	RequestsController(CreateRequest createRequest, GetRequest getRequest, ListRequests listRequests) {
		this.createRequest = createRequest;
		this.getRequest = getRequest;
		this.listRequests = listRequests;
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

	@GetMapping
	PageResponse list(@RequestParam(required = false) RequestStatus status,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
			@AuthenticationPrincipal Jwt jwt) {
		return PageResponse.from(listRequests.execute(CurrentUser.from(jwt), status, page, size));
	}

	@GetMapping("/{code}")
	DetailResponse detail(@PathVariable String code, @AuthenticationPrincipal Jwt jwt) {
		return DetailResponse.from(getRequest.execute(CurrentUser.from(jwt), code));
	}

}
