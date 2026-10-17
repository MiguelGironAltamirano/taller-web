package pe.edu.unmsm.servicios.requests.infrastructure;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import pe.edu.unmsm.servicios.catalog.application.CatalogLookup;
import pe.edu.unmsm.servicios.requests.application.AllowedTransitions;
import pe.edu.unmsm.servicios.requests.application.CreateRequest;
import pe.edu.unmsm.servicios.requests.application.GetRequest;
import pe.edu.unmsm.servicios.requests.application.ListRequests;
import pe.edu.unmsm.servicios.requests.application.RequestCodeGenerator;
import pe.edu.unmsm.servicios.requests.application.RequestQueries;
import pe.edu.unmsm.servicios.requests.application.RequestRepository;

@Configuration
class RequestsConfig {

	@Bean
	CreateRequest createRequest(CatalogLookup catalog, RequestCodeGenerator codes, RequestRepository requests,
			Clock clock) {
		return new CreateRequest(catalog, codes, requests, clock);
	}

	/** {@link AllowedTransitions} is provided by the workflow module, which owns the state machine. */
	@Bean
	GetRequest getRequest(RequestQueries queries, AllowedTransitions allowedTransitions, Clock clock) {
		return new GetRequest(queries, allowedTransitions, clock);
	}

	@Bean
	ListRequests listRequests(RequestQueries queries, Clock clock) {
		return new ListRequests(queries, clock);
	}

}
