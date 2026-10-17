package pe.edu.unmsm.servicios.workflow.application;

import java.util.List;

import pe.edu.unmsm.servicios.requests.application.AllowedTransitions;
import pe.edu.unmsm.servicios.requests.application.RequestDetailView;
import pe.edu.unmsm.servicios.requests.domain.RequestStatus;
import pe.edu.unmsm.servicios.shared.domain.AuthenticatedUser;
import pe.edu.unmsm.servicios.workflow.domain.StateMachine;
import pe.edu.unmsm.servicios.workflow.domain.WorkflowSubject;

/** The statuses the detail offers to each user, computed by the state machine of this module. */
public class WorkflowAllowedTransitions implements AllowedTransitions {

	@Override
	public List<RequestStatus> forUser(AuthenticatedUser user, RequestDetailView request) {
		return StateMachine.allowedTransitions(user, new WorkflowSubject(request.summary().status(),
				request.requesterId(), request.areaId(), request.technicianId()));
	}

}
