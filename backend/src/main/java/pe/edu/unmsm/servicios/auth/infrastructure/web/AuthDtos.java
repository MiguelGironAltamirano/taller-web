package pe.edu.unmsm.servicios.auth.infrastructure.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import pe.edu.unmsm.servicios.auth.domain.RequesterType;
import pe.edu.unmsm.servicios.auth.domain.User;

final class AuthDtos {

	private AuthDtos() {
	}

	record RegisterRequest(
			@NotBlank(message = "El nombre completo es obligatorio")
			@Size(min = 3, max = 150, message = "El nombre completo debe tener entre 3 y 150 caracteres") String fullName,
			@NotBlank(message = "El correo es obligatorio")
			@Size(max = 150, message = "El correo no puede superar los 150 caracteres")
			@Pattern(regexp = "(?i)^[^@\\s]+@unmsm\\.edu\\.pe$",
					message = "El correo debe terminar en @unmsm.edu.pe") String email,
			@NotBlank(message = "La contraseña es obligatoria")
			@Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
			@Pattern(regexp = "^(?=.*\\p{L})(?=.*\\d).*$",
					message = "La contraseña debe incluir al menos una letra y un dígito") String password,
			@NotNull(message = "El tipo de solicitante es obligatorio") RequesterType requesterType) {
	}

	record LoginRequest(
			@NotBlank(message = "El correo es obligatorio") String email,
			@NotBlank(message = "La contraseña es obligatoria") String password) {
	}

	record UserResponse(long id, String fullName, String email, String role, Long areaId) {

		static UserResponse from(User user) {
			return new UserResponse(user.id(), user.fullName(), user.email(), user.role().name(), user.areaId());
		}

	}

	record TokenResponse(String accessToken, long expiresIn, UserResponse user) {
	}

}
