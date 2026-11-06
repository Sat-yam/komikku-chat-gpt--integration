package eu.kanade.domain.chatgpt.service

import java.security.SecureRandom
import java.util.Base64
import java.util.UUID

// KMK -->
/**
 * A short-lived, public pairing request. This is a discovery payload, not proof
 * of authentication. The relay must issue its own challenge and verify a
 * signature made by the device's Android Keystore private key.
 */
data class ChatGptPairingRequest(
    val requestId: String,
    val requestCode: String,
    val deviceId: String,
    val fingerprint: String,
    val payloadJson: String,
    val createdAtEpochSeconds: Long,
    val expiresAtEpochSeconds: Long,
) {
    fun isExpired(nowEpochSeconds: Long = System.currentTimeMillis() / 1_000L): Boolean {
        return nowEpochSeconds >= expiresAtEpochSeconds
    }

    fun remainingSeconds(nowEpochSeconds: Long = System.currentTimeMillis() / 1_000L): Long {
        return (expiresAtEpochSeconds - nowEpochSeconds).coerceAtLeast(0L)
    }
}

/**
 * Builds a QR payload for the future relay pairing endpoint.
 *
 * The payload contains only public key material and a random nonce. The short
 * request code is a lookup reference only; it must never be treated as a
 * bearer credential by the relay.
 */
class ChatGptPairingRequestFactory(
    private val secureRandom: SecureRandom = SecureRandom(),
) {
    fun create(
        identity: ChatGptDeviceIdentityInfo,
        nowEpochSeconds: Long = System.currentTimeMillis() / 1_000L,
    ): ChatGptPairingRequest {
        require(identity.deviceId.isNotBlank()) { "Device ID is required" }
        require(identity.publicKeyBase64.isNotBlank()) { "Public key is required" }
        require(identity.fingerprint.isNotBlank()) { "Device fingerprint is required" }

        val nonce = ByteArray(NONCE_BYTES).also(secureRandom::nextBytes)
        val requestId = UUID.randomUUID().toString()
        val requestCode = nonce
            .take(REQUEST_CODE_BYTES)
            .joinToString("") { byte -> "%02X".format(byte.toInt() and 0xFF) }
            .chunked(REQUEST_CODE_GROUP_SIZE)
            .joinToString("-")
        val expiresAt = nowEpochSeconds + REQUEST_TTL_SECONDS
        val nonceBase64 = Base64.getUrlEncoder().withoutPadding().encodeToString(nonce)

        val payload = buildString {
            append("{")
            append("\"type\":\"komikku_chatgpt_pairing_request\",")
            append("\"version\":1,")
            append("\"request_id\":\"$requestId\",")
            append("\"request_code\":\"$requestCode\",")
            append("\"device_id\":\"" + identity.deviceId + "\",")
            append("\"public_key\":\"" + identity.publicKeyBase64 + "\",")
            append("\"key_algorithm\":\"" + identity.keyAlgorithm + "\",")
            append("\"signature_algorithm\":\"" + identity.signatureAlgorithm + "\",")
            append("\"fingerprint\":\"" + identity.fingerprint + "\",")
            append("\"nonce\":\"$nonceBase64\",")
            append("\"issued_at\":$nowEpochSeconds,")
            append("\"expires_at\":$expiresAt")
            append("}")
        }

        return ChatGptPairingRequest(
            requestId = requestId,
            requestCode = requestCode,
            deviceId = identity.deviceId,
            fingerprint = identity.fingerprint,
            payloadJson = payload,
            createdAtEpochSeconds = nowEpochSeconds,
            expiresAtEpochSeconds = expiresAt,
        )
    }

    companion object {
        const val REQUEST_TTL_SECONDS = 600L
        private const val NONCE_BYTES = 32
        private const val REQUEST_CODE_BYTES = 6
        private const val REQUEST_CODE_GROUP_SIZE = 4
    }
}
// KMK <--
