package pe.edu.unmsm.servicios.collaboration.domain;

public final class EvidenceRules {

	public static final int MAX_EVIDENCES_PER_REQUEST = 5;
	public static final long MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024; // 5 MB

	private EvidenceRules() {
	}

	public static void validate(long currentCount, long sizeBytes) {
		if (currentCount >= MAX_EVIDENCES_PER_REQUEST) {
			throw new EvidenceLimitExceededException("Se ha alcanzado el límite de 5 evidencias para esta solicitud.");
		}
		if (sizeBytes <= 0) {
			throw new EmptyFileException("El archivo no puede estar vacío.");
		}
		if (sizeBytes > MAX_FILE_SIZE_BYTES) {
			throw new EvidenceSizeLimitExceededException("El archivo supera el tamaño máximo permitido de 5 MB.");
		}
	}

}
