package pe.edu.unmsm.servicios.auth.domain;

public record User(Long id, String fullName, String email, String passwordHash, Role role,
		RequesterType requesterType, Long areaId, boolean active) {
}
