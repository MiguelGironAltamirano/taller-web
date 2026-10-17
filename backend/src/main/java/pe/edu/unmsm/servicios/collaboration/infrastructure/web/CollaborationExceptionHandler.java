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

}
