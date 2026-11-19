/**
 * Puente con la API WebAuthn del navegador (passkeys). El servidor envía las opciones con binarios en base64url
 * (forma JSON del estándar) y espera la respuesta igual; aquí se convierten a `ArrayBuffer` y de vuelta, sin depender
 * de `PublicKeyCredential.parseCreationOptionsFromJSON`, que no todos los navegadores tienen. La huella nunca sale del
 * dispositivo: el navegador solo entrega la firma.
 */

type Descriptor = { type: string; id: string; transports?: string[] }

export type CreationOptionsJSON = {
  challenge: string
  rp: { id: string; name: string }
  user: { id: string; name: string; displayName: string }
  pubKeyCredParams: { type: string; alg: number }[]
  timeout?: number
  excludeCredentials?: Descriptor[]
  authenticatorSelection?: { residentKey?: string; requireResidentKey?: boolean; userVerification?: string }
  attestation?: string
}

export type RequestOptionsJSON = {
  challenge: string
  rpId?: string
  timeout?: number
  userVerification?: string
  allowCredentials?: Descriptor[]
}

/** El navegador admite passkeys (todos los modernos; falta en contextos no seguros, p. ej. HTTP fuera de localhost). */
export function isPasskeySupported(): boolean {
  return typeof window !== 'undefined' && typeof window.PublicKeyCredential === 'function' && !!navigator.credentials
}

/** El usuario cerró o canceló el diálogo del dispositivo (o se agotó el tiempo): no es un error del sistema. */
export function isPasskeyCancelled(error: unknown): boolean {
  return error instanceof DOMException && (error.name === 'NotAllowedError' || error.name === 'AbortError')
}

const toBuffer = (base64url: string): ArrayBuffer => {
  const base64 = base64url.replace(/-/g, '+').replace(/_/g, '/')
  const binary = atob(base64.padEnd(base64.length + ((4 - (base64.length % 4)) % 4), '='))
  const bytes = new Uint8Array(binary.length)
  for (let i = 0; i < binary.length; i++) bytes[i] = binary.charCodeAt(i)
  return bytes.buffer
}

const toBase64url = (buffer: ArrayBuffer): string => {
  let binary = ''
  for (const byte of new Uint8Array(buffer)) binary += String.fromCharCode(byte)
  return btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '')
}

const descriptors = (list: Descriptor[] = []): PublicKeyCredentialDescriptor[] =>
  list.map((d) => ({ type: 'public-key', id: toBuffer(d.id), transports: d.transports as AuthenticatorTransport[] }))

/** Crea una passkey con las opciones del servidor y devuelve el `RegistrationResponseJSON`. */
export async function createPasskey(options: CreationOptionsJSON): Promise<unknown> {
  const credential = (await navigator.credentials.create({
    publicKey: {
      ...options,
      challenge: toBuffer(options.challenge),
      user: { ...options.user, id: toBuffer(options.user.id) },
      pubKeyCredParams: options.pubKeyCredParams as PublicKeyCredentialParameters[],
      excludeCredentials: descriptors(options.excludeCredentials),
      authenticatorSelection: options.authenticatorSelection as AuthenticatorSelectionCriteria,
      attestation: options.attestation as AttestationConveyancePreference,
    },
  })) as PublicKeyCredential
  const response = credential.response as AuthenticatorAttestationResponse
  return {
    id: credential.id,
    rawId: toBase64url(credential.rawId),
    type: credential.type,
    authenticatorAttachment: credential.authenticatorAttachment ?? undefined,
    response: {
      clientDataJSON: toBase64url(response.clientDataJSON),
      attestationObject: toBase64url(response.attestationObject),
      transports: typeof response.getTransports === 'function' ? response.getTransports() : [],
    },
    clientExtensionResults: credential.getClientExtensionResults(),
  }
}

/** Pide al dispositivo que firme el desafío de inicio de sesión; devuelve el `AuthenticationResponseJSON`. */
export async function getPasskey(options: RequestOptionsJSON): Promise<unknown> {
  const credential = (await navigator.credentials.get({
    publicKey: {
      ...options,
      challenge: toBuffer(options.challenge),
      allowCredentials: descriptors(options.allowCredentials),
      userVerification: options.userVerification as UserVerificationRequirement,
    },
  })) as PublicKeyCredential
  const response = credential.response as AuthenticatorAssertionResponse
  return {
    id: credential.id,
    rawId: toBase64url(credential.rawId),
    type: credential.type,
    authenticatorAttachment: credential.authenticatorAttachment ?? undefined,
    response: {
      clientDataJSON: toBase64url(response.clientDataJSON),
      authenticatorData: toBase64url(response.authenticatorData),
      signature: toBase64url(response.signature),
      userHandle: response.userHandle ? toBase64url(response.userHandle) : undefined,
    },
    clientExtensionResults: credential.getClientExtensionResults(),
  }
}
