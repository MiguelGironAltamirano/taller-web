package pe.edu.unmsm.servicios.workflow.infrastructure.web;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import pe.edu.unmsm.servicios.shared.infrastructure.security.CurrentUser;
import pe.edu.unmsm.servicios.workflow.application.ListTechnicians;
import pe.edu.unmsm.servicios.workflow.infrastructure.web.WorkflowDtos.TechnicianResponse;

@RestController
@RequestMapping("/api/v1/users")
class TechniciansController {

	private final ListTechnicians listTechnicians;

	TechniciansController(ListTechnicians listTechnicians) {
		this.listTechnicians = listTechnicians;
	}

	/** Active technicians of the area, to assign and reassign requests. Supervisors only, their own area. */
	@GetMapping("/technicians")
	@PreAuthorize("hasRole('SUPERVISOR')")
	List<TechnicianResponse> technicians(@RequestParam(required = false) Long areaId, @AuthenticationPrincipal Jwt jwt) {
		return listTechnicians.execute(CurrentUser.from(jwt), areaId).stream().map(TechnicianResponse::from).toList();
	}

}
