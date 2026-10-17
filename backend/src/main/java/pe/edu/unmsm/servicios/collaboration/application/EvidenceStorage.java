package pe.edu.unmsm.servicios.collaboration.application;

public interface EvidenceStorage {

	void store(String storedName, byte[] content);

	byte[] load(String storedName);

	void delete(String storedName);

}
