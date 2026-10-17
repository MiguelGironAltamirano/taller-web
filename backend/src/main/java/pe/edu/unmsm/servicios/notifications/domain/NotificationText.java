package pe.edu.unmsm.servicios.notifications.domain;

public final class NotificationText {

	private NotificationText() {
	}

	public static String forTransition(String requestCode, String statusLabel) {
		return "La solicitud " + requestCode + " pasó a " + statusLabel + ".";
	}

	public static String forAssignment(String requestCode) {
		return "Se te asignó la solicitud " + requestCode + ".";
	}

	public static String forReassignment(String requestCode) {
		return "Se te reasignó la solicitud " + requestCode + ".";
	}

	public static String forComment(String authorName, String requestCode) {
		return authorName + " comentó en la solicitud " + requestCode + ".";
	}

}
