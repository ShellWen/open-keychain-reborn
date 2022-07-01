package org.sufficientlysecure.keychain.util

import java.lang.StringBuilder
import kotlin.experimental.and

/**
 * Utilities for handling ZBase-32 encoding.
 *
 * @see [Z-Base32 encoding as used in RFC 6189](https://tools.ietf.org/html/rfc6189.section-5.1.6)
 */
object ZBase32 {
    private val ALPHABET = "ybndrfg8ejkmcpqxot1uwisza345h769".toCharArray()
    private val SHIFT = Integer.numberOfTrailingZeros(ALPHABET.size)
    private val MASK = ALPHABET.size - 1

    @JvmStatic
    fun encode(data: ByteArray): String {
        if (data.isEmpty()) {
            return ""
        }
        val result = StringBuilder()
        var buffer = data[0].toInt()
        var index = 1
        var bitsLeft = 8
        while (bitsLeft > 0 || index < data.size) {
            if (bitsLeft < SHIFT) {
                if (index < data.size) {
                    buffer = buffer shl 8
                    buffer = buffer or ((data[index++] and 0xff.toByte()).toInt())
                    bitsLeft += 8
                } else {
                    val pad = SHIFT - bitsLeft
                    buffer = buffer shl pad
                    bitsLeft += pad
                }
            }
            bitsLeft -= SHIFT
            result.append(ALPHABET[MASK and (buffer shr bitsLeft)])
        }
        return result.toString()
    }
}