package pe.edu.unmsm.servicios.workflow.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import pe.edu.unmsm.servicios.requests.application.RequestDetailView;
import pe.edu.unmsm.servicios.requests.application.RequestSummaryView;
import pe.edu.unmsm.servicios.requests.application.TechnicianWorkload;
import pe.edu.unmsm.servicios.requests.domain.RequestStatus;
import pe.edu.unmsm.servicios.shared.domain.AuthenticatedUser;
import pe.edu.unmsm.servicios.workflow.domain.Technician;
import pe.edu.unmsm.servicios.workflow.domain.TransitionForbiddenException;

class ListTechniciansAndAllowedTransitionsTest {

	private static final AuthenticatedUser SUPERVISOR = new AuthenticatedUser(2, "s@unmsm.edu.pe", "Sup", "SUPERVISOR",
			1L);
	private static final AuthenticatedUser TECHNICIAN = new AuthenticatedUser(3, "t@unmsm.edu.pe", "Tec", "TECNICO", 1L);
	private static final AuthenticatedUser OWNER = new AuthenticatedUser(4, "o@unmsm.edu.pe", "Owner", "SOLICITANTE");

	private final TechnicianDirectory directory = new TechnicianDirectory() {

		@Override
		public Optional<Technician> findTechnician(long id) {
			return Optional.empty();
		}

		@Override
		public List<Technician> activeTechniciansOfArea(long areaId) {
			return areaId == 1
					? List.of(new Technician(3, "Carlos Mendoza", 1L, true), new Technician(5, "Jorge Paucar", 1L, true))
					: List.of(new Technician(9, "Rosa Alarcón", 3L, true));
		}

	};

	private final TechnicianWorkload workload = new TechnicianWorkload() {

		@Override
		public Map<Long, Long> activeRequestsByTechnician(Collection<Long> technicianIds) {
			return Map.of(3L, 2L);
		}

	};

	private final ListTechnicians listTechnicians = new ListTechnicians(directory, workload);

	@Test
	void aSupervisorSeesTheTechniciansOfTheirAreaWithTheirLoad() {
		List<TechnicianView> views = listTechnicians.execute(SUPERVISOR, null);

		assertThat(views).extracting(TechnicianView::fullName).containsExactly("Carlos Mendoza", "Jorge Paucar");
		assertThat(views).extracting(TechnicianView::activeRequests).containsExactly(2L, 0L);
	}

	@Test
	void theAreaParameterMayRepeatTheirOwnArea() {
		assertThat(listTechnicians.execute(SUPERVISOR, 1L)).hasSize(2);
	}

	@Test
	void aSupervisorCannotListTheTechniciansOfAnotherArea() {
		assertThatThrownBy(() -> listTechnicians.execute(SUPERVISOR, 3L))
				.isInstanceOf(TransitionForbiddenException.class);
	}

	@Test
	void onlySupervisorsMayListTechnicians() {
		assertThatThrownBy(() -> listTechnicians.execute(TECHNICIAN, 1L))
				.isInstanceOf(TransitionForbiddenException.class);
		assertThatThrownBy(() -> listTechnicians.execute(OWNER, null)).isInstanceOf(TransitionForbiddenException.class);
	}

	private static RequestDetailView detail(RequestStatus status, long requesterId, long areaId, Long technicianId) {
		Instant now = Instant.parse("2026-10-16T15:00:00Z");
		RequestSummaryView summary = new RequestSummaryView("SOL-2026-000001", "Electricidad", "F → P → A", "Alta",
				status, now, now.plusSeconds(3600), false, "Descripción", "Owner", "Mantenimiento", null, "F", "P",
				"A");
		return new RequestDetailView(summary, requesterId, areaId, technicianId, null, List.of(), List.of());
	}

	@Test
	void allowedTransitionsAreComputedForTheUserWhoAsks() {
		WorkflowAllowedTransitions allowed = new WorkflowAllowedTransitions();

		assertThat(allowed.forUser(SUPERVISOR, detail(RequestStatus.REGISTRADA, 4, 1, null)))
				.containsExactlyInAnyOrder(RequestStatus.EN_EVALUACION, RequestStatus.CERRADA);
		assertThat(allowed.forUser(TECHNICIAN, detail(RequestStatus.ASIGNADA, 4, 1, 3L)))
				.containsExactly(RequestStatus.EN_ATENCION);
		assertThat(allowed.forUser(OWNER, detail(RequestStatus.RESUELTA, 4, 1, 3L)))
				.containsExactlyInAnyOrder(RequestStatus.CERRADA, RequestStatus.EN_ATENCION);
		assertThat(allowed.forUser(OWNER, detail(RequestStatus.REGISTRADA, 4, 1, null))).isEmpty();
	}

}
