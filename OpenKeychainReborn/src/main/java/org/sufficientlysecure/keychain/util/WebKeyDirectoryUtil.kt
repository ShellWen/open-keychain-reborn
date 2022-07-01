package org.sufficientlysecure.keychain.util

import java.io.UnsupportedEncodingException
import java.lang.AssertionError
import java.net.MalformedURLException
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.*
import java.util.regex.Pattern

object WebKeyDirectoryUtil {
    private val EMAIL_PATTERN = Pattern.compile("^\\s*([^\\s]+)@([^\\s]+)\\s*$")

    /**
     * Tries to construct a Web Key Directory from a given name.
     * Returns `null` if unsuccessful.
     *
     * @see [Key Discovery](https://tools.ietf.org/html/draft-koch-openpgp-webkey-service-05.section-3.1)
     */
    @JvmStatic
    fun toWebKeyDirectoryURL(name: String?, wkdMethodAdvanced: Boolean): URL? {
        if (name == null) {
            return null
        }
        if (name.startsWith("https://") && name.contains("/.well-known/openpgpkey/")) {
            return try {
                URL(name)
            } catch (e: MalformedURLException) {
                null
            }
        }
        val matcher = EMAIL_PATTERN.matcher(name)
        if (!matcher.matches()) {
            return null
        }
        val localPart = matcher.group(1) ?: return null
        val encodedPart = ZBase32.encode(toSHA1(localPart.lowercase(Locale.getDefault()).toByteArray()))
        val domain = matcher.group(2)
        return try {
            if (wkdMethodAdvanced) {
                // Advanced method
                URL(
                    "https://openpgpkey.$domain/.well-known/openpgpkey/$domain/hu/$encodedPart?l=" + URLEncoder.encode(
                        localPart,
                        "UTF-8"
                    )
                )
            } else {
                // Direct method
                URL(
                    "https://$domain/.well-known/openpgpkey/hu/$encodedPart?l=" + URLEncoder.encode(
                        localPart,
                        "UTF-8"
                    )
                )
            }
        } catch (e: MalformedURLException) {
            null
        } catch (e: UnsupportedEncodingException) {
            null
        }
    }

    private fun toSHA1(input: ByteArray): ByteArray {
        return try {
            MessageDigest.getInstance("SHA-1").digest(input)
        } catch (e: NoSuchAlgorithmException) {
            throw AssertionError("SHA-1 should always be available")
        }
    }
}