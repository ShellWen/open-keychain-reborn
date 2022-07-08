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
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.shellwen.keychainreborn.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.sufficientlysecure.keychain.compatibility.ClipboardReflection.getClipboardText
import org.sufficientlysecure.keychain.pgp.PgpHelper
import org.sufficientlysecure.keychain.ui.util.Notify
import org.sufficientlysecure.keychain.ui.util.SubtleAttentionSeeker
import org.sufficientlysecure.keychain.util.FileHelper.openDocument
import timber.log.Timber

class EncryptDecryptFragment : Fragment() {
    var mClipboardIcon: View? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.encrypt_decrypt_fragment, container, false)
        val mEncryptFile = view.findViewById<View>(R.id.encrypt_files)
        val mEncryptText = view.findViewById<View>(R.id.encrypt_text)
        val mDecryptFile = view.findViewById<View>(R.id.decrypt_files)
        val mDecryptFromClipboard = view.findViewById<View>(R.id.decrypt_from_clipboard)
        mClipboardIcon = view.findViewById(R.id.clipboard_icon)
        mEncryptFile.setOnClickListener {
            val encrypt = Intent(activity, EncryptFilesActivity::class.java)
            startActivity(encrypt)
        }
        mEncryptText.setOnClickListener {
            val encrypt = Intent(activity, EncryptTextActivity::class.java)
            startActivity(encrypt)
        }
        mDecryptFile.setOnClickListener {
            openDocument(
                this@EncryptDecryptFragment,
                "*/*",
                false,
                REQUEST_CODE_INPUT
            )
        }
        mDecryptFromClipboard.setOnClickListener { decryptFromClipboard() }
        return view
    }

    private fun decryptFromClipboard() {
        val activity = activity ?: return
        val clipboardText: CharSequence? = getClipboardText(activity)
        if (TextUtils.isEmpty(clipboardText)) {
            Notify.create(activity, R.string.error_clipboard_empty, Notify.Style.ERROR).show()
            return
        }
        val clipMan =
            requireActivity().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
        if (clipMan == null) {
            Timber.e("Couldn't get ClipboardManager instance!")
            return
        }
        val clip = clipMan.primaryClip
        if (clip == null) {
            Timber.e("Couldn't get clipboard data!")
            return
        }
        val clipboardDecrypt = Intent(getActivity(), DecryptActivity::class.java)
        clipboardDecrypt.putExtra(DecryptActivity.EXTRA_CLIPDATA, clip)
        clipboardDecrypt.action = DecryptActivity.ACTION_DECRYPT_FROM_CLIPBOARD
        startActivityForResult(clipboardDecrypt, 0)
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            checkClipboardForEncryptedText()
        }
    }

    private suspend fun checkClipboardForEncryptedText() {
        withContext(Dispatchers.Main) {
            val clipboardText: CharSequence? = getClipboardText(activity)
            if (clipboardText.isNullOrEmpty()) {
                return@withContext
            }

            val animate = PgpHelper.PGP_MESSAGE.matcher(clipboardText)
                .matches() || PgpHelper.PGP_CLEARTEXT_SIGNATURE.matcher(clipboardText).matches()
            if (animate) {
                // if so, animate the clipboard icon just a bit~
                SubtleAttentionSeeker.tada(mClipboardIcon, 1.5f).start()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode != REQUEST_CODE_INPUT) {
            return
        }
        if (resultCode == Activity.RESULT_OK && data != null) {
            val uri = data.data
            if (uri == null) {
                Notify.create(activity, R.string.no_file_selected, Notify.Style.ERROR).show()
                return
            }
            val intent = Intent(activity, DecryptActivity::class.java)
            intent.action = Intent.ACTION_VIEW
            intent.data = uri
            startActivity(intent)
        }
    }

    companion object {
        private const val REQUEST_CODE_INPUT = 0x00007003
    }
}