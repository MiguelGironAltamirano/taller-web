package pe.edu.unmsm.servicios.admin.infrastructure.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import pe.edu.unmsm.servicios.admin.application.AdminUserRepository;
import pe.edu.unmsm.servicios.admin.application.AreaDirectory;
import pe.edu.unmsm.servicios.admin.application.UserData;
import pe.edu.unmsm.servicios.admin.domain.ManagedUser;
import pe.edu.unmsm.servicios.admin.domain.ManagedUserNotFoundException;
import pe.edu.unmsm.servicios.auth.domain.EmailAlreadyRegisteredException;
import pe.edu.unmsm.servicios.auth.domain.RequesterType;
import pe.edu.unmsm.servicios.auth.domain.Role;

@Repository
class JpaAdminUserRepository implements AdminUserRepository, AreaDirectory {

	private final SpringDataAdminUserRepository users;
	private final SpringDataAdminAreaRepository areas;

	JpaAdminUserRepository(SpringDataAdminUserRepository users, SpringDataAdminAreaRepository areas) {
		this.users = users;
		this.areas = areas;
	}

	@Override
	public List<ManagedUser> findAllOrderedByName() {
		Map<Long, String> areaNames = areas.findAll().stream()
				.collect(Collectors.toMap(area -> area.id, area -> area.name));
		return users.findAllByOrderByFullNameAscIdAsc().stream().map(user -> toDomain(user, areaNames)).toList();
	}

	@Override
	public Optional<ManagedUser> findById(long id) {
		return users.findById(id).map(this::toDomain);
	}

	@Override
	public ManagedUser create(UserData data, String passwordHash) {
		if (users.existsByEmail(data.email())) {
			throw new EmailAlreadyRegisteredException(data.email());
		}
		AdminUserEntity entity = new AdminUserEntity();
		entity.passwordHash = passwordHash;
		entity.active = true;
		entity.createdAt = Instant.now();
		apply(entity, data);
		return save(entity, data.email());
	}

	@Override
	public ManagedUser update(long id, UserData data) {
		if (users.existsByEmailAndIdNot(data.email(), id)) {
			throw new EmailAlreadyRegisteredException(data.email());
		}
		AdminUserEntity entity = users.findById(id).orElseThrow(() -> new ManagedUserNotFoundException(id));
		apply(entity, data);
		return save(entity, data.email());
	}

	@Override
	public ManagedUser setActive(long id, boolean active) {
		AdminUserEntity entity = users.findById(id).orElseThrow(() -> new ManagedUserNotFoundException(id));
		entity.active = active;
		return toDomain(users.saveAndFlush(entity));
	}

	@Override
	public boolean isActiveArea(long areaId) {
		return areas.existsByIdAndActiveTrue(areaId);
	}

	private static void apply(AdminUserEntity entity, UserData data) {
		entity.fullName = data.fullName();
		entity.email = data.email();
		entity.role = data.role().name();
		entity.areaId = data.areaId();
		entity.requesterType = data.requesterType() == null ? null : data.requesterType().name();
	}

	/** The unique index on the email is the final guard against concurrent creations with the same email. */
	private ManagedUser save(AdminUserEntity entity, String email) {
		try {
			return toDomain(users.saveAndFlush(entity));
		}
		catch (DataIntegrityViolationException ex) {
			throw new EmailAlreadyRegisteredException(email);
		}
	}

	private ManagedUser toDomain(AdminUserEntity entity) {
		String areaName = entity.areaId == null ? null
				: areas.findById(entity.areaId).map(area -> area.name).orElse(null);
		return toDomain(entity, areaName == null ? Map.of() : Map.of(entity.areaId, areaName));
	}

	private static ManagedUser toDomain(AdminUserEntity entity, Map<Long, String> areaNames) {
		return new ManagedUser(entity.id, entity.fullName, entity.email, Role.valueOf(entity.role),
				entity.areaId, entity.areaId == null ? null : areaNames.get(entity.areaId),
				entity.requesterType == null ? null : RequesterType.valueOf(entity.requesterType), entity.active);
	}

}
