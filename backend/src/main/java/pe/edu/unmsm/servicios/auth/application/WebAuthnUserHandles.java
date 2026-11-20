package pe.edu.unmsm.servicios.auth.application;

import java.util.Optional;

/** The opaque WebAuthn user handle of each user (64 random bytes), never the id nor the email. */
public interface WebAuthnUserHandles {

	Optional<byte[]> find(long userId);

	/** The user's handle, created and stored the first time. */
	byte[] getOrCreate(long userId);

}
