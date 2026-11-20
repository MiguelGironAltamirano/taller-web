package pe.edu.unmsm.servicios.workflow.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static pe.edu.unmsm.servicios.requests.domain.RequestStatus.ASIGNADA;
import static pe.edu.unmsm.servicios.requests.domain.RequestStatus.CANCELADA;
import static pe.edu.unmsm.servicios.requests.domain.RequestStatus.CERRADA;
import static pe.edu.unmsm.servicios.requests.domain.RequestStatus.EN_ATENCION;
import static pe.edu.unmsm.servicios.requests.domain.RequestStatus.EN_EVALUACION;
import static pe.edu.unmsm.servicios.requests.domain.RequestStatus.REGISTRADA;
import static pe.edu.unmsm.servicios.requests.domain.RequestStatus.RESUELTA;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import pe.edu.unmsm.servicios.requests.domain.ClosureReason;
import pe.edu.unmsm.servicios.requests.domain.RequestAction;
import pe.edu.unmsm.servicios.requests.domain.RequestStatus;
import pe.edu.unmsm.servicios.shared.domain.AuthenticatedUser;

class StateMachineTest {

	// The request belongs to requester 4, area 1; technician 3 is assigned when the status requires one.
	private static final AuthenticatedUser OWNER = new AuthenticatedUser(4, "o@unmsm.edu.pe", "Owner", "SOLICITANTE");
	private static final AuthenticatedUser OTHER_REQUESTER = new AuthenticatedUser(5, "r@unmsm.edu.pe", "Other",
			"SOLICITANTE");
	private static final AuthenticatedUser SUPERVISOR = new AuthenticatedUser(2, "s@unmsm.edu.pe", "Sup", "SUPERVISOR",
			1L);
	private static final AuthenticatedUser OTHER_AREA_SUPERVISOR = new AuthenticatedUser(6, "s2@unmsm.edu.pe", "Sup2",
			"SUPERVISOR", 3L);
	private static final AuthenticatedUser TECHNICIAN = new AuthenticatedUser(3, "t@unmsm.edu.pe", "Tec", "TECNICO",
			1L);
	private static final AuthenticatedUser OTHER_TECHNICIAN = new AuthenticatedUser(7, "t2@unmsm.edu.pe", "Tec2",
			"TECNICO", 1L);
	private static final AuthenticatedUser ADMIN = new AuthenticatedUser(1, "a@unmsm.edu.pe", "Admin",
			"ADMINISTRADOR");

	private static final Technician TECH_3 = new Technician(3, "Carlos Mendoza", 1L, true);
	private static final Technician TECH_8 = new Technician(8, "Jorge Paucar", 1L, true);
	private static final Technician OTHER_AREA_TECH = new Technician(9, "Rosa Alarcón", 3L, true);
	private static final Technician INACTIVE_TECH = new Technician(10, "Luis Huamán", 1L, false);

	private static WorkflowSubject subject(RequestStatus status) {
		boolean assigned = status == ASIGNADA || status == EN_ATENCION || status == RESUELTA;
		return new WorkflowSubject(status, 4, 1, assigned ? 3L : null);
	}

	private static TransitionCommand to(RequestStatus status) {
		return new TransitionCommand(status, null, null, null);
	}

	private static TransitionOutcome move(AuthenticatedUser actor, RequestStatus from, TransitionCommand command,
			Technician technician) {
		return StateMachine.transition(actor, subject(from), command, technician);
	}

	// ───── The transition table: every listed row is valid ─────

	@Test
	void supervisorMovesRegistradaToEnEvaluacion() {
		TransitionOutcome outcome = move(SUPERVISOR, REGISTRADA, to(EN_EVALUACION), null);

		assertThat(outcome.to()).isEqualTo(EN_EVALUACION);
		assertThat(outcome.technicianId()).isNull();
		assertThat(outcome.reason()).isEqualTo("Se inició la evaluación del caso");
	}

