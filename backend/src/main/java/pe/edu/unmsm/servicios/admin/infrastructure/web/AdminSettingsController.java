package pe.edu.unmsm.servicios.admin.infrastructure.web;

import java.time.Instant;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import pe.edu.unmsm.servicios.admin.application.ListAuditEvents;
import pe.edu.unmsm.servicios.admin.application.ManageSystemSettings;
import pe.edu.unmsm.servicios.admin.domain.AuditEvent;
import pe.edu.unmsm.servicios.shared.domain.SystemSettings;
import pe.edu.unmsm.servicios.shared.infrastructure.security.CurrentUser;

/** System configuration and the audit log derived from the request history. */
@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMINISTRADOR')")
class AdminSettingsController {

	private final ManageSystemSettings settings;
	private final ListAuditEvents audit;

	AdminSettingsController(ManageSystemSettings settings, ListAuditEvents audit) {
		this.settings = settings;
		this.audit = audit;
	}

	@GetMapping("/config")
	ConfigResponse config() {
		return ConfigResponse.from(settings.current());
	}

	@PutMapping("/config")
	ConfigResponse updateConfig(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ConfigRequest request) {
		return ConfigResponse.from(settings.update(CurrentUser.from(jwt).id(), request.autoCloseHours(),
				request.emailNotifications(), request.triageSuggestions()));
	}

	@GetMapping("/audit")
	List<AuditEventResponse> audit(@RequestParam(required = false) String q) {
		return audit.execute(q).stream().map(AuditEventResponse::from).toList();
	}

	record ConfigRequest(
			@NotNull(message = "Ingresa horas mayores a 0.") Integer autoCloseHours,
			@NotNull(message = "Indica si se envían notificaciones por correo") Boolean emailNotifications,
			@NotNull(message = "Indica si se sugiere categoría y prioridad") Boolean triageSuggestions) {
	}

	record ConfigResponse(int autoCloseHours, boolean emailNotifications, boolean triageSuggestions) {

		static ConfigResponse from(SystemSettings settings) {
			return new ConfigResponse(settings.autoCloseHours(), settings.emailNotifications(),
					settings.triageSuggestions());
		}

	}

	record AuditEventResponse(long id, Instant createdAt, String actorName, String requestCode, String detail) {

		static AuditEventResponse from(AuditEvent event) {
			return new AuditEventResponse(event.id(), event.createdAt(), event.actorName(), event.requestCode(),
					event.detail());
		}

	}

}
