package pe.edu.unmsm.servicios.satisfaction.domain;

public class InvalidRatingException extends RuntimeException {

	private final String field;

	public InvalidRatingException(String field, String message) {
		super(message);
		this.field = field;
	}

	public String field() {
		return field;
	}

}
