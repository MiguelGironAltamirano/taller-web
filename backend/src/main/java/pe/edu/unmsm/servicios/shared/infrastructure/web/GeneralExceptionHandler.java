package pe.edu.unmsm.servicios.shared.infrastructure.web;

import java.util.Map;

import org.springframework.beans.TypeMismatchException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * The errors Spring raises before a request reaches a module (malformed JSON, a missing parameter, an unknown route...)
 * answered like every other error of the API: problem details whose {@code detail} is a Spanish sentence a person can
 * act on, never the framework's English text (docs/api/contrato-v1.md «Errores»). Module handlers keep their own
 * messages; this one only covers what none of them handles.
 */
@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
class GeneralExceptionHandler extends ResponseEntityExceptionHandler {

	static final String UNEXPECTED = "Ocurrió un error inesperado. Inténtalo de nuevo en unos minutos";
	/** Same text as the evidence upload (CollaborationExceptionHandler), whichever handler answers. */
	static final String FILE_TOO_LARGE = "El archivo supera el tamaño máximo permitido de 5 MB";

	@Override
	protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
			HttpStatusCode status, WebRequest request) {
		// The framework builds the body of most of these exceptions inside the parent method, so it is translated after.
		ResponseEntity<Object> response = super.handleExceptionInternal(ex, body, headers, status, request);
		if (response != null && response.getBody() instanceof ProblemDetail problem) {
			problem.setDetail(spanishDetail(ex, status));
			if (ex instanceof MaxUploadSizeExceededException) {
				problem.setProperty("errors", Map.of("file", FILE_TOO_LARGE));
			}
		}
		return response;
	}

	static String spanishDetail(Exception ex, HttpStatusCode status) {
		return switch (ex) {
			case HttpMessageNotReadableException e -> "Los datos enviados no tienen el formato esperado";
			case MissingServletRequestParameterException e -> "Falta el dato «" + e.getParameterName() + "»";
			case TypeMismatchException e when e.getPropertyName() != null ->
				"El dato «" + e.getPropertyName() + "» no es válido";
			case HttpRequestMethodNotSupportedException e -> "Esta operación no está disponible";
			case HttpMediaTypeNotSupportedException e -> "El formato de los datos enviados no es compatible";
			case HttpMediaTypeNotAcceptableException e -> "No podemos responder en el formato solicitado";
			case NoResourceFoundException e -> "La dirección solicitada no existe";
			case NoHandlerFoundException e -> "La dirección solicitada no existe";
			case MaxUploadSizeExceededException e -> FILE_TOO_LARGE;
			default -> status.is5xxServerError() ? UNEXPECTED : "No pudimos procesar la solicitud. Revisa los datos";
		};
	}

}
