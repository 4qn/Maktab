package com.edu.maktab

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.edu.maktab.helper.AppConstant
import com.edu.maktab.helper.PreferenceHelper
import com.edu.maktab.helper.PreferenceHelper.set
import com.edu.maktab.manager.LocaleManager
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import android.os.StrictMode
import android.os.StrictMode.VmPolicy


class MainActivity : AppCompatActivity() {
    private lateinit var remoteConfig: FirebaseRemoteConfig
    var toolBar: Toolbar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LocaleManager.setNewLocale(this, LocaleManager.LANGUAGE_URDU)
        setContentView(R.layout.activity_main)
        toolBar = findViewById(R.id.toolbar)
        toolBar?.title = getString(R.string.app_header)
        setSupportActionBar(toolBar)
        remoteConfig = Firebase.remoteConfig
        val remoteConfigSetting = remoteConfigSettings {
            minimumFetchIntervalInSeconds = 3600
        }
        remoteConfig.setConfigSettingsAsync(remoteConfigSetting)
        val builder: VmPolicy.Builder = VmPolicy.Builder()
        StrictMode.setVmPolicy(builder.build())
    }

    /*  override fun onCreateOptionsMenu(menu: Menu): Boolean {
          // Inflate the menu; this adds items to the action bar if it is present.
          menuInflater.inflate(R.menu.menu_main, menu)
          return true
      }*/

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LocaleManager.setLocale(base))
    }

    private fun persistLanguage(c: Context, language: String) {
        val prefs = PreferenceHelper.defaultPrefs(c)
        prefs[AppConstant.LANGUAGE_KEY] = language
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        LocaleManager.setLocale(this)
    }
}