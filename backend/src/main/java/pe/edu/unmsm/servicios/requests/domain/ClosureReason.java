package pe.edu.unmsm.servicios.requests.domain;

/**
 * Why a request was closed: without resolving it ({@code NO_PROCEDE}, {@code DUPLICADA}), after the requester
 * confirmed the solution ({@code CONFIRMADA}), or by the system once the confirmation period expired
 * ({@code AUTOMATICA}).
 */
public enum ClosureReason {

	NO_PROCEDE, DUPLICADA, CONFIRMADA, AUTOMATICA

}
