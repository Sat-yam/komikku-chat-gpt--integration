package eu.kanade.tachiyomi.integration.chatgpt

// KMK -->
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test

class ChatGptIntegrationPolicyTest {

    private val enabledAndPaired = ChatGptIntegrationState(enabled = true, paired = true)

    @Test
    fun disabledIntegrationDeniesEveryOperation() {
        val result = ChatGptIntegrationPolicy.evaluate(
            operation = ChatGptOperation.READ_LIBRARY,
            state = enabledAndPaired.copy(enabled = false),
            requestWasUserInitiated = false,
            deviceConfirmationGranted = false,
        )

        assertEquals(
            ChatGptAccessDecision.Denied(ChatGptDenialReason.INTEGRATION_DISABLED),
            result,
        )
    }

    @Test
    fun unpairedDeviceCannotBeAccessed() {
        val result = ChatGptIntegrationPolicy.evaluate(
            operation = ChatGptOperation.READ_LIBRARY,
            state = enabledAndPaired.copy(paired = false),
            requestWasUserInitiated = false,
            deviceConfirmationGranted = false,
        )

        assertEquals(
            ChatGptAccessDecision.Denied(ChatGptDenialReason.DEVICE_NOT_PAIRED),
            result,
        )
    }

    @Test
    fun readOperationsDoNotNeedPerRequestConfirmation() {
        val result = ChatGptIntegrationPolicy.evaluate(
            operation = ChatGptOperation.READ_LIBRARY,
            state = enabledAndPaired,
            requestWasUserInitiated = false,
            deviceConfirmationGranted = false,
        )

        assertInstanceOf(ChatGptAccessDecision.Granted::class.java, result)
    }

    @Test
    fun routineWritesRequireAnExplicitUserRequest() {
        val result = ChatGptIntegrationPolicy.evaluate(
            operation = ChatGptOperation.ADD_TO_LIBRARY,
            state = enabledAndPaired,
            requestWasUserInitiated = false,
            deviceConfirmationGranted = false,
        )

        assertEquals(
            ChatGptAccessDecision.Denied(ChatGptDenialReason.USER_INTENT_REQUIRED),
            result,
        )
    }

    @Test
    fun routineWritesAreAllowedAfterAnExplicitUserRequest() {
        val result = ChatGptIntegrationPolicy.evaluate(
            operation = ChatGptOperation.UPDATE_READING_PROGRESS,
            state = enabledAndPaired,
            requestWasUserInitiated = true,
            deviceConfirmationGranted = false,
        )

        assertInstanceOf(ChatGptAccessDecision.Granted::class.java, result)
    }

    @Test
    fun destructiveOperationsRequireConfirmationOnThePhone() {
        val result = ChatGptIntegrationPolicy.evaluate(
            operation = ChatGptOperation.REMOVE_FROM_LIBRARY,
            state = enabledAndPaired,
            requestWasUserInitiated = true,
            deviceConfirmationGranted = false,
        )

        assertEquals(
            ChatGptAccessDecision.Denied(ChatGptDenialReason.DEVICE_CONFIRMATION_REQUIRED),
            result,
        )
    }

    @Test
    fun destructiveOperationsProceedAfterPhoneConfirmation() {
        val result = ChatGptIntegrationPolicy.evaluate(
            operation = ChatGptOperation.DOWNLOAD_CHAPTERS,
            state = enabledAndPaired,
            requestWasUserInitiated = true,
            deviceConfirmationGranted = true,
        )

        assertInstanceOf(ChatGptAccessDecision.Granted::class.java, result)
    }
}
// KMK <--
