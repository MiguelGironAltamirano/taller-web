package pe.edu.unmsm.servicios.collaboration.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileTypeValidatorTest {

	@Test
	void detectJpegFromMagicBytes() {
		byte[] jpegHeader = new byte[] { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10, 0x4A, 0x46 };
		FileTypeValidator.Detection detection = FileTypeValidator.detect(jpegHeader);
		assertThat(detection.kind()).isEqualTo(EvidenceKind.IMG);
		assertThat(detection.mimeType()).isEqualTo("image/jpeg");
	}

	@Test
	void detectPngFromMagicBytes() {
		byte[] pngHeader = new byte[] { (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A };
		FileTypeValidator.Detection detection = FileTypeValidator.detect(pngHeader);
		assertThat(detection.kind()).isEqualTo(EvidenceKind.IMG);
		assertThat(detection.mimeType()).isEqualTo("image/png");
	}

	@Test
	void detectPdfFromMagicBytes() {
		byte[] pdfHeader = new byte[] { 0x25, 0x50, 0x44, 0x46, 0x2D, 0x31, 0x2E, 0x34 }; // %PDF-1.4
		FileTypeValidator.Detection detection = FileTypeValidator.detect(pdfHeader);
		assertThat(detection.kind()).isEqualTo(EvidenceKind.PDF);
		assertThat(detection.mimeType()).isEqualTo("application/pdf");
	}

	@Test
	void rejectExeHeaderEvenIfCalledPng() {
		byte[] exeHeader = new byte[] { 0x4D, 0x5A, (byte) 0x90, 0x00, 0x03, 0x00, 0x00, 0x00 }; // MZ
		assertThatThrownBy(() -> FileTypeValidator.detect(exeHeader))
				.isInstanceOf(FileTypeNotAllowedException.class)
				.hasMessageContaining("Solo se permiten archivos JPG, PNG o PDF");
	}

	@Test
	void rejectHeaderShorterThan4Bytes() {
		byte[] shortHeader = new byte[] { 0x01, 0x02 };
		assertThatThrownBy(() -> FileTypeValidator.detect(shortHeader))
				.isInstanceOf(FileTypeNotAllowedException.class);
	}

}
