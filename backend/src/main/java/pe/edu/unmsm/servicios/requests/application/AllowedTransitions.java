package pe.edu.unmsm.servicios.requests.application;

import java.util.List;

import pe.edu.unmsm.servicios.requests.domain.RequestStatus;
import pe.edu.unmsm.servicios.shared.domain.AuthenticatedUser;

/**
 * The statuses the user may move this request to. The workflow module owns that state machine and provides
 * the implementation; the client only shows buttons for the statuses returned here.
 */
public interface AllowedTransitions {

	List<RequestStatus> forUser(AuthenticatedUser user, RequestDetailView request);

}
