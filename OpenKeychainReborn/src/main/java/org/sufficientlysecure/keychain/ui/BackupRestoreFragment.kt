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
package org.sufficientlysecure.keychain.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Pair
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.lifecycle.lifecycleScope
import com.shellwen.keychainreborn.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.sufficientlysecure.keychain.Constants
import org.sufficientlysecure.keychain.daos.KeyRepository
import org.sufficientlysecure.keychain.operations.results.ExportResult
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKey.SecretKeyType
import org.sufficientlysecure.keychain.provider.TemporaryFileProvider
import org.sufficientlysecure.keychain.service.BackupKeyringParcel
import org.sufficientlysecure.keychain.service.input.CryptoInputParcel
import org.sufficientlysecure.keychain.service.input.RequiredInputParcel
import org.sufficientlysecure.keychain.ui.base.CryptoOperationFragment
import org.sufficientlysecure.keychain.ui.util.Notify
import org.sufficientlysecure.keychain.util.FileHelper.copyUriData
import org.sufficientlysecure.keychain.util.FileHelper.openDocument
import org.sufficientlysecure.keychain.util.FileHelper.saveDocument
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class BackupRestoreFragment : CryptoOperationFragment<BackupKeyringParcel?, ExportResult?>() {
    // masterKeyId & subKeyId for multi-key export
    private var mIdsForRepeatAskPassphrase: Iterator<Pair<Long, Long>>? = null
    private lateinit var backupPublicKeys: View
    private var cachedBackupUri: Uri? = null
    private var shareNotSave = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.backup_restore_fragment, container, false)
        val backupAll = view.findViewById<View>(R.id.backup_all)
        backupPublicKeys = view.findViewById(R.id.backup_public_keys)
        val restore = view.findViewById<View>(R.id.restore)
        backupAll.setOnClickListener { backupAllKeys() }
        backupPublicKeys.setOnClickListener { exportContactKeys() }
        restore.setOnClickListener { restore() }
        return view
    }

    private fun backupAllKeys() {
        // This can probably be optimized quite a bit.
        // Typically there are only few secret keys though, so it doesn't really matter.

        lifecycleScope.launch {
            val keyRepository = KeyRepository.create(requireContext())
            val askPassphraseIds = ArrayList<Pair<Long, Long>>()

            fun getFirstSubKeyWithPassphrase(masterKeyId: Long): Long? {
                for (subKey in keyRepository.getSubKeysByMasterKeyId(masterKeyId)) {
                    return when (subKey.has_secret()) {
                        SecretKeyType.PASSPHRASE_EMPTY, SecretKeyType.DIVERT_TO_CARD, SecretKeyType.UNAVAILABLE -> null
                        SecretKeyType.GNU_DUMMY -> continue
                        else -> {
                            subKey.key_id()
                        }
                    }
                }
                return null
            }

            for (keyInfo in keyRepository.allUnifiedKeyInfoWithSecret) {
                val masterKeyId = keyInfo.master_key_id()
                val secretKeyType: SecretKeyType = try {
                    keyRepository.getSecretKeyType(keyInfo.master_key_id())
                } catch (e: KeyRepository.NotFoundException) {
                    throw IllegalStateException("Error: no secret key type for secret key!")
                }
                when (secretKeyType) {
                    SecretKeyType.PASSPHRASE_EMPTY, SecretKeyType.DIVERT_TO_CARD, SecretKeyType.UNAVAILABLE -> continue
                    SecretKeyType.GNU_DUMMY -> {
                        val subKeyId = getFirstSubKeyWithPassphrase(masterKeyId)
                        if (subKeyId != null) {
                            askPassphraseIds.add(Pair(masterKeyId, subKeyId))
                        }
                        continue
                    }
                    else -> {
                        askPassphraseIds.add(Pair(masterKeyId, masterKeyId))
                    }
                }
            }
            withContext(Dispatchers.Main) {
                mIdsForRepeatAskPassphrase = askPassphraseIds.iterator()
                if (mIdsForRepeatAskPassphrase!!.hasNext()) {
                    startPassphraseActivity()
                    return@withContext
                }
                startBackup(true)
            }
        }
    }

    private fun exportContactKeys() {
        val popupMenu = PopupMenu(context, backupPublicKeys)
        popupMenu.inflate(R.menu.export_public)
        popupMenu.setOnMenuItemClickListener { item: MenuItem ->
            when (item.itemId) {
                R.id.menu_export_file -> {
                    shareNotSave = false
                    exportContactKeysToFileOrShare()
                }
                R.id.menu_export_share -> {
                    shareNotSave = true
                    exportContactKeysToFileOrShare()
                }
            }
            false
        }
        popupMenu.show()
    }

    private fun exportContactKeysToFileOrShare() {
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val filename =
            Constants.FILE_ENCRYPTED_BACKUP_PREFIX + date + Constants.FILE_EXTENSION_ENCRYPTED_BACKUP_PUBLIC
        if (cachedBackupUri == null) {
            cachedBackupUri = TemporaryFileProvider.createFile(
                context, filename,
                Constants.MIME_TYPE_ENCRYPTED_ALTERNATE
            )
            cryptoOperation(CryptoInputParcel.createCryptoInputParcel())
            return
        }
        if (shareNotSave) {
            val intent = Intent(Intent.ACTION_SEND)
            intent.type = Constants.MIME_TYPE_KEYS
            intent.putExtra(Intent.EXTRA_STREAM, cachedBackupUri)
            startActivity(intent)
        } else {
            saveFile(filename)
        }
    }

    private fun saveFile(filename: String) {
        saveDocument(this, filename, REQUEST_SAVE_FILE, Constants.MIME_TYPE_ENCRYPTED_ALTERNATE)
    }

    private fun startPassphraseActivity() {
        val activity = activity ?: return
        val intent = Intent(activity, PassphraseDialogActivity::class.java)
        val keyPair = mIdsForRepeatAskPassphrase!!.next()
        val masterKeyId = keyPair.first
        val subKeyId = keyPair.second
        val requiredInput =
            RequiredInputParcel.createRequiredDecryptPassphrase(masterKeyId, subKeyId)
        requiredInput.mSkipCaching = true
        intent.putExtra(PassphraseDialogActivity.EXTRA_REQUIRED_INPUT, requiredInput)
        startActivityForResult(intent, REQUEST_REPEAT_PASSPHRASE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, `data`: Intent?) {
        when (requestCode) {
            REQUEST_REPEAT_PASSPHRASE -> {
                if (resultCode != Activity.RESULT_OK) {
                    return
                }
                if (mIdsForRepeatAskPassphrase!!.hasNext()) {
                    startPassphraseActivity()
                    return
                }
                startBackup(true)
            }
            REQUEST_CODE_INPUT -> {
                if (resultCode != Activity.RESULT_OK || `data` == null) {
                    return
                }
                val uri = `data`.`data`
                if (uri == null) {
                    Notify.create(activity, R.string.no_file_selected, Notify.Style.ERROR).show()
                    return
                }
                val intent = Intent(activity, DecryptActivity::class.java)
                intent.action = Intent.ACTION_VIEW
                intent.`data` = uri
                startActivity(intent)
            }
            REQUEST_SAVE_FILE -> {
                val activity = activity
                if (resultCode != Activity.RESULT_OK || activity == null || `data` == null) {
                    return
                }
                try {
                    val outputUri = `data`.`data`
                    copyUriData(activity, cachedBackupUri!!, outputUri!!)
                    Notify.create(activity, R.string.snack_backup_saved, Notify.Style.OK).show()
                } catch (e: IOException) {
                    Notify.create(
                        activity,
                        R.string.snack_backup_error_saving,
                        Notify.Style.ERROR
                    ).show()
                }
                super.onActivityResult(requestCode, resultCode, data)
            }
            else -> {
                super.onActivityResult(requestCode, resultCode, data)
            }
        }
    }

    override fun createOperationInput(): BackupKeyringParcel? {
        return BackupKeyringParcel
            .create(null, false, false, true, cachedBackupUri)
    }

    override fun onCryptoOperationSuccess(result: ExportResult?) {
        exportContactKeysToFileOrShare()
    }

    override fun onCryptoOperationError(result: ExportResult?) {
        result?.createNotify(activity)?.show()
        cachedBackupUri = null
    }

    private fun startBackup(exportSecret: Boolean) {
        val intent = Intent(activity, BackupActivity::class.java)
        intent.putExtra(BackupActivity.EXTRA_SECRET, exportSecret)
        startActivity(intent)
    }

    private fun restore() {
        openDocument(this, "*/*", false, REQUEST_CODE_INPUT)
    }

    companion object {
        private const val REQUEST_SAVE_FILE = 1
        private const val REQUEST_REPEAT_PASSPHRASE = 0x00007002
        private const val REQUEST_CODE_INPUT = 0x00007003
    }
}