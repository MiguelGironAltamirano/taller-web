package pe.edu.unmsm.servicios.auth.application;

public interface RefreshTokenCodec {

	/** Generates a new opaque, URL-safe random token value. */
	String generate();

	/** Returns the lowercase SHA-256 hex digest used to store the token. */
	String hash(String token);

}
