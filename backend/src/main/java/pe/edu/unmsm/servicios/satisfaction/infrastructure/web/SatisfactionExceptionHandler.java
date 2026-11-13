package pe.edu.unmsm.servicios.satisfaction.infrastructure.web;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import pe.edu.unmsm.servicios.satisfaction.domain.InvalidRatingException;
import pe.edu.unmsm.servicios.satisfaction.domain.RatingAlreadyExistsException;
import pe.edu.unmsm.servicios.satisfaction.domain.RatingNotAllowedException;

@RestControllerAdvice
class SatisfactionExceptionHandler {

	@ExceptionHandler(InvalidRatingException.class)
	ProblemDetail invalid(InvalidRatingException ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
				"La solicitud contiene datos inválidos");
		problem.setTitle("Bad Request");
		problem.setProperty("errors", Map.of(ex.field(), ex.getMessage()));
		return problem;
	}

	@ExceptionHandler(RatingNotAllowedException.class)
	ProblemDetail notAllowed() {
		return conflict("Solo se puede calificar una solicitud cuya solución confirmaste");
	}

	@ExceptionHandler(RatingAlreadyExistsException.class)
	ProblemDetail alreadyRated() {
		return conflict("Ya calificaste esta solicitud");
	}

	private static ProblemDetail conflict(String detail) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, detail);
		problem.setTitle("Conflict");
		return problem;
	}

}
