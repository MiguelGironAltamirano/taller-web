package pe.edu.unmsm.servicios.workflow.infrastructure;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import pe.edu.unmsm.servicios.auth.application.UserRepository;
import pe.edu.unmsm.servicios.auth.domain.Role;
import pe.edu.unmsm.servicios.auth.domain.User;
import pe.edu.unmsm.servicios.workflow.application.TechnicianDirectory;
import pe.edu.unmsm.servicios.workflow.domain.Technician;

/** Technicians are user accounts with the TECNICO role; they are read through the auth module. */
@Component
class AuthTechnicianDirectory implements TechnicianDirectory {

	private final UserRepository users;

	AuthTechnicianDirectory(UserRepository users) {
		this.users = users;
	}

	@Override
	public Optional<Technician> findTechnician(long id) {
		return users.findById(id).filter(user -> user.role() == Role.TECNICO).map(AuthTechnicianDirectory::toTechnician);
	}

	@Override
	public List<Technician> activeTechniciansOfArea(long areaId) {
		return users.findActiveByRoleAndArea(Role.TECNICO, areaId).stream()
				.map(AuthTechnicianDirectory::toTechnician).toList();
	}

	private static Technician toTechnician(User user) {
		return new Technician(user.id(), user.fullName(), user.areaId(), user.active());
	}

}
