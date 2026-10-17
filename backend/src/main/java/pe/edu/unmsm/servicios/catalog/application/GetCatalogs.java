package pe.edu.unmsm.servicios.catalog.application;

import pe.edu.unmsm.servicios.catalog.domain.Catalogs;

public class GetCatalogs {

	private final CatalogLookup catalogs;

	public GetCatalogs(CatalogLookup catalogs) {
		this.catalogs = catalogs;
	}

	public Catalogs execute() {
		return catalogs.activeCatalogs();
	}

}
