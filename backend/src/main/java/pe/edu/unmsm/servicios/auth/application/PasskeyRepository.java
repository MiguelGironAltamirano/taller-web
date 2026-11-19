package pe.edu.unmsm.servicios.auth.application;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import pe.edu.unmsm.servicios.auth.domain.Passkey;

public interface PasskeyRepository {

	/** Persists a new passkey; throws PasskeyAlreadyRegisteredException when the credential id is taken. */
	Passkey save(Passkey passkey);

	Optional<Passkey> findByCredentialId(byte[] credentialId);

	/** The user's passkeys, most recent first. */
	List<Passkey> findByUser(long userId);

	int countByUser(long userId);

	/** Renames a passkey of the user; empty when it does not exist or belongs to someone else. */
	Optional<Passkey> rename(long id, long userId, String name);

	/** Deletes a passkey of the user; false when it does not exist or belongs to someone else. */
	boolean delete(long id, long userId);

	/** Records a successful login with the new signature counter and backup state. */
	void recordUse(long id, long signCount, boolean backedUp, Instant usedAt);

}
