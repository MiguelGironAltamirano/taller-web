package pe.edu.unmsm.servicios.admin.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import pe.edu.unmsm.servicios.admin.domain.InvalidUserDataException;
import pe.edu.unmsm.servicios.admin.domain.ManagedUser;
import pe.edu.unmsm.servicios.admin.domain.ManagedUserNotFoundException;
import pe.edu.unmsm.servicios.admin.domain.SelfManagementException;
import pe.edu.unmsm.servicios.auth.application.PasswordHasher;
import pe.edu.unmsm.servicios.auth.application.RefreshTokenRepository;
import pe.edu.unmsm.servicios.auth.domain.EmailAlreadyRegisteredException;
import pe.edu.unmsm.servicios.auth.domain.RefreshToken;
import pe.edu.unmsm.servicios.auth.domain.RequesterType;
import pe.edu.unmsm.servicios.auth.domain.Role;

class AdminUserUseCasesTest {

	private static final long ADMIN_ID = 1L;
	private static final long ACTIVE_AREA = 1L;
	private static final long INACTIVE_AREA = 9L;
	private static final Instant NOW = Instant.parse("2026-10-20T10:00:00Z");

	private final InMemoryUsers users = new InMemoryUsers();
	private final RecordingRefreshTokens refreshTokens = new RecordingRefreshTokens();
	private final AreaDirectory areas = id -> id == ACTIVE_AREA;
	private final PasswordHasher hasher = new PasswordHasher() {
		@Override
		public String hash(String rawPassword) {
			return "hash:" + rawPassword;
		}

		@Override
		public boolean matches(String rawPassword, String hash) {
			return hash.equals("hash:" + rawPassword);
		}
	};

	private CreateManagedUser create;
	private UpdateManagedUser update;
	private SetUserActive setActive;
	private ListManagedUsers list;

	@BeforeEach
	void setUp() {
		create = new CreateManagedUser(users, areas, hasher, new TemporaryPasswordGenerator());
		update = new UpdateManagedUser(users, areas);
		setActive = new SetUserActive(users, refreshTokens, Clock.fixed(NOW, ZoneOffset.UTC));
		list = new ListManagedUsers(users);
		users.seed(new ManagedUser(ADMIN_ID, "Administrador Demo", "admin@unmsm.edu.pe", Role.ADMINISTRADOR, null,
				null, null, true));
	}

	private static UserData technician(String email, Long areaId) {
		return new UserData("Carlos Técnico", email, Role.TECNICO, areaId, null);
	}

	@Test
	void createStoresOnlyTheHashOfAGeneratedTemporaryPassword() {
		CreateManagedUser.Result result = create.execute(technician("  Carlos.Tec@UNMSM.edu.pe ", ACTIVE_AREA));

		assertThat(result.temporaryPassword()).hasSize(12);
		assertThat(result.user().email()).isEqualTo("carlos.tec@unmsm.edu.pe");
		assertThat(result.user().fullName()).isEqualTo("Carlos Técnico");
		assertThat(result.user().role()).isEqualTo(Role.TECNICO);
		assertThat(result.user().areaId()).isEqualTo(ACTIVE_AREA);
		assertThat(result.user().active()).isTrue();
		assertThat(users.passwordHashOf(result.user().id())).isEqualTo("hash:" + result.temporaryPassword());
	}

	@Test
	void createRejectsATechnicianWithoutArea() {
		assertThatThrownBy(() -> create.execute(technician("tec@unmsm.edu.pe", null)))
				.isInstanceOfSatisfying(InvalidUserDataException.class,
						ex -> assertThat(ex.errors()).containsKey("areaId"));
		assertThat(users.all()).hasSize(1);
	}

	@Test
	void createRejectsAnUnknownOrInactiveArea() {
		assertThatThrownBy(() -> create.execute(technician("tec@unmsm.edu.pe", INACTIVE_AREA)))
				.isInstanceOfSatisfying(InvalidUserDataException.class,
						ex -> assertThat(ex.errors()).containsKey("areaId"));
	}

	@Test
	void createGivesRequestersADefaultRequesterType() {
		CreateManagedUser.Result result = create
				.execute(new UserData("Ana Pérez", "ana@unmsm.edu.pe", Role.SOLICITANTE, null, null));

		assertThat(result.user().requesterType()).isEqualTo(RequesterType.ESTUDIANTE);
	}

	@Test
	void createPropagatesADuplicateEmail() {
		assertThatThrownBy(() -> create.execute(technician("admin@unmsm.edu.pe", ACTIVE_AREA)))
				.isInstanceOf(EmailAlreadyRegisteredException.class);
	}

	@Test
	void listReturnsUsersSortedByName() {
		create.execute(new UserData("Zoe Zapata", "zoe@unmsm.edu.pe", Role.SOLICITANTE, null, null));
		create.execute(new UserData("Beto Borja", "beto@unmsm.edu.pe", Role.SOLICITANTE, null, null));

		assertThat(list.execute()).extracting(ManagedUser::fullName).containsExactly("Administrador Demo",
				"Beto Borja", "Zoe Zapata");
	}

	@Test
	void updateChangesTheUserData() {
		long id = create.execute(technician("tec@unmsm.edu.pe", ACTIVE_AREA)).user().id();

		ManagedUser updated = update.execute(ADMIN_ID, id,
				new UserData("Carla Supervisora", "carla@unmsm.edu.pe", Role.SUPERVISOR, ACTIVE_AREA, null));

		assertThat(updated.fullName()).isEqualTo("Carla Supervisora");
		assertThat(updated.email()).isEqualTo("carla@unmsm.edu.pe");
		assertThat(updated.role()).isEqualTo(Role.SUPERVISOR);
	}

