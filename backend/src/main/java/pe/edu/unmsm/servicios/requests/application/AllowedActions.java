package pe.edu.unmsm.servicios.requests.application;

import java.util.List;

import pe.edu.unmsm.servicios.requests.domain.RequestAction;
import pe.edu.unmsm.servicios.shared.domain.AuthenticatedUser;

/**
 * The workflow actions the user may perform on this request. The workflow module owns the state machine and
 * provides the implementation; the priority change is decided by this module (see {@link GetRequest}).
 */
public interface AllowedActions {

	List<RequestAction> forUser(AuthenticatedUser user, RequestDetailView request);

}
