package pe.edu.unmsm.servicios.dashboard.infrastructure.web;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import pe.edu.unmsm.servicios.dashboard.application.GetDashboardIndicators;
import pe.edu.unmsm.servicios.dashboard.application.GetTeamLoad;
import pe.edu.unmsm.servicios.dashboard.infrastructure.web.DashboardDtos.IndicatorsResponse;
import pe.edu.unmsm.servicios.dashboard.infrastructure.web.DashboardDtos.TeamMemberResponse;
import pe.edu.unmsm.servicios.shared.infrastructure.security.CurrentUser;

@RestController
@RequestMapping("/api/v1/dashboard")
class DashboardController {

	private final GetDashboardIndicators getIndicators;
	private final GetTeamLoad getTeamLoad;

	DashboardController(GetDashboardIndicators getIndicators, GetTeamLoad getTeamLoad) {
		this.getIndicators = getIndicators;
		this.getTeamLoad = getTeamLoad;
	}

	/** {@code range}: 7, 30 (default) or mes. {@code areaId} only applies to administrators. */
	@GetMapping("/indicators")
	@PreAuthorize("hasAnyRole('SUPERVISOR', 'ADMINISTRADOR')")
	IndicatorsResponse indicators(@RequestParam(required = false) String range,
			@RequestParam(required = false) Long areaId, @AuthenticationPrincipal Jwt jwt) {
		return IndicatorsResponse.from(getIndicators.execute(CurrentUser.from(jwt), range, areaId));
	}

	@GetMapping("/team")
	@PreAuthorize("hasRole('SUPERVISOR')")
	List<TeamMemberResponse> team(@AuthenticationPrincipal Jwt jwt) {
		return getTeamLoad.execute(CurrentUser.from(jwt)).stream().map(TeamMemberResponse::from).toList();
	}

}
