# Security model

## Trust boundaries

- **Komikku app (trusted authority):** owns source access, library state, pairing keys, approval UI and policy evaluation.
- **Phone-to-relay channel:** authenticated outbound TLS connection. The app must not open an unauthenticated public inbound port.
- **Relay / remote MCP:** untrusted command transport. It may route requests but must not be able to manufacture local approval.
- **ChatGPT:** only receives fields explicitly included in tool responses.

## Pairing and identity

Implemented local identity foundation:
1. User enables integration from Komikku settings.
2. App creates a P-256 ECDSA signing key pair in Android Keystore (private key is non-exportable through the app API).
3. App derives a stable device ID and a human-checkable fingerprint from SHA-256 of the encoded public key.
4. A future relay-issued challenge of 32–512 bytes can be signed with the private key using SHA256withECDSA and a protocol-specific domain prefix.

Pairing request preview now implemented:
- The phone creates a QR payload containing protocol version, request ID/reference, device ID, public key, fingerprint, a random 256-bit nonce, issued-at time and a 10-minute expiry.
- Komikku hashes the unsigned payload and signs that digest with the Android Keystore private key. The private key never enters the QR.
- The short request reference is a lookup hint only; it must never be accepted as a bearer credential.

Still planned:
1. A relay endpoint registers and rate-limits the short-lived request.
2. The relay issues a fresh server challenge after request registration.
3. The phone signs that server challenge and submits it over an authenticated outbound channel.
4. The relay verifies the challenge response and obtains explicit user approval before creating a narrowly scoped, revocable pairing.
5. The app displays the authenticated remote client and supports immediate disconnect/revocation.

Never store private keys in ordinary preferences or source code. Never put provider credentials in logs. The current settings screen creates and displays public identity details and a locally generated QR request preview. It does not claim pairing, authenticate a remote client, or expose a network endpoint.

## Request validation

Every command must carry:
- stable request ID
- paired-client identity
- operation name and validated arguments
- issue and expiry time
- signature / authenticated channel context

Reject expired requests, replayed request IDs, oversized lists, unknown operations and mismatched manga/chapter relationships.

## Controlled access profile

Routine writes are permitted only for explicit user-originated requests. Removal, category deletion, downloads and bulk destructive work require a phone-side approval. An approval must be one-time, short-lived, and cryptographically or transactionally bound to the exact operation and arguments.

## Data minimisation

Allowed in read responses: library titles, source identifiers/URLs, chapter names/numbers, read/bookmark state, user category names and optional public cover URLs.

Never expose: source login cookies, tracker OAuth refresh tokens, local storage paths, downloaded page files, app database files, private device identifiers beyond the pairing ID, or arbitrary extension runtime objects.

## Operational safety

- Maintain a local audit log with operation, time, request ID and result; avoid logging sensitive arguments.
- Provide a prominent disable/disconnect control.
- Back up before bulk destructive operations.
- Downloads require confirmation because they consume storage and network data.
- A background worker may deliver queued commands later; never claim immediate execution while the phone is offline or Android has stopped the service.
