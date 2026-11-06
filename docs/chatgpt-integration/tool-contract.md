# ChatGPT tool contract (draft v1)

This document describes the future remote MCP surface. The tool transport is not implemented yet.

## Read tools

### `get_library`
Returns the user's library entries:
- Komikku manga ID, title, source ID and source URL
- author/artist/description/genres/status/cover URL
- total/read/unread chapter counts
- category IDs and names
- added and last-read timestamps

### `get_chapters`
Input: `manga_id`

Returns chapter metadata for manga currently in the library:
- chapter ID, name and chapter number
- read/bookmark state
- last page read, upload timestamp and scanlator

Do not return page image URLs, local file paths, extension cookies or tracker credentials.

### `search_manga` (planned)
Input should include a query and optional source IDs. Results must carry a stable source ID + manga URL so a later add operation cannot depend on ambiguous titles.

## Routine write tools

All writes require an authenticated, user-originated request:
- `add_to_library`: source ID + source-specific manga URL or a verified Komikku manga ID
- `update_reading_progress`: manga ID + chapter ID(s) + read/unread intent
- `manage_favourites`: manga ID + desired state
- `manage_categories`: create/rename/assign categories; category deletion is a sensitive operation

## Confirmation-gated tools

These require a separate approval inside Komikku, bound to the specific request:
- `remove_from_library`
- `delete_category`
- `download_chapters`
- `bulk_remove`
- `bulk_download`

The remote client cannot set a `confirmed=true` field to bypass the phone approval. Komikku must issue and consume a one-time approval grant after local user interaction.

## Common requirements

- Use stable numeric IDs internally; never use title alone as a write identifier.
- Validate that manga/chapter IDs belong to the expected source/manga.
- Bound page sizes and list sizes.
- Return structured errors for not-found, not-in-library, permission-denied, confirmation-required and source-unavailable cases.
- Make mutating operations idempotent where possible and include a request ID.
- Do not offer bulk deletion or downloads as a hidden side effect of adding manga.
