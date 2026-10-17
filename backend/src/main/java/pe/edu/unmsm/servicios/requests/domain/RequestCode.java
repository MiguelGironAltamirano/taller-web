package pe.edu.unmsm.servicios.requests.domain;

/** Request code {@code SOL-AAAA-NNNNNN}: sequential per year, unique and immutable. */
public record RequestCode(int year, int sequence) {

	public RequestCode {
		if (year < 2000 || sequence < 1) {
			throw new IllegalArgumentException("Invalid request code: year=" + year + ", sequence=" + sequence);
		}
	}

	public String value() {
		return "SOL-%04d-%06d".formatted(year, sequence);
	}

}
