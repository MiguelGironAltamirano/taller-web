package pe.edu.unmsm.servicios.auth.infrastructure.web;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import tools.jackson.databind.JsonNode;

import pe.edu.unmsm.servicios.auth.domain.Passkey;

/** Bodies of docs/api/passkeys-v1.md. {@code credential} is the browser's response JSON, passed on as received. */
final class PasskeyDtos {

	private PasskeyDtos() {
	}

	record PasskeyLoginRequest(
			@NotBlank(message = "Falta el desafío") String challengeId,
			@NotNull(message = "Falta la respuesta de la passkey") JsonNode credential) {
	}

	record RegistrationOptionsRequest(@NotBlank(message = "Ingresa tu contraseña actual") String currentPassword) {
	}

	record RegisterPasskeyRequest(
			@NotBlank(message = "Falta el desafío") String challengeId,
			@Size(max = Passkey.NAME_MAX, message = "El nombre no puede superar los 60 caracteres") String name,
			@NotNull(message = "Falta la respuesta de la passkey") JsonNode credential) {
	}

	record RenamePasskeyRequest(
			@NotBlank(message = "El nombre es obligatorio")
			@Size(max = Passkey.NAME_MAX, message = "El nombre no puede superar los 60 caracteres") String name) {
	}

	/** {@code synced}: the provider backs it up to the cloud, so deleting it here does not remove it elsewhere. */
	record PasskeyResponse(long id, String name, Instant createdAt, Instant lastUsedAt, boolean synced) {

		static PasskeyResponse from(Passkey passkey) {
			return new PasskeyResponse(passkey.id(), passkey.name(), passkey.createdAt(), passkey.lastUsedAt(),
					passkey.backedUp());
		}

	}

}
