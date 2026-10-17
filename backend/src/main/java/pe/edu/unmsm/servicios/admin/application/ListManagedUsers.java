package pe.edu.unmsm.servicios.admin.application;

import java.util.List;

import org.springframework.transaction.annotation.Transactional;

import pe.edu.unmsm.servicios.admin.domain.ManagedUser;

public class ListManagedUsers {

	private final AdminUserRepository users;

	public ListManagedUsers(AdminUserRepository users) {
		this.users = users;
	}

	@Transactional(readOnly = true)
	public List<ManagedUser> execute() {
		return users.findAllOrderedByName();
	}

}
