package pe.edu.unmsm.servicios.catalog.domain;

import java.util.List;

/** Reference data the client needs to fill in a request: only active areas and categories. */
public record Catalogs(List<Area> areas, List<Category> categories, List<PriorityLevel> priorities,
		List<Location> locations) {
}
