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
@file:Suppress("DEPRECATION")

package org.sufficientlysecure.keychain

import android.content.Context
import org.sufficientlysecure.keychain.Constants.IS_RUNNING_UNITTEST
import org.sufficientlysecure.keychain.Constants.DEBUG
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import org.sufficientlysecure.keychain.KeychainDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import timber.log.Timber
import org.sufficientlysecure.keychain.KeyRingsPublicModel
import org.sufficientlysecure.keychain.KeysModel
import org.sufficientlysecure.keychain.UserPacketsModel
import org.sufficientlysecure.keychain.CertsModel
import org.sufficientlysecure.keychain.KeyMetadataModel
import org.sufficientlysecure.keychain.KeySignaturesModel
import org.sufficientlysecure.keychain.ApiAppsModel
import org.sufficientlysecure.keychain.OverriddenWarningsModel
import org.sufficientlysecure.keychain.AutocryptPeersModel
import org.sufficientlysecure.keychain.ApiAllowedKeysModel
import org.sufficientlysecure.keychain.util.Preferences
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.lang.RuntimeException
import kotlin.Throws

/**
 * SQLite Datatypes (from http://www.sqlite.org/datatype3.html)
 * - NULL. The value is a NULL value.
 * - INTEGER. The value is a signed integer, stored in 1, 2, 3, 4, 6, or 8 bytes depending on the magnitude of the value.
 * - REAL. The value is a floating point value, stored as an 8-byte IEEE floating point number.
 * - TEXT. The value is a text string, stored using the database encoding (UTF-8, UTF-16BE or UTF-16LE).
 * - BLOB. The value is a blob of data, stored exactly as it was input.
 */
class KeychainDatabase private constructor(context: Context) {
    private val supportSQLiteOpenHelper: SupportSQLiteOpenHelper

