package pe.edu.unmsm.servicios.admin.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import pe.edu.unmsm.servicios.auth.domain.RequesterType;
import pe.edu.unmsm.servicios.auth.domain.Role;

class UserAccountRulesTest {

	@Test
	void technicianAndSupervisorRequireAnArea() {
		assertThat(UserAccountRules.violations(Role.TECNICO, null)).containsKey("areaId");
		assertThat(UserAccountRules.violations(Role.SUPERVISOR, null)).containsKey("areaId");
	}

	@Test
	void technicianAndSupervisorWithAnAreaAreValid() {
		assertThat(UserAccountRules.violations(Role.TECNICO, 1L)).isEmpty();
		assertThat(UserAccountRules.violations(Role.SUPERVISOR, 2L)).isEmpty();
	}

	@Test
	void requesterAndAdministratorMustNotHaveAnArea() {
		assertThat(UserAccountRules.violations(Role.SOLICITANTE, 1L)).containsKey("areaId");
		assertThat(UserAccountRules.violations(Role.ADMINISTRADOR, 1L)).containsKey("areaId");
		assertThat(UserAccountRules.violations(Role.SOLICITANTE, null)).isEmpty();
		assertThat(UserAccountRules.violations(Role.ADMINISTRADOR, null)).isEmpty();
	}

	@Test
	void missingRoleIsAViolation() {
		assertThat(UserAccountRules.violations(null, null)).containsKey("role");
	}

	@Test
	void requesterTypeDefaultsToStudentAndOnlyAppliesToRequesters() {
		assertThat(UserAccountRules.requesterTypeFor(Role.SOLICITANTE, null)).isEqualTo(RequesterType.ESTUDIANTE);
		assertThat(UserAccountRules.requesterTypeFor(Role.SOLICITANTE, RequesterType.DOCENTE))
				.isEqualTo(RequesterType.DOCENTE);
		assertThat(UserAccountRules.requesterTypeFor(Role.TECNICO, RequesterType.DOCENTE)).isNull();
		assertThat(UserAccountRules.requesterTypeFor(Role.ADMINISTRADOR, null)).isNull();
	}

	@Test
	void requestStatesAreTheSixStatesInWorkflowOrder() {
		assertThat(RequestStates.ALL).extracting(RequestState::status).containsExactly("REGISTRADA", "EN_EVALUACION",
				"ASIGNADA", "EN_ATENCION", "RESUELTA", "CERRADA");
		assertThat(RequestStates.ALL).extracting(RequestState::order).containsExactly(1, 2, 3, 4, 5, 6);
		assertThat(RequestStates.ALL).allSatisfy(state -> assertThat(state.description()).isNotBlank());
	}

}
