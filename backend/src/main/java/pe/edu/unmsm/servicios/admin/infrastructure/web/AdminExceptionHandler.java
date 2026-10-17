package pe.edu.unmsm.servicios.admin.infrastructure.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import pe.edu.unmsm.servicios.admin.domain.CatalogConflictException;
import pe.edu.unmsm.servicios.admin.domain.CatalogEntryNotFoundException;
import pe.edu.unmsm.servicios.admin.domain.InvalidCatalogDataException;
import pe.edu.unmsm.servicios.admin.domain.InvalidUserDataException;
import pe.edu.unmsm.servicios.admin.domain.ManagedUserNotFoundException;
import pe.edu.unmsm.servicios.admin.domain.SelfManagementException;

/** Validation of @Valid bodies and duplicate emails are handled by the auth module's global handler. */
@RestControllerAdvice(assignableTypes = { AdminUsersController.class, AdminCatalogsController.class })
class AdminExceptionHandler {

	@ExceptionHandler(InvalidCatalogDataException.class)
	ProblemDetail invalidCatalogData(InvalidCatalogDataException ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
				"La solicitud contiene datos inválidos");
		problem.setTitle("Bad Request");
		problem.setProperty("errors", ex.errors());
		return problem;
	}

	@ExceptionHandler(CatalogEntryNotFoundException.class)
	ProblemDetail catalogEntryNotFound(CatalogEntryNotFoundException ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
		problem.setTitle("Not Found");
		return problem;
	}

	@ExceptionHandler(CatalogConflictException.class)
	ProblemDetail catalogConflict(CatalogConflictException ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
		problem.setTitle("Conflict");
		if (!ex.errors().isEmpty()) {
			problem.setProperty("errors", ex.errors());
		}
		return problem;
	}

	@ExceptionHandler(InvalidUserDataException.class)
	ProblemDetail invalidData(InvalidUserDataException ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
				"La solicitud contiene datos inválidos");
		problem.setTitle("Bad Request");
		problem.setProperty("errors", ex.errors());
		return problem;
	}

	@ExceptionHandler(ManagedUserNotFoundException.class)
	ProblemDetail notFound() {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "El usuario no existe");
		problem.setTitle("Not Found");
		return problem;
	}

	@ExceptionHandler(SelfManagementException.class)
	ProblemDetail selfManagement(SelfManagementException ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
		problem.setTitle("Conflict");
		return problem;
	}

}
