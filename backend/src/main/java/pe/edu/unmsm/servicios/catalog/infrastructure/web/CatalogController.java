package pe.edu.unmsm.servicios.catalog.infrastructure.web;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import pe.edu.unmsm.servicios.catalog.application.GetCatalogs;
import pe.edu.unmsm.servicios.catalog.domain.Catalogs;

@RestController
@RequestMapping("/api/v1/catalogs")
class CatalogController {

	private final GetCatalogs getCatalogs;

	CatalogController(GetCatalogs getCatalogs) {
		this.getCatalogs = getCatalogs;
	}

	@GetMapping
	CatalogsResponse catalogs() {
		return CatalogsResponse.from(getCatalogs.execute());
	}

	record AreaResponse(long id, String name) {
	}

	record CategoryResponse(long id, String name, long areaId) {
	}

	record PriorityResponse(long id, String name, int slaHours) {
	}

	record LocationResponse(long id, String name, String kind, Long parentId) {
	}

	record CatalogsResponse(List<AreaResponse> areas, List<CategoryResponse> categories,
			List<PriorityResponse> priorities, List<LocationResponse> locations) {

		static CatalogsResponse from(Catalogs catalogs) {
			return new CatalogsResponse(
					catalogs.areas().stream().map(a -> new AreaResponse(a.id(), a.name())).toList(),
					catalogs.categories().stream().map(c -> new CategoryResponse(c.id(), c.name(), c.areaId())).toList(),
					catalogs.priorities().stream().map(p -> new PriorityResponse(p.id(), p.name(), p.slaHours())).toList(),
					catalogs.locations().stream()
							.map(l -> new LocationResponse(l.id(), l.name(), l.kind().name(), l.parentId())).toList());
		}

	}

}