	@Test
	void supervisorAssignsATechnicianOfTheAreaFromEnEvaluacion() {
		TransitionOutcome outcome = move(SUPERVISOR, EN_EVALUACION,
				new TransitionCommand(ASIGNADA, null, 3L, null), TECH_3);

		assertThat(outcome.to()).isEqualTo(ASIGNADA);
		assertThat(outcome.technicianId()).isEqualTo(3L);
		assertThat(outcome.historyTechnicianId()).isEqualTo(3L);
		assertThat(outcome.reason()).isEqualTo("Asignada a Carlos Mendoza");
	}

	@Test
	void supervisorReassignsWithAReason() {
		TransitionOutcome outcome = move(SUPERVISOR, ASIGNADA,
				new TransitionCommand(ASIGNADA, "Carga de trabajo", 8L, null), TECH_8);

		assertThat(outcome.to()).isEqualTo(ASIGNADA);
		assertThat(outcome.technicianId()).isEqualTo(8L);
		assertThat(outcome.reason()).isEqualTo("Reasignada a Jorge Paucar. Motivo: Carga de trabajo");
	}

	@Test
	void assignedTechnicianStartsAttending() {
		TransitionOutcome outcome = move(TECHNICIAN, ASIGNADA, to(EN_ATENCION), null);

		assertThat(outcome.to()).isEqualTo(EN_ATENCION);
		assertThat(outcome.technicianId()).as("the technician stays assigned").isEqualTo(3L);
		assertThat(outcome.historyTechnicianId()).isNull();
	}

	@Test
	void assignedTechnicianResolvesWithASolutionComment() {
		TransitionOutcome outcome = move(TECHNICIAN, EN_ATENCION,
				new TransitionCommand(RESUELTA, "Se cambió el tomacorriente", null, null), null);

		assertThat(outcome.to()).isEqualTo(RESUELTA);
		assertThat(outcome.reason()).isEqualTo("Solución: Se cambió el tomacorriente");
		assertThat(outcome.solution()).isEqualTo("Se cambió el tomacorriente");
	}

	@Test
	void onlyResolvingProposesASolution() {
		assertThat(move(TECHNICIAN, ASIGNADA, to(EN_ATENCION), null).solution()).isNull();
		assertThat(move(OWNER, RESUELTA, to(CERRADA), null).solution()).isNull();
	}

	@Test
	void requesterConfirmsTheSolutionAndClosesTheRequest() {
		TransitionOutcome outcome = move(OWNER, RESUELTA, to(CERRADA), null);

		assertThat(outcome.to()).isEqualTo(CERRADA);
		assertThat(outcome.closureReason()).isEqualTo(ClosureReason.CONFIRMADA);
		assertThat(outcome.reason()).isEqualTo("Solución confirmada por el solicitante");
	}

	@Test
	void requesterRejectsTheSolutionWithAReason() {
		TransitionOutcome outcome = move(OWNER, RESUELTA,
				new TransitionCommand(EN_ATENCION, "Sigue la fuga en el techo", null, null), null);

		assertThat(outcome.to()).isEqualTo(EN_ATENCION);
		assertThat(outcome.closureReason()).isNull();
		assertThat(outcome.technicianId()).isEqualTo(3L);
		assertThat(outcome.reason()).isEqualTo("Solución rechazada: Sigue la fuga en el techo");
	}

	@ParameterizedTest
	@CsvSource({ "REGISTRADA,NO_PROCEDE", "REGISTRADA,DUPLICADA", "EN_EVALUACION,NO_PROCEDE", "EN_EVALUACION,DUPLICADA" })
	void supervisorClosesAsNoProcedeOrDuplicadaFromRegistradaOrEnEvaluacion(RequestStatus from, ClosureReason why) {
		TransitionOutcome outcome = move(SUPERVISOR, from, new TransitionCommand(CERRADA, null, null, why), null);

		assertThat(outcome.to()).isEqualTo(CERRADA);
		assertThat(outcome.closureReason()).isEqualTo(why);
	}

