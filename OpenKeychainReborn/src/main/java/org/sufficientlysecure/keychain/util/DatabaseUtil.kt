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

/**
 * Shamelessly copied from android.database.DatabaseUtils
 */
object DatabaseUtil {
    /**
     * Concatenates two SQL WHERE clauses, handling empty or null values.
     */
    @JvmStatic
    fun concatenateWhere(a: String, b: String): String {
        return if (a.isEmpty()) {
            b
        } else if (b.isEmpty()) {
            a
        } else "($a) AND ($b)"
    }

    /**
     * Appends one set of selection args to another. This is useful when adding a selection
     * argument to a user provided set.
     */
    @JvmStatic
    fun appendSelectionArgs(
        originalValues: Array<String?>?,
        newValues: Array<String?>
    ): Array<String?> {
        if (originalValues == null || originalValues.isEmpty()) {
            return newValues
        }
        val result = arrayOfNulls<String>(originalValues.size + newValues.size)
        System.arraycopy(originalValues, 0, result, 0, originalValues.size)
        System.arraycopy(newValues, 0, result, originalValues.size, newValues.size)
        return result
    }
}