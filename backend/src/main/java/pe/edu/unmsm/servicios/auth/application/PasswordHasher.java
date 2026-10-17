package pe.edu.unmsm.servicios.auth.application;

public interface PasswordHasher {

	String hash(String rawPassword);

	boolean matches(String rawPassword, String hash);

}