	@Test
	void closingKeepsTheFreeTextReasonWhenGiven() {
		TransitionOutcome outcome = move(SUPERVISOR, REGISTRADA,
				new TransitionCommand(CERRADA, "Es igual a SOL-2026-000002", null, ClosureReason.DUPLICADA), null);

		assertThat(outcome.reason()).isEqualTo("Duplicada: Es igual a SOL-2026-000002");
	}

	@Test
	void theContractStyleClosureReasonInTheReasonFieldIsAccepted() {
		// docs/api/contrato-v1.md: "reason ∈ NO_PROCEDE, DUPLICADA".
		TransitionOutcome outcome = move(SUPERVISOR, REGISTRADA,
				new TransitionCommand(CERRADA, "NO_PROCEDE", null, null), null);

		assertThat(outcome.closureReason()).isEqualTo(ClosureReason.NO_PROCEDE);
		assertThat(outcome.reason()).isEqualTo("No procede");
	}

	// ───── Transitions outside the table are conflicts ─────

	@ParameterizedTest
	@CsvSource({ "REGISTRADA,RESUELTA", "REGISTRADA,ASIGNADA", "REGISTRADA,EN_ATENCION", "REGISTRADA,REGISTRADA",
			"EN_EVALUACION,EN_ATENCION", "EN_EVALUACION,RESUELTA", "EN_EVALUACION,REGISTRADA", "ASIGNADA,RESUELTA",
			"ASIGNADA,CERRADA", "ASIGNADA,EN_EVALUACION", "EN_ATENCION,CERRADA", "EN_ATENCION,ASIGNADA",
			"EN_ATENCION,EN_ATENCION", "RESUELTA,ASIGNADA", "RESUELTA,RESUELTA", "CERRADA,EN_ATENCION",
			"CERRADA,REGISTRADA", "CERRADA,CERRADA" })
	void transitionsOutsideTheTableAreConflicts(RequestStatus from, RequestStatus to) {
		assertThatThrownBy(() -> move(SUPERVISOR, from, new TransitionCommand(to, "motivo válido", 3L,
				ClosureReason.NO_PROCEDE), TECH_3))
				.isInstanceOf(InvalidTransitionException.class);
	}

	@Test
	void aConflictIsReportedBeforeAPermissionProblem() {
		// A requester asking for Registrada → Resuelta gets 409, not 403: the transition does not exist.
		assertThatThrownBy(() -> move(OWNER, REGISTRADA, to(RESUELTA), null))
				.isInstanceOf(InvalidTransitionException.class);
	}

	// ───── Only the right person may perform each transition ─────

	@Test
	void anUnassignedTechnicianCannotStartAttending() {
		assertThatThrownBy(() -> move(OTHER_TECHNICIAN, ASIGNADA, to(EN_ATENCION), null))
				.isInstanceOf(TransitionForbiddenException.class);
	}

	@Test
	void onlyTheAssignedTechnicianResolves() {
		assertThatThrownBy(() -> move(OTHER_TECHNICIAN, EN_ATENCION,
				new TransitionCommand(RESUELTA, "Listo", null, null), null))
				.isInstanceOf(TransitionForbiddenException.class);
		assertThatThrownBy(() -> move(SUPERVISOR, EN_ATENCION, new TransitionCommand(RESUELTA, "Listo", null, null),
				null)).isInstanceOf(TransitionForbiddenException.class);
	}

	@Test
	void onlyTheSupervisorOfTheAreaEvaluatesAssignsAndCloses() {
		for (AuthenticatedUser user : List.of(OTHER_AREA_SUPERVISOR, TECHNICIAN, OWNER, OTHER_REQUESTER, ADMIN)) {
			assertThatThrownBy(() -> move(user, REGISTRADA, to(EN_EVALUACION), null)).as(user.role())
					.isInstanceOf(TransitionForbiddenException.class);
			assertThatThrownBy(() -> move(user, EN_EVALUACION, new TransitionCommand(ASIGNADA, null, 3L, null), TECH_3))
					.as(user.role()).isInstanceOf(TransitionForbiddenException.class);
			assertThatThrownBy(() -> move(user, REGISTRADA,
					new TransitionCommand(CERRADA, null, null, ClosureReason.NO_PROCEDE), null)).as(user.role())
					.isInstanceOf(TransitionForbiddenException.class);
		}
	}

