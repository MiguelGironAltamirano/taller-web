package pe.edu.unmsm.servicios.satisfaction.infrastructure.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import pe.edu.unmsm.servicios.satisfaction.domain.InvalidRatingException;
import pe.edu.unmsm.servicios.satisfaction.domain.RatingNotAllowedException;

@RestControllerAdvice
class SatisfactionExceptionHandler {

	@ExceptionHandler(RatingNotAllowedException.class)
	ProblemDetail notAllowed(RatingNotAllowedException ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
		problem.setTitle("Conflict");
		return problem;
	}

	@ExceptionHandler(InvalidRatingException.class)
	ProblemDetail invalid(InvalidRatingException ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
				"La calificación contiene datos inválidos");
		problem.setTitle("Bad Request");
		problem.setProperty("errors", ex.errors());
		return problem;
	}

}