	@Test
	void updateOfAMissingUserFails() {
		assertThatThrownBy(() -> update.execute(ADMIN_ID, 999L, technician("x@unmsm.edu.pe", ACTIVE_AREA)))
				.isInstanceOf(ManagedUserNotFoundException.class);
	}

	@Test
	void anAdministratorCannotChangeTheirOwnRole() {
		assertThatThrownBy(() -> update.execute(ADMIN_ID, ADMIN_ID,
				new UserData("Administrador Demo", "admin@unmsm.edu.pe", Role.SOLICITANTE, null, null)))
				.isInstanceOf(SelfManagementException.class);
	}

	@Test
	void anAdministratorCanEditTheirOwnName() {
		ManagedUser updated = update.execute(ADMIN_ID, ADMIN_ID,
				new UserData("Admin Renombrado", "admin@unmsm.edu.pe", Role.ADMINISTRADOR, null, null));

		assertThat(updated.fullName()).isEqualTo("Admin Renombrado");
	}

	@Test
	void deactivatingAUserRevokesTheirRefreshTokens() {
		long id = create.execute(technician("tec@unmsm.edu.pe", ACTIVE_AREA)).user().id();

		ManagedUser result = setActive.execute(ADMIN_ID, id, false);

		assertThat(result.active()).isFalse();
		assertThat(refreshTokens.revokedForUsers).containsExactly(Map.entry(id, NOW));
	}

	@Test
	void reactivatingAUserDoesNotRevokeAnything() {
		long id = create.execute(technician("tec@unmsm.edu.pe", ACTIVE_AREA)).user().id();
		setActive.execute(ADMIN_ID, id, false);
		refreshTokens.revokedForUsers.clear();

		ManagedUser result = setActive.execute(ADMIN_ID, id, true);

		assertThat(result.active()).isTrue();
		assertThat(refreshTokens.revokedForUsers).isEmpty();
	}

	@Test
	void anAdministratorCannotDeactivateThemselves() {
		assertThatThrownBy(() -> setActive.execute(ADMIN_ID, ADMIN_ID, false))
				.isInstanceOf(SelfManagementException.class);
		assertThat(users.findById(ADMIN_ID)).hasValueSatisfying(user -> assertThat(user.active()).isTrue());
	}

	@Test
	void activatingAMissingUserFails() {
		assertThatThrownBy(() -> setActive.execute(ADMIN_ID, 999L, false))
				.isInstanceOf(ManagedUserNotFoundException.class);
	}

	static class InMemoryUsers implements AdminUserRepository {

		private final Map<Long, ManagedUser> users = new LinkedHashMap<>();
		private final Map<Long, String> hashes = new LinkedHashMap<>();
		private long nextId = 100;

		void seed(ManagedUser user) {
			users.put(user.id(), user);
		}

		String passwordHashOf(long id) {
			return hashes.get(id);
		}

		List<ManagedUser> all() {
			return new ArrayList<>(users.values());
		}

		@Override
		public List<ManagedUser> findAllOrderedByName() {
			return users.values().stream().sorted(Comparator.comparing(ManagedUser::fullName)).toList();
		}

		@Override
		public Optional<ManagedUser> findById(long id) {
			return Optional.ofNullable(users.get(id));
		}

		@Override
		public ManagedUser create(UserData data, String passwordHash) {
			ensureEmailFree(data.email(), null);
			long id = nextId++;
			ManagedUser user = new ManagedUser(id, data.fullName(), data.email(), data.role(), data.areaId(), null,
					data.requesterType(), true);
			users.put(id, user);
			hashes.put(id, passwordHash);
			return user;
		}

		@Override
		public ManagedUser update(long id, UserData data) {
			ensureEmailFree(data.email(), id);
			ManagedUser current = users.get(id);
			ManagedUser user = new ManagedUser(id, data.fullName(), data.email(), data.role(), data.areaId(), null,
					data.requesterType(), current.active());
			users.put(id, user);
			return user;
		}

		@Override
		public ManagedUser setActive(long id, boolean active) {
			ManagedUser current = users.get(id);
			ManagedUser user = new ManagedUser(id, current.fullName(), current.email(), current.role(),
					current.areaId(), current.areaName(), current.requesterType(), active);
			users.put(id, user);
			return user;
		}

		private void ensureEmailFree(String email, Long exceptId) {
			Set<Long> owners = new java.util.HashSet<>();
			users.values().stream().filter(user -> user.email().equals(email)).forEach(user -> owners.add(user.id()));
			owners.remove(exceptId);
			if (!owners.isEmpty()) {
				throw new EmailAlreadyRegisteredException(email);
			}
		}

	}

	static class RecordingRefreshTokens implements RefreshTokenRepository {

		final List<Map.Entry<Long, Instant>> revokedForUsers = new ArrayList<>();

		@Override
		public void save(RefreshToken token) {
		}

		@Override
		public Optional<RefreshToken> findByTokenHash(String tokenHash) {
			return Optional.empty();
		}

		@Override
		public boolean revoke(String tokenHash, Instant at) {
			return false;
		}

		@Override
		public int revokeAllForUser(long userId, Instant at) {
			revokedForUsers.add(Map.entry(userId, at));
			return 0;
		}

	}

}
