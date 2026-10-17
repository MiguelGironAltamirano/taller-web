package pe.edu.unmsm.servicios.requests.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import pe.edu.unmsm.servicios.requests.domain.RequestNotFoundException;
import pe.edu.unmsm.servicios.requests.domain.RequestStatus;
import pe.edu.unmsm.servicios.shared.domain.AuthenticatedUser;

class GetAndListRequestsTest {

	private static final Instant NOW = Instant.parse("2026-10-16T15:00:00Z");
	private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

	// The request belongs to requester 4, area 1, technician 3.
	private static final RequestSummaryView SUMMARY = new RequestSummaryView("SOL-2026-000001", "Electricidad",
			"Facultad → Pabellón → Aula 101", "Alta", RequestStatus.ASIGNADA, NOW, NOW.plusSeconds(3600), false,
			"El tomacorriente del aula no funciona", "Estudiante Demo", "Mantenimiento", "Técnico Demo", "Facultad",
			"Pabellón", "Aula 101");
	private static final RequestDetailView DETAIL = new RequestDetailView(SUMMARY, 4, 1, 3L, null, List.of(),
			List.of());

	private static final AuthenticatedUser OWNER = new AuthenticatedUser(4, "a@unmsm.edu.pe", "Owner", "SOLICITANTE");
	private static final AuthenticatedUser OTHER_REQUESTER = new AuthenticatedUser(5, "b@unmsm.edu.pe", "Other",
			"SOLICITANTE");
	private static final AuthenticatedUser SUPERVISOR = new AuthenticatedUser(2, "s@unmsm.edu.pe", "Sup",
			"SUPERVISOR", 1L);
	private static final AuthenticatedUser OTHER_SUPERVISOR = new AuthenticatedUser(6, "s2@unmsm.edu.pe", "Sup2",
			"SUPERVISOR", 3L);
	private static final AuthenticatedUser TECHNICIAN = new AuthenticatedUser(3, "t@unmsm.edu.pe", "Tec", "TECNICO",
			1L);
	private static final AuthenticatedUser OTHER_TECHNICIAN = new AuthenticatedUser(7, "t2@unmsm.edu.pe", "Tec2",
			"TECNICO", 1L);
	private static final AuthenticatedUser ADMIN = new AuthenticatedUser(1, "ad@unmsm.edu.pe", "Admin",
			"ADMINISTRADOR");

	private final List<RequestScope> scopes = new ArrayList<>();
	private int lastPage;
	private int lastSize;

	private final RequestQueries queries = new RequestQueries() {

		@Override
		public PageView<RequestSummaryView> search(RequestScope scope, RequestStatus status, int page, int size,
				Instant now) {
			scopes.add(scope);
			lastPage = page;
			lastSize = size;
			return new PageView<>(List.of(SUMMARY), page, size, 1);
		}

		@Override
		public Optional<RequestDetailView> findDetail(String code, Instant now) {
			return DETAIL.summary().code().equals(code) ? Optional.of(DETAIL) : Optional.empty();
		}

	};

	private final GetRequest get = new GetRequest(queries, (user, view) -> List.of(RequestStatus.EN_ATENCION), CLOCK);
	private final ListRequests list = new ListRequests(queries, CLOCK);

	@Test
	void everyoneWhoCanViewTheRequestGetsIt() {
		for (AuthenticatedUser user : List.of(OWNER, SUPERVISOR, TECHNICIAN, ADMIN)) {
			assertThat(get.execute(user, "SOL-2026-000001").summary().code()).as(user.role()).isEqualTo("SOL-2026-000001");
		}
	}

	@Test
	void anInvisibleRequestLooksLikeItDoesNotExist() {
		for (AuthenticatedUser user : List.of(OTHER_REQUESTER, OTHER_SUPERVISOR, OTHER_TECHNICIAN)) {
			assertThatThrownBy(() -> get.execute(user, "SOL-2026-000001")).as(user.role())
					.isInstanceOf(RequestNotFoundException.class);
		}
	}

	@Test
	void anUnknownCodeIsNotFound() {
		assertThatThrownBy(() -> get.execute(ADMIN, "SOL-2026-999999")).isInstanceOf(RequestNotFoundException.class);
	}

	@Test
	void theDetailCarriesTheTransitionsAllowedForTheUser() {
		assertThat(get.execute(SUPERVISOR, "SOL-2026-000001").allowedTransitions())
				.containsExactly(RequestStatus.EN_ATENCION);
	}

	@Test
	void listScopeFollowsTheRoleOfTheUser() {
		list.execute(OWNER, null, 0, 20);
		list.execute(SUPERVISOR, null, 0, 20);
		list.execute(TECHNICIAN, null, 0, 20);
		list.execute(ADMIN, null, 0, 20);

		assertThat(scopes).extracting(RequestScope::role)
				.containsExactly("SOLICITANTE", "SUPERVISOR", "TECNICO", "ADMINISTRADOR");
		assertThat(scopes.get(0).userId()).isEqualTo(4);
		assertThat(scopes.get(1).areaId()).isEqualTo(1L);
		assertThat(scopes.get(2).userId()).isEqualTo(3);
	}

	@Test
	void listClampsPageAndSize() {
		list.execute(ADMIN, null, -3, 0);
		assertThat(lastPage).isZero();
		assertThat(lastSize).isEqualTo(1);

		list.execute(ADMIN, null, 2, 100000);
		assertThat(lastPage).isEqualTo(2);
		assertThat(lastSize).isEqualTo(ListRequests.MAX_PAGE_SIZE);
	}

}
