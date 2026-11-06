package eu.kanade.domain.chatgpt.service

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter

// KMK -->
/**
 * Converts a short-lived pairing request payload into a QR bitmap.
 * No camera permission or network access is needed to display this code.
 */
class ChatGptPairingQrCodeGenerator {

    fun generate(payload: String, size: Int = DEFAULT_SIZE): Bitmap {
        require(payload.isNotBlank()) { "QR payload must not be blank" }
        require(size in MIN_SIZE..MAX_SIZE) { "QR size is outside the supported range" }

        val matrix = QRCodeWriter().encode(
            payload,
            BarcodeFormat.QR_CODE,
            size,
            size,
            mapOf(
                EncodeHintType.MARGIN to QR_MARGIN,
                EncodeHintType.CHARACTER_SET to CHARSET_UTF8,
            ),
        )

        val pixels = IntArray(size * size) { index ->
            val x = index % size
            val y = index / size
            if (matrix[x, y]) Color.BLACK else Color.WHITE
        }

        return Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).apply {
            setPixels(pixels, 0, size, 0, 0, size, size)
        }
    }

    companion object {
        private const val DEFAULT_SIZE = 640
        private const val MIN_SIZE = 256
        private const val MAX_SIZE = 1_024
        private const val QR_MARGIN = 2
        private const val CHARSET_UTF8 = "UTF-8"
    }
}
// KMK <--
