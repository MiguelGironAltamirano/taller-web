package pe.edu.unmsm.servicios.satisfaction.domain;

/** Only a request whose solution the requester confirmed (Cerrada, CONFIRMADA) can be rated. */
public class RatingNotAllowedException extends RuntimeException {

	public RatingNotAllowedException(String code) {
		super("Solo se puede calificar una solicitud cuya solución confirmaste: " + code);
	}

}
