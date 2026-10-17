package pe.edu.unmsm.servicios.requests.infrastructure.persistence;

import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

interface SpringDataServiceRequestRepository extends JpaRepository<ServiceRequestEntity, Long> {

	/** Locks the row until the transaction ends, so concurrent transitions of one request are serialized. */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<ServiceRequestEntity> findByCode(String code);

}
