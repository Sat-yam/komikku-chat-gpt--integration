package eu.kanade.tachiyomi.integration.chatgpt

import eu.kanade.domain.chatgpt.service.ChatGptDeviceIdentity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

// KMK -->
class ChatGptDeviceIdentityTest {

    @Test
    fun publicIdentityIsDeterministicForSameKey() {
        val publicKey = ByteArray(91) { index -> index.toByte() }

        val first = ChatGptDeviceIdentity.createIdentityInfo(publicKey)
        val second = ChatGptDeviceIdentity.createIdentityInfo(publicKey)

        assertEquals(first.deviceId, second.deviceId)
        assertEquals(first.fingerprint, second.fingerprint)
        assertEquals(first.publicKeyBase64, second.publicKeyBase64)
    }

    @Test
    fun differentKeysProduceDifferentDeviceIds() {
        val first = ChatGptDeviceIdentity.createIdentityInfo(ByteArray(91) { 1 })
        val second = ChatGptDeviceIdentity.createIdentityInfo(ByteArray(91) { 2 })

        org.junit.jupiter.api.Assertions.assertNotEquals(first.deviceId, second.deviceId)
        org.junit.jupiter.api.Assertions.assertNotEquals(first.fingerprint, second.fingerprint)
    }

    @Test
    fun pairingChallengeAcceptsExpectedSize() {
        ChatGptDeviceIdentity.validatePairingChallenge(ByteArray(32))
        ChatGptDeviceIdentity.validatePairingChallenge(ByteArray(512))
    }

    @Test
    fun pairingChallengeRejectsTooShortOrTooLongValues() {
        assertThrows(IllegalArgumentException::class.java) {
            ChatGptDeviceIdentity.validatePairingChallenge(ByteArray(31))
        }
        assertThrows(IllegalArgumentException::class.java) {
            ChatGptDeviceIdentity.validatePairingChallenge(ByteArray(513))
        }
    }
}
// KMK <--
