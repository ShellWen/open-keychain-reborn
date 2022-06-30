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

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import com.mikepenz.iconics.typeface.library.fontawesome.FontAwesome
import com.mikepenz.iconics.typeface.library.googlematerial.GoogleMaterial
import com.mikepenz.materialdrawer.iconics.iconicsIcon
import com.mikepenz.materialdrawer.model.DividerDrawerItem
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem
import com.mikepenz.materialdrawer.model.interfaces.nameRes
import com.mikepenz.materialdrawer.widget.MaterialDrawerSliderView
import com.shellwen.keychainreborn.R
import org.sufficientlysecure.keychain.operations.results.OperationResult
import org.sufficientlysecure.keychain.remote.ui.AppsListFragment
import org.sufficientlysecure.keychain.ui.base.BaseSecurityTokenActivity
import org.sufficientlysecure.keychain.util.FabContainer
import org.sufficientlysecure.keychain.util.Preferences

class MainActivity : BaseSecurityTokenActivity(), FabContainer {
    lateinit var mSlider: MaterialDrawerSliderView
    lateinit var mDrawer: DrawerLayout
    var actionBarDrawerToggle: ActionBarDrawerToggle? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        mToolbar = findViewById(R.id.toolbar)
        setSupportActionBar(mToolbar)
        mToolbar.setTitle(R.string.app_name)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        mDrawer = findViewById(R.id.drawer)
        mSlider = findViewById(R.id.slider)
        mSlider.apply {
            headerView = LayoutInflater.from(this@MainActivity)
                .inflate(R.layout.main_drawer_header, this, false)
            itemAdapter.add(PrimaryDrawerItem().apply {
                nameRes = R.string.nav_keys
                iconicsIcon = CommunityMaterial.Icon2.cmd_key
                identifier = ID_KEYS
                isSelectable = false
            }, PrimaryDrawerItem().apply {
                nameRes = R.string.nav_encrypt_decrypt
                iconicsIcon = FontAwesome.Icon.faw_lock
                identifier = ID_ENCRYPT_DECRYPT
                isSelectable = false
            }, PrimaryDrawerItem().apply {
                nameRes = R.string.title_api_registered_apps
                iconicsIcon = CommunityMaterial.Icon.cmd_apps
                identifier = ID_APPS
                isSelectable = false
            }, PrimaryDrawerItem().apply {
                nameRes = R.string.nav_backup
                iconicsIcon = CommunityMaterial.Icon.cmd_backup_restore
                identifier = ID_BACKUP
                isSelectable = false
            }, DividerDrawerItem(), PrimaryDrawerItem().apply {
                nameRes = R.string.menu_preferences
                iconicsIcon = GoogleMaterial.Icon.gmd_settings
                identifier = ID_SETTINGS
                isSelectable = false
            }, PrimaryDrawerItem().apply {
                nameRes = R.string.menu_help
                iconicsIcon = CommunityMaterial.Icon2.cmd_help_circle
                identifier = ID_HELP
                isSelectable = false
            })
            onDrawerItemClickListener = { _, drawerItem, _ ->
                var intent: Intent? = null
                when (drawerItem.identifier) {
                    ID_KEYS -> onKeysSelected()
                    ID_ENCRYPT_DECRYPT -> onEnDecryptSelected()
                    ID_APPS -> onAppsSelected()
                    ID_BACKUP -> onBackupSelected()
                    ID_SETTINGS -> intent = Intent(
                        this@MainActivity, SettingsActivity::class.java
                    )
                    ID_HELP -> intent = Intent(
                        this@MainActivity, HelpActivity::class.java
                    )
                }
                if (intent != null) {
                    this@MainActivity.startActivity(intent)
                }
                false
            }
            setSelectionAtPosition(-1)
        }

        actionBarDrawerToggle = ActionBarDrawerToggle(
            this,
            mDrawer,
            mToolbar,
            R.string.drawer_open,
            R.string.drawer_close
        )

        mDrawer.addDrawerListener(actionBarDrawerToggle!!)

