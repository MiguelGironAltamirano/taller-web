package pe.edu.unmsm.servicios.workflow.infrastructure;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import pe.edu.unmsm.servicios.requests.application.AllowedTransitions;
import pe.edu.unmsm.servicios.requests.application.RequestStateStore;
import pe.edu.unmsm.servicios.requests.application.TechnicianWorkload;
import pe.edu.unmsm.servicios.workflow.application.ListTechnicians;
import pe.edu.unmsm.servicios.workflow.application.TechnicianDirectory;
import pe.edu.unmsm.servicios.workflow.application.TransitionRequest;
import pe.edu.unmsm.servicios.workflow.application.WorkflowAllowedTransitions;

import org.springframework.context.ApplicationEventPublisher;

@Configuration
class WorkflowConfig {

	@Bean
	TransitionRequest transitionRequest(RequestStateStore requests, TechnicianDirectory technicians, Clock clock,
			ApplicationEventPublisher eventPublisher) {
		return new TransitionRequest(requests, technicians, clock, eventPublisher);
	}

	@Bean
	ListTechnicians listTechnicians(TechnicianDirectory technicians, TechnicianWorkload workload) {
		return new ListTechnicians(technicians, workload);
	}

	/** The detail of a request offers each user only the transitions the state machine allows them. */
	@Bean
	AllowedTransitions allowedTransitions() {
		return new WorkflowAllowedTransitions();
	}

}
