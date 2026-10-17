package pe.edu.unmsm.servicios.admin.application;

import java.util.Optional;

import pe.edu.unmsm.servicios.admin.domain.AdminArea;
import pe.edu.unmsm.servicios.admin.domain.AdminCategory;
import pe.edu.unmsm.servicios.catalog.domain.Location;
import pe.edu.unmsm.servicios.catalog.domain.LocationKind;
import pe.edu.unmsm.servicios.catalog.domain.PriorityLevel;

/**
 * Write access to the catalog tables for the administrator. Name lookups follow the database collation
 * (case and accent insensitive). Inactive areas and categories are included so they can be reactivated.
 */
public interface CatalogAdminStore {

	Optional<AdminArea> findArea(long id);

	Optional<AdminArea> findAreaByName(String name);

	/** Throws CatalogConflictException when the name is taken (concurrent creation). */
	AdminArea insertArea(String name);

	void setAreaActive(long id, boolean active);

	boolean areaHasActiveCategories(long areaId);

	/** Active technicians or supervisors belong to the area. */
	boolean areaHasActiveStaff(long areaId);

	Optional<AdminCategory> findCategory(long id);

	Optional<AdminCategory> findCategoryByName(String name);

	/** Throws CatalogConflictException when the name is taken (concurrent creation). */
	AdminCategory insertCategory(String name, long areaId);

	void updateCategory(long id, long areaId, boolean active);

	Optional<PriorityLevel> findPriority(long id);

	void updatePrioritySla(long id, int slaHours);

	Optional<Location> findLocation(long id);

	Optional<Location> findLocationByName(String name, LocationKind kind, Long parentId);

	Location insertLocation(String name, LocationKind kind, Long parentId);

	boolean locationHasChildren(long id);

	boolean locationHasRequests(long id);

	void deleteLocation(long id);

}
