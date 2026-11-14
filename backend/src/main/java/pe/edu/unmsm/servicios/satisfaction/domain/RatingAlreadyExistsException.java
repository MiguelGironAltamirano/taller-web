package pe.edu.unmsm.servicios.satisfaction.domain;

public class RatingAlreadyExistsException extends RuntimeException {

	public RatingAlreadyExistsException(String code) {
		super("La solicitud " + code + " ya fue calificada");
	}

}
