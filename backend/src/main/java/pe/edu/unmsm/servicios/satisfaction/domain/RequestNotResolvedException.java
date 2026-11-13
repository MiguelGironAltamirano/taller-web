package pe.edu.unmsm.servicios.satisfaction.domain;

public class RequestNotResolvedException extends RuntimeException {

	public RequestNotResolvedException(String code) {
		super("La solicitud " + code + " no está en estado Resuelta");
	}

}
