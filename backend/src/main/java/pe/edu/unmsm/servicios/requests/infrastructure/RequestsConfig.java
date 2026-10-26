package pe.edu.unmsm.servicios.requests.infrastructure;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import pe.edu.unmsm.servicios.catalog.application.CatalogLookup;
import pe.edu.unmsm.servicios.requests.application.AllowedActions;
import pe.edu.unmsm.servicios.requests.application.AllowedTransitions;
import pe.edu.unmsm.servicios.requests.application.ChangePriority;
import pe.edu.unmsm.servicios.requests.application.CreateRequest;
import pe.edu.unmsm.servicios.requests.application.GetRequest;
import pe.edu.unmsm.servicios.requests.application.ListRequests;
import pe.edu.unmsm.servicios.requests.application.RequestCodeGenerator;
import pe.edu.unmsm.servicios.requests.application.RequestCollaboration;
import pe.edu.unmsm.servicios.requests.application.RequestQueries;
import pe.edu.unmsm.servicios.requests.application.RequestRatings;
import pe.edu.unmsm.servicios.requests.application.RequestRepository;
import pe.edu.unmsm.servicios.requests.application.RequestStateStore;

@Configuration
class RequestsConfig {

	@Bean
	CreateRequest createRequest(CatalogLookup catalog, RequestCodeGenerator codes, RequestRepository requests,
			Clock clock) {
		return new CreateRequest(catalog, codes, requests, clock);
	}

	/**
	 * {@link AllowedTransitions} and {@link AllowedActions} are provided by the workflow module, which owns the
	 * state machine; {@link RequestCollaboration} by the collaboration module and {@link RequestRatings} by the
	 * satisfaction module.
	 */
	@Bean
	GetRequest getRequest(RequestQueries queries, AllowedTransitions allowedTransitions, AllowedActions allowedActions,
			RequestCollaboration collaboration, RequestRatings ratings, Clock clock) {
		return new GetRequest(queries, allowedTransitions, allowedActions, collaboration, ratings, clock);
	}

	@Bean
	ChangePriority changePriority(RequestStateStore requests, CatalogLookup catalog, Clock clock) {
		return new ChangePriority(requests, catalog, clock);
	}

	@Bean
	ListRequests listRequests(RequestQueries queries, Clock clock) {
		return new ListRequests(queries, clock);
	}

}
