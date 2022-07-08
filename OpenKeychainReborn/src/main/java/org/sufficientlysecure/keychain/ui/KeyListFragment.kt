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
package org.sufficientlysecure.keychain.ui

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.Button
import android.widget.ViewAnimator
import androidx.annotation.WorkerThread
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.getbase.floatingactionbutton.FloatingActionButton
import com.getbase.floatingactionbutton.FloatingActionsMenu
import com.shellwen.keychainreborn.R
import eu.davidea.fastscroller.FastScroller
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.SelectableAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.sufficientlysecure.keychain.Constants.DEBUG
import org.sufficientlysecure.keychain.KeychainDatabase
import org.sufficientlysecure.keychain.compatibility.ClipboardReflection.getClipboardText
import org.sufficientlysecure.keychain.daos.DatabaseNotifyManager
import org.sufficientlysecure.keychain.daos.KeyRepository
import org.sufficientlysecure.keychain.keysync.KeyserverSyncManager
import org.sufficientlysecure.keychain.livedata.GenericLiveData.GenericDataLoader
import org.sufficientlysecure.keychain.operations.KeySyncParcel
import org.sufficientlysecure.keychain.operations.results.BenchmarkResult
import org.sufficientlysecure.keychain.operations.results.ImportKeyResult
import org.sufficientlysecure.keychain.operations.results.OperationResult
import org.sufficientlysecure.keychain.pgp.PgpHelper
import org.sufficientlysecure.keychain.service.BenchmarkInputParcel
import org.sufficientlysecure.keychain.ui.adapter.*
import org.sufficientlysecure.keychain.ui.adapter.FlexibleKeyItem.FlexibleSectionableKeyItem
import org.sufficientlysecure.keychain.ui.base.CryptoOperationHelper
import org.sufficientlysecure.keychain.ui.base.RecyclerFragment
import org.sufficientlysecure.keychain.ui.keyview.GenericViewModel
import org.sufficientlysecure.keychain.ui.keyview.ViewKeyActivity
import org.sufficientlysecure.keychain.ui.util.Notify
import org.sufficientlysecure.keychain.util.FabContainer
import org.sufficientlysecure.keychain.util.Preferences
import timber.log.Timber
import java.io.IOException
import kotlin.time.Duration.Companion.seconds

