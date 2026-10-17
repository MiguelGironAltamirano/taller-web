package pe.edu.unmsm.servicios.shared.domain;

public record AuthenticatedUser(long id, String email, String fullName, String role) {
}
