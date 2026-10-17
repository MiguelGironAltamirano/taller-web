package pe.edu.unmsm.servicios.requests.infrastructure.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import pe.edu.unmsm.servicios.requests.domain.InvalidRequestException;
import pe.edu.unmsm.servicios.requests.domain.RequestNotFoundException;

@RestControllerAdvice
class RequestsExceptionHandler {

	@ExceptionHandler(InvalidRequestException.class)
	ProblemDetail invalid(InvalidRequestException ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
				"La solicitud contiene datos inválidos");
		problem.setTitle("Bad Request");
		problem.setProperty("errors", ex.errors());
		return problem;
	}

	@ExceptionHandler(RequestNotFoundException.class)
	ProblemDetail notFound() {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "La solicitud no existe");
		problem.setTitle("Not Found");
		return problem;
	}

}
