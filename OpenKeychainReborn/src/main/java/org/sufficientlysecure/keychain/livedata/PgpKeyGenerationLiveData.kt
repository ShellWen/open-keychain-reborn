package org.sufficientlysecure.keychain.livedata

import android.content.Context
import org.sufficientlysecure.keychain.ui.keyview.loader.AsyncTaskLiveData
import org.sufficientlysecure.keychain.operations.results.PgpEditKeyResult
import org.sufficientlysecure.keychain.service.SaveKeyringParcel
import org.sufficientlysecure.keychain.pgp.PgpKeyOperation
import org.sufficientlysecure.keychain.util.ProgressScaler

class PgpKeyGenerationLiveData(context: Context) : AsyncTaskLiveData<PgpEditKeyResult?>(
    context, null
) {
    private var saveKeyringParcel: SaveKeyringParcel? = null
    fun setSaveKeyringParcel(saveKeyringParcel: SaveKeyringParcel) {
        if (this.saveKeyringParcel === saveKeyringParcel) {
            return
        }
        this.saveKeyringParcel = saveKeyringParcel
        updateDataInBackground()
    }

    override fun asyncLoadData(): PgpEditKeyResult? {
        if (saveKeyringParcel == null) {
            return null
        }
        val keyOperations = PgpKeyOperation(ProgressScaler())
        return keyOperations.createSecretKeyRing(saveKeyringParcel)
    }
}