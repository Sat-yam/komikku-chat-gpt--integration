package eu.kanade.tachiyomi.integration.chatgpt

import eu.kanade.domain.chatgpt.service.ChatGptDeviceIdentityInfo
import eu.kanade.domain.chatgpt.service.ChatGptPairingRequestFactory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

// KMK -->
class ChatGptPairingRequestFactoryTest {

    private val identity = ChatGptDeviceIdentityInfo(
        deviceId = "device_abcdefghijklmnop",
        fingerprint = "AA:BB:CC:DD:EE:FF",
        publicKeyBase64 = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE",
    )

    private fun newFactory() = ChatGptPairingRequestFactory(
        signChallenge = { challenge -> challenge.copyOf() },
    )

    @Test
    fun requestContainsPublicIdentityAndProtocolMetadata() {
        val request = newFactory().create(identity, nowEpochSeconds = 1_000L)

        assertTrue(request.payloadJson.contains("\"type\":\"komikku_chatgpt_pairing_request\""))
        assertTrue(request.payloadJson.contains("\"device_id\":\"device_abcdefghijklmnop\""))
        assertTrue(request.payloadJson.contains("\"public_key\":\"MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE\""))
        assertTrue(request.payloadJson.contains(request.requestId))
        assertTrue(request.payloadJson.contains(request.requestCode))
        assertTrue(request.payloadJson.contains("\"device_signature\""))
        assertFalse(request.payloadJson.contains("private_key"))
    }

    @Test
    fun deviceSignsTheHashOfTheUnsignedRequestPayload() {
        var signedChallengeSize = 0
        val factory = ChatGptPairingRequestFactory(
            signChallenge = { challenge ->
                signedChallengeSize = challenge.size
                ByteArray(64) { 7 }
            },
        )

        val request = factory.create(identity, nowEpochSeconds = 1_000L)

        assertEquals(32, signedChallengeSize)
        assertTrue(request.payloadJson.contains("\"device_signature\""))
    }

    @Test
    fun requestHasTenMinuteLifetime() {
        val request = newFactory().create(identity, nowEpochSeconds = 1_000L)

        assertEquals(1_000L, request.createdAtEpochSeconds)
        assertEquals(1_000L + ChatGptPairingRequestFactory.REQUEST_TTL_SECONDS, request.expiresAtEpochSeconds)
        assertEquals(600L, request.remainingSeconds(1_000L))
        assertFalse(request.isExpired(1_599L))
        assertTrue(request.isExpired(1_600L))
    }

    @Test
    fun requestCodeHasGroupedTwelveHexCharacters() {
        val request = newFactory().create(identity, nowEpochSeconds = 1_000L)

        assertTrue(request.requestCode.matches(Regex("[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{4}")))
    }

    @Test
    fun requestsUseDifferentNoncesAndReferences() {
        val factory = newFactory()
        val first = factory.create(identity, nowEpochSeconds = 1_000L)
        val second = factory.create(identity, nowEpochSeconds = 1_000L)

        assertFalse(first.requestId == second.requestId)
        assertFalse(first.requestCode == second.requestCode)
        assertFalse(first.payloadJson == second.payloadJson)
    }
}
// KMK <--
