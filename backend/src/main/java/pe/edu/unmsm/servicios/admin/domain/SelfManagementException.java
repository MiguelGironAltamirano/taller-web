package pe.edu.unmsm.servicios.admin.domain;

/** An administrator tried to deactivate their own account or change their own role. */
public class SelfManagementException extends RuntimeException {

	public SelfManagementException(String message) {
		super(message);
	}

}
