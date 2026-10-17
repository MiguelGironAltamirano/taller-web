package pe.edu.unmsm.servicios.catalog.infrastructure;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import pe.edu.unmsm.servicios.catalog.application.CatalogLookup;
import pe.edu.unmsm.servicios.catalog.application.GetCatalogs;

@Configuration
class CatalogConfig {

	@Bean
	GetCatalogs getCatalogs(CatalogLookup catalogs) {
		return new GetCatalogs(catalogs);
	}

}
