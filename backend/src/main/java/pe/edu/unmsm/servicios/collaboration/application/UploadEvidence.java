package pe.edu.unmsm.servicios.collaboration.application;

import java.io.InputStream;
import java.time.Instant;
import java.util.UUID;

import org.springframework.transaction.annotation.Transactional;

import pe.edu.unmsm.servicios.collaboration.domain.Evidence;
import pe.edu.unmsm.servicios.collaboration.domain.EvidenceRules;
import pe.edu.unmsm.servicios.collaboration.domain.FileTypeValidator;
import pe.edu.unmsm.servicios.shared.domain.AuthenticatedUser;

public class UploadEvidence {

	private final RequestAccess requestAccess;
	private final EvidenceRepository evidenceRepository;
	private final EvidenceStorage evidenceStorage;

	public UploadEvidence(RequestAccess requestAccess, EvidenceRepository evidenceRepository,
			EvidenceStorage evidenceStorage) {
		this.requestAccess = requestAccess;
		this.evidenceRepository = evidenceRepository;
		this.evidenceStorage = evidenceStorage;
	}

	@Transactional
	public Evidence execute(AuthenticatedUser user, String code, String originalFileName, long sizeBytes,
			InputStream inputStream) {
		RequestSnapshot request = requestAccess.findVisibleOrThrow(user, code);
		long currentCount = evidenceRepository.countByRequestId(request.id());
		EvidenceRules.validate(currentCount, sizeBytes);

		byte[] content;
		try {
			content = inputStream.readAllBytes();
		}
		catch (Exception e) {
			throw new RuntimeException("Error al leer el archivo de evidencia", e);
		}

		FileTypeValidator.Detection detection = FileTypeValidator.detect(content);
		String storedName = UUID.randomUUID().toString();

		evidenceStorage.store(storedName, content);

		Evidence evidence = new Evidence(null, request.id(), user.id(), originalFileName, storedName,
				detection.kind(), sizeBytes, Instant.now());
		return evidenceRepository.save(evidence);
	}

}
