package pe.edu.unmsm.servicios.satisfaction.infrastructure.web;

import java.time.Instant;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import pe.edu.unmsm.servicios.satisfaction.application.RateRequest;
import pe.edu.unmsm.servicios.satisfaction.domain.Rating;
import pe.edu.unmsm.servicios.shared.infrastructure.security.CurrentUser;

@RestController
@RequestMapping("/api/v1/requests/{code}/rating")
class RatingController {

	private final RateRequest rateRequest;

	RatingController(RateRequest rateRequest) {
		this.rateRequest = rateRequest;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	RatingResponse rate(@PathVariable String code, @Valid @RequestBody RatingBody body,
			@AuthenticationPrincipal Jwt jwt) {
		Rating rating = rateRequest.execute(CurrentUser.from(jwt), code, body.score(), body.comment());
		return new RatingResponse(rating.id(), rating.score(), rating.comment(), rating.createdAt());
	}

	record RatingBody(
			@NotNull(message = "El puntaje es obligatorio")
			@Min(value = 1, message = "El puntaje debe estar entre 1 y 5")
			@Max(value = 5, message = "El puntaje debe estar entre 1 y 5")
			Integer score,
			@Size(max = 1000, message = "El comentario no puede superar los 1000 caracteres")
			String comment) {
	}

	record RatingResponse(Long id, int score, String comment, Instant createdAt) {
	}

}