	@Test
	void onlyTheSupervisorOfTheAreaReassigns() {
		assertThatThrownBy(() -> move(OTHER_AREA_SUPERVISOR, ASIGNADA,
				new TransitionCommand(ASIGNADA, "Motivo", 8L, null), TECH_8))
				.isInstanceOf(TransitionForbiddenException.class);
		assertThatThrownBy(() -> move(TECHNICIAN, ASIGNADA, new TransitionCommand(ASIGNADA, "Motivo", 8L, null),
				TECH_8)).isInstanceOf(TransitionForbiddenException.class);
	}

	@Test
	void onlyTheOwnerConfirmsOrRejectsTheSolution() {
		for (AuthenticatedUser user : List.of(OTHER_REQUESTER, SUPERVISOR, TECHNICIAN, ADMIN)) {
			assertThatThrownBy(() -> move(user, RESUELTA, to(CERRADA), null)).as(user.role())
					.isInstanceOf(TransitionForbiddenException.class);
			assertThatThrownBy(() -> move(user, RESUELTA, new TransitionCommand(EN_ATENCION, "Sigue mal", null, null),
					null)).as(user.role()).isInstanceOf(TransitionForbiddenException.class);
		}
	}

	@Test
	void aPermissionProblemIsReportedBeforeMissingData() {
		// No technician and no reason, but the actor is not even allowed: 403 wins over 400.
		assertThatThrownBy(() -> move(TECHNICIAN, ASIGNADA, new TransitionCommand(ASIGNADA, null, null, null), null))
				.isInstanceOf(TransitionForbiddenException.class);
	}

	// ───── Required data ─────

	@Test
	void assigningRequiresAnActiveTechnicianOfTheRequestArea() {
		for (Technician invalid : new Technician[] { null, OTHER_AREA_TECH, INACTIVE_TECH }) {
			assertThatThrownBy(() -> move(SUPERVISOR, EN_EVALUACION, new TransitionCommand(ASIGNADA, null, 9L, null),
					invalid)).isInstanceOf(InvalidTransitionDataException.class)
					.satisfies(ex -> assertThat(((InvalidTransitionDataException) ex).errors())
							.containsKey("technicianId"));
		}
		assertThatThrownBy(() -> move(SUPERVISOR, EN_EVALUACION, to(ASIGNADA), null))
				.isInstanceOf(InvalidTransitionDataException.class);
	}

	@Test
	void reassigningRequiresAReasonAndADifferentTechnician() {
		assertThatThrownBy(() -> move(SUPERVISOR, ASIGNADA, new TransitionCommand(ASIGNADA, null, 8L, null), TECH_8))
				.isInstanceOf(InvalidTransitionDataException.class)
				.satisfies(ex -> assertThat(((InvalidTransitionDataException) ex).errors()).containsKey("reason"));
		assertThatThrownBy(() -> move(SUPERVISOR, ASIGNADA, new TransitionCommand(ASIGNADA, "   ", 8L, null), TECH_8))
				.isInstanceOf(InvalidTransitionDataException.class);
		assertThatThrownBy(() -> move(SUPERVISOR, ASIGNADA, new TransitionCommand(ASIGNADA, "Motivo", 3L, null),
				TECH_3)).isInstanceOf(InvalidTransitionDataException.class)
				.satisfies(ex -> assertThat(((InvalidTransitionDataException) ex).errors())
						.containsKey("technicianId"));
	}

	@Test
	void resolvingRequiresASolutionComment() {
		assertThatThrownBy(() -> move(TECHNICIAN, EN_ATENCION, to(RESUELTA), null))
				.isInstanceOf(InvalidTransitionDataException.class)
				.satisfies(ex -> assertThat(((InvalidTransitionDataException) ex).errors()).containsKey("reason"));
		assertThatThrownBy(() -> move(TECHNICIAN, EN_ATENCION, new TransitionCommand(RESUELTA, "  ", null, null), null))
				.isInstanceOf(InvalidTransitionDataException.class);
	}

