package pe.edu.unmsm.servicios.dashboard.domain;

/** The user is not allowed to see these indicators. */
public class DashboardAccessDeniedException extends RuntimeException {

	public DashboardAccessDeniedException(String message) {
		super(message);
	}

}
