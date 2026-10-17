package pe.edu.unmsm.servicios.requests.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataRequestHistoryRepository extends JpaRepository<RequestHistoryEntity, Long> {
}
