package pe.edu.unmsm.servicios.notifications.application;

public class NotificationNotFoundException extends RuntimeException {

	public NotificationNotFoundException(long id) {
		super("Notificación no encontrada o no pertenece al usuario: " + id);
	}

}