class KeyListFragment : RecyclerFragment<FlexibleAdapter<FlexibleKeyItem<*>?>?>(),
    SearchView.OnQueryTextListener, FlexibleAdapter.OnItemClickListener,
    FlexibleAdapter.OnItemLongClickListener, FabContainer {
    private var mActionMode: ActionMode? = null
    lateinit var vSearchButton: Button
    lateinit var vSearchContainer: ViewAnimator
    lateinit var mFab: FloatingActionsMenu
    private var keyRepository: KeyRepository? = null
    private var flexibleKeyItemFactory: FlexibleKeyItemFactory? = null
    private var queuedHighlightMasterKeyId: Long? = null
    private val mActionCallback: ActionMode.Callback = object : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            mode.menuInflater.inflate(R.menu.key_list_multi, menu)
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
            return false
        }

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            when (item.itemId) {
                R.id.menu_key_list_multi_encrypt -> {
                    val keyIds = selectedMasterKeyIds
                    multiSelectEncrypt(keyIds)
                    mode.finish()
                }
                R.id.menu_key_list_multi_delete -> {
                    val keyIds = selectedMasterKeyIds
                    val hasSecret = isAnySecretKeySelected
                    multiSelectDelete(keyIds, hasSecret)
                    mode.finish()
                }
            }
            return false
        }

        override fun onDestroyActionMode(mode: ActionMode) {
            mActionMode = null
            if (adapter != null) {
                adapter!!.clearSelection()
            }
        }
    }
    private var fastScroller: FastScroller? = null
    private fun multiSelectDelete(keyIds: LongArray, hasSecret: Boolean) {
        val intent = Intent(activity, DeleteKeyDialogActivity::class.java)
        intent.putExtra(DeleteKeyDialogActivity.EXTRA_DELETE_MASTER_KEY_IDS, keyIds)
        intent.putExtra(DeleteKeyDialogActivity.EXTRA_HAS_SECRET, hasSecret)
        if (hasSecret) {
            intent.putExtra(
                DeleteKeyDialogActivity.EXTRA_KEYSERVER,
                Preferences.getPreferences(activity).preferredKeyserver
            )
        }
        startActivityForResult(intent, REQUEST_DELETE)
    }

    private fun multiSelectEncrypt(keyIds: LongArray) {
        val intent = Intent(activity, EncryptFilesActivity::class.java)
        intent.action = EncryptFilesActivity.ACTION_ENCRYPT_DATA
        intent.putExtra(EncryptFilesActivity.EXTRA_ENCRYPTION_KEY_IDS, keyIds)
        startActivityForResult(intent, REQUEST_ACTION)
    }

    private val selectedMasterKeyIds: LongArray
        get() {
            val selectedPositions = adapter!!.selectedPositions
            val keyIds = LongArray(selectedPositions.size)
            for (i in selectedPositions.indices) {
                val selectedItem =
                    adapter!!.getItem(selectedPositions[i], FlexibleKeyDetailsItem::class.java)
                if (selectedItem != null) {
                    keyIds[i] = selectedItem.keyInfo.master_key_id()
                }
            }
            return keyIds
        }
    private val isAnySecretKeySelected: Boolean
        get() {
            for (position in adapter!!.selectedPositions) {
                val item = adapter!!.getItem(position, FlexibleKeyDetailsItem::class.java)
                if (item != null && item.keyInfo.has_any_secret()) {
                    return true
                }
            }
            return false
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.key_list_fragment, container, false)
        mFab = view.findViewById(R.id.fab_main)
        view.findViewById<FloatingActionButton>(R.id.fab_add_qr_code).apply {
            setOnClickListener {
                mFab.collapse()
                scanQrCode()
            }
        }
        view.findViewById<FloatingActionButton>(R.id.fab_add_cloud).apply {
            setOnClickListener {
                mFab.collapse()
                searchCloud()
            }
        }
        view.findViewById<FloatingActionButton>(R.id.fab_add_file).apply {
            setOnClickListener {
                mFab.collapse()
                importFile()
            }
        }
        fastScroller = view.findViewById(R.id.fast_scroller)
        vSearchContainer = view.findViewById(R.id.search_container)
        vSearchButton = view.findViewById<Button>(R.id.search_button).apply {
            setOnClickListener { startSearchForQuery() }
        }
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lifecycleScope.launchWhenStarted {
            delay(0.5.seconds)
            checkClipboardForPublicKey()
        }
        // We have a menu item to show in action bar.
        setHasOptionsMenu(true)
        layoutManager = LinearLayoutManager(activity)
        keyRepository = KeyRepository.create(requireContext())
        flexibleKeyItemFactory = FlexibleKeyItemFactory(requireContext().resources)
        val intent = requireActivity().intent
        if (intent != null && intent.hasExtra(ImportKeyResult.EXTRA_RESULT)) {
            val importKeyResult =
                intent.getParcelableExtra<ImportKeyResult>(ImportKeyResult.EXTRA_RESULT)
            val importedMasterKeyIds = importKeyResult!!.importedMasterKeyIds
            if (importedMasterKeyIds != null && importedMasterKeyIds.isNotEmpty()) {
                queuedHighlightMasterKeyId = importedMasterKeyIds[0]
            }
        }
        val viewModel = ViewModelProvider(this)[GenericViewModel::class.java]
        val liveData = viewModel.getGenericLiveData(
            requireContext(),
            object : GenericDataLoader<List<FlexibleKeyItem<*>>> {
                override fun loadData(): List<FlexibleKeyItem<*>> {
                    return loadFlexibleKeyItems()
                }
            })
        liveData.observe(viewLifecycleOwner) { flexibleKeyItems: List<FlexibleKeyItem<*>?> ->
            onLoadKeyItems(
                flexibleKeyItems
            )
        }
    }

    @WorkerThread
    private fun loadFlexibleKeyItems(): List<FlexibleKeyItem<*>> {
        val unifiedKeyInfo = keyRepository!!.allUnifiedKeyInfo
        return flexibleKeyItemFactory!!.mapUnifiedKeyInfoToFlexibleKeyItems(unifiedKeyInfo)
    }

    private fun onLoadKeyItems(flexibleKeyItems: List<FlexibleKeyItem<*>?>) {
        var adapter = adapter
        if (adapter == null) {
            adapter = object : FlexibleAdapter<FlexibleKeyItem<*>?>(flexibleKeyItems, this, true) {
                override fun getItemId(position: Int): Long {
                    val item = getItem(position)
                    return if (item is FlexibleKeyDetailsItem) {
                        item.keyInfo.master_key_id()
                    } else super.getItemId(position)
                }
            }
            adapter.setDisplayHeadersAtStartUp(true)
            adapter.setStickyHeaders(true)
            adapter.setMode(SelectableAdapter.Mode.MULTI)
            setAdapter(adapter)
            adapter.setFastScroller(fastScroller)
            fastScroller!!.setBubbleTextCreator { position: Int -> getBubbleText(position) }
        } else {
            adapter.updateDataSet(flexibleKeyItems, true)
        }
        maybeHighlightKey(adapter)
    }

    private fun maybeHighlightKey(adapter: FlexibleAdapter<FlexibleKeyItem<*>?>) {
        if (queuedHighlightMasterKeyId == null) {
            return
        }
        for (position in 0 until adapter.itemCount) {
            if (adapter.getItemId(position) == queuedHighlightMasterKeyId) {
                adapter.smoothScrollToPosition(position)
            }
        }
        queuedHighlightMasterKeyId = null
    }

    private fun getBubbleText(position: Int): String {
        val item = adapter!!.getItem(position) ?: return ""
        if (item is FlexibleSectionableKeyItem<*>) {
            val header = item.header
            return header.sectionTitle
        }
        return if (item is FlexibleKeyHeader) {
            item.sectionTitle
        } else ""
    }

    private suspend fun checkClipboardForPublicKey() {
        withContext(Dispatchers.Main) {
            val clipboardText: CharSequence? = getClipboardText(activity)
            if (!clipboardText.isNullOrEmpty() && PgpHelper.getPgpPublicKeyContent(clipboardText) != null) {
                showClipboardDataSnackbar()
            }
        }
    }

    private fun showClipboardDataSnackbar() {
        val activity = activity ?: return
        Notify.create(
            activity,
            R.string.snack_keylist_clipboard_title,
            Notify.LENGTH_INDEFINITE,
            Notify.Style.OK,
            {
                val intentImportExisting = Intent(getActivity(), ImportKeysActivity::class.java)
                intentImportExisting.action = ImportKeysActivity.ACTION_IMPORT_KEY_FROM_CLIPBOARD
                startActivity(intentImportExisting)
            },
            R.string.snack_keylist_clipboard_action
        ).show(this)
    }

    private fun startSearchForQuery() {
        val activity = activity ?: return
        val searchIntent = Intent(activity, ImportKeysActivity::class.java)
        searchIntent.putExtra(
            ImportKeysActivity.EXTRA_QUERY, adapter!!.getFilter(
                String::class.java
            )
        )
        searchIntent.action = ImportKeysActivity.ACTION_IMPORT_KEY_FROM_KEYSERVER
        startActivity(searchIntent)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.key_list, menu)
        if (DEBUG) {
            menu.findItem(R.id.menu_key_list_debug_bench).isVisible = true
            menu.findItem(R.id.menu_key_list_debug_read).isVisible = true
            menu.findItem(R.id.menu_key_list_debug_write).isVisible = true
            menu.findItem(R.id.menu_key_list_debug_first_time).isVisible = true
            menu.findItem(R.id.menu_key_list_debug_bgsync).isVisible = true
        }

        // Get the searchview
        val searchItem = menu.findItem(R.id.menu_key_list_search)
        val searchView = searchItem.actionView as SearchView

        // Execute this when searching
        searchView.setOnQueryTextListener(this)

        // Erase search result without focus
        searchItem.setOnActionExpandListener(
            object : MenuItem.OnActionExpandListener {
                override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                    return true
                }

                override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                    adapter!!.setFilter(null)
                    adapter!!.filterItems()
                    return true
                }
            })
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onItemClick(view: View, position: Int): Boolean {
        val item = adapter!!.getItem(position) ?: return false
        if (item is FlexibleKeyDummyItem) {
            createKey()
            return false
        }
        if (item !is FlexibleKeyDetailsItem) {
            return false
        }
        if (mActionMode != null && position != RecyclerView.NO_POSITION) {
            toggleSelection(position)
            return true
        }
        val masterKeyId = item.keyInfo.master_key_id()
        val viewIntent = ViewKeyActivity.getViewKeyActivityIntent(requireActivity(), masterKeyId)
        startActivityForResult(viewIntent, REQUEST_VIEW_KEY)
        return false
    }

    override fun onItemLongClick(position: Int) {
        if (adapter!!.getItem(position) is FlexibleKeyDetailsItem) {
            if (mActionMode == null) {
                val activity = activity
                if (activity != null) {
                    mActionMode = activity.startActionMode(mActionCallback)
                }
            }
            toggleSelection(position)
        }
    }

    private fun toggleSelection(position: Int) {
        adapter!!.toggleSelection(position)
        val count = adapter!!.selectedItemCount
        if (count == 0) {
            mActionMode!!.finish()
        } else {
            setContextTitle(count)
        }
    }

    private fun setContextTitle(selectedCount: Int) {
        val keysSelected = resources.getQuantityString(
            R.plurals.key_list_selected_keys, selectedCount, selectedCount
        )
        mActionMode!!.title = keysSelected
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_key_list_create -> {
                createKey()
                true
            }
            R.id.menu_key_list_update_all_keys -> {
                updateAllKeys()
                true
            }
            R.id.menu_key_list_debug_read -> {
                try {
                    KeychainDatabase.debugBackup(requireActivity(), true)
                    Notify.create(activity, "Restored debug_backup.db", Notify.Style.OK).show()
                    DatabaseNotifyManager.create(requireContext()).notifyAllKeysChange()
                } catch (e: IOException) {
                    Timber.e(e, "IO Error")
                    Notify.create(activity, "IO Error " + e.message, Notify.Style.ERROR).show()
                }
                true
            }
            R.id.menu_key_list_debug_write -> {
                try {
                    KeychainDatabase.debugBackup(requireActivity(), false)
                    Notify.create(activity, "Backup to debug_backup.db completed", Notify.Style.OK)
                        .show()
                } catch (e: IOException) {
                    Timber.e(e, "IO Error")
                    Notify.create(activity, "IO Error: " + e.message, Notify.Style.ERROR).show()
                }
                true
            }
            R.id.menu_key_list_debug_first_time -> {
                val prefs = Preferences.getPreferences(activity)
                prefs.isFirstTime = true
                val intent = Intent(activity, CreateKeyActivity::class.java)
                intent.putExtra(CreateKeyActivity.EXTRA_FIRST_TIME, true)
                startActivity(intent)
                requireActivity().finish()
                true
            }
            R.id.menu_key_list_debug_bgsync -> {
                KeyserverSyncManager.debugRunSyncNow(requireContext())
                true
            }
            R.id.menu_key_list_debug_bench -> {
                benchmark()
                true
            }
            else -> {
                super.onOptionsItemSelected(item)
            }
        }
    }

    override fun onQueryTextSubmit(s: String): Boolean {
        return true
    }

    override fun onQueryTextChange(searchText: String): Boolean {
        adapter!!.setFilter(searchText)
        adapter!!.filterItems(300)
        if (searchText.length > 2) {
            vSearchButton.text = getString(R.string.btn_search_for_query, searchText)
            vSearchContainer.displayedChild = 1
            vSearchContainer.visibility = View.VISIBLE
        } else {
            vSearchContainer.displayedChild = 0
            vSearchContainer.visibility = View.GONE
        }
        return true
    }

    private fun searchCloud() {
        val importIntent = Intent(activity, ImportKeysActivity::class.java)
        importIntent.putExtra(
            ImportKeysActivity.EXTRA_QUERY, null as String?
        ) // hack to show only cloud tab
        startActivity(importIntent)
    }

    private fun scanQrCode() {
        val scanQrCode = Intent(activity, ImportKeysProxyActivity::class.java)
        scanQrCode.action = ImportKeysProxyActivity.ACTION_SCAN_IMPORT
        startActivityForResult(scanQrCode, REQUEST_ACTION)
    }

    private fun importFile() {
        val intentImportExisting = Intent(activity, ImportKeysActivity::class.java)
        intentImportExisting.action = ImportKeysActivity.ACTION_IMPORT_KEY_FROM_FILE_AND_RETURN
        startActivityForResult(intentImportExisting, REQUEST_ACTION)
    }

    private fun createKey() {
        val intent = Intent(activity, CreateKeyActivity::class.java)
        startActivityForResult(intent, REQUEST_ACTION)
    }

    private fun updateAllKeys() {
        val callback: CryptoOperationHelper.Callback<KeySyncParcel, ImportKeyResult> =
            object : CryptoOperationHelper.Callback<KeySyncParcel, ImportKeyResult> {
                override fun createOperationInput(): KeySyncParcel {
                    return KeySyncParcel.createRefreshAll()
                }

                override fun onCryptoOperationSuccess(result: ImportKeyResult) {
                    result.createNotify(activity).show()
                }

                override fun onCryptoOperationCancelled() {}
                override fun onCryptoOperationError(result: ImportKeyResult) {
                    result.createNotify(activity).show()
                }

                override fun onCryptoSetProgress(msg: String, progress: Int, max: Int): Boolean {
                    return false
                }
            }
        val opHelper: CryptoOperationHelper<*, *> =
            CryptoOperationHelper(3, this, callback, R.string.progress_importing)
        opHelper.setProgressCancellable(true)
        opHelper.cryptoOperation()
    }

    private fun benchmark() {
        val callback: CryptoOperationHelper.Callback<BenchmarkInputParcel, BenchmarkResult> =
            object : CryptoOperationHelper.Callback<BenchmarkInputParcel, BenchmarkResult> {
                override fun createOperationInput(): BenchmarkInputParcel {
                    return BenchmarkInputParcel.newInstance() // we want to perform a full consolidate
                }

                override fun onCryptoOperationSuccess(result: BenchmarkResult) {
                    result.createNotify(activity).show()
                }

                override fun onCryptoOperationCancelled() {}
                override fun onCryptoOperationError(result: BenchmarkResult) {
                    result.createNotify(activity).show()
                }

                override fun onCryptoSetProgress(msg: String, progress: Int, max: Int): Boolean {
                    return false
                }
            }
        val opHelper: CryptoOperationHelper<*, *> =
            CryptoOperationHelper(2, this, callback, R.string.progress_importing)
        opHelper.cryptoOperation()
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQUEST_DELETE -> {
                if (mActionMode != null) {
                    mActionMode!!.finish()
                }
                if (data != null && data.hasExtra(OperationResult.EXTRA_RESULT)) {
                    val result =
                        data.getParcelableExtra<OperationResult>(OperationResult.EXTRA_RESULT)
                    result!!.createNotify(activity).show()
                } else {
                    super.onActivityResult(requestCode, resultCode, data)
                }
            }
            REQUEST_ACTION -> {

                // if a result has been returned, display a notify
                if (data != null && data.hasExtra(OperationResult.EXTRA_RESULT)) {
                    val result =
                        data.getParcelableExtra<OperationResult>(OperationResult.EXTRA_RESULT)
                    result!!.createNotify(activity).show()
                } else {
                    super.onActivityResult(requestCode, resultCode, data)
                }
            }
            REQUEST_VIEW_KEY -> {
                if (data != null && data.hasExtra(OperationResult.EXTRA_RESULT)) {
                    val result =
                        data.getParcelableExtra<OperationResult>(OperationResult.EXTRA_RESULT)
                    result!!.createNotify(activity).show()
                } else {
                    super.onActivityResult(requestCode, resultCode, data)
                }
            }
        }
    }

    override fun fabMoveUp(height: Int) {
        ObjectAnimator.ofFloat(mFab, "translationY", 0f, -height.toFloat()).apply {
            // we're a little behind, so skip 1/10 of the time
            duration = 270
            start()
        }
    }

    override fun fabRestorePosition() {
        ObjectAnimator.ofFloat(mFab, "translationY", 0f).apply {
            // we're a little ahead, so wait a few ms
            startDelay = 70
            duration = 300
            start()
        }
    }

    companion object {
        private const val REQUEST_ACTION = 1
        private const val REQUEST_DELETE = 2
        private const val REQUEST_VIEW_KEY = 3
    }
}