        // if this is the first time show first time activity
        val prefs = Preferences.getPreferences(this)
        if (!intent.getBooleanExtra(EXTRA_SKIP_FIRST_TIME, false) && prefs.isFirstTime) {
            val intent = Intent(this, CreateKeyActivity::class.java)
            intent.putExtra(CreateKeyActivity.EXTRA_FIRST_TIME, true)
            startActivity(intent)
            finish()
            return
        }

        // all further initialization steps are saved as instance state
        if (savedInstanceState != null) {
            return
        }
        val data = intent
        // If we got an EXTRA_RESULT in the intent, show the notification
        if (data != null && data.hasExtra(OperationResult.EXTRA_RESULT)) {
            val result = data.getParcelableExtra<OperationResult>(OperationResult.EXTRA_RESULT)
            result!!.createNotify(this).show()
        }
        onKeysSelected()
        if (data != null && data.hasExtra(EXTRA_INIT_FRAG)) {
            // initialize FragmentLayout with KeyListFragment at first
            when (data.getIntExtra(EXTRA_INIT_FRAG, -1)) {
                ID_ENCRYPT_DECRYPT.toInt() -> onEnDecryptSelected()
                ID_APPS.toInt() -> onAppsSelected()
            }
        }
    }

    override fun onNewIntent(data: Intent) {
        super.onNewIntent(data)
        intent = data
        if (data != null && data.hasExtra(EXTRA_INIT_FRAG)) {
            // initialize FragmentLayout with KeyListFragment at first
            when (data.getIntExtra(EXTRA_INIT_FRAG, -1)) {
                ID_ENCRYPT_DECRYPT.toInt() -> onEnDecryptSelected()
                ID_APPS.toInt() -> onAppsSelected()
            }
        }
    }

    private fun setFragment(frag: Fragment) {
        val fragmentManager = supportFragmentManager
        val ft = fragmentManager.beginTransaction()
        ft.replace(R.id.main_fragment_container, frag)
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
        ft.commit()
    }

    fun onKeysSelected() {
        mToolbar.setTitle(R.string.app_name)
        mSlider.setSelection(ID_KEYS, false)
        val frag: Fragment = KeyListFragment()
        setFragment(frag)
    }

    private fun onEnDecryptSelected() {
        mToolbar.setTitle(R.string.nav_encrypt_decrypt)
        mSlider.setSelection(ID_ENCRYPT_DECRYPT, false)
        val frag: Fragment = EncryptDecryptFragment()
        setFragment(frag)
    }

    private fun onAppsSelected() {
        mToolbar.setTitle(R.string.nav_apps)
        mSlider.setSelection(ID_APPS, false)
        val frag: Fragment = AppsListFragment()
        setFragment(frag)
    }

    private fun onBackupSelected() {
        mToolbar.setTitle(R.string.nav_backup)
        mSlider.setSelection(ID_BACKUP, false)
        val frag: Fragment = BackupRestoreFragment()
        setFragment(frag)
    }

    override fun onBackPressed() {
        // close the drawer first and if the drawer is closed do regular backstack handling
        if (mDrawer.isDrawerOpen(mSlider)) {
            mDrawer.closeDrawer(mSlider)
        } else {
            val fragmentManager = supportFragmentManager
            if (fragmentManager.findFragmentById(R.id.main_fragment_container) is KeyListFragment) {
                super.onBackPressed()
            } else {
                onKeysSelected()
            }
        }
    }

    override fun fabMoveUp(height: Int) {
        val fragment: Any? = supportFragmentManager.findFragmentById(R.id.main_fragment_container)
        if (fragment is FabContainer) {
            fragment.fabMoveUp(height)
        }
    }

    override fun fabRestorePosition() {
        val fragment: Any? = supportFragmentManager.findFragmentById(R.id.main_fragment_container)
        if (fragment is FabContainer) {
            fragment.fabRestorePosition()
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        actionBarDrawerToggle?.syncState()
    }

    companion object {
        const val ID_KEYS = 1L
        const val ID_ENCRYPT_DECRYPT = 2L
        const val ID_APPS = 3L
        const val ID_BACKUP = 4L
        const val ID_SETTINGS = 6L
        const val ID_HELP = 7L

        // both of these are used for instrumentation testing only
        const val EXTRA_SKIP_FIRST_TIME = "skip_first_time"
        const val EXTRA_INIT_FRAG = "init_frag"
    }
}