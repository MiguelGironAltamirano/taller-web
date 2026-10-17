package pe.edu.unmsm.servicios.collaboration.application;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import pe.edu.unmsm.servicios.collaboration.domain.Evidence;
import pe.edu.unmsm.servicios.collaboration.domain.EvidenceKind;
import pe.edu.unmsm.servicios.collaboration.domain.EvidenceLimitExceededException;
import pe.edu.unmsm.servicios.requests.domain.RequestNotFoundException;
import pe.edu.unmsm.servicios.shared.domain.AuthenticatedUser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EvidenceUseCasesTest {

	private FakeRequestAccess requestAccess;
	private InMemoryEvidenceRepository evidenceRepository;
	private InMemoryEvidenceStorage evidenceStorage;
	private UploadEvidence uploadEvidence;
	private ListEvidences listEvidences;
	private DownloadEvidence downloadEvidence;

	private final AuthenticatedUser requester = new AuthenticatedUser(100L, "req@unmsm.edu.pe", "Ana Solicitante",
			"SOLICITANTE", null);
	private final AuthenticatedUser stranger = new AuthenticatedUser(999L, "stranger@unmsm.edu.pe", "Extraño",
			"SOLICITANTE", null);

	private static final byte[] VALID_PNG_BYTES = new byte[] {
			(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00, 0x00, 0x0D
	};

	@BeforeEach
	void setUp() {
		requestAccess = new FakeRequestAccess();
		evidenceRepository = new InMemoryEvidenceRepository();
		evidenceStorage = new InMemoryEvidenceStorage();
		uploadEvidence = new UploadEvidence(requestAccess, evidenceRepository, evidenceStorage);
		listEvidences = new ListEvidences(requestAccess, evidenceRepository);
		downloadEvidence = new DownloadEvidence(requestAccess, evidenceRepository, evidenceStorage);

		requestAccess.requests.add(new RequestSnapshot(1L, "SOL-2026-0001", 100L, 5L, 200L));
		requestAccess.requests.add(new RequestSnapshot(2L, "SOL-2026-0002", 100L, 5L, 200L));
	}

	@Test
	void uploadEvidenceSavesFileAndReturnsEvidence() {
		Evidence evidence = uploadEvidence.execute(requester, "SOL-2026-0001", "foto.png",
				VALID_PNG_BYTES.length, new ByteArrayInputStream(VALID_PNG_BYTES));

		assertThat(evidence.id()).isNotNull();
		assertThat(evidence.fileName()).isEqualTo("foto.png");
		assertThat(evidence.kind()).isEqualTo(EvidenceKind.IMG);
		assertThat(evidence.sizeBytes()).isEqualTo(VALID_PNG_BYTES.length);
		assertThat(evidenceStorage.storedFiles).containsKey(evidence.storedName());
	}

	@Test
	void uploadEvidenceExceedingLimitThrowsEvidenceLimitExceededException() {
		for (int i = 0; i < 5; i++) {
			uploadEvidence.execute(requester, "SOL-2026-0001", "foto" + i + ".png",
					VALID_PNG_BYTES.length, new ByteArrayInputStream(VALID_PNG_BYTES));
		}

		assertThatThrownBy(() -> uploadEvidence.execute(requester, "SOL-2026-0001", "foto6.png",
				VALID_PNG_BYTES.length, new ByteArrayInputStream(VALID_PNG_BYTES)))
				.isInstanceOf(EvidenceLimitExceededException.class);
	}

	@Test
	void downloadEvidenceReturnsContentAndMetadata() {
		Evidence uploaded = uploadEvidence.execute(requester, "SOL-2026-0001", "foto.png",
				VALID_PNG_BYTES.length, new ByteArrayInputStream(VALID_PNG_BYTES));

		DownloadedEvidence downloaded = downloadEvidence.execute(requester, "SOL-2026-0001", uploaded.id());

		assertThat(downloaded.fileName()).isEqualTo("foto.png");
		assertThat(downloaded.contentType()).isEqualTo("image/png");
		assertThat(downloaded.content()).isEqualTo(VALID_PNG_BYTES);
	}

	@Test
	void downloadEvidenceFromWrongRequestThrowsEvidenceNotFoundException() {
		Evidence uploaded = uploadEvidence.execute(requester, "SOL-2026-0001", "foto.png",
				VALID_PNG_BYTES.length, new ByteArrayInputStream(VALID_PNG_BYTES));

		// Intenta descargar la evidencia de SOL-2026-0001 usando el código SOL-2026-0002
		assertThatThrownBy(() -> downloadEvidence.execute(requester, "SOL-2026-0002", uploaded.id()))
				.isInstanceOf(EvidenceNotFoundException.class);
	}

	@Test
	void downloadEvidenceByUnauthorizedUserThrowsRequestNotFoundException() {
		Evidence uploaded = uploadEvidence.execute(requester, "SOL-2026-0001", "foto.png",
				VALID_PNG_BYTES.length, new ByteArrayInputStream(VALID_PNG_BYTES));

		assertThatThrownBy(() -> downloadEvidence.execute(stranger, "SOL-2026-0001", uploaded.id()))
				.isInstanceOf(RequestNotFoundException.class);
	}

	private static class FakeRequestAccess implements RequestAccess {
		final List<RequestSnapshot> requests = new ArrayList<>();

		@Override
		public RequestSnapshot findVisibleOrThrow(AuthenticatedUser user, String code) {
			RequestSnapshot found = requests.stream().filter(r -> r.code().equals(code)).findFirst()
					.orElseThrow(() -> new RequestNotFoundException(code));
			boolean visible = user.role().equals("ADMINISTRADOR")
					|| (user.role().equals("SOLICITANTE") && user.id() == found.requesterId())
					|| (user.role().equals("SUPERVISOR") && user.areaId() != null && user.areaId() == found.areaId())
					|| (user.role().equals("TECNICO") && found.technicianId() != null && user.id() == found.technicianId());
			if (!visible) {
				throw new RequestNotFoundException(code);
			}
			return found;
		}

		@Override
		public List<Long> findSupervisorIds(long areaId) {
			return List.of();
		}
	}

	private static class InMemoryEvidenceRepository implements EvidenceRepository {
		final List<Evidence> evidences = new ArrayList<>();
		long seq = 0;

		@Override
		public Evidence save(Evidence evidence) {
			Evidence withId = new Evidence(++seq, evidence.requestId(), evidence.uploaderId(),
					evidence.fileName(), evidence.storedName(), evidence.kind(), evidence.sizeBytes(),
					evidence.createdAt());
			evidences.add(withId);
			return withId;
		}

		@Override
		public List<Evidence> findByRequestId(long requestId) {
			return evidences.stream().filter(e -> e.requestId() == requestId).toList();
		}

		@Override
		public Optional<Evidence> findByIdAndRequestId(long id, long requestId) {
			return evidences.stream().filter(e -> e.id() != null && e.id() == id && e.requestId() == requestId).findFirst();
		}

		@Override
		public long countByRequestId(long requestId) {
			return evidences.stream().filter(e -> e.requestId() == requestId).count();
		}
	}

	private static class InMemoryEvidenceStorage implements EvidenceStorage {
		final Map<String, byte[]> storedFiles = new HashMap<>();

		@Override
		public void store(String storedName, byte[] content) {
			storedFiles.put(storedName, content);
		}

		@Override
		public byte[] load(String storedName) {
			byte[] bytes = storedFiles.get(storedName);
			if (bytes == null) throw new EvidenceNotFoundException("Archivo no encontrado");
			return bytes;
		}

		@Override
		public void delete(String storedName) {
			storedFiles.remove(storedName);
		}
	}

}
