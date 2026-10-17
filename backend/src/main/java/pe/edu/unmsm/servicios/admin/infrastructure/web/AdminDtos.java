package pe.edu.unmsm.servicios.admin.infrastructure.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import pe.edu.unmsm.servicios.admin.application.UserData;
import pe.edu.unmsm.servicios.admin.domain.ManagedUser;
import pe.edu.unmsm.servicios.admin.domain.RequestState;
import pe.edu.unmsm.servicios.auth.domain.RequesterType;
import pe.edu.unmsm.servicios.auth.domain.Role;

final class AdminDtos {

	private AdminDtos() {
	}

	record UserRequest(
			@NotBlank(message = "El nombre completo es obligatorio")
			@Size(min = 3, max = 150, message = "El nombre completo debe tener entre 3 y 150 caracteres") String fullName,
			@NotBlank(message = "El correo es obligatorio")
			@Size(max = 150, message = "El correo no puede superar los 150 caracteres")
			@Pattern(regexp = "(?i)^\\s*[^@\\s]+@unmsm\\.edu\\.pe\\s*$",
					message = "El correo debe terminar en @unmsm.edu.pe") String email,
			@NotNull(message = "El rol es obligatorio") Role role,
			Long areaId,
			RequesterType requesterType) {

		UserData toData() {
			return new UserData(fullName, email, role, areaId, requesterType);
		}

	}

	record ActiveRequest(@NotNull(message = "Indica si la cuenta queda activa") Boolean active) {
	}

	record AdminUserResponse(long id, String fullName, String email, String role, Long areaId, String areaName,
			String requesterType, boolean active) {

		static AdminUserResponse from(ManagedUser user) {
			return new AdminUserResponse(user.id(), user.fullName(), user.email(), user.role().name(), user.areaId(),
					user.areaName(), user.requesterType() == null ? null : user.requesterType().name(), user.active());
		}

	}

	/** Response of the creation only: the one moment the temporary password is shown. */
	record CreatedUserResponse(long id, String fullName, String email, String role, Long areaId, String areaName,
			String requesterType, boolean active, String temporaryPassword) {

		static CreatedUserResponse from(ManagedUser user, String temporaryPassword) {
			AdminUserResponse base = AdminUserResponse.from(user);
			return new CreatedUserResponse(base.id(), base.fullName(), base.email(), base.role(), base.areaId(),
					base.areaName(), base.requesterType(), base.active(), temporaryPassword);
		}

	}

	record StateResponse(int order, String status, String description) {

		static StateResponse from(RequestState state) {
			return new StateResponse(state.order(), state.status(), state.description());
		}

	}

}
