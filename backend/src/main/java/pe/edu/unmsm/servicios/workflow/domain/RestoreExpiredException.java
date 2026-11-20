package pe.edu.unmsm.servicios.workflow.domain;

/** The request was cancelled longer ago than the restore window allows (409). */
public class RestoreExpiredException extends RuntimeException {

	public RestoreExpiredException() {
		super("The request can no longer be restored");
	}

}
