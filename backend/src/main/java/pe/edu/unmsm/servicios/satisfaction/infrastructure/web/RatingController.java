package pe.edu.unmsm.servicios.satisfaction.infrastructure.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

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
class RatingController {

	private final RateRequest rateRequest;
	private final GetRequest getRequest;

	RatingController(RateRequest rateRequest, GetRequest getRequest) {
		this.rateRequest = rateRequest;
		this.getRequest = getRequest;
	}

	/** The contract answers with the updated detail, which now carries the rating. */
	@PostMapping
	DetailResponse rate(@PathVariable String code, @Valid @RequestBody RatingBody body,
			@AuthenticationPrincipal Jwt jwt) {
		AuthenticatedUser user = CurrentUser.from(jwt);
		rateRequest.execute(user, code, body.score(), body.comment());
		return DetailResponse.from(getRequest.execute(user, code));
	}

	record RatingBody(
			@NotNull(message = "Califica de 1 a 5 estrellas")
			@Min(value = 1, message = "Califica de 1 a 5 estrellas")
			@Max(value = 5, message = "Califica de 1 a 5 estrellas")
			Integer score,
			@Size(max = 1000, message = "El comentario no puede superar los 1000 caracteres")
			String comment) {
	}

}
