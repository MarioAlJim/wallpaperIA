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
import androidx.fragment.app.Fragment
import com.google.android.material.card.MaterialCardView
import com.google.android.material.tabs.TabLayout
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

        configManager = ConfigManager(this)
        tutorialPreference = TutorialPreference(this)

        setupThemeModeControl()

        val tabLayoutWeather = findViewById<TabLayout>(R.id.tabLayoutWeather)
        val initialActiveEffect = configManager.getActiveEffect()

        // Select initial tab and load fragment
        tabLayoutWeather.getTabAt(initialActiveEffect)?.select()
        loadSettingsFragment(initialActiveEffect)

        if (!tutorialPreference.hasSeenTutorial()) {
            tutorialPreference.setSeenTutorial(true)
            supportFragmentManager.executePendingTransactions()
            findViewById<View>(R.id.settings_fragment_container).post {
                startTutorial(initialActiveEffect)
            }
        }

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

    /**
     * Runs the step-by-step onboarding tutorial for [tabIndex] (0 = Tormenta, 1 = Día y Noche).
     * Called automatically on first run, and can be replayed any time via the "Ver tutorial"
     * button in each tab's help dialog.
     */
    fun startTutorial(tabIndex: Int = configManager.getActiveEffect()) {
        val steps = if (tabIndex == 0) buildStormTutorialSteps() else buildSunnyTutorialSteps()
        tutorialOverlay.start(steps)
    }

    private fun buildStormTutorialSteps(): List<TutorialStep> = listOf(
        TutorialStep(
            R.id.tabLayoutWeather,
            "Elige el efecto",
            "Aquí cambias entre Tormenta y Día y Noche: cada pestaña configura un fondo animado distinto para tu pantalla."
        ),
        TutorialStep(
            R.id.layoutThemeMode,
            "Tema de la app",
            "Cambia el tema visual de esta pantalla de ajustes (Auto/Claro/Oscuro). No afecta al fondo de pantalla animado."
        ),
        TutorialStep(
            R.id.cardStormVariable,
            "Tormenta Variable",
            "Actívala para que la tormenta cambie sola, alternando ciclos de calma y ráfagas, sin que tengas que ajustar nada manualmente."
        ),
        TutorialStep(
            R.id.cardGlobalBrightness,
            "Brillo Global",
            "Ajusta el nivel de brillo base del fondo y las nubes: en niveles bajos la escena se oscurece y se ilumina con los relámpagos."
        ),
        TutorialStep(
            R.id.headerCloudsWind,
            "Nubes y Viento",
            "Toca aquí para desplegar la densidad y velocidad de las nubes, y la dirección/intensidad del viento."
        ),
        TutorialStep(
            R.id.headerRain,
            "Lluvia",
            "Aquí controlas qué tan intensa y rápida cae la lluvia, su color y si nace desde las nubes o los bordes de la pantalla."
        ),
        TutorialStep(
            R.id.headerLightning,
            "Rayos",
            "Configura la frecuencia, duración y color de los relámpagos, y los destellos de luz que producen sobre las nubes."
        ),
        TutorialStep(
            R.id.headerBackground,
            "Fondo",
            "Elige la imagen o color que se ve detrás de la tormenta (paisaje, negro o foto personalizada)."
        ),
        TutorialStep(
            R.id.btnStormHelp,
            "Botón de Ayuda",
            "Toca este ícono en cualquier momento para ver un resumen de todas las secciones, o repetir este tutorial."
        ),
        TutorialStep(
            R.id.buttonApplyWallpaper,
            "Aplicar",
            "Cuando estés conforme con los ajustes, toca aquí para establecer esta escena como tu fondo de pantalla en vivo."
        )
    )

    private fun buildSunnyTutorialSteps(): List<TutorialStep> = listOf(
        TutorialStep(
            R.id.tabLayoutWeather,
            "Elige el efecto",
            "Aquí cambias entre Tormenta y Día y Noche: cada pestaña configura un fondo animado distinto para tu pantalla."
        ),
        TutorialStep(
            R.id.layoutThemeMode,
            "Tema de la app",
            "Cambia el tema visual de esta pantalla de ajustes (Auto/Claro/Oscuro). No afecta al fondo de pantalla animado."
        ),
        TutorialStep(
            R.id.cardTimeMode,
            "Modo de Tiempo",
            "Elige si el fondo muestra solo Día, solo Noche, un ciclo Combinado que alterna entre ambos, o el Reloj en tiempo real según la hora de tu dispositivo."
        ),
        TutorialStep(
            R.id.headerSunnyEnvironment,
            "Ajuste de Entorno",
            "Configura la densidad de nubes diurnas, el fondo de paisaje y el efecto de inclinación 3D con el giroscopio."
        ),
        TutorialStep(
            R.id.headerSunnySun,
            "Configuración del Sol",
            "Ajusta el tamaño y pulso del sol, el tema de color del cielo, su trayectoria de movimiento y los rayos de luz."
        ),
        TutorialStep(
            R.id.headerSunnyMoon,
            "Configuración de Luna y Estrellas",
            "Ajusta la fase lunar, el brillo/estrellas del cielo nocturno y la nebulosa de fondo."
        ),
        TutorialStep(
            R.id.headerSunnyCombined,
            "Combinado",
            "Solo aplica en modo Combinado o Reloj: define la velocidad del ciclo día/noche y la trayectoria compartida del sol y la luna."
        ),
        TutorialStep(
            R.id.btnSunnyHelp,
            "Botón de Ayuda",
            "Toca este ícono en cualquier momento para ver un resumen de todas las secciones, o repetir este tutorial."
        ),
        TutorialStep(
            R.id.buttonApplyWallpaper,
            "Aplicar",
            "Cuando estés conforme con los ajustes, toca aquí para establecer esta escena como tu fondo de pantalla en vivo."
        )
    )
}
