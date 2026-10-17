package pe.edu.unmsm.servicios.admin.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import pe.edu.unmsm.servicios.admin.domain.AdminArea;
import pe.edu.unmsm.servicios.admin.domain.AdminCategory;
import pe.edu.unmsm.servicios.admin.domain.CatalogConflictException;
import pe.edu.unmsm.servicios.admin.domain.CatalogEntryNotFoundException;
import pe.edu.unmsm.servicios.admin.domain.InvalidCatalogDataException;
import pe.edu.unmsm.servicios.catalog.domain.Location;
import pe.edu.unmsm.servicios.catalog.domain.LocationKind;
import pe.edu.unmsm.servicios.catalog.domain.PriorityLevel;

class ManageCatalogsTest {

	private final InMemoryCatalogStore store = new InMemoryCatalogStore();
	private final ManageCatalogs catalogs = new ManageCatalogs(store);

	private long maintenance;
	private long faculty;
	private long pavilion;
	private long room;

	@BeforeEach
	void setUp() {
		maintenance = store.insertArea("Mantenimiento").id();
		store.insertCategory("Electricidad", maintenance);
		store.priorities.put(1L, new PriorityLevel(1, "Crítica", 8));
		faculty = store.insertLocation("FISI", LocationKind.FACULTAD, null).id();
		pavilion = store.insertLocation("Pabellón Principal", LocationKind.PABELLON, faculty).id();
		room = store.insertLocation("Aula 101", LocationKind.AMBIENTE, pavilion).id();
	}

	@Test
	void createsANewAreaWithATrimmedName() {
		AdminArea area = catalogs.createArea("  Biblioteca ");

		assertThat(area.name()).isEqualTo("Biblioteca");
		assertThat(area.active()).isTrue();
	}

	@Test
	void anActiveAreaWithTheSameNameIsAConflict() {
		assertThatThrownBy(() -> catalogs.createArea("MANTENIMIENTO"))
				.isInstanceOfSatisfying(CatalogConflictException.class, ex -> assertThat(ex.errors()).containsKey("name"));
	}

	@Test
	void creatingTheNameOfAnInactiveAreaReactivatesIt() {
		long id = catalogs.createArea("Biblioteca").id();
		catalogs.deactivateArea(id);

		AdminArea again = catalogs.createArea("biblioteca");

		assertThat(again.id()).isEqualTo(id);
		assertThat(again.active()).isTrue();
	}

	@Test
	void anAreaWithActiveCategoriesCannotBeDeactivated() {
		assertThatThrownBy(() -> catalogs.deactivateArea(maintenance)).isInstanceOf(CatalogConflictException.class);
		assertThat(store.findArea(maintenance)).hasValueSatisfying(area -> assertThat(area.active()).isTrue());
	}

	@Test
	void anAreaWithActiveStaffCannotBeDeactivated() {
		long id = catalogs.createArea("Biblioteca").id();
		store.areasWithStaff.add(id);

		assertThatThrownBy(() -> catalogs.deactivateArea(id)).isInstanceOf(CatalogConflictException.class);
	}

	@Test
	void deactivatingAMissingOrInactiveAreaIsNotFound() {
		long id = catalogs.createArea("Biblioteca").id();
		catalogs.deactivateArea(id);

		assertThatThrownBy(() -> catalogs.deactivateArea(id)).isInstanceOf(CatalogEntryNotFoundException.class);
		assertThatThrownBy(() -> catalogs.deactivateArea(999)).isInstanceOf(CatalogEntryNotFoundException.class);
	}

	@Test
	void categoriesNeedAnActiveArea() {
		long inactive = catalogs.createArea("Biblioteca").id();
		catalogs.deactivateArea(inactive);

		assertThatThrownBy(() -> catalogs.createCategory("Libros", inactive)).isInstanceOfSatisfying(
				InvalidCatalogDataException.class, ex -> assertThat(ex.errors()).containsKey("areaId"));
		assertThatThrownBy(() -> catalogs.createCategory("Libros", 999)).isInstanceOf(InvalidCatalogDataException.class);
	}

