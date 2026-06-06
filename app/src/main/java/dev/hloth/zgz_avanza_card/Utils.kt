package dev.hloth.zaragoza_tarjeta_bus

import android.content.Intent
import android.os.Build
import android.os.Parcelable

const val LOG_TAG = "ZGZAvanzaCard"

@Suppress("DEPRECATION")
fun <T : Parcelable> Intent.getParcelableExtraCompat(key: String, clazz: Class<T>): T? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableExtra(key, clazz)
    } else {
        getParcelableExtra(key)
    }
}

fun bytesToHex(bytes: ByteArray): String {
    val sb = StringBuilder()
    for (b in bytes) {
        sb.append(String.format("%02X", b))
    }
    return sb.toString()
}

fun hexToBytes(s: String): ByteArray {
    if (s.length % 2 != 0) return ByteArray(0)
    val len = s.length
    val data = ByteArray(len / 2)
    var i = 0
    while (i < len) {
        val hi = Character.digit(s[i], 16)
        val lo = Character.digit(s[i + 1], 16)
        data[i / 2] = ((hi shl 4) + lo).toByte()
        i += 2
    }
    return data
}
