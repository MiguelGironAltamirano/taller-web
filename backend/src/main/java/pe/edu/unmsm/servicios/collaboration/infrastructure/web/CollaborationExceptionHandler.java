package pe.edu.unmsm.servicios.collaboration.infrastructure.web;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import pe.edu.unmsm.servicios.collaboration.domain.CommentTextInvalidException;

@RestControllerAdvice
class CollaborationExceptionHandler {

	@ExceptionHandler(CommentTextInvalidException.class)
	ProblemDetail invalidCommentText(CommentTextInvalidException ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
		problem.setTitle("Bad Request");
		problem.setProperty("errors", Map.of("text", ex.getMessage()));
		return problem;
	}

	@ExceptionHandler(pe.edu.unmsm.servicios.collaboration.domain.FileTypeNotAllowedException.class)
	ProblemDetail fileTypeNotAllowed(pe.edu.unmsm.servicios.collaboration.domain.FileTypeNotAllowedException ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
		problem.setTitle("Bad Request");
		problem.setProperty("errors", Map.of("file", ex.getMessage()));
		return problem;
	}

	@ExceptionHandler({
			pe.edu.unmsm.servicios.collaboration.domain.EvidenceLimitExceededException.class,
			pe.edu.unmsm.servicios.collaboration.domain.EvidenceSizeLimitExceededException.class,
			pe.edu.unmsm.servicios.collaboration.domain.EmptyFileException.class
	})
	ProblemDetail evidenceRulesViolation(RuntimeException ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
		problem.setTitle("Bad Request");
		problem.setProperty("errors", Map.of("file", ex.getMessage()));
		return problem;
	}

	@ExceptionHandler(pe.edu.unmsm.servicios.collaboration.application.EvidenceNotFoundException.class)
	ProblemDetail evidenceNotFound(pe.edu.unmsm.servicios.collaboration.application.EvidenceNotFoundException ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
		problem.setTitle("Not Found");
		return problem;
	}

	@ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
	ProblemDetail maxUploadSize(org.springframework.web.multipart.MaxUploadSizeExceededException ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
				"El archivo supera el tamaño máximo permitido de 5 MB");
		problem.setTitle("Bad Request");
		problem.setProperty("errors", Map.of("file", "El archivo supera el tamaño máximo permitido de 5 MB"));
		return problem;
	}

}
