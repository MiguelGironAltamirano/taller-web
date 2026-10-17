package pe.edu.unmsm.servicios.admin.infrastructure.web;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import pe.edu.unmsm.servicios.admin.application.CreateManagedUser;
import pe.edu.unmsm.servicios.admin.application.ListManagedUsers;
import pe.edu.unmsm.servicios.admin.application.SetUserActive;
import pe.edu.unmsm.servicios.admin.application.UpdateManagedUser;
import pe.edu.unmsm.servicios.admin.domain.RequestStates;
import pe.edu.unmsm.servicios.admin.infrastructure.web.AdminDtos.ActiveRequest;
import pe.edu.unmsm.servicios.admin.infrastructure.web.AdminDtos.AdminUserResponse;
import pe.edu.unmsm.servicios.admin.infrastructure.web.AdminDtos.CreatedUserResponse;
import pe.edu.unmsm.servicios.admin.infrastructure.web.AdminDtos.StateResponse;
import pe.edu.unmsm.servicios.admin.infrastructure.web.AdminDtos.UserRequest;
import pe.edu.unmsm.servicios.shared.infrastructure.security.CurrentUser;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMINISTRADOR')")
class AdminUsersController {

	private final ListManagedUsers listUsers;
	private final CreateManagedUser createUser;
	private final UpdateManagedUser updateUser;
	private final SetUserActive setUserActive;

	AdminUsersController(ListManagedUsers listUsers, CreateManagedUser createUser, UpdateManagedUser updateUser,
			SetUserActive setUserActive) {
		this.listUsers = listUsers;
		this.createUser = createUser;
		this.updateUser = updateUser;
		this.setUserActive = setUserActive;
	}

	@GetMapping("/users")
	List<AdminUserResponse> list() {
		return listUsers.execute().stream().map(AdminUserResponse::from).toList();
	}

	@PostMapping("/users")
	ResponseEntity<CreatedUserResponse> create(@Valid @RequestBody UserRequest request) {
		CreateManagedUser.Result result = createUser.execute(request.toData());
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(CreatedUserResponse.from(result.user(), result.temporaryPassword()));
	}

	@PutMapping("/users/{id}")
	AdminUserResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable long id,
			@Valid @RequestBody UserRequest request) {
		return AdminUserResponse.from(updateUser.execute(CurrentUser.from(jwt).id(), id, request.toData()));
	}

	@PatchMapping("/users/{id}/active")
	AdminUserResponse setActive(@AuthenticationPrincipal Jwt jwt, @PathVariable long id,
			@Valid @RequestBody ActiveRequest request) {
		return AdminUserResponse.from(setUserActive.execute(CurrentUser.from(jwt).id(), id, request.active()));
	}

	@GetMapping("/states")
	List<StateResponse> states() {
		return RequestStates.ALL.stream().map(StateResponse::from).toList();
	}

}
