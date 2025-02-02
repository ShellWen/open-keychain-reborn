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

import android.util.Log
import org.sufficientlysecure.keychain.pgp.Progressable
import timber.log.Timber

/**
 * This is a simple class that wraps a Progressable, scaling the progress
 * values into a specified range.
 */
open class ProgressScaler : Progressable {
    @JvmField
    val mWrapped: Progressable?
    @JvmField
    val mFrom: Int
    @JvmField
    val mTo: Int
    @JvmField
    val mMax: Int

    constructor() {
        mWrapped = null
        mMax = 0
        mTo = mMax
        mFrom = mTo
    }

    constructor(wrapped: Progressable, from: Int, to: Int, max: Int) {
        mWrapped = wrapped
        mFrom = from
        mTo = to
        mMax = max
    }

    override fun setProgress(resourceId: Int?, progress: Int, max: Int) {
        mWrapped?.setProgress(resourceId, mFrom + progress * (mTo - mFrom) / max, mMax)
    }

    override fun setPreventCancel() {
        mWrapped?.setPreventCancel()
    }
}