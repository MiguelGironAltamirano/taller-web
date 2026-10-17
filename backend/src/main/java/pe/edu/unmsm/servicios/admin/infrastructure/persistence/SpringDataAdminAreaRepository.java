package pe.edu.unmsm.servicios.admin.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataAdminAreaRepository extends JpaRepository<AdminAreaEntity, Long> {

	boolean existsByIdAndActiveTrue(Long id);

}
