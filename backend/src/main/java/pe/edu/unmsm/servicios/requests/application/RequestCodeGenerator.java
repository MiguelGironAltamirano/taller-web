package pe.edu.unmsm.servicios.requests.application;

import pe.edu.unmsm.servicios.requests.domain.RequestCode;

public interface RequestCodeGenerator {

	/**
	 * The next code of the given year. It must run inside the transaction that creates the request, so a
	 * rollback also gives the number back and codes stay consecutive.
	 */
	RequestCode next(int year);

}
