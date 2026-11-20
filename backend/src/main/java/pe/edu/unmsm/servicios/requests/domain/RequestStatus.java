package pe.edu.unmsm.servicios.requests.domain;

public enum RequestStatus {
	REGISTRADA, EN_EVALUACION, ASIGNADA, EN_ATENCION, RESUELTA, CERRADA, CANCELADA;

	/**
	 * A request that reached Resuelta, was closed or was cancelled no longer counts against its SLA: it is neither
	 * pending nor overdue.
	 */
	public boolean isFinished() {
		return this == RESUELTA || this == CERRADA || this == CANCELADA;
	}

	/** The name people see, the same as the interface: messages never show the code. */
	public String label() {
		return switch (this) {
			case REGISTRADA -> "Registrada";
			case EN_EVALUACION -> "En evaluación";
			case ASIGNADA -> "Asignada";
			case EN_ATENCION -> "En atención";
			case RESUELTA -> "Resuelta";
			case CERRADA -> "Cerrada";
			case CANCELADA -> "Cancelada";
		};
	}

	/** The supervisor may reprioritize a request while it is still being handled. */
	public boolean allowsPriorityChange() {
		return !isFinished();
	}

}
