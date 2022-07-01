package org.sufficientlysecure.keychain.ui.keyview.loader

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.AsyncTask
import android.os.Handler
import android.os.Looper
import androidx.core.os.CancellationSignal
import androidx.core.os.OperationCanceledException
import androidx.lifecycle.LiveData

abstract class AsyncTaskLiveData<T> protected constructor(
    val context: Context,
    private val observedUri: Uri?
) : LiveData<T>() {
    private val observer: ForceLoadContentObserver
    private var cancellationSignal: CancellationSignal? = null

    init {
        observer = ForceLoadContentObserver()
    }

    protected abstract fun asyncLoadData(): T
    protected fun updateDataInBackground() {
        object : AsyncTask<Void?, Void?, T?>() {
            protected override fun doInBackground(vararg params: Void?): T? {
                return try {
                    synchronized(this@AsyncTaskLiveData) {
                        cancellationSignal = CancellationSignal()
                    }
                    try {
                        asyncLoadData()
                    } finally {
                        synchronized(this@AsyncTaskLiveData) { cancellationSignal = null }
                    }
                } catch (e: OperationCanceledException) {
                    if (hasActiveObservers()) {
                        throw e
                    }
                    null
                }
            }

            override fun onPostExecute(value: T?) {
                setValue(value)
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }

    override fun onActive() {
        val value = value
        if (value == null) {
            updateDataInBackground()
        }
        if (observedUri != null) {
            context.contentResolver.registerContentObserver(observedUri, true, observer)
        }
    }

    override fun onInactive() {
        synchronized(this@AsyncTaskLiveData) {
            if (cancellationSignal != null) {
                cancellationSignal!!.cancel()
            }
        }
        if (observedUri != null) {
            context.contentResolver.registerContentObserver(observedUri, true, observer)
        }
    }

    inner class ForceLoadContentObserver internal constructor() : ContentObserver(Handler(Looper.myLooper()!!)) {
        override fun deliverSelfNotifications(): Boolean {
            return true
        }

        override fun onChange(selfChange: Boolean) {
            updateDataInBackground()
        }
    }
}