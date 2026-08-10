package com.wolf.wallpaper

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.card.MaterialCardView
import com.google.android.material.tabs.TabLayout
import com.wolf.wallpaper.core.ConfigManager

class WallpaperSettingsActivity : AppCompatActivity() {

    lateinit var configManager: ConfigManager
        private set

    private lateinit var themePreference: SettingsThemePreference

    override fun onCreate(savedInstanceState: Bundle?) {
        // Must run before super.onCreate()/setContentView() so the chosen theme applies
        // to this Activity's very first layout pass.
        themePreference = SettingsThemePreference(this)
        AppCompatDelegate.setDefaultNightMode(nightModeFor(themePreference.getThemeMode()))

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        configManager = ConfigManager(this)

        setupThemeModeControl()

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

        title = if (position == 0) {
            getString(R.string.settings_title_storm)
        } else {
            getString(R.string.settings_title_sunny)
        }
    }

    /** Wires the compact Auto/Claro/Oscuro selector shown near the screen title. */
    private fun setupThemeModeControl() {
        val cards = listOf(
            findViewById<MaterialCardView>(R.id.cardThemeAuto),
            findViewById<MaterialCardView>(R.id.cardThemeLight),
            findViewById<MaterialCardView>(R.id.cardThemeDark)
        )
        val texts = listOf(
            findViewById<TextView>(R.id.textThemeAuto),
            findViewById<TextView>(R.id.textThemeLight),
            findViewById<TextView>(R.id.textThemeDark)
        )

        val activeStroke = ContextCompat.getColor(this, R.color.settings_accent)
        val inactiveStroke = ContextCompat.getColor(this, R.color.settings_chip_bg_inactive)
        val activeBg = ContextCompat.getColor(this, R.color.settings_chip_bg_active)
        val inactiveBg = ContextCompat.getColor(this, R.color.settings_card_bg_alt)
        val activeText = ContextCompat.getColor(this, R.color.settings_accent_text)
        val inactiveText = ContextCompat.getColor(this, R.color.settings_text_muted)
        val strokeWidthPx = resources.displayMetrics.density

        fun updateSelection(mode: Int) {
            cards.forEachIndexed { index, card ->
                val isSelected = index == mode
                card.strokeColor = if (isSelected) activeStroke else inactiveStroke
                card.strokeWidth = ((if (isSelected) 2 else 1) * strokeWidthPx).toInt()
                card.setCardBackgroundColor(if (isSelected) activeBg else inactiveBg)
                texts[index].setTextColor(if (isSelected) activeText else inactiveText)
            }
        }

        // Reflect the currently saved selection.
        updateSelection(themePreference.getThemeMode())

        cards.forEachIndexed { index, card ->
            card.setOnClickListener {
                if (themePreference.getThemeMode() == index) return@setOnClickListener
                themePreference.setThemeMode(index)
                AppCompatDelegate.setDefaultNightMode(nightModeFor(index))
                recreate()
            }
        }
    }

    private fun nightModeFor(mode: Int): Int = when (mode) {
        SettingsThemePreference.MODE_LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
        SettingsThemePreference.MODE_DARK -> AppCompatDelegate.MODE_NIGHT_YES
        else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
    }
}
