package pe.edu.unmsm.servicios.collaboration.domain;

public class FileTypeNotAllowedException extends RuntimeException {

	public FileTypeNotAllowedException(String message) {
		super(message);
	}

}
