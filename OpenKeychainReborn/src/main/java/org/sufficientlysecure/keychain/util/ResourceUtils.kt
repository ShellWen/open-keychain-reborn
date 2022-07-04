package org.sufficientlysecure.keychain.util

import android.content.Context
import android.graphics.Bitmap
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap

object ResourceUtils {
    @JvmStatic
    fun getDrawableAsNotificationBitmap(context: Context, @DrawableRes iconRes: Int): Bitmap {
        val iconDrawable = ContextCompat.getDrawable(context, iconRes)
            ?: throw RuntimeException("drawable $iconRes not found")
        context.resources.let {
            val width = it.getDimensionPixelSize(android.R.dimen.notification_large_icon_width)
            val height = it.getDimensionPixelSize(android.R.dimen.notification_large_icon_height)
            return iconDrawable.toBitmap(width, height)
        }
    }
}