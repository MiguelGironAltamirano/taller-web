package pe.edu.unmsm.servicios.requests.infrastructure.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataRequestHistoryRepository extends JpaRepository<RequestHistoryEntity, Long> {

	Optional<RequestHistoryEntity> findFirstByRequestIdAndToStatusOrderByCreatedAtDescIdDesc(long requestId,
			String toStatus);

}
