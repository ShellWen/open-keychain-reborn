/*
 * Copyright (C) 2014 Dominik Schürmann <dominik@dominikschuermann.de>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.sufficientlysecure.keychain.util

import org.sufficientlysecure.keychain.Constants.TAG
import timber.log.Timber
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets

object Utf8Util {
    @JvmStatic
    fun isValidUTF8(input: ByteArray): Boolean {
        val cs = StandardCharsets.UTF_8.newDecoder()
        return try {
            cs.decode(ByteBuffer.wrap(input))
            true
        } catch (e: CharacterCodingException) {
            false
        }
    }

    @JvmStatic
    fun fromUTF8ByteArrayReplaceBadEncoding(input: ByteArray): String {
        val charsetDecoder = StandardCharsets.UTF_8.newDecoder().apply {
            onMalformedInput(CodingErrorAction.REPLACE)
            onUnmappableCharacter(CodingErrorAction.REPLACE)
        }
        return try {
            charsetDecoder.decode(ByteBuffer.wrap(input)).toString()
        } catch (e: CharacterCodingException) {
            Timber.tag(TAG).e(e, "Decoding failed!")
            charsetDecoder.replacement()
        }
    }
}