    init {
        supportSQLiteOpenHelper = FrameworkSQLiteOpenHelperFactory()
            .create(SupportSQLiteOpenHelper.Configuration.builder(context).name(DATABASE_NAME)
                .callback(
                    object : SupportSQLiteOpenHelper.Callback(DATABASE_VERSION) {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            this@KeychainDatabase.onCreate(db, context)
                        }

                        override fun onUpgrade(
                            db: SupportSQLiteDatabase,
                            oldVersion: Int,
                            newVersion: Int
                        ) {
                            this@KeychainDatabase.onUpgrade(db, context, oldVersion, newVersion)
                        }

                        override fun onDowngrade(
                            db: SupportSQLiteDatabase,
                            oldVersion: Int,
                            newVersion: Int
                        ) {
                            this@KeychainDatabase.onDowngrade()
                        }

                        override fun onOpen(db: SupportSQLiteDatabase) {
                            super.onOpen(db)
                            if (!db.isReadOnly) {
                                // Enable foreign key constraints
                                db.execSQL("PRAGMA foreign_keys=ON;")
                                if (DEBUG) {
                                    recreateUnifiedKeyView(db)
                                }
                            }
                        }
                    }).build()
            )
    }

    val readableDatabase: SupportSQLiteDatabase
        get() = supportSQLiteOpenHelper.readableDatabase
    val writableDatabase: SupportSQLiteDatabase
        get() = supportSQLiteOpenHelper.writableDatabase

    // using some sqldelight constants
    private fun onCreate(db: SupportSQLiteDatabase, context: Context) {
        Timber.w("Creating database...")
        db.execSQL(KeyRingsPublicModel.CREATE_TABLE)
        db.execSQL(KeysModel.CREATE_TABLE)
        db.execSQL(UserPacketsModel.CREATE_TABLE)
        db.execSQL(CertsModel.CREATE_TABLE)
        db.execSQL(KeyMetadataModel.CREATE_TABLE)
        db.execSQL(KeySignaturesModel.CREATE_TABLE)
        db.execSQL(ApiAppsModel.CREATE_TABLE)
        db.execSQL(OverriddenWarningsModel.CREATE_TABLE)
        db.execSQL(AutocryptPeersModel.CREATE_TABLE)
        db.execSQL(ApiAllowedKeysModel.CREATE_TABLE)
        db.execSQL(KeysModel.UNIFIEDKEYVIEW)
        db.execSQL(KeysModel.VALIDKEYSVIEW)
        db.execSQL(KeysModel.VALIDMASTERKEYSVIEW)
        db.execSQL(UserPacketsModel.UIDSTATUS)
        db.execSQL("CREATE INDEX keys_by_rank ON keys (" + KeysModel.RANK + ", " + KeysModel.MASTER_KEY_ID + ");")
        db.execSQL(
            "CREATE INDEX uids_by_rank ON user_packets (" + UserPacketsModel.RANK + ", "
                    + UserPacketsModel.USER_ID + ", " + UserPacketsModel.MASTER_KEY_ID + ");"
        )
        db.execSQL(
            "CREATE INDEX verified_certs ON certs ("
                    + CertsModel.VERIFIED + ", " + CertsModel.MASTER_KEY_ID + ");"
        )
        db.execSQL(
            "CREATE INDEX uids_by_email ON user_packets ("
                    + UserPacketsModel.EMAIL + ");"
        )
        Preferences.getPreferences(context).setKeySignaturesTableInitialized()
    }

    private fun onUpgrade(
        db: SupportSQLiteDatabase,
        context: Context,
        oldVersion: Int,
        newVersion: Int
    ) {
        Timber.d("Upgrading db from $oldVersion to $newVersion")
        if (oldVersion < 34) {
            throw RuntimeException("Database migrating support before db version 34 has been removed.")
        }

//        switch (oldVersion) {
//        }
    }

    private fun recreateUnifiedKeyView(db: SupportSQLiteDatabase) {
        db.execSQL("DROP VIEW IF EXISTS " + KeysModel.UNIFIEDKEYVIEW_VIEW_NAME)
        db.execSQL(KeysModel.UNIFIEDKEYVIEW)
        db.execSQL("DROP VIEW IF EXISTS " + KeysModel.VALIDKEYS_VIEW_NAME)
        db.execSQL(KeysModel.VALIDKEYSVIEW)
        db.execSQL("DROP VIEW IF EXISTS " + KeysModel.VALIDMASTERKEYS_VIEW_NAME)
        db.execSQL(KeysModel.VALIDMASTERKEYSVIEW)
        db.execSQL("DROP VIEW IF EXISTS " + UserPacketsModel.UIDSTATUS_VIEW_NAME)
        db.execSQL(UserPacketsModel.UIDSTATUS)
    }

    private fun onDowngrade() {
        // Downgrade is ok for the debug version, makes it easier to work with branches
        if (DEBUG) {
            return
        }
        throw RuntimeException("Downgrading the database is not allowed!")
    }

    companion object {
        private const val DATABASE_NAME = "openkeychain.db"
        private const val DATABASE_VERSION = 34
        private var sInstance: KeychainDatabase? = null
        @JvmStatic
        fun getInstance(context: Context): KeychainDatabase? {
            if (sInstance == null || IS_RUNNING_UNITTEST) {
                sInstance = KeychainDatabase(context.applicationContext)
            }
            return sInstance
        }

        @Throws(IOException::class)
        private fun copy(`in`: File, out: File) {
            `in`.inputStream().copyTo(out.outputStream(), 512)
        }

        @Throws(IOException::class)
        fun debugBackup(context: Context, restore: Boolean) {
            assert(DEBUG)
            val `in`: File
            val out: File
            if (restore) {
                `in` = context.getDatabasePath("debug_backup.db")
                out = context.getDatabasePath(DATABASE_NAME)
            } else {
                `in` = context.getDatabasePath(DATABASE_NAME)
                out = context.getDatabasePath("debug_backup.db")
                out.createNewFile()
            }
            if (!`in`.canRead()) {
                throw IOException("Cannot read " + `in`.name)
            }
            if (!out.canWrite()) {
                throw IOException("Cannot write " + out.name)
            }
            copy(`in`, out)
        }
    }
}