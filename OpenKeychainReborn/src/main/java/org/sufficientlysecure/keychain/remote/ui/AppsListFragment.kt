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
package org.sufficientlysecure.keychain.remote.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.shellwen.keychainreborn.R
import org.sufficientlysecure.keychain.ui.base.RecyclerFragment
import org.sufficientlysecure.keychain.remote.ui.AppsListFragment.ApiAppAdapter
import org.sufficientlysecure.keychain.livedata.ApiAppsLiveData.ListedApp
import timber.log.Timber
import org.sufficientlysecure.keychain.livedata.ApiAppsLiveData

class AppsListFragment : RecyclerFragment<ApiAppAdapter?>() {
    private lateinit var mAdapter: ApiAppAdapter

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setHasOptionsMenu(true)
        mAdapter = ApiAppAdapter(requireActivity())
        adapter = mAdapter
        layoutManager = LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
        val viewModel = ViewModelProvider(this)[ApiAppsViewModel::class.java]
        viewModel.getListedAppLiveData(requireContext())
            .observe(viewLifecycleOwner) { apiApps: List<ListedApp>? -> onLoad(apiApps) }
    }

    private fun onLoad(apiApps: List<ListedApp>?) {
        if (apiApps == null) {
            hideList(false)
            mAdapter.`data` = null
            return
        }
        mAdapter.`data` = apiApps
        showList(true)
    }

    fun onItemClick(position: Int) {
        val listedApp = mAdapter.`data`!![position]
        if (listedApp.isInstalled) {
            if (listedApp.isRegistered) {
                // Edit app settings
                val intent = Intent(activity, AppSettingsActivity::class.java)
                intent.putExtra(AppSettingsActivity.EXTRA_PACKAGE_NAME, listedApp.packageName)
                startActivity(intent)
            } else {
                val i: Intent?
                val manager = requireActivity().packageManager
                try {
                    i = manager.getLaunchIntentForPackage(listedApp.packageName)
                    if (i == null) {
                        throw PackageManager.NameNotFoundException()
                    }
                    // Start like the Android launcher would do
                    i.flags =
                        Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                    i.addCategory(Intent.CATEGORY_LAUNCHER)
                    startActivity(i)
                } catch (e: PackageManager.NameNotFoundException) {
                    Timber.e(e, "startApp")
                }
            }
        } else {
            try {
                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("market://details?id=" + listedApp.packageName)
                    )
                )
            } catch (e: ActivityNotFoundException) {
                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/apps/details?id=" + listedApp.packageName)
                    )
                )
            }
        }
    }

    inner class ApiAppAdapter internal constructor(context: Context) :
        RecyclerView.Adapter<ApiAppViewHolder>() {
        private val inflater: LayoutInflater
        var `data`: List<ListedApp>? = null
            set(newData) {
                field = newData
                notifyDataSetChanged()
            }

        init {
            inflater = LayoutInflater.from(context)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ApiAppViewHolder {
            return ApiAppViewHolder(
                inflater.inflate(
                    R.layout.api_apps_adapter_list_item,
                    parent,
                    false
                )
            )
        }

        override fun onBindViewHolder(holder: ApiAppViewHolder, position: Int) {
            val item = `data`!![position]
            holder.bind(item)
        }

        override fun getItemCount(): Int {
            return `data`?.size ?: 0
        }
    }

    inner class ApiAppViewHolder internal constructor(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        private val text: TextView
        private val icon: ImageView
        private val installIcon: ImageView

        init {
            text = itemView.findViewById(R.id.api_apps_adapter_item_name)
            icon = itemView.findViewById(R.id.api_apps_adapter_item_icon)
            installIcon = itemView.findViewById(R.id.api_apps_adapter_install_icon)
            itemView.setOnClickListener { onItemClick(bindingAdapterPosition) }
        }

        fun bind(listedApp: ListedApp) {
            text.text = listedApp.readableName
            if (listedApp.applicationIconRes != null) {
                icon.setImageResource(listedApp.applicationIconRes)
            } else {
                icon.setImageDrawable(listedApp.applicationIcon)
            }
            installIcon.visibility =
                if (listedApp.isInstalled) View.GONE else View.VISIBLE
        }
    }

    class ApiAppsViewModel : ViewModel() {
        lateinit var listedAppLiveData: LiveData<List<ListedApp>>
        fun getListedAppLiveData(context: Context): LiveData<List<ListedApp>> {
            if (!::listedAppLiveData.isInitialized) {
                listedAppLiveData = ApiAppsLiveData(context)
            }
            return listedAppLiveData
        }
    }
}