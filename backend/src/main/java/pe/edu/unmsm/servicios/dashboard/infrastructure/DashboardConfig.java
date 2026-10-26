package pe.edu.unmsm.servicios.dashboard.infrastructure;

import java.time.Clock;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import pe.edu.unmsm.servicios.catalog.application.CatalogLookup;
import pe.edu.unmsm.servicios.dashboard.application.GetDashboardIndicators;
import pe.edu.unmsm.servicios.dashboard.application.GetTeamLoad;
import pe.edu.unmsm.servicios.requests.application.RequestAnalyticsQuery;
import pe.edu.unmsm.servicios.workflow.application.TechnicianDirectory;

/**
 * The dashboard only reads: requests through {@link RequestAnalyticsQuery}, catalogs through {@link CatalogLookup}
 * and technicians through the {@link TechnicianDirectory} of the workflow module.
 */
@Configuration
class DashboardConfig {

	@Bean
	GetDashboardIndicators getDashboardIndicators(RequestAnalyticsQuery analytics, CatalogLookup catalog,
			Clock clock) {
		return new GetDashboardIndicators(analytics, catalog, clock);
	}

	@Bean
	GetTeamLoad getTeamLoad(TechnicianDirectory technicians, RequestAnalyticsQuery analytics, Clock clock,
			@Value("${app.dashboard.technician-capacity:8}") int technicianCapacity) {
		return new GetTeamLoad(technicians, analytics, clock, technicianCapacity);
	}

}
