package pe.edu.unmsm.servicios.workflow.application;

import java.util.List;

import pe.edu.unmsm.servicios.requests.application.AllowedActions;
import pe.edu.unmsm.servicios.requests.application.RequestDetailView;
import pe.edu.unmsm.servicios.requests.domain.RequestAction;
import pe.edu.unmsm.servicios.shared.domain.AuthenticatedUser;
import pe.edu.unmsm.servicios.workflow.domain.StateMachine;
import pe.edu.unmsm.servicios.workflow.domain.WorkflowSubject;

/** The interface actions the detail offers to each user, derived from the state machine of this module. */
public class WorkflowAllowedActions implements AllowedActions {

	@Override
	public List<RequestAction> forUser(AuthenticatedUser user, RequestDetailView request) {
		return StateMachine.allowedActions(user, new WorkflowSubject(request.summary().status(),
				request.requesterId(), request.areaId(), request.technicianId()));
	}

}
