package pe.edu.unmsm.servicios.dashboard.application;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import pe.edu.unmsm.servicios.catalog.application.CatalogLookup;
import pe.edu.unmsm.servicios.catalog.domain.Area;
import pe.edu.unmsm.servicios.catalog.domain.Catalogs;
import pe.edu.unmsm.servicios.catalog.domain.PriorityLevel;
import pe.edu.unmsm.servicios.dashboard.domain.DashboardAccessDeniedException;
import pe.edu.unmsm.servicios.dashboard.domain.DashboardRange;
import pe.edu.unmsm.servicios.dashboard.domain.Indicators;
import pe.edu.unmsm.servicios.dashboard.domain.IndicatorsCalculator;
import pe.edu.unmsm.servicios.dashboard.domain.InvalidDashboardQueryException;
import pe.edu.unmsm.servicios.dashboard.domain.ReportingPeriod;
import pe.edu.unmsm.servicios.requests.application.RequestAnalyticsQuery;
import pe.edu.unmsm.servicios.requests.application.RequestAnalyticsRow;
import pe.edu.unmsm.servicios.shared.domain.AuthenticatedUser;

/** {@code GET /dashboard/indicators}: the indicators of the area of a supervisor, or of any area for an admin. */
public class GetDashboardIndicators {

	private static final String SUPERVISOR = "SUPERVISOR";
	private static final String ADMINISTRATOR = "ADMINISTRADOR";

	private final RequestAnalyticsQuery analytics;
	private final CatalogLookup catalog;
	private final Clock clock;

	public GetDashboardIndicators(RequestAnalyticsQuery analytics, CatalogLookup catalog, Clock clock) {
		this.analytics = analytics;
		this.catalog = catalog;
		this.clock = clock;
	}

	/**
	 * @param range  {@code 7}, {@code 30} or {@code mes}; null means 30
	 * @param areaId only for administrators (null = all areas); a supervisor always gets their own area
	 */
	public Indicators execute(AuthenticatedUser user, String range, Long areaId) {
		Long scopeArea = scopeArea(user, areaId);
		DashboardRange dashboardRange = DashboardRange.parse(range);
		Catalogs catalogs = catalog.activeCatalogs();
		String areaName = scopeArea == null ? null : areaName(catalogs, scopeArea, user);

		Instant now = clock.instant();
		ReportingPeriod period = dashboardRange.periodAt(now);
		List<RequestAnalyticsRow> registered = analytics.registeredBetween(scopeArea, period.previous().start(),
				period.end());
		List<RequestAnalyticsRow> open = analytics.openRequests(scopeArea);
		if (areaName == null && scopeArea != null) {
			// The area of a supervisor may have been deactivated; its requests still carry its name.
			areaName = registered.stream().map(RequestAnalyticsRow::areaName).findFirst().orElse(null);
		}

		List<String> priorities = catalogs.priorities().stream().map(PriorityLevel::name).toList();
		return IndicatorsCalculator.calculate(period, areaName, TrackedRequests.from(registered, now), priorities,
				TrackedRequests.from(open, now), now);
	}

	private static Long scopeArea(AuthenticatedUser user, Long areaId) {
		if (SUPERVISOR.equals(user.role()) && user.areaId() != null) {
			return user.areaId();
		}
		if (ADMINISTRATOR.equals(user.role())) {
			return areaId;
		}
		throw new DashboardAccessDeniedException("Solo un supervisor o un administrador puede consultar los indicadores");
	}

	private static String areaName(Catalogs catalogs, long areaId, AuthenticatedUser user) {
		String name = catalogs.areas().stream().filter(a -> a.id() == areaId).map(Area::name).findFirst()
				.orElse(null);
		if (name == null && ADMINISTRATOR.equals(user.role())) {
			throw new InvalidDashboardQueryException("areaId", "El área no existe o está inactiva");
		}
		return name;
	}

}
