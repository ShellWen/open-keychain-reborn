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

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.res.AssetFileDescriptor
import android.graphics.Bitmap
import android.graphics.Point
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.os.Process
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.system.ErrnoException
import android.system.Os
import androidx.fragment.app.Fragment
import kotlin.Throws
import timber.log.Timber
import java.io.*
import java.lang.Exception
import java.nio.charset.Charset
import java.security.SecureRandom
import java.text.DecimalFormat
import kotlin.math.log10
import kotlin.math.pow

/** This class offers a number of helper functions for saving documents.
 *
 * There are two entry points here: openDocument and saveDocument.
 *
 * - openDocument queries for a document for reading. Used in "open encrypted
 * file" ui flow.
 *
 * - saveDocument queries for a document name for saving. It will directly
 * triggers a "save document" intent. Used in "save encrypted file" ui flow.
 *
 */
object FileHelper {
    /** Opens the storage browser for saving a file.  */
    @JvmOverloads
    @JvmStatic
    fun saveDocument(
        fragment: Fragment,
        suggestedName: String?,
        requestCode: Int,
        mimeType: String? = "*/*"
    ) {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = mimeType
        // Note: This is not documented, but works: Show the Internal Storage menu item in the drawer!
        intent.putExtra("android.content.extra.SHOW_ADVANCED", true)
        intent.putExtra(Intent.EXTRA_TITLE, suggestedName)
        fragment.startActivityForResult(intent, requestCode)
    }

