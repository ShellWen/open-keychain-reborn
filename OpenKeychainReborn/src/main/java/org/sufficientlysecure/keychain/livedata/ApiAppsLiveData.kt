package org.sufficientlysecure.keychain.livedata

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import com.shellwen.keychainreborn.R
import org.sufficientlysecure.keychain.ui.keyview.loader.AsyncTaskLiveData
import org.sufficientlysecure.keychain.livedata.ApiAppsLiveData.ListedApp
import org.sufficientlysecure.keychain.daos.DatabaseNotifyManager
import org.sufficientlysecure.keychain.daos.ApiAppDao
import org.sufficientlysecure.keychain.model.ApiApp
import org.sufficientlysecure.keychain.livedata.ApiAppsLiveData
import java.util.*

class ApiAppsLiveData(context: Context) : AsyncTaskLiveData<List<ListedApp>>(
    context, DatabaseNotifyManager.getNotifyUriAllApps()
) {
    private val apiAppDao: ApiAppDao
    private val packageManager: PackageManager = context.packageManager
    override fun asyncLoadData(): List<ListedApp> {
        val result = ArrayList<ListedApp>()
        loadRegisteredApps(result)
        addPlaceholderApps(result)
        result.sortWith { o1: ListedApp, o2: ListedApp -> o1.readableName.compareTo(o2.readableName) }
        return result
    }

    private fun loadRegisteredApps(result: ArrayList<ListedApp>) {
        val registeredApiApps = apiAppDao.allApiApps
        for (apiApp in registeredApiApps) {
            var listedApp: ListedApp
            listedApp = try {
                val ai = packageManager.getApplicationInfo(apiApp.package_name(), 0)
                val applicationLabel = packageManager.getApplicationLabel(ai)
                val applicationIcon = packageManager.getApplicationIcon(ai)
                ListedApp(
                    apiApp.package_name(),
                    isInstalled = true,
                    isRegistered = true,
                    applicationLabel,
                    applicationIcon,
                    null
                )
            } catch (e: PackageManager.NameNotFoundException) {
                ListedApp(
                    apiApp.package_name(),
                    isInstalled = false,
                    isRegistered = true,
                    apiApp.package_name(),
                    null,
                    null
                )
            }
            result.add(listedApp)
        }
    }

    private fun addPlaceholderApps(result: ArrayList<ListedApp>) {
        for (placeholderApp in PLACERHOLDER_APPS) {
            if (!containsByPackageName(result, placeholderApp.packageName)) {
                try {
                    packageManager.getApplicationInfo(placeholderApp.packageName, 0)
                    result.add(placeholderApp.withIsInstalled())
                } catch (e: PackageManager.NameNotFoundException) {
                    result.add(placeholderApp)
                }
            }
        }
    }

    private fun containsByPackageName(result: ArrayList<ListedApp>, packageName: String): Boolean {
        for (app in result) {
            if (packageName == app.packageName) {
                return true
            }
        }
        return false
    }

    class ListedApp internal constructor(
        val packageName: String,
        val isInstalled: Boolean,
        val isRegistered: Boolean,
        readableName: CharSequence,
        applicationIcon: Drawable?,
        applicationIconRes: Int?
    ) {
        @JvmField
        val readableName: String

        @JvmField
        val applicationIcon: Drawable?

        @JvmField
        val applicationIconRes: Int?

        init {
            this.readableName = readableName.toString()
            this.applicationIcon = applicationIcon
            this.applicationIconRes = applicationIconRes
        }

        fun withIsInstalled(): ListedApp {
            return ListedApp(
                packageName,
                true,
                isRegistered,
                readableName,
                applicationIcon,
                applicationIconRes
            )
        }
    }

    init {
        apiAppDao = ApiAppDao.getInstance(context)
    }

    companion object {
        private val PLACERHOLDER_APPS = arrayOf(
            ListedApp(
                "com.fsck.k9",
                isInstalled = false,
                isRegistered = false,
                "K-9 Mail",
                null,
                R.drawable.apps_k9
            ),
            ListedApp(
                "dev.msfjarvis.aps",
                isInstalled = false,
                isRegistered = false,
                "Password Store",
                null,
                R.drawable.apps_password_store
            ),
            ListedApp(
                "eu.siacs.conversations",
                isInstalled = false,
                isRegistered = false,
                "Conversations (Instant Messaging)",
                null,
                R.drawable.apps_conversations
            )
        )
    }
}