package pe.edu.unmsm.servicios.collaboration.application;

import java.util.List;
import java.util.Optional;

import pe.edu.unmsm.servicios.collaboration.domain.Evidence;

public interface EvidenceRepository {

	Evidence save(Evidence evidence);

	List<Evidence> findByRequestId(long requestId);

	Optional<Evidence> findByIdAndRequestId(long id, long requestId);

	long countByRequestId(long requestId);

}
