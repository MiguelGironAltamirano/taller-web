package pe.edu.unmsm.servicios.admin.application;

import java.util.Map;

import org.springframework.transaction.annotation.Transactional;

import pe.edu.unmsm.servicios.admin.domain.AdminArea;
import pe.edu.unmsm.servicios.admin.domain.AdminCategory;
import pe.edu.unmsm.servicios.admin.domain.CatalogConflictException;
import pe.edu.unmsm.servicios.admin.domain.CatalogEntryNotFoundException;
import pe.edu.unmsm.servicios.admin.domain.InvalidCatalogDataException;
import pe.edu.unmsm.servicios.catalog.domain.Location;
import pe.edu.unmsm.servicios.catalog.domain.LocationKind;
import pe.edu.unmsm.servicios.catalog.domain.PriorityLevel;

/**
 * Catalog administration (spec §5 admin). Areas and categories are deactivated instead of deleted so the
 * requests and users that reference them keep working; creating an inactive name reactivates it.
 */
public class ManageCatalogs {

	static final int MAX_SLA_HOURS = 8760;

	private final CatalogAdminStore store;

	public ManageCatalogs(CatalogAdminStore store) {
		this.store = store;
	}

	@Transactional
	public AdminArea createArea(String name) {
		String clean = name.trim();
		var existing = store.findAreaByName(clean);
		if (existing.isEmpty()) {
			return store.insertArea(clean);
		}
		AdminArea area = existing.get();
		if (area.active()) {
			throw new CatalogConflictException("Ya existe un área con ese nombre",
					Map.of("name", "Ya existe un área con ese nombre"));
		}
		store.setAreaActive(area.id(), true);
		return new AdminArea(area.id(), area.name(), true);
	}

	@Transactional
	public void deactivateArea(long id) {
		store.findArea(id).filter(AdminArea::active)
				.orElseThrow(() -> new CatalogEntryNotFoundException("El área no existe"));
		if (store.areaHasActiveCategories(id)) {
			throw new CatalogConflictException("No se puede eliminar: el área tiene categorías activas");
		}
		if (store.areaHasActiveStaff(id)) {
			throw new CatalogConflictException("No se puede eliminar: el área tiene técnicos o supervisores activos");
		}
		store.setAreaActive(id, false);
	}

	@Transactional
	public AdminCategory createCategory(String name, long areaId) {
		if (store.findArea(areaId).filter(AdminArea::active).isEmpty()) {
			throw new InvalidCatalogDataException(Map.of("areaId", "El área no existe o está inactiva"));
		}
		String clean = name.trim();
		var existing = store.findCategoryByName(clean);
		if (existing.isEmpty()) {
			return store.insertCategory(clean, areaId);
		}
		AdminCategory category = existing.get();
		if (category.active()) {
			throw new CatalogConflictException("Ya existe una categoría con ese nombre",
					Map.of("name", "Ya existe una categoría con ese nombre"));
		}
		store.updateCategory(category.id(), areaId, true);
		return new AdminCategory(category.id(), category.name(), areaId, true);
	}

	/** Existing requests keep the category; it only stops being offered when registering. */
	@Transactional
	public void deactivateCategory(long id) {
		AdminCategory category = store.findCategory(id).filter(AdminCategory::active)
				.orElseThrow(() -> new CatalogEntryNotFoundException("La categoría no existe"));
		store.updateCategory(id, category.areaId(), false);
	}

	/** Applies to requests registered afterwards: due dates already stored are not recalculated. */
	@Transactional
	public PriorityLevel updatePrioritySla(long id, int slaHours) {
		if (slaHours < 1 || slaHours > MAX_SLA_HOURS) {
			throw new InvalidCatalogDataException(
					Map.of("slaHours", "Las horas de SLA deben estar entre 1 y " + MAX_SLA_HOURS));
		}
		PriorityLevel priority = store.findPriority(id)
				.orElseThrow(() -> new CatalogEntryNotFoundException("La prioridad no existe"));
		store.updatePrioritySla(id, slaHours);
		return new PriorityLevel(priority.id(), priority.name(), slaHours);
	}

	@Transactional
	public Location createLocation(String faculty, String pavilion, String room) {
		Location facultyLevel = ensure(faculty.trim(), LocationKind.FACULTAD, null);
		Location pavilionLevel = ensure(pavilion.trim(), LocationKind.PABELLON, facultyLevel.id());
		String roomName = room.trim();
		if (store.findLocationByName(roomName, LocationKind.AMBIENTE, pavilionLevel.id()).isPresent()) {
			throw new CatalogConflictException("El ambiente ya existe en ese pabellón",
					Map.of("room", "El ambiente ya existe en ese pabellón"));
		}
		return store.insertLocation(roomName, LocationKind.AMBIENTE, pavilionLevel.id());
	}

	/** Deletes a room without requests, then its pavilion and faculty when they are left empty. */
	@Transactional
	public void deleteLocation(long id) {
		Location location = store.findLocation(id)
				.orElseThrow(() -> new CatalogEntryNotFoundException("La ubicación no existe"));
		if (location.kind() != LocationKind.AMBIENTE) {
			throw new CatalogConflictException("Solo se pueden eliminar ambientes");
		}
		if (store.locationHasRequests(id)) {
			throw new CatalogConflictException("No se puede eliminar: hay solicitudes en este ambiente");
		}
		store.deleteLocation(id);
		Long parentId = location.parentId();
		while (parentId != null && !store.locationHasChildren(parentId) && !store.locationHasRequests(parentId)) {
			Long next = store.findLocation(parentId).map(Location::parentId).orElse(null);
			store.deleteLocation(parentId);
			parentId = next;
		}
	}

	private Location ensure(String name, LocationKind kind, Long parentId) {
		return store.findLocationByName(name, kind, parentId)
				.orElseGet(() -> store.insertLocation(name, kind, parentId));
	}

}
