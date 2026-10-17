package pe.edu.unmsm.servicios.shared.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AccessPolicyTest {

	private static final long REQUESTER_ID = 10;
	private static final long AREA_ID = 3;
	private static final long TECHNICIAN_ID = 20;

	private static AuthenticatedUser user(long id, String role, Long areaId) {
		return new AuthenticatedUser(id, "u" + id + "@unmsm.edu.pe", "User " + id, role, areaId);
	}

	@Test
	void ownerIsMatchedById() {
		assertThat(AccessPolicy.isOwner(user(10, "SOLICITANTE", null), 10)).isTrue();
		assertThat(AccessPolicy.isOwner(user(11, "SOLICITANTE", null), 10)).isFalse();
	}

	@Test
	void supervisorOfAreaRequiresSupervisorRoleAndSameArea() {
		assertThat(AccessPolicy.isSupervisorOfArea(user(1, "SUPERVISOR", AREA_ID), AREA_ID)).isTrue();
		assertThat(AccessPolicy.isSupervisorOfArea(user(1, "SUPERVISOR", 4L), AREA_ID)).isFalse();
		assertThat(AccessPolicy.isSupervisorOfArea(user(1, "SUPERVISOR", null), AREA_ID)).isFalse();
		assertThat(AccessPolicy.isSupervisorOfArea(user(1, "TECNICO", AREA_ID), AREA_ID)).isFalse();
	}

	@Test
	void assignedTechnicianRequiresTechnicianRoleAndMatchingId() {
		assertThat(AccessPolicy.isAssignedTechnician(user(20, "TECNICO", AREA_ID), TECHNICIAN_ID)).isTrue();
		assertThat(AccessPolicy.isAssignedTechnician(user(21, "TECNICO", AREA_ID), TECHNICIAN_ID)).isFalse();
		assertThat(AccessPolicy.isAssignedTechnician(user(20, "TECNICO", AREA_ID), null)).isFalse();
		assertThat(AccessPolicy.isAssignedTechnician(user(20, "SUPERVISOR", AREA_ID), TECHNICIAN_ID)).isFalse();
	}

	@Test
	void administratorIsMatchedByRole() {
		assertThat(AccessPolicy.isAdministrator(user(1, "ADMINISTRADOR", null))).isTrue();
		assertThat(AccessPolicy.isAdministrator(user(1, "SUPERVISOR", AREA_ID))).isFalse();
	}

	@Test
	void requesterSeesOnlyOwnRequests() {
		assertThat(AccessPolicy.canViewRequest(user(10, "SOLICITANTE", null), REQUESTER_ID, AREA_ID, null)).isTrue();
		assertThat(AccessPolicy.canViewRequest(user(11, "SOLICITANTE", null), REQUESTER_ID, AREA_ID, null)).isFalse();
	}

	@Test
	void supervisorSeesOnlyTheirArea() {
		assertThat(AccessPolicy.canViewRequest(user(1, "SUPERVISOR", AREA_ID), REQUESTER_ID, AREA_ID, null)).isTrue();
		assertThat(AccessPolicy.canViewRequest(user(1, "SUPERVISOR", 4L), REQUESTER_ID, AREA_ID, null)).isFalse();
	}

	@Test
	void technicianSeesOnlyAssignedRequests() {
		assertThat(AccessPolicy.canViewRequest(user(20, "TECNICO", AREA_ID), REQUESTER_ID, AREA_ID, TECHNICIAN_ID)).isTrue();
		assertThat(AccessPolicy.canViewRequest(user(21, "TECNICO", AREA_ID), REQUESTER_ID, AREA_ID, TECHNICIAN_ID)).isFalse();
		assertThat(AccessPolicy.canViewRequest(user(20, "TECNICO", AREA_ID), REQUESTER_ID, AREA_ID, null)).isFalse();
	}

	@Test
	void administratorSeesEverything() {
		assertThat(AccessPolicy.canViewRequest(user(1, "ADMINISTRADOR", null), REQUESTER_ID, AREA_ID, null)).isTrue();
	}

	@Test
	void unknownRoleSeesNothing() {
		assertThat(AccessPolicy.canViewRequest(user(10, "OTRO", null), REQUESTER_ID, AREA_ID, null)).isFalse();
	}

}
