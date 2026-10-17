package pe.edu.unmsm.servicios.admin.infrastructure.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import pe.edu.unmsm.servicios.admin.application.ManageCatalogs;
import pe.edu.unmsm.servicios.admin.domain.AdminArea;
import pe.edu.unmsm.servicios.admin.domain.AdminCategory;
import pe.edu.unmsm.servicios.catalog.domain.Location;
import pe.edu.unmsm.servicios.catalog.domain.PriorityLevel;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMINISTRADOR')")
class AdminCatalogsController {

	private final ManageCatalogs catalogs;

	AdminCatalogsController(ManageCatalogs catalogs) {
		this.catalogs = catalogs;
	}

	@PostMapping("/areas")
	ResponseEntity<AreaResponse> createArea(@Valid @RequestBody AreaRequest request) {
		AdminArea area = catalogs.createArea(request.name());
		return ResponseEntity.status(HttpStatus.CREATED).body(new AreaResponse(area.id(), area.name()));
	}

	@DeleteMapping("/areas/{id}")
	ResponseEntity<Void> deactivateArea(@PathVariable long id) {
		catalogs.deactivateArea(id);
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/categories")
	ResponseEntity<CategoryResponse> createCategory(@Valid @RequestBody CategoryRequest request) {
		AdminCategory category = catalogs.createCategory(request.name(), request.areaId());
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(new CategoryResponse(category.id(), category.name(), category.areaId()));
	}

	@DeleteMapping("/categories/{id}")
	ResponseEntity<Void> deactivateCategory(@PathVariable long id) {
		catalogs.deactivateCategory(id);
		return ResponseEntity.noContent().build();
	}

	@PutMapping("/priorities/{id}")
	PriorityResponse updatePriority(@PathVariable long id, @Valid @RequestBody PriorityRequest request) {
		PriorityLevel priority = catalogs.updatePrioritySla(id, request.slaHours());
		return new PriorityResponse(priority.id(), priority.name(), priority.slaHours());
	}

	@PostMapping("/locations")
	ResponseEntity<LocationResponse> createLocation(@Valid @RequestBody LocationRequest request) {
		Location room = catalogs.createLocation(request.faculty(), request.pavilion(), request.room());
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(new LocationResponse(room.id(), room.name(), room.kind().name(), room.parentId()));
	}

	@DeleteMapping("/locations/{id}")
	ResponseEntity<Void> deleteLocation(@PathVariable long id) {
		catalogs.deleteLocation(id);
		return ResponseEntity.noContent().build();
	}

	record AreaRequest(
			@NotBlank(message = "El nombre es obligatorio")
			@Size(max = 100, message = "El nombre no puede superar los 100 caracteres") String name) {
	}

	record CategoryRequest(
			@NotBlank(message = "El nombre es obligatorio")
			@Size(max = 100, message = "El nombre no puede superar los 100 caracteres") String name,
			@NotNull(message = "El área es obligatoria") Long areaId) {
	}

	record PriorityRequest(@NotNull(message = "Las horas de SLA son obligatorias") Integer slaHours) {
	}

	record LocationRequest(
			@NotBlank(message = "La facultad es obligatoria")
			@Size(max = 150, message = "La facultad no puede superar los 150 caracteres") String faculty,
			@NotBlank(message = "El pabellón es obligatorio")
			@Size(max = 150, message = "El pabellón no puede superar los 150 caracteres") String pavilion,
			@NotBlank(message = "El ambiente es obligatorio")
			@Size(max = 150, message = "El ambiente no puede superar los 150 caracteres") String room) {
	}

	record AreaResponse(long id, String name) {
	}

	record CategoryResponse(long id, String name, long areaId) {
	}

	record PriorityResponse(long id, String name, int slaHours) {
	}

	record LocationResponse(long id, String name, String kind, Long parentId) {
	}

}