	@Test
	void rejectingTheSolutionRequiresAReason() {
		assertThatThrownBy(() -> move(OWNER, RESUELTA, to(EN_ATENCION), null))
				.isInstanceOf(InvalidTransitionDataException.class)
				.satisfies(ex -> assertThat(((InvalidTransitionDataException) ex).errors()).containsKey("reason"));
	}

	@Test
	void closingWithoutResolvingRequiresNoProcedeOrDuplicada() {
		assertThatThrownBy(() -> move(SUPERVISOR, REGISTRADA, to(CERRADA), null))
				.isInstanceOf(InvalidTransitionDataException.class)
				.satisfies(ex -> assertThat(((InvalidTransitionDataException) ex).errors())
						.containsKey("closureReason"));
		assertThatThrownBy(() -> move(SUPERVISOR, REGISTRADA,
				new TransitionCommand(CERRADA, null, null, ClosureReason.CONFIRMADA), null))
				.isInstanceOf(InvalidTransitionDataException.class);
		assertThatThrownBy(() -> move(SUPERVISOR, REGISTRADA, new TransitionCommand(CERRADA, "porque sí", null, null),
				null)).isInstanceOf(InvalidTransitionDataException.class);
	}

	@Test
	void aReasonLongerThanTheHistoryColumnIsRejected() {
		assertThatThrownBy(() -> move(TECHNICIAN, EN_ATENCION,
				new TransitionCommand(RESUELTA, "x".repeat(StateMachine.MAX_REASON_LENGTH + 1), null, null), null))
				.isInstanceOf(InvalidTransitionDataException.class);
		assertThat(move(TECHNICIAN, EN_ATENCION,
				new TransitionCommand(RESUELTA, "x".repeat(StateMachine.MAX_REASON_LENGTH), null, null), null).to())
				.isEqualTo(RESUELTA);
	}

	@Test
	void everyInvalidFieldIsReportedAtOnce() {
		assertThatThrownBy(() -> move(SUPERVISOR, ASIGNADA, new TransitionCommand(ASIGNADA, null, null, null), null))
				.satisfies(ex -> assertThat(((InvalidTransitionDataException) ex).errors())
						.containsOnlyKeys("technicianId", "reason"));
	}

	// ───── Which transitions a user may attempt ─────

	@Test
	void supervisorOfTheAreaMayEvaluateAssignReassignAndCloseUnresolved() {
		assertThat(StateMachine.allowedTransitions(SUPERVISOR, subject(REGISTRADA)))
				.containsExactlyInAnyOrder(EN_EVALUACION, CERRADA);
		assertThat(StateMachine.allowedTransitions(SUPERVISOR, subject(EN_EVALUACION)))
				.containsExactlyInAnyOrder(ASIGNADA, CERRADA);
		assertThat(StateMachine.allowedTransitions(SUPERVISOR, subject(ASIGNADA))).containsExactly(ASIGNADA);
		assertThat(StateMachine.allowedTransitions(SUPERVISOR, subject(EN_ATENCION))).isEmpty();
		assertThat(StateMachine.allowedTransitions(SUPERVISOR, subject(RESUELTA))).isEmpty();
		assertThat(StateMachine.allowedTransitions(SUPERVISOR, subject(CERRADA))).isEmpty();
	}

	@Test
	void assignedTechnicianMayStartAndResolve() {
		assertThat(StateMachine.allowedTransitions(TECHNICIAN, subject(ASIGNADA))).containsExactly(EN_ATENCION);
		assertThat(StateMachine.allowedTransitions(TECHNICIAN, subject(EN_ATENCION))).containsExactly(RESUELTA);
		assertThat(StateMachine.allowedTransitions(TECHNICIAN, subject(RESUELTA))).isEmpty();
	}

