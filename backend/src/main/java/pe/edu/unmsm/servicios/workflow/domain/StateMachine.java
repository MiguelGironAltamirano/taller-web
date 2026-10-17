package pe.edu.unmsm.servicios.workflow.domain;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import pe.edu.unmsm.servicios.requests.domain.ClosureReason;
import pe.edu.unmsm.servicios.requests.domain.RequestStatus;
import pe.edu.unmsm.servicios.shared.domain.AccessPolicy;
import pe.edu.unmsm.servicios.shared.domain.AuthenticatedUser;

/**
 * The life cycle of a request: the transition table of the spec (section 6) with who may perform each step and
 * what each one requires. Pure domain logic, so every row of the table can be tested without a database.
 *
 * <p>
 * The checks run in a fixed order so the HTTP status is predictable: a transition outside the table is a
 * conflict (409) whoever asks; a listed transition by someone who may not do it is forbidden (403); and only an
 * authorized actor is told which data is missing (400).
 */
public final class StateMachine {

	/** Longest free text kept in the history (the column holds 500 characters, including the fixed prefix). */
	public static final int MAX_REASON_LENGTH = 400;

	private static final String REQUESTER = "SOLICITANTE";

	private enum Step {
		EVALUATE, ASSIGN, REASSIGN, START, RESOLVE, CONFIRM, REJECT, CLOSE_UNRESOLVED
	}

	private StateMachine() {
	}

	public static TransitionOutcome transition(AuthenticatedUser actor, WorkflowSubject subject,
			TransitionCommand command, Technician technician) {
		Step step = stepFor(subject.status(), command.to())
				.orElseThrow(() -> new InvalidTransitionException(subject.status(), command.to()));
		if (!mayPerform(step, actor, subject)) {
			throw new TransitionForbiddenException("No tienes permiso para pasar la solicitud de " + subject.status()
					+ " a " + command.to());
		}

		Map<String, String> errors = new LinkedHashMap<>();
		String reason = command.reason() == null ? "" : command.reason().strip();
		if (reason.length() > MAX_REASON_LENGTH) {
			errors.put("reason", "El motivo no puede superar los " + MAX_REASON_LENGTH + " caracteres");
		}

		TransitionOutcome outcome = switch (step) {
			case EVALUATE -> same(subject, command.to(), "Se inició la evaluación del caso");
			case START -> same(subject, command.to(), "Atención iniciada en sitio");
			case ASSIGN -> {
				checkTechnician(errors, subject, command, technician, false);
				yield assigned(command.to(), technician, technician == null ? "" : "Asignada a " + technician.fullName());
			}
			case REASSIGN -> {
				checkTechnician(errors, subject, command, technician, true);
				requireText(errors, reason, "Indica el motivo de la reasignación");
				yield assigned(command.to(), technician,
						technician == null ? "" : "Reasignada a " + technician.fullName() + ". Motivo: " + reason);
			}
			case RESOLVE -> {
				requireText(errors, reason, "Indica el comentario de solución");
				yield same(subject, command.to(), "Solución: " + reason);
			}
			case CONFIRM -> new TransitionOutcome(command.to(), subject.technicianId(), ClosureReason.CONFIRMADA,
					"Solución confirmada por el solicitante" + suffix(reason), null);
			case REJECT -> {
				requireText(errors, reason, "Indica por qué rechazas la solución");
				yield same(subject, command.to(), "Solución rechazada: " + reason);
			}
			case CLOSE_UNRESOLVED -> closeUnresolved(errors, subject, command, reason);
		};

		if (!errors.isEmpty()) {
			throw new InvalidTransitionDataException(errors);
		}
		return outcome;
	}

	/** The statuses this user may move the request to (the data each one needs is checked when it is attempted). */
	public static List<RequestStatus> allowedTransitions(AuthenticatedUser actor, WorkflowSubject subject) {
		Set<RequestStatus> allowed = new LinkedHashSet<>();
		for (RequestStatus to : RequestStatus.values()) {
			Optional<Step> step = stepFor(subject.status(), to);
			if (step.isPresent() && mayPerform(step.get(), actor, subject)) {
				allowed.add(to);
			}
		}
		return List.copyOf(allowed);
	}

