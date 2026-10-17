package pe.edu.unmsm.servicios.collaboration.domain;

public final class FileTypeValidator {

	private FileTypeValidator() {
	}

	public record Detection(EvidenceKind kind, String mimeType) {
	}

	public static Detection detect(byte[] header) {
		if (header == null || header.length < 4) {
			throw new FileTypeNotAllowedException("Solo se permiten archivos JPG, PNG o PDF.");
		}

		// JPEG: FF D8 FF
		if ((header[0] & 0xFF) == 0xFF && (header[1] & 0xFF) == 0xD8 && (header[2] & 0xFF) == 0xFF) {
			return new Detection(EvidenceKind.IMG, "image/jpeg");
		}

		// PNG: 89 50 4E 47 0D 0A 1A 0A
		if (header.length >= 8
				&& (header[0] & 0xFF) == 0x89
				&& header[1] == 0x50
				&& header[2] == 0x4E
				&& header[3] == 0x47
				&& header[4] == 0x0D
				&& header[5] == 0x0A
				&& header[6] == 0x1A
				&& header[7] == 0x0A) {
			return new Detection(EvidenceKind.IMG, "image/png");
		}

		// PDF: %PDF (25 50 44 46)
		if (header[0] == 0x25 && header[1] == 0x50 && header[2] == 0x44 && header[3] == 0x46) {
			return new Detection(EvidenceKind.PDF, "application/pdf");
		}

		throw new FileTypeNotAllowedException("Solo se permiten archivos JPG, PNG o PDF.");
	}

}
