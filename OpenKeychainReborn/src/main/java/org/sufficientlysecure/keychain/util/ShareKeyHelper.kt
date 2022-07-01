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
package org.sufficientlysecure.keychain.util

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.ParcelFileDescriptor
import com.shellwen.keychainreborn.R
import org.sufficientlysecure.keychain.Constants
import kotlin.Throws
import org.sufficientlysecure.keychain.daos.KeyRepository
import org.sufficientlysecure.keychain.pgp.exception.PgpGeneralException
import org.sufficientlysecure.keychain.pgp.CanonicalizedPublicKey
import org.sufficientlysecure.keychain.provider.TemporaryFileProvider
import org.sufficientlysecure.keychain.model.SubKey.UnifiedKeyInfo
import org.sufficientlysecure.keychain.pgp.SshPublicKey
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils
import timber.log.Timber
import org.sufficientlysecure.keychain.ui.util.Notify
import org.sufficientlysecure.keychain.util.ShareKeyHelper
import java.io.BufferedWriter
import java.io.FileNotFoundException
import java.io.IOException
import java.io.OutputStreamWriter
import java.security.NoSuchAlgorithmException

object ShareKeyHelper {
    @Throws(IOException::class)
    private fun getKeyContent(masterKeyId: Long, keyRepository: KeyRepository): String? {
        return try {
            keyRepository.getPublicKeyRingAsArmoredString(masterKeyId)
        } catch (e: KeyRepository.NotFoundException) {
            null
        }
    }

    @Throws(PgpGeneralException::class, NoSuchAlgorithmException::class)
    private fun getSshKeyContent(masterKeyId: Long, keyRepository: KeyRepository): String? {
        return try {
            val authSubKeyId = keyRepository.getEffectiveAuthenticationKeyId(masterKeyId)
            val publicKey = keyRepository.getCanonicalizedPublicKeyRing(masterKeyId)
                .getPublicKey(authSubKeyId)
            val sshPublicKey = SshPublicKey(publicKey)
            sshPublicKey.encodedKey
        } catch (e: KeyRepository.NotFoundException) {
            null
        }
    }

    @Throws(IOException::class)
    private fun shareKeyIntent(activity: Activity, masterKeyId: Long, content: String) {
        // let user choose application
        val sendIntent = Intent(Intent.ACTION_SEND)
        sendIntent.type = Constants.MIME_TYPE_KEYS

        // NOTE: Don't use Intent.EXTRA_TEXT to send the key
        // better send it via a Uri!
        // example: Bluetooth Share will convert text/plain sent via Intent.EXTRA_TEXT to HTML
        try {
            val shareFileProv = TemporaryFileProvider()
            val unifiedKeyInfo = KeyRepository.create(activity).getUnifiedKeyInfo(masterKeyId)
            val filename: String? = if (unifiedKeyInfo.name() != null) {
                unifiedKeyInfo.name()
            } else {
                KeyFormattingUtils.convertFingerprintToHex(unifiedKeyInfo.fingerprint())
            }
            val contentUri =
                TemporaryFileProvider.createFile(activity, filename + Constants.FILE_EXTENSION_ASC)
            BufferedWriter(
                OutputStreamWriter(
                    ParcelFileDescriptor.AutoCloseOutputStream(
                        shareFileProv.openFile(contentUri, "w")
                    )
                )
            ).apply {
                write(content)
                close()
            }
            sendIntent.putExtra(Intent.EXTRA_STREAM, contentUri)
        } catch (e: FileNotFoundException) {
            Timber.e(e, "Error creating temporary key share file!")
            // no need for a snackbar because one sharing option doesn't work
            // Notify.create(getActivity(), R.string.error_temp_file, Notify.Style.ERROR).show();
        }
        val title = activity.getString(R.string.title_share_key)
        val shareChooser = Intent.createChooser(sendIntent, title)
        activity.startActivity(shareChooser)
    }

    private fun shareKeyToClipBoard(activity: Activity, content: String) {
        val clipMan = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
        if (clipMan == null) {
            Notify.create(activity, R.string.error_clipboard_copy, Notify.Style.ERROR).show()
            return
        }
        val clip = ClipData.newPlainText(Constants.CLIPBOARD_LABEL, content)
        clipMan.setPrimaryClip(clip)
        Notify.create(activity, R.string.key_copied_to_clipboard, Notify.Style.OK).show()
    }

    private fun shareKey(activity: Activity?, masterKeyId: Long, toClipboard: Boolean) {
        if (activity == null) {
            return
        }
        try {
            val content = getKeyContent(masterKeyId, KeyRepository.create(activity))
            if (content == null) {
                Notify.create(activity, R.string.error_key_not_found, Notify.Style.ERROR).show()
                return
            }
            if (toClipboard) {
                shareKeyToClipBoard(activity, content)
            } else {
                shareKeyIntent(activity, masterKeyId, content)
            }
        } catch (e: IOException) {
            Timber.e(e, "error processing key!")
            Notify.create(activity, R.string.error_key_processing, Notify.Style.ERROR).show()
        }
    }

    private fun shareSshKey(activity: Activity?, masterKeyId: Long, toClipboard: Boolean) {
        if (activity == null) {
            return
        }
        try {
            val content = getSshKeyContent(masterKeyId, KeyRepository.create(activity))
            if (content == null) {
                Notify.create(
                    activity,
                    R.string.authentication_subkey_not_found,
                    Notify.Style.ERROR
                ).show()
                return
            }
            if (toClipboard) {
                shareKeyToClipBoard(activity, content)
            } else {
                shareKeyIntent(activity, masterKeyId, content)
            }
        } catch (e: PgpGeneralException) {
            Timber.e(e, "error processing key!")
            Notify.create(activity, R.string.error_key_processing, Notify.Style.ERROR).show()
        } catch (e: IOException) {
            Timber.e(e, "error processing key!")
            Notify.create(activity, R.string.error_key_processing, Notify.Style.ERROR).show()
        } catch (e: NoSuchAlgorithmException) {
            Timber.e(e, "error processing key!")
            Notify.create(activity, R.string.error_key_processing, Notify.Style.ERROR).show()
        }
    }

    @JvmStatic
    fun shareKeyToClipboard(activity: Activity?, masterKeyId: Long) {
        shareKey(activity, masterKeyId, true)
    }

    @JvmStatic
    fun shareKey(activity: Activity?, masterKeyId: Long) {
        shareKey(activity, masterKeyId, false)
    }

    @JvmStatic
    fun shareSshKey(activity: Activity?, masterKeyId: Long) {
        shareSshKey(activity, masterKeyId, false)
    }

    @JvmStatic
    fun shareSshKeyToClipboard(activity: Activity?, masterKeyId: Long) {
        shareSshKey(activity, masterKeyId, true)
    }
}