	@Test
	void anActiveCategoryWithTheSameNameIsAConflict() {
		assertThatThrownBy(() -> catalogs.createCategory("electricidad", maintenance))
				.isInstanceOfSatisfying(CatalogConflictException.class, ex -> assertThat(ex.errors()).containsKey("name"));
	}

	@Test
	void creatingTheNameOfAnInactiveCategoryReactivatesItInTheGivenArea() {
		long library = catalogs.createArea("Biblioteca").id();
		AdminCategory category = catalogs.createCategory("Préstamos", maintenance);
		catalogs.deactivateCategory(category.id());

		AdminCategory again = catalogs.createCategory("Préstamos", library);

		assertThat(again.id()).isEqualTo(category.id());
		assertThat(again.areaId()).isEqualTo(library);
		assertThat(again.active()).isTrue();
	}

	@Test
	void deactivatingAMissingCategoryIsNotFound() {
		assertThatThrownBy(() -> catalogs.deactivateCategory(999)).isInstanceOf(CatalogEntryNotFoundException.class);
	}

	@Test
	void slaHoursMustBeBetweenOneAndAYear() {
		assertThatThrownBy(() -> catalogs.updatePrioritySla(1, 0)).isInstanceOfSatisfying(
				InvalidCatalogDataException.class, ex -> assertThat(ex.errors()).containsKey("slaHours"));
		assertThatThrownBy(() -> catalogs.updatePrioritySla(1, 8761)).isInstanceOf(InvalidCatalogDataException.class);

		assertThat(catalogs.updatePrioritySla(1, 12).slaHours()).isEqualTo(12);
	}

	@Test
	void updatingAMissingPriorityIsNotFound() {
		assertThatThrownBy(() -> catalogs.updatePrioritySla(99, 10)).isInstanceOf(CatalogEntryNotFoundException.class);
	}

	@Test
	void creatingALocationReusesExistingLevelsAndCreatesTheMissingOnes() {
		Location lab = catalogs.createLocation("fisi", "Pabellón Principal", "Laboratorio 3");
		Location newRoom = catalogs.createLocation("FISI", "Pabellón Nuevo", "Aula 1");

		assertThat(lab.kind()).isEqualTo(LocationKind.AMBIENTE);
		assertThat(lab.parentId()).isEqualTo(pavilion);
		Location newPavilion = store.findLocation(newRoom.parentId()).orElseThrow();
		assertThat(newPavilion.name()).isEqualTo("Pabellón Nuevo");
		assertThat(newPavilion.parentId()).isEqualTo(faculty);
	}

	@Test
	void anExistingRoomInTheSamePavilionIsAConflict() {
		assertThatThrownBy(() -> catalogs.createLocation("FISI", "Pabellón Principal", "aula 101"))
				.isInstanceOfSatisfying(CatalogConflictException.class, ex -> assertThat(ex.errors()).containsKey("room"));
	}

	@Test
	void onlyRoomsWithoutRequestsCanBeDeleted() {
		assertThatThrownBy(() -> catalogs.deleteLocation(pavilion)).isInstanceOf(CatalogConflictException.class);
		store.locationsWithRequests.add(room);
		assertThatThrownBy(() -> catalogs.deleteLocation(room)).isInstanceOf(CatalogConflictException.class);
		assertThatThrownBy(() -> catalogs.deleteLocation(999)).isInstanceOf(CatalogEntryNotFoundException.class);
		assertThat(store.findLocation(room)).isPresent();
	}

	@Test
	void deletingTheLastRoomAlsoRemovesItsEmptyPavilionAndFaculty() {
		catalogs.deleteLocation(room);

		assertThat(store.findLocation(room)).isEmpty();
		assertThat(store.findLocation(pavilion)).isEmpty();
		assertThat(store.findLocation(faculty)).isEmpty();
	}

