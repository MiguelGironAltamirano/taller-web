package pe.edu.unmsm.servicios.requests.domain;

public enum RequestStatus {
	REGISTRADA, EN_EVALUACION, ASIGNADA, EN_ATENCION, RESUELTA, CERRADA;

	/** A request that reached Resuelta (or was closed) no longer counts against its SLA. */
	public boolean isFinished() {
		return this == RESUELTA || this == CERRADA;
	}

	/** The supervisor may reprioritize a request while it is still being handled. */
	public boolean allowsPriorityChange() {
		return !isFinished();
	}

}
