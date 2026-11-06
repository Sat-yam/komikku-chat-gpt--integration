package eu.kanade.domain.chatgpt.service

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.nio.charset.StandardCharsets
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import java.util.Base64
import java.util.Locale

// KMK -->
/**
 * Public identity for one Komikku installation.
 *
 * The private key never leaves Android Keystore. The public key and its
 * fingerprint are safe to share with a pairing service.
 */
data class ChatGptDeviceIdentityInfo(
    val deviceId: String,
    val fingerprint: String,
    val publicKeyBase64: String,
    val keyAlgorithm: String = "EC",
    val signatureAlgorithm: String = "SHA256withECDSA",
)

/**
 * Owns the device signing key used by the future ChatGPT relay pairing flow.
 *
 * This class deliberately does not perform networking or claim that a device
 * is paired. A remote relay must issue a fresh challenge and verify a
 * signature with [ChatGptDeviceIdentityInfo.publicKeyBase64] before pairing.
 */
class ChatGptDeviceIdentity {

    fun getExistingIdentity(): ChatGptDeviceIdentityInfo? {
        val keyStore = loadKeyStore()
        if (!keyStore.containsAlias(KEY_ALIAS)) return null

        val publicKey = keyStore.getCertificate(KEY_ALIAS)?.publicKey
            ?: throw IllegalStateException("Device identity certificate is missing")

        return createIdentityInfo(publicKey.encoded)
    }

    /**
     * Creates the P-256 signing key once, or returns the identity already held
     * by Android Keystore. This is safe to call repeatedly.
     */
    fun getOrCreateIdentity(): ChatGptDeviceIdentityInfo {
        val keyStore = loadKeyStore()
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            val generator = KeyPairGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_EC,
                ANDROID_KEY_STORE,
            )
            generator.initialize(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_SIGN,
                )
                    .setAlgorithmParameterSpec(ECGenParameterSpec(EC_CURVE))
                    .setDigests(KeyProperties.DIGEST_SHA256)
                    .setUserAuthenticationRequired(false)
                    .build(),
            )
            generator.generateKeyPair()
        }

        val publicKey = loadKeyStore().getCertificate(KEY_ALIAS)?.publicKey
            ?: throw IllegalStateException("Device identity was not created")

        return createIdentityInfo(publicKey.encoded)
    }

    /**
     * Signs a fresh, server-issued pairing challenge.
     *
     * The fixed protocol prefix prevents the signature from being confused
     * with a signature created for another application protocol.
     */
    fun signPairingChallenge(challenge: ByteArray): ByteArray {
        validatePairingChallenge(challenge)

        val keyStore = loadKeyStore()
        val privateKey = keyStore.getKey(KEY_ALIAS, null) as? PrivateKey
            ?: throw IllegalStateException("Device identity has not been created")

        val signature = Signature.getInstance(SIGNATURE_ALGORITHM)
        signature.initSign(privateKey)
        signature.update(PAIRING_DOMAIN)
        signature.update(challenge)
        return signature.sign()
    }

    /**
     * Removes only this app's local signing key. It does not notify or unpair
     * a remote service; remote revocation will be added with relay support.
     */
    fun removeLocalIdentity() {
        val keyStore = loadKeyStore()
        if (keyStore.containsAlias(KEY_ALIAS)) {
            keyStore.deleteEntry(KEY_ALIAS)
        }
    }

    private fun loadKeyStore(): KeyStore {
        return KeyStore.getInstance(ANDROID_KEY_STORE).apply {
            load(null)
        }
    }

    companion object {
        private const val ANDROID_KEY_STORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "komikku.chatgpt.device-signing.v1"
        private const val EC_CURVE = "secp256r1"
        private const val SIGNATURE_ALGORITHM = "SHA256withECDSA"
        private const val MIN_CHALLENGE_BYTES = 32
        private const val MAX_CHALLENGE_BYTES = 512
        private val PAIRING_DOMAIN =
            "Komikku-ChatGPT-Pairing-v1\u0000".toByteArray(StandardCharsets.UTF_8)

        internal fun validatePairingChallenge(challenge: ByteArray) {
            require(challenge.size in MIN_CHALLENGE_BYTES..MAX_CHALLENGE_BYTES) {
                "Pairing challenge must contain 32 to 512 bytes"
            }
        }

        internal fun createIdentityInfo(publicKeyEncoded: ByteArray): ChatGptDeviceIdentityInfo {
            require(publicKeyEncoded.isNotEmpty()) { "Public key must not be empty" }
            val digest = MessageDigest.getInstance("SHA-256").digest(publicKeyEncoded)
            val deviceId = Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
            val fingerprint = digest
                .take(FINGERPRINT_BYTES)
                .joinToString(":") { byte ->
                    (byte.toInt() and 0xFF).toString(16).padStart(2, '0').uppercase(Locale.ROOT)
                }
            val publicKey = Base64.getUrlEncoder().withoutPadding().encodeToString(publicKeyEncoded)

            return ChatGptDeviceIdentityInfo(
                deviceId = deviceId,
                fingerprint = fingerprint,
                publicKeyBase64 = publicKey,
            )
        }

        private const val FINGERPRINT_BYTES = 16
    }
}
// KMK <--
