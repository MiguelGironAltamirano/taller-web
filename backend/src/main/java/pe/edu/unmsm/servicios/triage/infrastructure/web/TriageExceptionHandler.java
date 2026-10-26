package pe.edu.unmsm.servicios.triage.infrastructure.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import pe.edu.unmsm.servicios.triage.domain.InvalidTriageRuleException;
import pe.edu.unmsm.servicios.triage.domain.TriageRuleNotFoundException;

/** Validation of @Valid bodies is handled by the auth module's global handler. */
@RestControllerAdvice(assignableTypes = { TriageController.class, AdminTriageRulesController.class })
class TriageExceptionHandler {

	@ExceptionHandler(InvalidTriageRuleException.class)
	ProblemDetail invalidRule(InvalidTriageRuleException ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
				"La solicitud contiene datos inválidos");
		problem.setTitle("Bad Request");
		problem.setProperty("errors", ex.errors());
		return problem;
	}

	@ExceptionHandler(TriageRuleNotFoundException.class)
	ProblemDetail notFound(TriageRuleNotFoundException ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
		problem.setTitle("Not Found");
		return problem;
	}

}
