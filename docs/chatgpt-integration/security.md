# Security model

## Trust boundaries

- **Komikku app (trusted authority):** owns source access, library state, pairing keys, approval UI and policy evaluation.
- **Phone-to-relay channel:** authenticated outbound TLS connection. The app must not open an unauthenticated public inbound port.
- **Relay / remote MCP:** untrusted command transport. It may route requests but must not be able to manufacture local approval.
- **ChatGPT:** only receives fields explicitly included in tool responses.

## Pairing and identity

Planned implementation:
1. User enables integration from Komikku settings.
2. App creates a device key pair in Android Keystore.
3. Pairing begins with a short-lived, one-time code displayed by the app.
4. Remote authorization exchanges the code for a narrowly scoped, revocable device pairing.
5. App displays the connected remote client and supports immediate disconnect/revocation.

Never store private keys in ordinary preferences or source code. Never put provider credentials in logs.

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
