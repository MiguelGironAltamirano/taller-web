package pe.edu.unmsm.servicios.workflow.infrastructure;

import java.time.Clock;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import pe.edu.unmsm.servicios.requests.application.AllowedActions;
import pe.edu.unmsm.servicios.requests.application.AllowedTransitions;
import pe.edu.unmsm.servicios.requests.application.RequestStateStore;
import pe.edu.unmsm.servicios.requests.application.TechnicianWorkload;
import pe.edu.unmsm.servicios.requests.application.UnconfirmedResolutions;
import pe.edu.unmsm.servicios.shared.application.SystemSettingsProvider;
import pe.edu.unmsm.servicios.workflow.application.AutoCloseUnconfirmedRequests;
import pe.edu.unmsm.servicios.workflow.application.CloseUnconfirmedRequest;
import pe.edu.unmsm.servicios.workflow.application.ListTechnicians;
import pe.edu.unmsm.servicios.workflow.application.TechnicianDirectory;
import pe.edu.unmsm.servicios.workflow.application.TransitionRequest;
import pe.edu.unmsm.servicios.workflow.application.WorkflowAllowedActions;
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
	CloseUnconfirmedRequest closeUnconfirmedRequest(RequestStateStore requests, UnconfirmedResolutions resolutions,
			Clock clock, ApplicationEventPublisher eventPublisher) {
		return new CloseUnconfirmedRequest(requests, resolutions, clock, eventPublisher);
	}

	/** The batch size bounds the work (and the notifications) of one run; the rest waits for the next run. */
	@Bean
	AutoCloseUnconfirmedRequests autoCloseUnconfirmedRequests(UnconfirmedResolutions resolutions,
			CloseUnconfirmedRequest closer, SystemSettingsProvider settings, Clock clock,
			@Value("${app.workflow.auto-close.batch-size:100}") int batchSize) {
		return new AutoCloseUnconfirmedRequests(resolutions, closer, settings, clock, batchSize);
	}

	@Bean
	ListTechnicians listTechnicians(TechnicianDirectory technicians, TechnicianWorkload workload) {
		return new ListTechnicians(technicians, workload);
	}

	/** The detail of a request offers each user only the transitions the state machine allows them. */
	@Bean
	AllowedTransitions allowedTransitions(Clock clock) {
		return new WorkflowAllowedTransitions(clock);
	}

	/** The interface actions (allowedActions) behind those transitions. */
	@Bean
	AllowedActions allowedActions(Clock clock) {
		return new WorkflowAllowedActions(clock);
	}

}
