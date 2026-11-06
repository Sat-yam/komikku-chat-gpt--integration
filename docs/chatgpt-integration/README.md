# Komikku ↔ ChatGPT integration

Status: **Milestone 1 — native read facade and access-policy foundation**

This is an opt-in integration for a user-authorized Komikku installation. The user selected:
- Custom Komikku build
- OnePlus 11R as the primary host
- Controlled access: reads and routine user-requested writes are allowed; removals, downloads and bulk destructive actions require an approval inside Komikku.

## Baseline

The fork was created from Komikku's upstream default branch. At the fork point, `app/build.gradle.kts` identified versionName `1.14.1` and versionCode `82`. The implementation uses the fork's current baseline rather than assuming the version installed on the phone is identical.

Repository conventions from `AGENTS.md` apply:
- JDK 17 / Kotlin
- Komikku-only code must be bounded by `// KMK -->` and `// KMK <--`
- run `./gradlew spotlessApply`, `./gradlew spotlessCheck`, then `./gradlew assembleDebug`

## Architecture decisions

1. **Use domain interactors.** The integration must use the same domain layer as Komikku UI. Never read or mutate SQLDelight tables directly.
2. **No public inbound Android listener.** The phone should initiate an authenticated outbound TLS connection to a separately deployed relay. Do not use port forwarding or expose a LAN HTTP server to the internet.
3. **Remote MCP adapter.** A remote MCP server will authenticate ChatGPT and dispatch narrowly scoped operations through the relay. The Android integration module remains the authority for whether a command is allowed.
4. **Explicit foreground mode.** An always-live phone connection must be user-enabled and visible through Android's foreground-service notification. WorkManager can provide deferred sync but cannot promise instantaneous command execution.
5. **Local approval for sensitive work.** Manga removal, category deletion, chapter downloads and bulk destructive actions must produce a phone-side approval prompt bound to a single expiring request.
6. **Data minimisation.** Do not expose tracker credentials, extension cookies, local filesystem paths, downloaded page contents or raw database handles.

## Current implementation

- `ChatGptLibraryService` reads the library and chapter metadata through `GetLibraryManga`, `GetCategories` and `GetChaptersByMangaId`.
- `ChatGptIntegrationPolicy` enforces enabled + paired state, explicit user intent for writes, and phone confirmation for sensitive operations.
- The Settings menu now contains a ChatGPT integration screen with a persisted local enable/disable preference and an explicit unpaired/not-yet-available status.
- The device identity manager creates a P-256 ECDSA signing key in Android Keystore, derives a stable public-key fingerprint/device ID, signs bounded relay challenges, and supports local key removal.
- The settings screen can create the key, display/copy its fingerprint, copy its public key, and remove the local key after confirmation.
- No network endpoint, pairing screen, relay client or remote MCP server is enabled yet. The service is not exposed outside the app.

## Milestones

### M1 — Foundation (this commit)
- [x] Feature branch created from fork baseline.
- [x] Read-only domain facade for library and chapter metadata.
- [x] Central controlled-access policy and unit tests.
- [ ] Build and test on Android CI / local Android SDK.

### M2 — Native integration controls
- [x] Settings screen: local enable/disable preference and honest unpaired status.
- [ ] Device pairing, connected state and disconnect.
- [x] Device key generation and local key removal using Android Keystore.
- [ ] Remote pairing and relay-side revocation.
- [ ] Audit history and phone-side confirmation queue.

### M3 — Write operations
- [ ] Add an existing source manga to the library through Komikku's supported source/domain flows.
- [ ] Set reading progress/read state without triggering automatic download deletion.
- [ ] Manage favourites and categories.
- [ ] Implement confirmation-gated removal and downloads.

### M4 — Transport
- [ ] Authenticated outbound TLS/WebSocket relay client.
- [ ] Reconnect/backoff and deferred command queue.
- [ ] Request IDs, expiry, replay protection and idempotency.

### M5 — Remote MCP
- [ ] Expose read and write tools using the relay.
- [ ] OAuth/device pairing and scoped authorization.
- [ ] End-to-end tests with the phone online/offline.

## Build

From the repository root:

```bash
./gradlew spotlessApply
./gradlew spotlessCheck
./gradlew test
./gradlew assembleDebug
```

A successful source commit is not equivalent to an installable APK. Do not install over the production Komikku package until the custom application ID, signing key, backup and restore path are verified.