	private static Optional<Step> stepFor(RequestStatus from, RequestStatus to) {
		return switch (from) {
			case REGISTRADA -> switch (to) {
				case EN_EVALUACION -> Optional.of(Step.EVALUATE);
				case CERRADA -> Optional.of(Step.CLOSE_UNRESOLVED);
				default -> Optional.empty();
			};
			case EN_EVALUACION -> switch (to) {
				case ASIGNADA -> Optional.of(Step.ASSIGN);
				case CERRADA -> Optional.of(Step.CLOSE_UNRESOLVED);
				default -> Optional.empty();
			};
			case ASIGNADA -> switch (to) {
				case ASIGNADA -> Optional.of(Step.REASSIGN);
				case EN_ATENCION -> Optional.of(Step.START);
				default -> Optional.empty();
			};
			case EN_ATENCION -> to == RequestStatus.RESUELTA ? Optional.of(Step.RESOLVE) : Optional.empty();
			case RESUELTA -> switch (to) {
				case CERRADA -> Optional.of(Step.CONFIRM);
				case EN_ATENCION -> Optional.of(Step.REJECT);
				default -> Optional.empty();
			};
			case CERRADA -> Optional.empty();
		};
	}

	private static boolean mayPerform(Step step, AuthenticatedUser actor, WorkflowSubject subject) {
		return switch (step) {
			case EVALUATE, ASSIGN, REASSIGN, CLOSE_UNRESOLVED -> AccessPolicy.isSupervisorOfArea(actor, subject.areaId());
			case START, RESOLVE -> AccessPolicy.isAssignedTechnician(actor, subject.technicianId());
			case CONFIRM, REJECT -> REQUESTER.equals(actor.role()) && AccessPolicy.isOwner(actor, subject.requesterId());
		};
	}

	private static TransitionOutcome same(WorkflowSubject subject, RequestStatus to, String reason) {
		return new TransitionOutcome(to, subject.technicianId(), null, reason, null);
	}

	private static TransitionOutcome assigned(RequestStatus to, Technician technician, String reason) {
		Long id = technician == null ? null : technician.id();
		return new TransitionOutcome(to, id, null, reason, id);
	}

	private static void checkTechnician(Map<String, String> errors, WorkflowSubject subject,
			TransitionCommand command, Technician technician, boolean mustDiffer) {
		boolean valid = command.technicianId() != null && technician != null && technician.active()
				&& technician.areaId() != null && technician.areaId() == subject.areaId();
		if (!valid) {
			errors.put("technicianId", "Selecciona un técnico activo del área");
		}
		else if (mustDiffer && technician.id() == (subject.technicianId() == null ? -1 : subject.technicianId())) {
			errors.put("technicianId", "Elige un técnico distinto del asignado");
		}
	}

	private static void requireText(Map<String, String> errors, String reason, String message) {
		if (reason.isEmpty()) {
			errors.putIfAbsent("reason", message);
		}
	}

	private static TransitionOutcome closeUnresolved(Map<String, String> errors, WorkflowSubject subject,
			TransitionCommand command, String reason) {
		ClosureReason closure = command.closureReason();
		String freeText = reason;
		if (closure == null && !reason.isEmpty()) {
			// The contract also allows sending the closure reason itself in the "reason" field.
			try {
				closure = ClosureReason.valueOf(reason);
				freeText = "";
			}
			catch (IllegalArgumentException ex) {
				closure = null;
			}
		}
		else if (closure != null && freeText.equals(closure.name())) {
			freeText = "";
		}
		if (closure != ClosureReason.NO_PROCEDE && closure != ClosureReason.DUPLICADA) {
			errors.put("closureReason", "Indica si no procede o es duplicada");
			return same(subject, command.to(), "");
		}
		String label = closure == ClosureReason.NO_PROCEDE ? "No procede" : "Duplicada";
		return new TransitionOutcome(command.to(), subject.technicianId(), closure, label + suffix(freeText), null);
	}

	private static String suffix(String text) {
		return text.isEmpty() ? "" : ": " + text;
	}

}