    @JvmStatic
    fun openDocument(fragment: Fragment, mimeType: String, multiple: Boolean, requestCode: Int) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = mimeType
        // Note: This is not documented, but works: Show the Internal Storage menu item in the drawer!
        intent.putExtra("android.content.extra.SHOW_ADVANCED", true)
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, multiple)
        fragment.startActivityForResult(intent, requestCode)
    }

    @JvmStatic
    fun getFilename(context: Context, uri: Uri): String {
        var filename: String? = null
        try {
            val cursor = context.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null
            )
            if (cursor != null) {
                if (cursor.moveToNext()) {
                    filename = cursor.getString(0)
                }
                cursor.close()
            }
        } catch (ignored: Exception) {
            // This happens in rare cases (eg: document deleted since selection) and should not cause a failure
        }
        if (filename == null) {
            val split = uri.toString().split("/").toTypedArray()
            filename = split[split.size - 1]
        }
        return filename
    }

    @JvmOverloads
    @JvmStatic
    fun getFileSize(context: Context, uri: Uri, def: Long = -1): Long {
        if (ContentResolver.SCHEME_FILE == uri.scheme) {
            var size = File(uri.path!!).length()
            if (size == 0L) {
                size = def
            }
            return size
        }
        var size = def
        try {
            val cursor =
                context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)
            if (cursor != null) {
                if (cursor.moveToNext()) {
                    size = cursor.getLong(0)
                }
                cursor.close()
            }
        } catch (ignored: Exception) {
            // This happens in rare cases (eg: document deleted since selection) and should not cause a failure
        }
        return size
    }

    /**
     * Retrieve thumbnail of file, document api feature
     */
    @JvmStatic
    fun getThumbnail(context: Context, uri: Uri, size: Point): Bitmap? {
        return try {
            DocumentsContract.getDocumentThumbnail(context.contentResolver, uri, size, null)
        } catch (e: Exception) {
            null
        }
    }

    @JvmStatic
    @Throws(IOException::class)
    fun isEncryptedFile(context: Context, uri: Uri): Boolean {
        var isEncrypted = false
        var br: BufferedReader? = null
        try {
            val `is` = context.contentResolver.openInputStream(uri)
            br = BufferedReader(InputStreamReader(`is`))
            val header = "-----BEGIN PGP MESSAGE-----"
            val length = header.length
            val buffer = CharArray(length)
            if (br.read(buffer, 0, length) == length) {
                isEncrypted = String(buffer) == header
            }
        } finally {
            try {
                br?.close()
            } catch (e: IOException) {
                Timber.e(e, "Error closing file")
            }
        }
        return isEncrypted
    }

    @JvmStatic
    fun readableFileSize(size: Long): String {
        if (size <= 0) return "0"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (log10(size.toDouble()) / log10(1024.0)).toInt()
        return DecimalFormat("#,##0.#").format(
            size / 1024.0.pow(digitGroups.toDouble())
        ) + " " + units[digitGroups]
    }

    @JvmOverloads
    @JvmStatic
    @Throws(IOException::class)
    fun readTextFromUri(context: Context, outputUri: Uri, charset: Charset? = null): String {
        val decryptedMessage = run {
            val inputStream = context.contentResolver.openInputStream(outputUri)
            val outputStream = ByteArrayOutputStream()
            val buf = ByteArray(256)
            var read: Int
            while (inputStream!!.read(buf).also { read = it } > 0) {
                outputStream.write(buf, 0, read)
            }
            inputStream.close()
            outputStream.close()
            outputStream.toByteArray()
        }
        val plaintext = if (charset != null) {
            try {
                String(decryptedMessage, charset)
            } catch (e: UnsupportedEncodingException) {
                // if we can't decode properly, just fall back to utf-8
                String(decryptedMessage)
            }
        } else {
            String(decryptedMessage)
        }
        return plaintext
    }

    @JvmStatic
    @Throws(IOException::class)
    fun copyUriData(context: Context, fromUri: Uri, toUri: Uri) {
        var bis: BufferedInputStream? = null
        var bos: BufferedOutputStream? = null
        try {
            val resolver = context.contentResolver
            bis = BufferedInputStream(resolver.openInputStream(fromUri))
            bos = BufferedOutputStream(resolver.openOutputStream(toUri))
            val buf = ByteArray(1024)
            var len: Int
            while (bis.read(buf).also { len = it } > 0) {
                bos.write(buf, 0, len)
            }
        } finally {
            bis?.close()
            bos?.close()
        }
    }

    /**
     * Deletes data at a URI securely by overwriting it with random data
     * before deleting it. This method is fail-fast - if we can't securely
     * delete the file, we don't delete it at all.
     */
    @JvmStatic
    @Throws(IOException::class)
    fun deleteFileSecurely(context: Context, uri: Uri): Int {
        val resolver = context.contentResolver
        var lengthLeft = getFileSize(context, uri)
        if (lengthLeft == -1L) {
            throw IOException("Error opening file!")
        }
        val random = SecureRandom()
        val randomData = ByteArray(1024)
        val out = BufferedOutputStream(
            resolver.openOutputStream(uri, "w") ?: throw IOException("Error opening file!")
        )
        while (lengthLeft > 0) {
            random.nextBytes(randomData)
            out.write(
                randomData,
                0,
                if (lengthLeft > randomData.size) randomData.size else lengthLeft.toInt()
            )
            lengthLeft -= randomData.size.toLong()
        }
        out.close()
        return if (ContentResolver.SCHEME_FILE == uri.scheme) {
            if (File(uri.path!!).delete()) 1 else 0
        } else {
            resolver.delete(uri, null, null)
        }
    }

    /** A replacement for ContentResolver.openInputStream() that does not allow
     * the usage of "file" Uris that point to private files owned by the
     * application only, *on Lollipop devices*.
     */
    @JvmStatic
    @Throws(FileNotFoundException::class)
    fun openInputStreamSafe(resolver: ContentResolver, uri: Uri): InputStream? {
        val scheme = uri.scheme
        return if (ContentResolver.SCHEME_FILE == scheme) {
            val pfd = ParcelFileDescriptor.open(
                File(uri.path!!), ParcelFileDescriptor.parseMode("r")
            )
            try {
                val st = Os.fstat(pfd.fileDescriptor)
                if (st.st_uid == Process.myUid()) {
                    Timber.e("File is owned by the application itself, aborting!")
                    throw FileNotFoundException("Unable to create stream")
                }
            } catch (e: ErrnoException) {
                Timber.e(e, "fstat() failed")
                throw FileNotFoundException("fstat() failed")
            }
            val fd = AssetFileDescriptor(pfd, 0, -1)
            try {
                fd.createInputStream()
            } catch (e: IOException) {
                throw FileNotFoundException("Unable to create stream")
            }
        } else {
            resolver.openInputStream(uri)
        }
    }
}