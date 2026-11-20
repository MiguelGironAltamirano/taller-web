package pe.edu.unmsm.servicios.workflow.infrastructure.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import pe.edu.unmsm.servicios.workflow.domain.InvalidTransitionDataException;
import pe.edu.unmsm.servicios.workflow.domain.InvalidTransitionException;
import pe.edu.unmsm.servicios.workflow.domain.RestoreExpiredException;
import pe.edu.unmsm.servicios.workflow.domain.TransitionForbiddenException;

@RestControllerAdvice
class WorkflowExceptionHandler {

	@ExceptionHandler(InvalidTransitionException.class)
	ProblemDetail conflict(InvalidTransitionException ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Esta solicitud no puede pasar de «"
				+ ex.from().label() + "» a «" + ex.to().label() + "». Actualiza la página para ver su estado actual");
		problem.setTitle("Conflict");
		return problem;
	}


	/** Another change to the same request was committed first (the locked read makes this very rare). */
	@ExceptionHandler(ObjectOptimisticLockingFailureException.class)
	ProblemDetail concurrentChange() {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
				"La solicitud cambió mientras la procesábamos. Vuelve a intentarlo");
		problem.setTitle("Conflict");
		return problem;
	}

	@ExceptionHandler(RestoreExpiredException.class)
	ProblemDetail restoreExpired() {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
				"Pasaron más de 7 días desde que la cancelaste; ya no se puede restaurar");
		problem.setTitle("Conflict");
		return problem;
	}

	@ExceptionHandler(TransitionForbiddenException.class)
	ProblemDetail forbidden(TransitionForbiddenException ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
		problem.setTitle("Forbidden");
		return problem;
	}

	@ExceptionHandler(InvalidTransitionDataException.class)
	ProblemDetail invalidData(InvalidTransitionDataException ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
				"La solicitud contiene datos inválidos");
		problem.setTitle("Bad Request");
		problem.setProperty("errors", ex.errors());
		return problem;
	}

}
