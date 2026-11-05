package com.wolf.wallpaper

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.google.android.material.card.MaterialCardView
import com.wolf.wallpaper.core.ConfigManager

class WallpaperSettingsActivity : AppCompatActivity() {

    lateinit var configManager: ConfigManager
        private set

    private lateinit var themePreference: SettingsThemePreference
    private lateinit var tutorialPreference: TutorialPreference
    private val tutorialOverlay: TutorialOverlay by lazy { TutorialOverlay(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Must run before super.onCreate()/setContentView() so the chosen theme applies
        // to this Activity's very first layout pass.
        themePreference = SettingsThemePreference(this)
        AppCompatDelegate.setDefaultNightMode(nightModeFor(themePreference.getThemeMode()))

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Edge-to-Edge handling (TargetSdk 35+)
        val rootContent = findViewById<View>(R.id.settingsRootContent)
        val buttonContainer = findViewById<View>(R.id.buttonContainer)
        val basePaddingTopPx = rootContent.paddingTop
        val basePaddingBottomPx = buttonContainer.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(rootContent) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(top = basePaddingTopPx + bars.top)
            insets
        }
        ViewCompat.setOnApplyWindowInsetsListener(buttonContainer) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(bottom = basePaddingBottomPx + bars.bottom)
            insets
        }

        supportActionBar?.hide()

        configManager = ConfigManager(this)
        tutorialPreference = TutorialPreference(this)

        setupThemeModeControl()

        findViewById<View>(R.id.buttonHeaderHelp)?.setOnClickListener {
            showHelpDialog()
        }

        // Load Storm settings directly
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.settings_fragment_container, StormSettingsFragment())
                .commit()
        }

        title = getString(R.string.settings_title_storm)

        if (!tutorialPreference.hasSeenTutorial()) {
            tutorialPreference.setSeenTutorial(true)
            supportFragmentManager.executePendingTransactions()
            findViewById<View>(R.id.settings_fragment_container).post {
                startTutorial()
            }
        }

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

    private fun showHelpDialog() {
        SettingsBottomSheetHelper.showHelpSheet(this, isStorm = true) {
            startTutorial()
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

    fun startTutorial(tabIndex: Int = 0) {
        val steps = buildStormTutorialSteps()
        tutorialOverlay.start(steps)
    }

    private fun buildStormTutorialSteps(): List<TutorialStep> = listOf(
        TutorialStep(
            R.id.layoutThemeMode,
            "Tema de la app",
            "Cambia el tema visual de esta pantalla de ajustes (Auto/Claro/Oscuro). No afecta al fondo de pantalla animado."
        ),
        TutorialStep(
            R.id.cardStormVariable,
            "Tormenta Variable",
            "Actívala para alternar ciclos automáticos de calma y ráfagas, y elegir la duración de cada fase (Corta, Media o Larga)."
        ),
        TutorialStep(
            R.id.cardGlobalBrightness,
            "Brillo Global",
            "Ajusta el nivel de iluminación ambiental base: en niveles bajos la noche se oscurece y los relámpagos iluminan dinámicamente toda la escena."
        ),
        TutorialStep(
            R.id.headerCloudsWind,
            "Nubes y Viento",
            "Despliega la densidad y velocidad de las nubes (escalonadas en capas con variación de formas), además de dirección, intensidad y líneas de viento."
        ),
        TutorialStep(
            R.id.headerRain,
            "Lluvia",
            "Controla la intensidad, velocidad y color de la lluvia, origen de caída (nubes o bordes) y las gotas sobre la pantalla con efecto lupa y refracción."
        ),
        TutorialStep(
            R.id.headerLightning,
            "Rayos",
            "Configura la frecuencia, duración y color de los rayos, destellos internos en nubes e interacción táctil para invocarlos al tocar la pantalla."
        ),
        TutorialStep(
            R.id.headerBackground,
            "Fondo",
            "Elige el modo de fondo: Puras Nubes (cielo dinámico sin siluetas, como en la app original), paisajes predeterminados o una imagen personalizada de tu galería."
        ),
        TutorialStep(
            R.id.buttonHeaderHelp,
            "Botón de Ayuda",
            "Toca este ícono en cualquier momento para ver un resumen de todas las secciones, o repetir este tutorial interactivo."
        ),
        TutorialStep(
            R.id.buttonApplyWallpaper,
            "Aplicar",
            "Cuando estés conforme con los ajustes, toca aquí para establecer esta escena como tu fondo de pantalla en vivo."
        )
    )
}