	@Test
	void deletingARoomKeepsAPavilionThatStillHasRooms() {
		long other = catalogs.createLocation("FISI", "Pabellón Principal", "Laboratorio 3").id();

		catalogs.deleteLocation(other);

		assertThat(store.findLocation(other)).isEmpty();
		assertThat(store.findLocation(pavilion)).isPresent();
		assertThat(store.findLocation(faculty)).isPresent();
	}

	/** Names compare ignoring case, like the MySQL collation of the real tables. */
	static class InMemoryCatalogStore implements CatalogAdminStore {

		final Map<Long, AdminArea> areas = new LinkedHashMap<>();
		final Map<Long, AdminCategory> categories = new LinkedHashMap<>();
		final Map<Long, PriorityLevel> priorities = new LinkedHashMap<>();
		final Map<Long, Location> locations = new LinkedHashMap<>();
		final Set<Long> areasWithStaff = new HashSet<>();
		final Set<Long> locationsWithRequests = new HashSet<>();
		private long nextId = 1;

		private static boolean same(String a, String b) {
			return a.toLowerCase(Locale.ROOT).equals(b.toLowerCase(Locale.ROOT));
		}

		@Override
		public Optional<AdminArea> findArea(long id) {
			return Optional.ofNullable(areas.get(id));
		}

		@Override
		public Optional<AdminArea> findAreaByName(String name) {
			return areas.values().stream().filter(area -> same(area.name(), name)).findFirst();
		}

		@Override
		public AdminArea insertArea(String name) {
			AdminArea area = new AdminArea(nextId++, name, true);
			areas.put(area.id(), area);
			return area;
		}

		@Override
		public void setAreaActive(long id, boolean active) {
			AdminArea area = areas.get(id);
			areas.put(id, new AdminArea(id, area.name(), active));
		}

		@Override
		public boolean areaHasActiveCategories(long areaId) {
			return categories.values().stream().anyMatch(c -> c.areaId() == areaId && c.active());
		}

		@Override
		public boolean areaHasActiveStaff(long areaId) {
			return areasWithStaff.contains(areaId);
		}

		@Override
		public Optional<AdminCategory> findCategory(long id) {
			return Optional.ofNullable(categories.get(id));
		}

		@Override
		public Optional<AdminCategory> findCategoryByName(String name) {
			return categories.values().stream().filter(category -> same(category.name(), name)).findFirst();
		}

		@Override
		public AdminCategory insertCategory(String name, long areaId) {
			AdminCategory category = new AdminCategory(nextId++, name, areaId, true);
			categories.put(category.id(), category);
			return category;
		}

		@Override
		public void updateCategory(long id, long areaId, boolean active) {
			AdminCategory category = categories.get(id);
			categories.put(id, new AdminCategory(id, category.name(), areaId, active));
		}

		@Override
		public Optional<PriorityLevel> findPriority(long id) {
			return Optional.ofNullable(priorities.get(id));
		}

		@Override
		public void updatePrioritySla(long id, int slaHours) {
			PriorityLevel priority = priorities.get(id);
			priorities.put(id, new PriorityLevel(id, priority.name(), slaHours));
		}

		@Override
		public Optional<Location> findLocation(long id) {
			return Optional.ofNullable(locations.get(id));
		}

		@Override
		public Optional<Location> findLocationByName(String name, LocationKind kind, Long parentId) {
			return locations.values().stream()
					.filter(l -> l.kind() == kind && Objects.equals(l.parentId(), parentId) && same(l.name(), name))
					.findFirst();
		}

		@Override
		public Location insertLocation(String name, LocationKind kind, Long parentId) {
			Location location = new Location(nextId++, name, kind, parentId);
			locations.put(location.id(), location);
			return location;
		}

		@Override
		public boolean locationHasChildren(long id) {
			return locations.values().stream().anyMatch(l -> Objects.equals(l.parentId(), id));
		}

		@Override
		public boolean locationHasRequests(long id) {
			return locationsWithRequests.contains(id);
		}

		@Override
		public void deleteLocation(long id) {
			locations.remove(id);
		}

	}

}
