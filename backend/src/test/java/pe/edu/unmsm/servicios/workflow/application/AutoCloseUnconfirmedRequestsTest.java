package pe.edu.unmsm.servicios.workflow.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import pe.edu.unmsm.servicios.requests.application.RequestStateStore;
import pe.edu.unmsm.servicios.requests.application.UnconfirmedResolutions;
import pe.edu.unmsm.servicios.requests.domain.ClosureReason;
import pe.edu.unmsm.servicios.requests.domain.RequestHistoryEntry;
import pe.edu.unmsm.servicios.requests.domain.RequestStatus;
import pe.edu.unmsm.servicios.requests.domain.ServiceRequest;
import pe.edu.unmsm.servicios.shared.domain.SystemSettings;
import pe.edu.unmsm.servicios.shared.domain.event.RequestTransitionedEvent;

class AutoCloseUnconfirmedRequestsTest {

	private static final Instant NOW = Instant.parse("2026-10-26T12:00:00Z");
	private static final Instant CREATED = NOW.minus(Duration.ofDays(10));

	private final Map<String, ServiceRequest> stored = new LinkedHashMap<>();
	private final Map<String, Instant> resolvedAt = new LinkedHashMap<>();
	private final List<RequestHistoryEntry> history = new ArrayList<>();
	private final List<Object> events = new ArrayList<>();
	private final List<String> failing = new ArrayList<>();
	private Instant requestedCutoff;
	private int requestedLimit;
	/** When set, the candidate list returned regardless of the current data (a list read before a concurrent change). */
	private List<String> staleCandidates;
	private int autoCloseHours = 72;

	private final RequestStateStore store = new RequestStateStore() {

		@Override
		public Optional<ServiceRequest> findByCodeForUpdate(String code) {
			if (failing.contains(code)) {
				throw new IllegalStateException("database unavailable");
			}
			return Optional.ofNullable(stored.get(code));
		}

		@Override
		public ServiceRequest update(ServiceRequest request, RequestHistoryEntry entry) {
			stored.put(request.code(), request);
			history.add(entry);
			return request;
		}

	};

	private final UnconfirmedResolutions resolutions = new UnconfirmedResolutions() {

		@Override
		public List<String> resolvedAtOrBefore(Instant cutoff, int limit) {
			requestedCutoff = cutoff;
			requestedLimit = limit;
			if (staleCandidates != null) {
				return staleCandidates;
			}
			return resolvedAt.entrySet().stream()
					.filter(e -> stored.get(e.getKey()).status() == RequestStatus.RESUELTA)
					.filter(e -> !e.getValue().isAfter(cutoff))
					.map(Map.Entry::getKey)
					.limit(limit)
					.toList();
		}

		@Override
		public Optional<Instant> resolvedAt(String code) {
			return Optional.ofNullable(resolvedAt.get(code));
		}

	};

	private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

	private final AutoCloseUnconfirmedRequests useCase = new AutoCloseUnconfirmedRequests(resolutions,
			new CloseUnconfirmedRequest(store, resolutions, clock, events::add),
			() -> new SystemSettings(autoCloseHours, false, true), clock, 50);

	private void request(String code, RequestStatus status, Instant resolved) {
		stored.put(code, new ServiceRequest((long) stored.size() + 1, code, 4, 1, 3, "Descripción de la solicitud", 2,
				status, 1, 3L, null, status == RequestStatus.RESUELTA ? "Se cambió la pieza" : null, CREATED,
				CREATED.plus(Duration.ofDays(1)), resolved));
		resolvedAt.put(code, resolved);
	}

