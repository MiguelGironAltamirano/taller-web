package pe.edu.unmsm.servicios.satisfaction.infrastructure.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import pe.edu.unmsm.servicios.satisfaction.domain.InvalidRatingException;
import pe.edu.unmsm.servicios.satisfaction.domain.RatingAlreadyExistsException;
import pe.edu.unmsm.servicios.satisfaction.domain.RequestNotResolvedException;

@RestControllerAdvice
class SatisfactionExceptionHandler {

	@ExceptionHandler(InvalidRatingException.class)
	ProblemDetail badRequest(InvalidRatingException ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
		problem.setTitle("Bad Request");
		return problem;
	}

	@ExceptionHandler(RequestNotResolvedException.class)
	ProblemDetail conflict(RequestNotResolvedException ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
		problem.setTitle("Conflict");
		return problem;
	}

	@ExceptionHandler(RatingAlreadyExistsException.class)
	ProblemDetail alreadyRated(RatingAlreadyExistsException ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
		problem.setTitle("Conflict");
		return problem;
	}

}
