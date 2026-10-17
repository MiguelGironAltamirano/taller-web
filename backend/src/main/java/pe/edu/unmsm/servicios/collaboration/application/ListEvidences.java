package pe.edu.unmsm.servicios.collaboration.application;

import java.util.List;

import pe.edu.unmsm.servicios.collaboration.domain.Evidence;
import pe.edu.unmsm.servicios.shared.domain.AuthenticatedUser;

public class ListEvidences {

	private final RequestAccess requestAccess;
	private final EvidenceRepository evidenceRepository;

	public ListEvidences(RequestAccess requestAccess, EvidenceRepository evidenceRepository) {
		this.requestAccess = requestAccess;
		this.evidenceRepository = evidenceRepository;
	}

	public List<Evidence> execute(AuthenticatedUser user, String code) {
		RequestSnapshot request = requestAccess.findVisibleOrThrow(user, code);
		return evidenceRepository.findByRequestId(request.id());
	}

}
