package pe.edu.unmsm.servicios.requests.domain;

/**
 * The actions the interface offers on a request ({@code allowedActions} of the API). Unlike the target statuses,
 * they tell the two unresolved closures apart and include the priority change, which keeps the status.
 */
public enum RequestAction {

	EVAL("eval"), ASSIGN("assign"), REASSIGN("reassign"), PRIORITY("prio"), NO_PROCEDE("noproc"),
	DUPLICATE("dup"), START("start"), RESOLVE("resolve");

	private final String apiName;

	RequestAction(String apiName) {
		this.apiName = apiName;
	}

	/** The identifier used by the API contract. */
	public String apiName() {
		return apiName;
	}

}
