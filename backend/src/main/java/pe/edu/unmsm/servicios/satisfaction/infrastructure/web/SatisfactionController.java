package pe.edu.unmsm.servicios.satisfaction.infrastructure.web;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import pe.edu.unmsm.servicios.requests.application.GetRequest;
import pe.edu.unmsm.servicios.requests.infrastructure.web.RequestDtos.DetailResponse;
import pe.edu.unmsm.servicios.satisfaction.application.RateRequest;
import pe.edu.unmsm.servicios.shared.domain.AuthenticatedUser;
import pe.edu.unmsm.servicios.shared.infrastructure.security.CurrentUser;

@RestController
@RequestMapping("/api/v1/requests/{code}/rating")
class SatisfactionController {

	private final RateRequest rateRequest;
	private final GetRequest getRequest;

	SatisfactionController(RateRequest rateRequest, GetRequest getRequest) {
		this.rateRequest = rateRequest;
		this.getRequest = getRequest;
	}

	/** Score from 1 to 5 and optional comment; validated by the domain after the 404, 403 and 409 checks. */
	record RateBody(Integer score, String comment) {
	}

	/** The owning requester rates a request closed by confirmation; the response is the updated detail. */
	@PostMapping
	@PreAuthorize("hasRole('SOLICITANTE')")
	DetailResponse rate(@PathVariable String code, @RequestBody RateBody body, @AuthenticationPrincipal Jwt jwt) {
		AuthenticatedUser user = CurrentUser.from(jwt);
		rateRequest.execute(user, code, body.score(), body.comment());
		return DetailResponse.from(getRequest.execute(user, code));
	}

}
