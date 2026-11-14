package pe.edu.unmsm.servicios.notifications.domain;

public final class NotificationText {

	private NotificationText() {
	}

	public static String forTransition(String requestCode, String statusLabel) {
		return "La solicitud " + requestCode + " pasó a " + statusLabel + ".";
	}

	/** The closure made by the system when the requester did not answer the solution in time. */
	public static String forAutomaticClosure(String requestCode) {
		return "La solicitud " + requestCode + " se cerró automáticamente porque la solución no se confirmó a tiempo.";
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
