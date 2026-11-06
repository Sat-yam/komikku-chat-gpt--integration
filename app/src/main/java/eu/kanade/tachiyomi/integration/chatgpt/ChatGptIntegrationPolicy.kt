package eu.kanade.tachiyomi.integration.chatgpt

// KMK -->
/**
 * Operations exposed to the optional ChatGPT integration.
 *
 * Mutating operations require an explicit user request. Destructive or
 * resource-intensive operations additionally require confirmation in Komikku.
 */
enum class ChatGptOperation(
    val isWrite: Boolean = false,
    val requiresDeviceConfirmation: Boolean = false,
) {
    READ_LIBRARY,
    READ_CHAPTERS,
    SEARCH_SOURCES,
    ADD_TO_LIBRARY(isWrite = true),
    UPDATE_READING_PROGRESS(isWrite = true),
    MANAGE_FAVOURITES(isWrite = true),
    MANAGE_CATEGORIES(isWrite = true),
    REMOVE_FROM_LIBRARY(isWrite = true, requiresDeviceConfirmation = true),
    DELETE_CATEGORY(isWrite = true, requiresDeviceConfirmation = true),
    DOWNLOAD_CHAPTERS(isWrite = true, requiresDeviceConfirmation = true),
    BULK_REMOVE(isWrite = true, requiresDeviceConfirmation = true),
    BULK_DOWNLOAD(isWrite = true, requiresDeviceConfirmation = true),
}

data class ChatGptIntegrationState(
    val enabled: Boolean,
    val paired: Boolean,
)

enum class ChatGptDenialReason {
    INTEGRATION_DISABLED,
    DEVICE_NOT_PAIRED,
    USER_INTENT_REQUIRED,
    DEVICE_CONFIRMATION_REQUIRED,
}

sealed interface ChatGptAccessDecision {
    data object Granted : ChatGptAccessDecision
    data class Denied(val reason: ChatGptDenialReason) : ChatGptAccessDecision
}

/**
 * Central policy for the controlled-access profile selected by the user.
 *
 * The caller must derive [requestWasUserInitiated] from an authenticated,
 * user-originated tool request, never from an arbitrary payload field.
 * [deviceConfirmationGranted] must only be true after a local Komikku UI
 * confirmation for the specific pending operation.
 */
object ChatGptIntegrationPolicy {

    fun evaluate(
        operation: ChatGptOperation,
        state: ChatGptIntegrationState,
        requestWasUserInitiated: Boolean,
        deviceConfirmationGranted: Boolean,
    ): ChatGptAccessDecision {
        if (!state.enabled) {
            return ChatGptAccessDecision.Denied(ChatGptDenialReason.INTEGRATION_DISABLED)
        }

        if (!state.paired) {
            return ChatGptAccessDecision.Denied(ChatGptDenialReason.DEVICE_NOT_PAIRED)
        }

        if (operation.isWrite && !requestWasUserInitiated) {
            return ChatGptAccessDecision.Denied(ChatGptDenialReason.USER_INTENT_REQUIRED)
        }

        if (operation.requiresDeviceConfirmation && !deviceConfirmationGranted) {
            return ChatGptAccessDecision.Denied(ChatGptDenialReason.DEVICE_CONFIRMATION_REQUIRED)
        }

        return ChatGptAccessDecision.Granted
    }
}
// KMK <--