	@Test
	void aSolutionUnansweredForTheConfiguredHoursIsClosedAutomaticallyBySystem() {
		request("SOL-2026-000001", RequestStatus.RESUELTA, NOW.minus(Duration.ofHours(73)));

		int closed = useCase.run();

		assertThat(closed).isEqualTo(1);
		ServiceRequest result = stored.get("SOL-2026-000001");
		assertThat(result.status()).isEqualTo(RequestStatus.CERRADA);
		assertThat(result.closureReason()).isEqualTo(ClosureReason.AUTOMATICA);
		assertThat(result.technicianId()).isEqualTo(3L);
		assertThat(result.solution()).as("the proposed solution is kept").isEqualTo("Se cambió la pieza");
		assertThat(result.updatedAt()).isEqualTo(NOW);
		assertThat(history).singleElement().satisfies(entry -> {
			assertThat(entry.fromStatus()).isEqualTo(RequestStatus.RESUELTA);
			assertThat(entry.toStatus()).isEqualTo(RequestStatus.CERRADA);
			assertThat(entry.actorId()).as("made by the system, not by a user").isNull();
			assertThat(entry.technicianId()).isNull();
			assertThat(entry.reason()).isEqualTo("Cierre automático: el solicitante no confirmó la solución en 72 h");
			assertThat(entry.createdAt()).isEqualTo(NOW);
		});
		assertThat(events).singleElement().isInstanceOfSatisfying(RequestTransitionedEvent.class, event -> {
			assertThat(event.code()).isEqualTo("SOL-2026-000001");
			assertThat(event.requesterId()).isEqualTo(4);
			assertThat(event.technicianId()).isEqualTo(3L);
			assertThat(event.toStatus()).isEqualTo("CERRADA");
			assertThat(event.bySystem()).isTrue();
			assertThat(event.isAssignment()).isFalse();
		});
	}

	@Test
	void thePeriodComesFromTheSettingsReadOnEveryRun() {
		request("SOL-2026-000001", RequestStatus.RESUELTA, NOW.minus(Duration.ofHours(5)));

		assertThat(useCase.run()).isZero();
		assertThat(requestedCutoff).isEqualTo(NOW.minus(Duration.ofHours(72)));
		assertThat(requestedLimit).isEqualTo(50);

		autoCloseHours = 4;
		assertThat(useCase.run()).isEqualTo(1);
		assertThat(requestedCutoff).isEqualTo(NOW.minus(Duration.ofHours(4)));
		assertThat(history.getLast().reason()).endsWith("en 4 h");
	}

	@Test
	void aSolutionStillWithinThePeriodIsKept() {
		request("SOL-2026-000001", RequestStatus.RESUELTA, NOW.minus(Duration.ofHours(71)));

		assertThat(useCase.run()).isZero();
		assertThat(stored.get("SOL-2026-000001").status()).isEqualTo(RequestStatus.RESUELTA);
		assertThat(history).isEmpty();
		assertThat(events).isEmpty();
	}

	@Test
	void aRequestAnsweredOrResolvedAgainAfterTheListWasReadIsLeftAlone() {
		request("SOL-2026-000001", RequestStatus.CERRADA, NOW.minus(Duration.ofHours(80)));
		request("SOL-2026-000002", RequestStatus.RESUELTA, NOW.minus(Duration.ofMinutes(10)));
		staleCandidates = List.of("SOL-2026-000001", "SOL-2026-000002", "SOL-2026-999999");

		assertThat(useCase.run()).isZero();
		assertThat(history).isEmpty();
		assertThat(events).isEmpty();
	}

	@Test
	void aFailureOnOneRequestDoesNotStopTheOthers() {
		request("SOL-2026-000001", RequestStatus.RESUELTA, NOW.minus(Duration.ofHours(90)));
		request("SOL-2026-000002", RequestStatus.RESUELTA, NOW.minus(Duration.ofHours(80)));
		failing.add("SOL-2026-000001");

		assertThat(useCase.run()).isEqualTo(1);
		assertThat(stored.get("SOL-2026-000001").status()).isEqualTo(RequestStatus.RESUELTA);
		assertThat(stored.get("SOL-2026-000002").status()).isEqualTo(RequestStatus.CERRADA);
	}

	@Test
	void runningAgainClosesNothingTwice() {
		request("SOL-2026-000001", RequestStatus.RESUELTA, NOW.minus(Duration.ofHours(73)));
		staleCandidates = List.of("SOL-2026-000001");

		assertThat(useCase.run()).isEqualTo(1);
		assertThat(useCase.run()).isZero();
		assertThat(history).hasSize(1);
		assertThat(events).hasSize(1);
	}

}
