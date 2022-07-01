package org.sufficientlysecure.keychain.livedata

import android.content.Context
import android.net.Uri
import android.os.SystemClock
import org.sufficientlysecure.keychain.ui.keyview.loader.AsyncTaskLiveData
import org.sufficientlysecure.keychain.livedata.GenericLiveData.GenericDataLoader
import org.sufficientlysecure.keychain.daos.DatabaseNotifyManager

class GenericLiveData<T> : AsyncTaskLiveData<T> {
    private var genericDataLoader: GenericDataLoader<T>
    private var minLoadTime: Long? = null

    constructor(context: Context, genericDataLoader: GenericDataLoader<T>) : super(
        context, null
    ) {
        this.genericDataLoader = genericDataLoader
    }

    constructor(
        context: Context,
        notifyUri: Uri?,
        genericDataLoader: GenericDataLoader<T>
    ) : super(
        context, notifyUri
    ) {
        this.genericDataLoader = genericDataLoader
    }

    constructor(
        context: Context,
        notifyMasterKeyId: Long,
        genericDataLoader: GenericDataLoader<T>
    ) : super(
        context, DatabaseNotifyManager.getNotifyUriMasterKeyId(notifyMasterKeyId)
    ) {
        this.genericDataLoader = genericDataLoader
    }

    fun setMinLoadTime(minLoadTime: Long) {
        this.minLoadTime = minLoadTime
    }

    override fun asyncLoadData(): T {
        val startTime = SystemClock.elapsedRealtime()
        val result = genericDataLoader.loadData()
        try {
            val elapsedTime = SystemClock.elapsedRealtime() - startTime
            if (minLoadTime != null && elapsedTime < minLoadTime!!) {
                Thread.sleep(minLoadTime!! - elapsedTime)
            }
        } catch (e: InterruptedException) {
            // nvm
        }
        return result
    }

    interface GenericDataLoader<T> {
        fun loadData(): T
    }
}