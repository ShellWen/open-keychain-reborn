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

import android.annotation.SuppressLint
import android.app.Application
import android.graphics.Bitmap
import android.os.Build
import com.shellwen.keychainreborn.BuildConfig
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.sufficientlysecure.keychain.keysync.KeyserverSyncManager
import org.sufficientlysecure.keychain.network.TlsCertificatePinning
import org.sufficientlysecure.keychain.provider.TemporaryFileProvider
import org.sufficientlysecure.keychain.util.Preferences
import timber.log.Timber
import timber.log.Timber.Forest.plant
import java.lang.reflect.InvocationTargetException
import java.security.Security

class KeychainApplication : Application() {
    /**
     * Called when the application is starting, before any activity, service, or receiver objects
     * (excluding content providers) have been created.
     */
    override fun onCreate() {
        super.onCreate()

        /*
         * Sets our own Bouncy Castle library as preferred security provider
         *
         * because Android's default provider config has BC at position 3,
         * we need to remove it and insert BC again at position 1 (above OpenSSLProvider!)
         *
         * (insertProviderAt() position starts from 1)
         */
        Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME)
        Security.insertProviderAt(BouncyCastleProvider(), 1)

        updateLoggingStatus()

        val preferences = Preferences.getPreferences(this)
        if (preferences.isAppExecutedFirstTime) {
            preferences.isAppExecutedFirstTime = false
            preferences.setPrefVersionToCurrentVersion()
        }

        // Upgrade preferences as needed
        preferences.upgradePreferences()
        TlsCertificatePinning.addPinnedCertificate(
            "hkps.pool.sks-keyservers.net",
            assets,
            "hkps.pool.sks-keyservers.net.CA.cer"
        )

        // only set up the rest on our main process
        if (BuildConfig.APPLICATION_ID != processName) {
            return
        }
        KeyserverSyncManager.updateKeyserverSyncScheduleAsync(this, false)
        TemporaryFileProvider.scheduleCleanupImmediately(applicationContext)
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= TRIM_MEMORY_UI_HIDDEN) {
            qrCodeCache.clear()
        }
    }

    private fun updateLoggingStatus() {
        Timber.uprootAll()
        val enableDebugLogging = Constants.DEBUG
        if (enableDebugLogging) {
            plant(Timber.DebugTree())
        }
    }

    companion object {
        var qrCodeCache =
            HashMap<String, Bitmap>()

        val processName: String
            @SuppressLint("DiscouragedPrivateApi")
            get() = if (Build.VERSION.SDK_INT >= 28) getProcessName() else try {
                // Using the same technique as Application.getProcessName() for older devices
                // Using reflection since ActivityThread is an internal API

                @SuppressLint("PrivateApi")
                val activityThread = Class.forName("android.app.ActivityThread")
                val getProcessName = activityThread.getDeclaredMethod("currentProcessName")
                getProcessName.invoke(null) as String
            } catch (e: ClassNotFoundException) {
                throw RuntimeException(e)
            } catch (e: NoSuchMethodException) {
                throw RuntimeException(e)
            } catch (e: IllegalAccessException) {
                throw RuntimeException(e)
            } catch (e: InvocationTargetException) {
                throw RuntimeException(e)
            }
    }
}