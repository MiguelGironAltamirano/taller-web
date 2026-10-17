package pe.edu.unmsm.servicios.collaboration.infrastructure.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import pe.edu.unmsm.servicios.collaboration.application.EvidenceNotFoundException;
import pe.edu.unmsm.servicios.collaboration.application.EvidenceStorage;

@Component
public class DiskEvidenceStorage implements EvidenceStorage {

	private final Path storageDir;

	public DiskEvidenceStorage(@Value("${app.evidences.storage-path:./evidences}") String storagePath) {
		this.storageDir = Paths.get(storagePath).toAbsolutePath().normalize();
		try {
			Files.createDirectories(this.storageDir);
		}
		catch (IOException e) {
			throw new RuntimeException("Could not create storage directory: " + storagePath, e);
		}
	}

	@Override
	public void store(String storedName, byte[] content) {
		Path target = resolvePath(storedName);
		try {
			Files.write(target, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
		}
		catch (IOException e) {
			throw new RuntimeException("Error storing evidence: " + storedName, e);
		}
	}

	@Override
	public byte[] load(String storedName) {
		Path target = resolvePath(storedName);
		if (!Files.exists(target)) {
			throw new EvidenceNotFoundException("Evidence file not found on disk");
		}
		try {
			return Files.readAllBytes(target);
		}
		catch (IOException e) {
			throw new RuntimeException("Error reading evidence: " + storedName, e);
		}
	}

	@Override
	public void delete(String storedName) {
		Path target = resolvePath(storedName);
		try {
			Files.deleteIfExists(target);
		}
		catch (IOException ignored) {
		}
	}

	private Path resolvePath(String storedName) {
		Path target = storageDir.resolve(storedName).normalize();
		if (!target.startsWith(storageDir)) {
			throw new SecurityException("Path traversal attempt detected");
		}
		return target;
	}

}
