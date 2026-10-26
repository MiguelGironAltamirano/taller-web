package pe.edu.unmsm.servicios.requests.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ServiceRequestTest {

	private static final Instant NOW = Instant.parse("2026-10-16T15:00:00Z");

	private static ServiceRequest register(String description, int slaHours) {
		return ServiceRequest.register("SOL-2026-000001", 4L, 1L, 1L, 3L, description, 2L, slaHours, NOW);
	}

	@Test
	void registersAsRegistradaWithTrimmedDescriptionAndNoTechnician() {
		ServiceRequest request = register("  El tomacorriente del aula no funciona  ", 24);

		assertThat(request.status()).isEqualTo(RequestStatus.REGISTRADA);
		assertThat(request.description()).isEqualTo("El tomacorriente del aula no funciona");
		assertThat(request.technicianId()).isNull();
		assertThat(request.closureReason()).isNull();
		assertThat(request.createdAt()).isEqualTo(NOW);
	}

	@ParameterizedTest
	@CsvSource({ "8", "24", "72", "120" })
	void dueDateIsRegistrationPlusSlaHours(int hours) {
		assertThat(register("x".repeat(30), hours).dueAt()).isEqualTo(NOW.plus(Duration.ofHours(hours)));
	}

	@Test
	void acceptsDescriptionsBetween20And2000Characters() {
		assertThat(register("x".repeat(20), 24).description()).hasSize(20);
		assertThat(register("x".repeat(2000), 24).description()).hasSize(2000);
	}

	@Test
	void rejectsDescriptionsOutsideTheRange() {
		assertThatThrownBy(() -> register("x".repeat(19), 24)).isInstanceOf(InvalidRequestException.class)
				.satisfies(ex -> assertThat(((InvalidRequestException) ex).errors()).containsKey("description"));
		assertThatThrownBy(() -> register("x".repeat(2001), 24)).isInstanceOf(InvalidRequestException.class);
	}

	@Test
	void surroundingWhitespaceDoesNotCountTowardsTheMinimum() {
		assertThatThrownBy(() -> register("x".repeat(10) + " ".repeat(30), 24))
				.isInstanceOf(InvalidRequestException.class);
		assertThatThrownBy(() -> register(null, 24)).isInstanceOf(InvalidRequestException.class);
	}

	@Test
	void descriptionErrorExplainsTheLimits() {
		assertThat(ServiceRequest.descriptionError("x".repeat(20))).isEmpty();
		assertThat(ServiceRequest.descriptionError("corta")).hasValueSatisfying(
				message -> assertThat(message).contains("20").contains("2000"));
	}

	// ───── Solution and priority ─────

	private static final Instant LATER = NOW.plus(Duration.ofHours(5));

	private static ServiceRequest inStatus(RequestStatus status) {
		ServiceRequest registered = register("x".repeat(30), 24);
		return registered.transitionedTo(status, 3L, null, null, NOW);
	}

	@Test
	void aNewRequestHasNoSolution() {
		assertThat(register("x".repeat(30), 24).solution()).isNull();
	}

	@Test
	void resolvingStoresTheProposedSolution() {
		ServiceRequest resolved = inStatus(RequestStatus.EN_ATENCION).transitionedTo(RequestStatus.RESUELTA, 3L, null,
				"Se cambió el tomacorriente", LATER);

		assertThat(resolved.solution()).isEqualTo("Se cambió el tomacorriente");
	}

	@Test
	void closingKeepsTheSolutionAndRejectingItDropsIt() {
		ServiceRequest resolved = inStatus(RequestStatus.EN_ATENCION).transitionedTo(RequestStatus.RESUELTA, 3L, null,
				"Se cambió el tomacorriente", LATER);

		assertThat(resolved.transitionedTo(RequestStatus.CERRADA, 3L, ClosureReason.CONFIRMADA, null, LATER)
				.solution()).isEqualTo("Se cambió el tomacorriente");
		assertThat(resolved.transitionedTo(RequestStatus.EN_ATENCION, 3L, null, null, LATER).solution())
				.as("a rejected solution is no longer the proposed one").isNull();
	}

	@Test
	void changingThePriorityRecalculatesTheDueDateFromTheRegistration() {
		ServiceRequest changed = register("x".repeat(30), 24).withPriority(1L, 8, LATER);

		assertThat(changed.priorityId()).isEqualTo(1L);
		assertThat(changed.dueAt()).isEqualTo(NOW.plus(Duration.ofHours(8)));
		assertThat(changed.updatedAt()).isEqualTo(LATER);
		assertThat(changed.createdAt()).isEqualTo(NOW);
	}

	@Test
	void thePriorityCanChangeUntilTheRequestIsResolved() {
		for (RequestStatus status : RequestStatus.values()) {
			assertThat(status.allowsPriorityChange()).as(status.name()).isEqualTo(!status.isFinished());
		}
		assertThat(inStatus(RequestStatus.EN_ATENCION).withPriority(1L, 8, LATER).priorityId()).isEqualTo(1L);
		for (RequestStatus finished : new RequestStatus[] { RequestStatus.RESUELTA, RequestStatus.CERRADA }) {
			assertThatThrownBy(() -> inStatus(finished).withPriority(1L, 8, LATER)).as(finished.name())
					.isInstanceOf(RequestNotModifiableException.class);
		}
	}

	@Test
	void theNewPriorityMustDifferFromTheCurrentOne() {
		assertThatThrownBy(() -> register("x".repeat(30), 24).withPriority(2L, 24, LATER))
				.isInstanceOf(InvalidRequestException.class)
				.satisfies(ex -> assertThat(((InvalidRequestException) ex).errors()).containsKey("priorityId"));
	}

	@Test
	void automaticClosureIsAClosureReason() {
		assertThat(ClosureReason.valueOf("AUTOMATICA")).isNotNull();
	}

}
