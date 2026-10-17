package pe.edu.unmsm.servicios.admin.domain;

import java.util.List;

/** Read-only catalog of request states (spec §6), shown in administration. */
public final class RequestStates {

	public static final List<RequestState> ALL = List.of(
			new RequestState(1, "REGISTRADA", "Solicitud creada"),
			new RequestState(2, "EN_EVALUACION", "El supervisor evalúa el caso"),
			new RequestState(3, "ASIGNADA", "Técnico asignado"),
			new RequestState(4, "EN_ATENCION", "El técnico trabaja en la solicitud"),
			new RequestState(5, "RESUELTA", "Solución propuesta, pendiente de confirmación"),
			new RequestState(6, "CERRADA", "Solicitud finalizada"));

	private RequestStates() {
	}

}
