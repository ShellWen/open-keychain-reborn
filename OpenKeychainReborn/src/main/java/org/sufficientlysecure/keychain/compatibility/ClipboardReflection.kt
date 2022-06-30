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
package org.sufficientlysecure.keychain.compatibility

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import timber.log.Timber

object ClipboardReflection {
    fun getClipboardText(context: Context?): String? {
        if (context == null) {
            return null
        }
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
            ?: return null
        val clip = clipboard.primaryClip
        if (clip == null || clip.itemCount == 0) {
            Timber.e("No clipboard data!")
            return null
        }
        val item = clip.getItemAt(0)
        val seq = item.coerceToText(context)
        return seq?.toString()
    }

    fun clearClipboard(context: Context?) {
        if (context == null) {
            return
        }
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
            ?: return
        clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
    }
}