/*
 * MaxLock, an Xposed applock module for Android
 * Copyright (C) 2014-2016 Max Rumpf alias Maxr1998
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.Maxr1998.xposed.maxlock.ui

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.core.content.edit
import de.Maxr1998.xposed.maxlock.Common
import de.Maxr1998.xposed.maxlock.util.MLPreferences
import de.Maxr1998.xposed.maxlock.util.Util
import kotlinx.coroutines.experimental.async
import org.apache.commons.io.FileUtils
import java.io.File
import java.io.IOException
import java.util.*

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    init {
        async {
            val prefs = MLPreferences.getPreferences(application)
            application.packageManager.setComponentEnabledSetting(
                    ComponentName(Common.MAXLOCK_PACKAGE_NAME, NewAppInstalledBroadcastReceiver::class.java.name),
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) PackageManager.COMPONENT_ENABLED_STATE_DISABLED else PackageManager.COMPONENT_ENABLED_STATE_DEFAULT, 0)

            prefs.edit().putInt(Common.RATING_DIALOG_APP_OPENING_COUNTER, prefs.getInt(Common.RATING_DIALOG_APP_OPENING_COUNTER, 0) + 1).apply()

            // Non-pro setup
            if (!prefs.getBoolean(Common.ENABLE_PRO, false)) {
                prefs.edit {
                    putBoolean(Common.ENABLE_LOGGING, false)
                    putBoolean(Common.ENABLE_IMOD_DELAY_APP, false)
                    putBoolean(Common.ENABLE_IMOD_DELAY_GLOBAL, false)
                }
            }

            // Default activity customizations
            val prefsApps = MLPreferences.getPrefsApps(application)
            val defaultCustomisations = arrayOf(
                    "com.instagram.mainactivity.MainTabActivity",
                    "com.laurencedawson.reddit_sync.ui.activities.HomeActivity",
                    "com.whatsapp.Main"
            )
            prefsApps.edit {
                for (i in defaultCustomisations.indices) {
                    if (!prefsApps.contains(defaultCustomisations[i]))
                        putBoolean(defaultCustomisations[i], false)
                }
            }

            // Clean up
            val backgroundFolder = File(Util.dataDir(application), "background")
            if (backgroundFolder.exists()) {
                try {
                    FileUtils.copyFile(File(backgroundFolder, "image"), application.openFileOutput("background", 0))
                    FileUtils.deleteQuietly(backgroundFolder)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }

            val filesToDelete = ArrayList<File>()
            File(Util.dataDir(application), "shared_prefs").listFiles { _, filename ->
                !Arrays.asList("com.google.android.gms.analytics.prefs.xml",
                        xml(Common.MAXLOCK_PACKAGE_NAME + "_preferences"),
                        xml(Common.PREFS_KEY), xml(Common.PREFS_APPS), xml(Common.MAXLOCK_PACKAGE_NAME), xml(Common.PREFS_KEYS_PER_APP),
                        xml("WebViewChromiumPrefs")).contains(filename)
            }?.let { filesToDelete.addAll(Arrays.asList(*it)) }
            application.filesDir.listFiles { _, filename ->
                !Arrays.asList("background", "gaClientId", "gaClientIdData").contains(filename)
            }?.let { filesToDelete.addAll(Arrays.asList(*it)) }
            File(Common.EXTERNAL_FILES_DIR).listFiles { _, filename ->
                !Arrays.asList("Backup", "dev_mode.key").contains(filename)
            }?.let { filesToDelete.addAll(Arrays.asList(*it)) }
            for (f in filesToDelete) {
                FileUtils.deleteQuietly(f)
            }
            FileUtils.deleteQuietly(File(Environment.getExternalStorageDirectory().toString() + "/MaxLock_Backup/"))
            prefs.edit().putBoolean(Common.FIRST_START, false).apply()
            Log.i(Util.LOG_TAG_STARTUP, "Finished!")
        }
    }

    private fun xml(name: String): String = "$name.xml"
}