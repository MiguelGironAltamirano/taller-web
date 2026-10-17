package pe.edu.unmsm.servicios.requests.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataServiceRequestRepository extends JpaRepository<ServiceRequestEntity, Long> {
}
