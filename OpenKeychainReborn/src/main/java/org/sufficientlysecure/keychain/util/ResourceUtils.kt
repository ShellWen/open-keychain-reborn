package org.sufficientlysecure.keychain.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap

object ResourceUtils {
    @JvmStatic
    fun getDrawableAsNotificationBitmap(context: Context, @DrawableRes iconRes: Int): Bitmap {
        val iconDrawable = ContextCompat.getDrawable(context, iconRes)?: 
        iconDrawable.toBitmap()
        val resources = context.resources
        val largeIconWidth = resources.getDimensionPixelSize(android.R.dimen.notification_large_icon_width)
        val largeIconHeight =
            resources.getDimensionPixelSize(android.R.dimen.notification_large_icon_height)
        val b = Bitmap.createBitmap(largeIconWidth, largeIconHeight, Bitmap.Config.ARGB_8888)
        val c = Canvas(b)
        iconDrawable.setBounds(0, 0, largeIconWidth, largeIconHeight)
        iconDrawable.draw(c)
        return b
    }
}