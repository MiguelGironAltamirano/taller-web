package pe.edu.unmsm.servicios.catalog.application;

import java.util.Optional;

import pe.edu.unmsm.servicios.catalog.domain.Catalogs;
import pe.edu.unmsm.servicios.catalog.domain.Category;
import pe.edu.unmsm.servicios.catalog.domain.Location;
import pe.edu.unmsm.servicios.catalog.domain.PriorityLevel;

/**
 * Read access to the catalogs. It is also the entry point other modules (requests, triage, dashboard)
 * use to validate catalog references, so they never touch the catalog tables directly.
 */
public interface CatalogLookup {

	Catalogs activeCatalogs();

	/** The category only when it exists and is active. */
	Optional<Category> findActiveCategory(long id);

	Optional<PriorityLevel> findPriority(long id);

	/** The location at any level of the hierarchy. */
	Optional<Location> findLocation(long id);

}