	@Test
	void ownerMayConfirmOrRejectAResolvedRequest() {
		assertThat(StateMachine.allowedTransitions(OWNER, subject(RESUELTA)))
				.containsExactlyInAnyOrder(CERRADA, EN_ATENCION);
		assertThat(StateMachine.allowedTransitions(OWNER, subject(REGISTRADA))).containsExactly(CANCELADA);
	}

	@Test
	void peopleWhoCannotActGetNoTransitions() {
		assertThat(StateMachine.allowedTransitions(OTHER_AREA_SUPERVISOR, subject(REGISTRADA))).isEmpty();
		assertThat(StateMachine.allowedTransitions(OTHER_TECHNICIAN, subject(ASIGNADA))).isEmpty();
		assertThat(StateMachine.allowedTransitions(OTHER_REQUESTER, subject(RESUELTA))).isEmpty();
		assertThat(StateMachine.allowedTransitions(ADMIN, subject(REGISTRADA))).isEmpty();
	}

	// ───── Which interface actions a user may perform ─────

	@Test
	void supervisorActionsFollowTheTransitionsTheyMayPerform() {
		assertThat(StateMachine.allowedActions(SUPERVISOR, subject(REGISTRADA)))
				.containsExactly(RequestAction.EVAL, RequestAction.NO_PROCEDE, RequestAction.DUPLICATE);
		assertThat(StateMachine.allowedActions(SUPERVISOR, subject(EN_EVALUACION)))
				.containsExactly(RequestAction.ASSIGN, RequestAction.NO_PROCEDE, RequestAction.DUPLICATE);
		assertThat(StateMachine.allowedActions(SUPERVISOR, subject(ASIGNADA))).containsExactly(RequestAction.REASSIGN);
		assertThat(StateMachine.allowedActions(SUPERVISOR, subject(EN_ATENCION))).isEmpty();
		assertThat(StateMachine.allowedActions(SUPERVISOR, subject(CERRADA))).isEmpty();
	}

	@Test
	void technicianActionsAreStartAndResolve() {
		assertThat(StateMachine.allowedActions(TECHNICIAN, subject(ASIGNADA))).containsExactly(RequestAction.START);
		assertThat(StateMachine.allowedActions(TECHNICIAN, subject(EN_ATENCION))).containsExactly(RequestAction.RESOLVE);
	}

	@Test
	void confirmingOrRejectingIsNotAnInterfaceAction() {
		// The requester answers a resolved request through allowedTransitions (confirm / reject).
		assertThat(StateMachine.allowedActions(OWNER, subject(RESUELTA))).isEmpty();
		assertThat(StateMachine.allowedActions(OTHER_AREA_SUPERVISOR, subject(REGISTRADA))).isEmpty();
		assertThat(StateMachine.allowedActions(OTHER_TECHNICIAN, subject(ASIGNADA))).isEmpty();
		assertThat(StateMachine.allowedActions(ADMIN, subject(REGISTRADA))).isEmpty();
	}

	// ───── Automatic closure (by the system, not by a user) ─────

	@Test
	void anUnconfirmedSolutionIsClosedAutomaticallyKeepingTheTechnician() {
		TransitionOutcome outcome = StateMachine.autoClose(subject(RESUELTA), 72);

		assertThat(outcome.to()).isEqualTo(CERRADA);
		assertThat(outcome.closureReason()).isEqualTo(ClosureReason.AUTOMATICA);
		assertThat(outcome.technicianId()).isEqualTo(3L);
		assertThat(outcome.historyTechnicianId()).isNull();
		assertThat(outcome.solution()).isNull();
		assertThat(outcome.reason()).isEqualTo("Cierre automático: el solicitante no confirmó la solución en 72 h");
	}

	@ParameterizedTest
	@CsvSource({ "REGISTRADA", "EN_EVALUACION", "ASIGNADA", "EN_ATENCION", "CERRADA" })
	void onlyAResolvedRequestIsClosedAutomatically(RequestStatus from) {
		assertThatThrownBy(() -> StateMachine.autoClose(subject(from), 72))
				.isInstanceOf(InvalidTransitionException.class);
	}

