package pe.edu.unmsm.servicios.workflow.infrastructure.web;

import jakarta.validation.Valid;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import pe.edu.unmsm.servicios.requests.application.GetRequest;
import pe.edu.unmsm.servicios.requests.infrastructure.web.RequestDtos.DetailResponse;
import pe.edu.unmsm.servicios.shared.domain.AuthenticatedUser;
import pe.edu.unmsm.servicios.shared.infrastructure.security.CurrentUser;
import pe.edu.unmsm.servicios.workflow.application.TransitionRequest;
import pe.edu.unmsm.servicios.workflow.domain.TransitionCommand;
import pe.edu.unmsm.servicios.workflow.infrastructure.web.WorkflowDtos.TransitionBody;

@RestController
@RequestMapping("/api/v1/requests/{code}/transitions")
class TransitionsController {

	private final TransitionRequest transitionRequest;
	private final GetRequest getRequest;

	TransitionsController(TransitionRequest transitionRequest, GetRequest getRequest) {
		this.transitionRequest = transitionRequest;
		this.getRequest = getRequest;
	}

	/**
	 * Who may do what is decided by the state machine (403 / 409 / 400), not by the URL; an unknown code is 404.
	 * The response is the updated detail, as the contract says.
	 */
	@PostMapping
	DetailResponse transition(@PathVariable String code, @Valid @RequestBody TransitionBody body,
			@AuthenticationPrincipal Jwt jwt) {
		AuthenticatedUser user = CurrentUser.from(jwt);
		transitionRequest.execute(user, code,
				new TransitionCommand(body.to(), body.reason(), body.technicianId(), body.closureReason()));
		return DetailResponse.from(getRequest.execute(user, code));
	}

}
