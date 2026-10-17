package pe.edu.unmsm.servicios.collaboration.application;

import pe.edu.unmsm.servicios.collaboration.domain.Evidence;
import pe.edu.unmsm.servicios.collaboration.domain.FileTypeValidator;
import pe.edu.unmsm.servicios.shared.domain.AuthenticatedUser;

public class DownloadEvidence {

	private final RequestAccess requestAccess;
	private final EvidenceRepository evidenceRepository;
	private final EvidenceStorage evidenceStorage;

	public DownloadEvidence(RequestAccess requestAccess, EvidenceRepository evidenceRepository,
			EvidenceStorage evidenceStorage) {
		this.requestAccess = requestAccess;
		this.evidenceRepository = evidenceRepository;
		this.evidenceStorage = evidenceStorage;
	}

	public DownloadedEvidence execute(AuthenticatedUser user, String code, long evidenceId) {
		RequestSnapshot request = requestAccess.findVisibleOrThrow(user, code);
		Evidence evidence = evidenceRepository.findByIdAndRequestId(evidenceId, request.id())
				.orElseThrow(() -> new EvidenceNotFoundException("Evidencia no encontrada para la solicitud"));

		byte[] content = evidenceStorage.load(evidence.storedName());
		FileTypeValidator.Detection detection = FileTypeValidator.detect(content);

		return new DownloadedEvidence(evidence.fileName(), detection.mimeType(), content);
	}

}
