package pe.edu.unmsm.servicios.admin.application;

import org.springframework.transaction.annotation.Transactional;

import pe.edu.unmsm.servicios.admin.domain.ManagedUser;
import pe.edu.unmsm.servicios.admin.domain.ManagedUserNotFoundException;
import pe.edu.unmsm.servicios.admin.domain.SelfManagementException;

public class UpdateManagedUser {

	private final AdminUserRepository users;
	private final AreaDirectory areas;

	public UpdateManagedUser(AdminUserRepository users, AreaDirectory areas) {
		this.users = users;
		this.areas = areas;
	}

	@Transactional
	public ManagedUser execute(long actorId, long userId, UserData data) {
		ManagedUser current = users.findById(userId).orElseThrow(() -> new ManagedUserNotFoundException(userId));
		UserData valid = UserDataValidator.validate(data, areas);
		if (actorId == userId && valid.role() != current.role()) {
			throw new SelfManagementException("No puedes cambiar tu propio rol");
		}
		return users.update(userId, valid);
	}

}