	@Test
	void errorsAreImmutableCopies() {
		Map<String, String> errors = new java.util.HashMap<>(Map.of("reason", "x"));
		InvalidTransitionDataException ex = new InvalidTransitionDataException(errors);
		errors.put("other", "y");

		assertThat(ex.errors()).containsOnlyKeys("reason");
	}

	// ───── cancel and restore (contract «Cancelar y restaurar una solicitud») ─────

	private static final java.time.Instant NOW = java.time.Instant.parse("2026-11-20T15:00:00Z");

	private static WorkflowSubject cancelled(RequestStatus previous, java.time.Duration ago) {
		return new WorkflowSubject(CANCELADA, 4, 1, null,
				new pe.edu.unmsm.servicios.requests.domain.Cancellation(previous, NOW.minus(ago)), NOW);
	}

	@ParameterizedTest
	@CsvSource({ "REGISTRADA", "EN_EVALUACION" })
	void theOwnerMayCancelBeforeATechnicianIsAssigned(RequestStatus from) {
		TransitionOutcome outcome = move(OWNER, from, new TransitionCommand(CANCELADA, "Ya no hace falta", null, null),
				null);

		assertThat(outcome.to()).isEqualTo(CANCELADA);
		assertThat(outcome.reason()).isEqualTo("Cancelada por el solicitante: Ya no hace falta");
	}

	@ParameterizedTest
	@CsvSource({ "ASIGNADA", "EN_ATENCION", "RESUELTA", "CERRADA" })
	void onceATechnicianIsAssignedItCanNoLongerBeCancelled(RequestStatus from) {
		assertThatThrownBy(() -> move(OWNER, from, new TransitionCommand(CANCELADA, "Ya no", null, null), null))
				.isInstanceOf(InvalidTransitionException.class);
	}

	@Test
	void onlyTheOwnerMayCancelAndAlwaysWithAReason() {
		assertThatThrownBy(() -> move(SUPERVISOR, REGISTRADA, new TransitionCommand(CANCELADA, "x", null, null), null))
				.isInstanceOf(TransitionForbiddenException.class);
		assertThatThrownBy(() -> move(OTHER_REQUESTER, REGISTRADA, new TransitionCommand(CANCELADA, "x", null, null),
				null)).isInstanceOf(TransitionForbiddenException.class);
		assertThatThrownBy(() -> move(OWNER, REGISTRADA, to(CANCELADA), null))
				.isInstanceOf(InvalidTransitionDataException.class);
	}

	@Test
	void restoringOnlyGoesBackToTheStatusItHadWithinSevenDays() {
		WorkflowSubject recent = cancelled(EN_EVALUACION, java.time.Duration.ofDays(6));

		assertThat(StateMachine.allowedTransitions(OWNER, recent)).containsExactly(EN_EVALUACION);
		assertThat(StateMachine.allowedActions(OWNER, recent)).containsExactly(RequestAction.RESTORE);
		assertThat(StateMachine.transition(OWNER, recent, to(EN_EVALUACION), null).to()).isEqualTo(EN_EVALUACION);
		assertThatThrownBy(() -> StateMachine.transition(OWNER, recent, to(REGISTRADA), null))
				.isInstanceOf(InvalidTransitionException.class);
		assertThatThrownBy(() -> StateMachine.transition(SUPERVISOR, recent, to(EN_EVALUACION), null))
				.isInstanceOf(TransitionForbiddenException.class);
	}

	@Test
	void afterSevenDaysItCanNoLongerBeRestored() {
		WorkflowSubject old = cancelled(REGISTRADA, java.time.Duration.ofDays(7).plusSeconds(1));

		assertThat(StateMachine.allowedTransitions(OWNER, old)).isEmpty();
		assertThat(StateMachine.allowedActions(OWNER, old)).isEmpty();
		assertThatThrownBy(() -> StateMachine.transition(OWNER, old, to(REGISTRADA), null))
				.isInstanceOf(RestoreExpiredException.class);
	}

}
