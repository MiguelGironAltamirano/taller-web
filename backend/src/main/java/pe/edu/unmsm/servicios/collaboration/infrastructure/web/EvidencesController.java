package pe.edu.unmsm.servicios.collaboration.infrastructure.web;

import java.io.IOException;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import pe.edu.unmsm.servicios.collaboration.application.DownloadEvidence;
import pe.edu.unmsm.servicios.collaboration.application.DownloadedEvidence;
import pe.edu.unmsm.servicios.collaboration.application.ListEvidences;
import pe.edu.unmsm.servicios.collaboration.application.UploadEvidence;
import pe.edu.unmsm.servicios.collaboration.domain.Evidence;
import pe.edu.unmsm.servicios.collaboration.infrastructure.web.CollaborationDtos.EvidenceResponse;
import pe.edu.unmsm.servicios.shared.domain.AuthenticatedUser;
import pe.edu.unmsm.servicios.shared.infrastructure.security.CurrentUser;

@RestController
@RequestMapping("/api/v1/requests/{code}/evidences")
class EvidencesController {

	private final UploadEvidence uploadEvidence;
	private final ListEvidences listEvidences;
	private final DownloadEvidence downloadEvidence;

	EvidencesController(UploadEvidence uploadEvidence, ListEvidences listEvidences, DownloadEvidence downloadEvidence) {
		this.uploadEvidence = uploadEvidence;
		this.listEvidences = listEvidences;
		this.downloadEvidence = downloadEvidence;
	}

	@GetMapping
	List<EvidenceResponse> list(@PathVariable String code, @AuthenticationPrincipal Jwt jwt) {
		AuthenticatedUser user = CurrentUser.from(jwt);
		return listEvidences.execute(user, code).stream().map(EvidenceResponse::from).toList();
	}

	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	ResponseEntity<EvidenceResponse> upload(
			@PathVariable String code,
			@RequestParam("file") MultipartFile file,
			@AuthenticationPrincipal Jwt jwt) throws IOException {
		AuthenticatedUser user = CurrentUser.from(jwt);
		String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "archivo";
		Evidence evidence = uploadEvidence.execute(user, code, originalFilename, file.getSize(), file.getInputStream());
		return ResponseEntity.status(HttpStatus.CREATED).body(EvidenceResponse.from(evidence));
	}

	@GetMapping("/{id}")
	ResponseEntity<byte[]> download(
			@PathVariable String code,
			@PathVariable long id,
			@AuthenticationPrincipal Jwt jwt) {
		AuthenticatedUser user = CurrentUser.from(jwt);
		DownloadedEvidence downloaded = downloadEvidence.execute(user, code, id);
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_TYPE, downloaded.contentType())
				.header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + downloaded.fileName() + "\"")
				.header("X-Content-Type-Options", "nosniff")
				.body(downloaded.content());
	}

}
