package com.wolf.wallpaper

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.tabs.TabLayout
import com.wolf.wallpaper.core.ConfigManager

class WallpaperSettingsActivity : AppCompatActivity() {

    lateinit var configManager: ConfigManager
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        configManager = ConfigManager(this)

        val tabLayoutWeather = findViewById<TabLayout>(R.id.tabLayoutWeather)
        val initialActiveEffect = configManager.getActiveEffect()
        
        // Select initial tab and load fragment
        tabLayoutWeather.getTabAt(initialActiveEffect)?.select()
        loadSettingsFragment(initialActiveEffect)

        tabLayoutWeather.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val position = tab?.position ?: 0
                configManager.setActiveEffect(position)
                loadSettingsFragment(position)
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        val buttonApply = findViewById<Button>(R.id.buttonApplyWallpaper)
        buttonApply.setOnClickListener {
            val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                putExtra(
                    WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                    ComponentName(this@WallpaperSettingsActivity, DynamicWallpaperService::class.java)
                )
            }
            startActivity(intent)
        }
    }

    private fun loadSettingsFragment(position: Int) {
        val fragment: Fragment = if (position == 0) {
            StormSettingsFragment()
        } else {
            SunnySettingsFragment()
        }
        supportFragmentManager.beginTransaction()
            .replace(R.id.settings_fragment_container, fragment)
            .commit()
    }
}
