package pe.edu.unmsm.servicios.auth.infrastructure.persistence;

import java.time.Instant;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import pe.edu.unmsm.servicios.auth.application.UserRepository;
import pe.edu.unmsm.servicios.auth.domain.EmailAlreadyRegisteredException;
import pe.edu.unmsm.servicios.auth.domain.RequesterType;
import pe.edu.unmsm.servicios.auth.domain.Role;
import pe.edu.unmsm.servicios.auth.domain.User;

@Repository
class JpaUserRepository implements UserRepository {

	private final SpringDataUserRepository repository;

	JpaUserRepository(SpringDataUserRepository repository) {
		this.repository = repository;
	}

	@Override
	public Optional<User> findByEmail(String email) {
		return repository.findByEmail(email).map(JpaUserRepository::toDomain);
	}

	@Override
	public Optional<User> findById(long id) {
		return repository.findById(id).map(JpaUserRepository::toDomain);
	}

	@Override
	public User register(User user) {
		if (repository.findByEmail(user.email()).isPresent()) {
			throw new EmailAlreadyRegisteredException(user.email());
		}
		UserEntity entity = new UserEntity();
		entity.fullName = user.fullName();
		entity.email = user.email();
		entity.passwordHash = user.passwordHash();
		entity.role = user.role().name();
		entity.requesterType = user.requesterType() == null ? null : user.requesterType().name();
		entity.areaId = user.areaId();
		entity.active = user.active();
		entity.createdAt = Instant.now();
		try {
			return toDomain(repository.saveAndFlush(entity));
		}
		catch (DataIntegrityViolationException ex) {
			throw new EmailAlreadyRegisteredException(user.email());
		}
	}

	private static User toDomain(UserEntity entity) {
		return new User(entity.id, entity.fullName, entity.email, entity.passwordHash, Role.valueOf(entity.role),
				entity.requesterType == null ? null : RequesterType.valueOf(entity.requesterType), entity.areaId,
				entity.active);
	}

}
