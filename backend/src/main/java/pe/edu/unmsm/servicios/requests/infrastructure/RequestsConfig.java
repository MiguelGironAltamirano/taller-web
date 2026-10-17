package pe.edu.unmsm.servicios.requests.infrastructure;

import java.time.Clock;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import pe.edu.unmsm.servicios.catalog.application.CatalogLookup;
import pe.edu.unmsm.servicios.requests.application.AllowedTransitions;
import pe.edu.unmsm.servicios.requests.application.CreateRequest;
import pe.edu.unmsm.servicios.requests.application.GetRequest;
import pe.edu.unmsm.servicios.requests.application.ListRequests;
import pe.edu.unmsm.servicios.requests.application.RequestCodeGenerator;
import pe.edu.unmsm.servicios.requests.application.RequestDetailView;
import pe.edu.unmsm.servicios.requests.application.RequestQueries;
import pe.edu.unmsm.servicios.requests.application.RequestRepository;
import pe.edu.unmsm.servicios.requests.domain.RequestStatus;
import pe.edu.unmsm.servicios.shared.domain.AuthenticatedUser;

@Configuration
class RequestsConfig {

	@Bean
	CreateRequest createRequest(CatalogLookup catalog, RequestCodeGenerator codes, RequestRepository requests,
			Clock clock) {
		return new CreateRequest(catalog, codes, requests, clock);
	}

	@Bean
	GetRequest getRequest(RequestQueries queries, AllowedTransitions allowedTransitions, Clock clock) {
		return new GetRequest(queries, allowedTransitions, clock);
	}

	@Bean
	ListRequests listRequests(RequestQueries queries, Clock clock) {
		return new ListRequests(queries, clock);
	}

	/**
	 * Until the workflow module defines the state machine, no transition is offered. That module replaces
	 * this bean with its own {@link AllowedTransitions}.
	 */
	@Component
	static class NoAllowedTransitions implements AllowedTransitions {

		@Override
		public List<RequestStatus> forUser(AuthenticatedUser user, RequestDetailView request) {
			return List.of();
		}

	}

}
