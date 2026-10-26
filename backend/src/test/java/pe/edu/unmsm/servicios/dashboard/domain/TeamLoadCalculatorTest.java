package pe.edu.unmsm.servicios.dashboard.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

class TeamLoadCalculatorTest {

	private static final Instant NOW = Instant.parse("2026-10-26T15:00:00Z");

	private static TrackedRequest request(String status, Long technicianId, boolean overdue) {
		return new TrackedRequest("X", status, "Electricidad", "Alta", "Aula 101", technicianId, "Tech", NOW,
				NOW.plus(Duration.ofHours(overdue ? -1 : 1)), null, true, overdue);
	}

	@Test
	void countsAssignedInProgressAndOverduePerTechnician() {
		List<TeamMember> technicians = List.of(new TeamMember(1, "Ana"), new TeamMember(2, "Beto"));
		List<TrackedRequest> open = List.of(request("ASIGNADA", 1L, false), request("ASIGNADA", 1L, true),
				request("EN_ATENCION", 1L, true), request("EN_ATENCION", 2L, false),
				// Not assigned to anybody, or to a technician outside the team: ignored.
				request("REGISTRADA", null, true), request("ASIGNADA", 99L, true));

		List<TechnicianLoad> loads = TeamLoadCalculator.calculate(technicians, open, 8);

		assertThat(loads).containsExactly(new TechnicianLoad(1, "Ana", 2, 1, 2, 8),
				new TechnicianLoad(2, "Beto", 0, 1, 0, 8));
	}

	@Test
	void techniciansWithoutWorkAppearWithZerosInTheGivenOrder() {
		List<TeamMember> technicians = List.of(new TeamMember(5, "Zoe"), new TeamMember(3, "Abel"));

		assertThat(TeamLoadCalculator.calculate(technicians, List.of(), 6)).containsExactly(
				new TechnicianLoad(5, "Zoe", 0, 0, 0, 6), new TechnicianLoad(3, "Abel", 0, 0, 0, 6));
	}

	@Test
	void anEmptyTeamGivesAnEmptyList() {
		assertThat(TeamLoadCalculator.calculate(List.of(), List.of(request("ASIGNADA", 1L, false)), 8)).isEmpty();
	}

}
