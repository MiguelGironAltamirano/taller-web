package pe.edu.unmsm.servicios.notifications.infrastructure.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import pe.edu.unmsm.servicios.notifications.application.NotificationNotFoundException;

@RestControllerAdvice(basePackageClasses = NotificationsController.class)
public class NotificationsExceptionHandler {

	@ExceptionHandler(NotificationNotFoundException.class)
	ProblemDetail handleNotificationNotFound(NotificationNotFoundException ex) {
		ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
		pd.setTitle("Notificación no encontrada");
		return pd;
	}

}
