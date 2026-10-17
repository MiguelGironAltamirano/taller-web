package pe.edu.unmsm.servicios.admin.infrastructure.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataAdminUserRepository extends JpaRepository<AdminUserEntity, Long> {

	List<AdminUserEntity> findAllByOrderByFullNameAscIdAsc();

	boolean existsByEmail(String email);

	boolean existsByEmailAndIdNot(String email, Long id);

}
