package pe.edu.unmsm.servicios.auth.infrastructure.web;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import pe.edu.unmsm.servicios.auth.domain.EmailAlreadyRegisteredException;
import pe.edu.unmsm.servicios.auth.domain.InvalidCredentialsException;
import pe.edu.unmsm.servicios.auth.domain.InvalidRefreshTokenException;
import pe.edu.unmsm.servicios.auth.domain.TooManyLoginAttemptsException;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
class AuthExceptionHandler {

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ProblemDetail validation(MethodArgumentNotValidException ex) {
		Map<String, String> errors = new LinkedHashMap<>();
		for (FieldError error : ex.getBindingResult().getFieldErrors()) {
			errors.putIfAbsent(error.getField(), error.getDefaultMessage());
		}
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
				"La solicitud contiene datos inválidos");
		problem.setTitle("Bad Request");
		problem.setProperty("errors", errors);
		return problem;
	}

	@ExceptionHandler(EmailAlreadyRegisteredException.class)
	ProblemDetail emailTaken() {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
				"El correo ya está registrado");
		problem.setTitle("Conflict");
		problem.setProperty("errors", Map.of("email", "El correo ya está registrado"));
		return problem;
	}

	@ExceptionHandler(InvalidCredentialsException.class)
	ProblemDetail invalidCredentials() {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Credenciales inválidas");
		problem.setTitle("Unauthorized");
		return problem;
	}

	@ExceptionHandler(TooManyLoginAttemptsException.class)
	ResponseEntity<ProblemDetail> tooManyAttempts(TooManyLoginAttemptsException ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS,
				"Demasiados intentos fallidos. Intenta de nuevo más tarde");
		problem.setTitle("Too Many Requests");
		return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
				.header(HttpHeaders.RETRY_AFTER, Long.toString(ex.retryAfterSeconds()))
				.contentType(MediaType.APPLICATION_PROBLEM_JSON)
				.body(problem);
	}

	@ExceptionHandler(InvalidRefreshTokenException.class)
	ProblemDetail invalidRefreshToken() {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED,
				"La sesión no es válida o ha expirado");
		problem.setTitle("Unauthorized");
		return problem;
	}

}
