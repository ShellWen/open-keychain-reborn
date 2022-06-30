package org.sufficientlysecure.keychain

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import com.shellwen.keychainreborn.R

class NotificationChannelManager private constructor(
    private val context: Context,
    private val notificationManager: NotificationManager?
) {
    fun createNotificationChannelsIfNecessary() {
        if (notificationManager == null) {
            return
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }
        createNotificationChannel(
            KEYSERVER_SYNC,
            R.string.notify_channel_keysync,
            NotificationManager.IMPORTANCE_MIN
        )
        createNotificationChannel(
            PERMISSION_REQUESTS,
            R.string.notify_channel_permission,
            NotificationManager.IMPORTANCE_MIN
        )
        createNotificationChannel(
            PASSPHRASE_CACHE,
            R.string.notify_channel_passcache,
            NotificationManager.IMPORTANCE_NONE
        )
        createNotificationChannel(
            ORBOT,
            R.string.notify_channel_orbot,
            NotificationManager.IMPORTANCE_DEFAULT
        )
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private fun createNotificationChannel(
        channelName: String,
        @StringRes channelDescription: Int,
        importance: Int
    ) {
        val descriptionText: CharSequence = context.getString(channelDescription)
        val channel = NotificationChannel(channelName, descriptionText, importance)
        notificationManager?.createNotificationChannel(channel)
    }

    companion object {
        const val KEYSERVER_SYNC = "keyserverSync"
        const val PERMISSION_REQUESTS = "permissionRequests"
        const val PASSPHRASE_CACHE = "passphraseCache"
        const val ORBOT = "orbot"
        fun getInstance(context: Context): NotificationChannelManager {
            val notifyMan =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?
            return NotificationChannelManager(context.applicationContext, notifyMan)
        }
    }
}