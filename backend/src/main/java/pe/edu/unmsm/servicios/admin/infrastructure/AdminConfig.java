package pe.edu.unmsm.servicios.admin.infrastructure;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import pe.edu.unmsm.servicios.admin.application.AdminUserRepository;
import pe.edu.unmsm.servicios.admin.application.AreaDirectory;
import pe.edu.unmsm.servicios.admin.application.CatalogAdminStore;
import pe.edu.unmsm.servicios.admin.application.ManageCatalogs;
import pe.edu.unmsm.servicios.admin.application.CreateManagedUser;
import pe.edu.unmsm.servicios.admin.application.ListManagedUsers;
import pe.edu.unmsm.servicios.admin.application.SetUserActive;
import pe.edu.unmsm.servicios.admin.application.TemporaryPasswordGenerator;
import pe.edu.unmsm.servicios.admin.application.UpdateManagedUser;
import pe.edu.unmsm.servicios.auth.application.PasswordHasher;
import pe.edu.unmsm.servicios.auth.application.RefreshTokenRepository;

@Configuration
class AdminConfig {

	@Bean
	ListManagedUsers listManagedUsers(AdminUserRepository users) {
		return new ListManagedUsers(users);
	}

	@Bean
	CreateManagedUser createManagedUser(AdminUserRepository users, AreaDirectory areas, PasswordHasher hasher) {
		return new CreateManagedUser(users, areas, hasher, new TemporaryPasswordGenerator());
	}

	@Bean
	UpdateManagedUser updateManagedUser(AdminUserRepository users, AreaDirectory areas) {
		return new UpdateManagedUser(users, areas);
	}

	@Bean
	ManageCatalogs manageCatalogs(CatalogAdminStore store) {
		return new ManageCatalogs(store);
	}

	@Bean
	SetUserActive setUserActive(AdminUserRepository users, RefreshTokenRepository refreshTokens, Clock clock) {
		return new SetUserActive(users, refreshTokens, clock);
	}

}
