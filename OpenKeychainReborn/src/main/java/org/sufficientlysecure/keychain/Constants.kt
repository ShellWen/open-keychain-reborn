/*
 * Copyright (C) 2017 Schürmann & Breitmoser GbR
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
package org.sufficientlysecure.keychain

import android.os.Environment
import com.shellwen.keychainreborn.BuildConfig
import org.bouncycastle.bcpg.sig.KeyFlags
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.sufficientlysecure.keychain.securitytoken.KeyFormat
import org.sufficientlysecure.keychain.securitytoken.RsaKeyFormat
import org.sufficientlysecure.keychain.service.SaveKeyringParcel
import org.sufficientlysecure.keychain.service.SaveKeyringParcel.SubkeyAdd
import java.io.File
import java.net.Proxy

object Constants {
    val DEBUG = BuildConfig.DEBUG
    const val DEBUG_KEYSERVER_SYNC = false
    val IS_RUNNING_UNITTEST = isRunningUnitTest
    val TAG = if (DEBUG) "Keychain D" else "Keychain"
    const val PACKAGE_NAME = "com.shellwen.keychainreborn"
    const val PROVIDER_AUTHORITY = BuildConfig.PROVIDER_CONTENT_AUTHORITY
    const val TEMP_FILE_PROVIDER_AUTHORITY = BuildConfig.APPLICATION_ID + ".tempstorage"
    const val CLIPBOARD_LABEL = "KeychainReborn"

    // as defined in http://tools.ietf.org/html/rfc3156
    const val MIME_TYPE_KEYS = "application/pgp-keys"

    // NOTE: Non-standard alternative, better use this, because application/octet-stream is too unspecific!
    // also see https://tools.ietf.org/html/draft-bray-pgp-message-00
    const val MIME_TYPE_ENCRYPTED_ALTERNATE = "application/pgp-message"
    const val MIME_TYPE_TEXT = "text/plain"
    const val FILE_EXTENSION_PGP_MAIN = ".pgp"
    const val FILE_EXTENSION_ASC = ".asc"
    const val FILE_BACKUP_PREFIX = "backup_"
    const val FILE_EXTENSION_BACKUP_SECRET = ".sec.asc"
    const val FILE_EXTENSION_BACKUP_PUBLIC = ".pub.asc"
    const val FILE_ENCRYPTED_BACKUP_PREFIX = "backup_"

    // actually it is ASCII Armor, so .asc would be more accurate, but Android displays a nice icon for .pgp files!
    const val FILE_EXTENSION_ENCRYPTED_BACKUP_SECRET = ".sec.pgp"
    const val FILE_EXTENSION_ENCRYPTED_BACKUP_PUBLIC = ".pub.pgp"

    // used by QR Codes (Guardian Project, Monkeysphere compatibility)
    const val FINGERPRINT_SCHEME = "openpgp4fpr"
    const val BOUNCY_CASTLE_PROVIDER_NAME = BouncyCastleProvider.PROVIDER_NAME

    // prefix packagename for exported Intents
    // as described in http://developer.android.com/guide/components/intents-filters.html
    const val INTENT_PREFIX = PACKAGE_NAME + ".action."
    const val EXTRA_PREFIX = PACKAGE_NAME + "."
    const val TEMPFILE_TTL = 24 * 60 * 60 * 1000 // 1 day

    // the maximal length of plaintext to read in encrypt/decrypt text activities
    const val TEXT_LENGTH_LIMIT = 1024 * 50

    // Intents API
    const val ENCRYPT_TEXT = INTENT_PREFIX + "ENCRYPT_TEXT"
    const val ENCRYPT_EXTRA_TEXT = EXTRA_PREFIX + "EXTRA_TEXT" // String
    const val ENCRYPT_DATA = INTENT_PREFIX + "ENCRYPT_DATA"
    const val ENCRYPT_EXTRA_ASCII_ARMOR = EXTRA_PREFIX + "EXTRA_ASCII_ARMOR" // boolean
    const val DECRYPT_DATA = INTENT_PREFIX + "DECRYPT_DATA"
    const val IMPORT_KEY = INTENT_PREFIX + "IMPORT_KEY"
    const val IMPORT_EXTRA_KEY_EXTRA_KEY_BYTES = EXTRA_PREFIX + "EXTRA_KEY_BYTES" // byte[]
    const val IMPORT_KEY_FROM_KEYSERVER = INTENT_PREFIX + "IMPORT_KEY_FROM_KEYSERVER"
    const val IMPORT_KEY_FROM_KEYSERVER_EXTRA_QUERY = EXTRA_PREFIX + "EXTRA_QUERY" // String
    const val IMPORT_KEY_FROM_KEYSERVER_EXTRA_FINGERPRINT =
        EXTRA_PREFIX + "EXTRA_FINGERPRINT" // String
    const val IMPORT_KEY_FROM_QR_CODE = INTENT_PREFIX + "IMPORT_KEY_FROM_QR_CODE"

    /**
     * Default key configuration: 3072 bit RSA (certify + sign, encrypt)
     */
    fun addDefaultSubkeys(builder: SaveKeyringParcel.Builder) {
        builder.addSubkeyAdd(
            SubkeyAdd.createSubkeyAdd(
                SaveKeyringParcel.Algorithm.RSA,
                3072, null, KeyFlags.CERTIFY_OTHER or KeyFlags.SIGN_DATA, 0L
            )
        )
        builder.addSubkeyAdd(
            SubkeyAdd.createSubkeyAdd(
                SaveKeyringParcel.Algorithm.RSA,
                3072, null, KeyFlags.ENCRYPT_COMMS or KeyFlags.ENCRYPT_STORAGE, 0L
            )
        )
    }

    /**
     * Default key format for OpenPGP smart cards v2: 2048 bit RSA (sign+certify, decrypt, auth)
     */
    private const val ELEN = 17 //65537
    val SECURITY_TOKEN_V2_SIGN: KeyFormat =
        RsaKeyFormat.getInstance(2048, ELEN, RsaKeyFormat.RsaImportFormat.CRT_WITH_MODULUS)
    val SECURITY_TOKEN_V2_DEC: KeyFormat =
        RsaKeyFormat.getInstance(2048, ELEN, RsaKeyFormat.RsaImportFormat.CRT_WITH_MODULUS)
    val SECURITY_TOKEN_V2_AUTH: KeyFormat =
        RsaKeyFormat.getInstance(2048, ELEN, RsaKeyFormat.RsaImportFormat.CRT_WITH_MODULUS)
    private val isRunningUnitTest: Boolean
        get() = try {
            Class.forName("org.sufficientlysecure.keychain.KeychainTestRunner")
            true
        } catch (e: ClassNotFoundException) {
            false
        }

    object Path {
        val APP_DIR = File(Environment.getExternalStorageDirectory(), "OpenKeychain")
    }

    object NotificationIds {
        const val PASSPHRASE_CACHE = 1
        const val KEYSERVER_SYNC_FAIL_ORBOT = 2
        const val KEYSERVER_SYNC = 3
    }

    object Pref {
        const val PASSPHRASE_CACHE_SUBS = "passphraseCacheSubs"
        const val PASSPHRASE_CACHE_LAST_TTL = "passphraseCacheLastTtl"
        const val LANGUAGE = "language"
        const val KEY_SERVERS = "keyServers"
        const val PREF_VERSION = "keyServersDefaultVersion"

        // false if first time wizard has been finished
        const val FIRST_TIME_WIZARD = "firstTime"

        // false if app has been started at least once (also from background etc)
        const val FIRST_TIME_APP = "firstTimeApp"
        const val CACHED_CONSOLIDATE = "cachedConsolidate"
        const val SEARCH_KEYSERVER = "search_keyserver_pref"
        const val SEARCH_WEB_KEY_DIRECTORY = "search_wkd_pref"
        const val USE_NUMKEYPAD_FOR_SECURITY_TOKEN_PIN = "useNumKeypadForYubikeyPin"
        const val ENCRYPT_FILENAMES = "encryptFilenames"
        const val FILE_USE_COMPRESSION = "useFileCompression"
        const val FILE_SELF_ENCRYPT = "fileSelfEncrypt"
        const val TEXT_USE_COMPRESSION = "useTextCompression"
        const val TEXT_SELF_ENCRYPT = "textSelfEncrypt"
        const val USE_ARMOR = "useArmor"

        // proxy settings
        const val USE_NORMAL_PROXY = "useNormalProxy"
        const val USE_TOR_PROXY = "useTorProxy"
        const val PROXY_HOST = "proxyHost"
        const val PROXY_PORT = "proxyPort"
        const val PROXY_TYPE = "proxyType"
        const val THEME = "theme"

        // keyserver sync settings
        const val SYNC_KEYSERVER = "syncKeyserver"
        const val ENABLE_WIFI_SYNC_ONLY = "enableWifiSyncOnly"
        const val SYNC_WORK_UUID = "syncWorkUuid"

        // other settings
        const val EXPERIMENTAL_USB_ALLOW_UNTESTED = "experimentalUsbAllowUntested"
        const val EXPERIMENTAL_SMARTPGP_VERIFY_AUTHORITY = "smartpgp_authorities_pref"
        const val EXPERIMENTAL_SMARTPGP_AUTHORITIES = "smartpgp_authorities"
        const val KEY_SIGNATURES_TABLE_INITIALIZED = "key_signatures_table_initialized"

        object Theme {
            const val LIGHT = "light"
            const val DARK = "dark"
            const val DEFAULT = LIGHT
        }

        object ProxyType {
            const val TYPE_HTTP = "proxyHttp"
            const val TYPE_SOCKS = "proxySocks"
        }
    }

    /**
     * Orbot's default localhost HTTP proxy
     * Orbot's SOCKS proxy is not fully supported by OkHttp
     */
    object Orbot {
        const val PROXY_HOST = "127.0.0.1"
        const val PROXY_PORT = 8118
        val PROXY_TYPE = Proxy.Type.HTTP
    }

    object Defaults {
        @Suppress("SpellCheckingInspection")
        const val KEY_SERVERS =
            "hkps://keys.openpgp.org;hkp://zkaan2xfbuxia2wpf7ofnkbz6r5zdbbvxbunvp5g2iebopbfc4iqmbad.onion"
        const val PREF_CURRENT_VERSION = 10
    }

    object Key {
        const val none: Long = 0
        const val symmetric: Long = -1
    }
}