package pe.edu.unmsm.servicios.dashboard.infrastructure.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import pe.edu.unmsm.servicios.dashboard.domain.DashboardAccessDeniedException;
import pe.edu.unmsm.servicios.dashboard.domain.InvalidDashboardQueryException;

@RestControllerAdvice
class DashboardExceptionHandler {

	@ExceptionHandler(InvalidDashboardQueryException.class)
	ProblemDetail invalidQuery(InvalidDashboardQueryException ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
				"La consulta contiene parámetros inválidos");
		problem.setTitle("Bad Request");
		problem.setProperty("errors", ex.errors());
		return problem;
	}

	@ExceptionHandler(DashboardAccessDeniedException.class)
	ProblemDetail forbidden(DashboardAccessDeniedException ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
		problem.setTitle("Forbidden");
		return problem;
	}

}
