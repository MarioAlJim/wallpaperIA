package com.wolf.wallpaper

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.text.Html
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.slider.Slider
import com.google.android.material.tabs.TabLayout
import com.wolf.wallpaper.core.ConfigManager

// Utility extension functions for clean premium UI logic
private fun View.toggleVisibility(divider: View?, arrow: ImageView, durationMs: Long = 250) {
    val parentView = this.parent as? ViewGroup ?: return
    TransitionManager.beginDelayedTransition(parentView, AutoTransition().apply {
        duration = durationMs
    })
    val isVisible = this.visibility == View.VISIBLE
    this.visibility = if (isVisible) View.GONE else View.VISIBLE
    divider?.visibility = if (isVisible) View.GONE else View.VISIBLE
    arrow.animate().rotation(if (isVisible) 0f else 180f).setDuration(durationMs).start()
}

private fun Slider.setup(initialValue: Int, onProgressChanged: (Int) -> Unit) {
    this.value = initialValue.toFloat()
    this.addOnChangeListener { _, value, fromUser ->
        if (fromUser) {
            onProgressChanged(value.toInt())
        }
    }
}

private fun AutoCompleteTextView.setup(options: Array<String>, initialIndex: Int, onSelected: (Int) -> Unit) {
    val adapter = ArrayAdapter(
        context,
        R.layout.spinner_dropdown_item,
        options
    )
    this.setAdapter(adapter)
    this.setText(options.getOrNull(initialIndex), false)
    this.setOnItemClickListener { _, _, position, _ ->
        onSelected(position)
    }
}

class WallpaperSettingsActivity : AppCompatActivity() {

    private lateinit var configManager: ConfigManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        configManager = ConfigManager(this)

        // 1. Setup Accordions
        setupAccordion(
            R.id.headerCloudsWind,
            R.id.contentCloudsWind,
            R.id.dividerCloudsWind,
            R.id.arrowCloudsWind
        )
        setupAccordion(
            R.id.headerRain,
            R.id.contentRain,
            R.id.dividerRain,
            R.id.arrowRain
        )
        setupAccordion(
            R.id.headerLightning,
            R.id.contentLightning,
            R.id.dividerLightning,
            R.id.arrowLightning
        )
        setupAccordion(
            R.id.headerBackground,
            R.id.contentBackground,
            R.id.dividerBackground,
            R.id.arrowBackground
        )
        setupAccordion(
            R.id.headerSunny,
            R.id.contentSunny,
            R.id.dividerSunny,
            R.id.arrowSunny
        )

        // 2. Setup Nubes y Viento Controls
        setupSlider(
            R.id.seekBarCloudDensity,
            R.id.textViewCloudDensityValue,
            configManager.getCloudDensity()
        ) { value ->
            configManager.setCloudDensity(value)
        }

        val windDirections = arrayOf("Izquierda", "Vertical", "Derecha")
        setupDropdown(
            R.id.spinnerWindDirection,
            windDirections,
            configManager.getWindDirection()
        ) { position ->
            configManager.setWindDirection(position)
        }

        setupSlider(
            R.id.seekBarWindIntensity,
            R.id.textViewWindIntensityValue,
            configManager.getWindIntensity()
        ) { value ->
            configManager.setWindIntensity(value)
        }

        setupSlider(
            R.id.seekBarCloudDynamicsSpeed,
            R.id.textViewCloudDynamicsSpeedValue,
            configManager.getCloudDynamicsSpeed()
        ) { value ->
            configManager.setCloudDynamicsSpeed(value)
        }

        // 3. Setup Lluvia Controls
        val rainIntensities = arrayOf("Nada (0%)", "Pocas (25%)", "Media (50%)", "Alta (75%)", "Muy alta (100%)")
        val initialRainValue = configManager.getRainIntensity()
        val initialRainProgress = (initialRainValue / 25).coerceIn(0, 4)
        setupDropdown(
            R.id.spinnerRainIntensity,
            rainIntensities,
            initialRainProgress
        ) { position ->
            configManager.setRainIntensity(position * 25)
        }

        setupSlider(
            R.id.seekBarRainSpeed,
            R.id.textViewRainSpeedValue,
            configManager.getRainSpeed()
        ) { value ->
            configManager.setRainSpeed(value)
        }

        val rainColors = arrayOf("Azul", "Blanco", "Rojo", "Verde", "Amarillo", "Morado")
        setupDropdown(
            R.id.spinnerRainColor,
            rainColors,
            configManager.getRainColorIndex()
        ) { position ->
            configManager.setRainColorIndex(position)
        }

        // 4. Setup Rayos Controls
        setupSlider(
            R.id.seekBarLightningFrequency,
            R.id.textViewLightningFrequencyValue,
            configManager.getLightningFrequency()
        ) { value ->
            configManager.setLightningFrequency(value)
        }

        val lightningColors = arrayOf("Blanco", "Azul", "Amarillo", "Rojo", "Verde", "Morado", "Aleatorio")
        setupDropdown(
            R.id.spinnerLightningColor,
            lightningColors,
            configManager.getLightningColorIndex()
        ) { position ->
            configManager.setLightningColorIndex(position)
        }

        setupSlider(
            R.id.seekBarLightningDuration,
            R.id.textViewLightningDurationValue,
            configManager.getLightningDuration()
        ) { value ->
            configManager.setLightningDuration(value)
        }

        // 4b. Setup Destellos en Nubes Controls
        setupSlider(
            R.id.seekBarCloudFlashFrequency,
            R.id.textViewCloudFlashFrequencyValue,
            configManager.getCloudFlashFrequency()
        ) { value ->
            configManager.setCloudFlashFrequency(value)
        }

        setupDropdown(
            R.id.spinnerCloudFlashColor,
            lightningColors,
            configManager.getCloudFlashColorIndex()
        ) { position ->
            configManager.setCloudFlashColorIndex(position)
        }

        val switchCloudFlash = findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.switchCloudFlash)
        if (switchCloudFlash != null) {
            switchCloudFlash.isChecked = configManager.isCloudFlashEnabled()
            switchCloudFlash.setOnCheckedChangeListener { _, isChecked ->
                configManager.setCloudFlashEnabled(isChecked)
                updateSummaries()
            }
        }

        val switchLightningFlash = findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.switchLightningFlash)
        if (switchLightningFlash != null) {
            switchLightningFlash.isChecked = configManager.isLightningFlashEnabled()
            switchLightningFlash.setOnCheckedChangeListener { _, isChecked ->
                configManager.setLightningFlashEnabled(isChecked)
                updateSummaries()
            }
        }

        val switchInteractiveLightning = findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.switchInteractiveLightning)
        if (switchInteractiveLightning != null) {
            switchInteractiveLightning.isChecked = configManager.isInteractiveLightningEnabled()
            switchInteractiveLightning.setOnCheckedChangeListener { _, isChecked ->
                configManager.setInteractiveLightningEnabled(isChecked)
                updateSummaries()
            }
        }

        val backgroundModes = arrayOf(
            "Color Oscuro (Original)",
            "Fondo 1 (Montaña)",
            "Fondo 2 (Valle)",
            "Fondo 3 (Bosque)",
            "Fondo 4 (Pico Rocoso)",
            "Fondo 5 (Lago Nebloso)"
        )
        setupDropdown(
            R.id.spinnerBackgroundMode,
            backgroundModes,
            configManager.getBackgroundIndex()
        ) { position ->
            configManager.setBackgroundIndex(position)
        }

        // 4c. Setup Sunny Controls
        setupSlider(
            R.id.seekBarSunSize,
            R.id.textViewSunSizeValue,
            configManager.getSunSize()
        ) { value ->
            configManager.setSunSize(value)
        }

        setupSlider(
            R.id.seekBarSunSpeed,
            R.id.textViewSunSpeedValue,
            configManager.getSunSpeed()
        ) { value ->
            configManager.setSunSpeed(value)
        }

        val sunnyThemes = arrayOf("Mediodía Celeste", "Atardecer Dorado", "Anochecer Púrpura")
        setupDropdown(
            R.id.spinnerSunnyTheme,
            sunnyThemes,
            configManager.getSunnyTheme()
        ) { position ->
            configManager.setSunnyTheme(position)
        }

        // 4d. Setup TabLayout Weather Selector
        val tabLayoutWeather = findViewById<TabLayout>(R.id.tabLayoutWeather)
        val initialActiveEffect = configManager.getActiveEffect()
        tabLayoutWeather.getTabAt(initialActiveEffect)?.select()
        updateVisibilityOfCards(initialActiveEffect)

        tabLayoutWeather.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val position = tab?.position ?: 0
                configManager.setActiveEffect(position)
                updateVisibilityOfCards(position)
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        // 5. Initialize summaries
        updateSummaries()

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

    private fun setupAccordion(headerId: Int, contentId: Int, dividerId: Int, arrowId: Int) {
        val header = findViewById<RelativeLayout>(headerId)
        val content = findViewById<LinearLayout>(contentId)
        val divider = findViewById<View>(dividerId)
        val arrow = findViewById<ImageView>(arrowId)

        header.setOnClickListener {
            content.toggleVisibility(divider, arrow)
        }
    }

    private fun updateVisibilityOfCards(weatherIndex: Int) {
        val cardCloudsWind = findViewById<View>(R.id.cardCloudsWind)
        val cardRain = findViewById<View>(R.id.cardRain)
        val cardLightning = findViewById<View>(R.id.cardLightning)
        val cardBackground = findViewById<View>(R.id.cardBackground)
        val cardSunny = findViewById<View>(R.id.cardSunny)

        val parentView = cardCloudsWind.parent as? ViewGroup ?: return
        TransitionManager.beginDelayedTransition(parentView, AutoTransition().apply {
            duration = 250
        })

        if (weatherIndex == 0) { // Storm
            cardCloudsWind.visibility = View.VISIBLE
            cardRain.visibility = View.VISIBLE
            cardLightning.visibility = View.VISIBLE
            cardBackground.visibility = View.VISIBLE
            cardSunny.visibility = View.GONE
        } else { // Sunny
            cardCloudsWind.visibility = View.GONE
            cardRain.visibility = View.GONE
            cardLightning.visibility = View.GONE
            cardBackground.visibility = View.GONE
            cardSunny.visibility = View.VISIBLE
        }
    }

    private fun setupDropdown(
        textViewId: Int,
        options: Array<String>,
        initialIndex: Int,
        onSelected: (Int) -> Unit
    ) {
        val autoCompleteTextView = findViewById<AutoCompleteTextView>(textViewId)
        autoCompleteTextView.setup(options, initialIndex) { position ->
            onSelected(position)
            updateSummaries()
        }
    }

    private fun setupSlider(
        sliderId: Int,
        valueTextViewId: Int,
        initialValue: Int,
        onValueChanged: (Int) -> Unit
    ) {
        val slider = findViewById<Slider>(sliderId)
        val valueTextView = findViewById<TextView>(valueTextViewId)

        slider.setup(initialValue) { progress ->
            onValueChanged(progress)
            updateTextView(sliderId, valueTextView, progress)
            updateSummaries()
        }
        updateTextView(sliderId, valueTextView, initialValue)
    }

    private fun updateTextView(sliderId: Int, textView: TextView, value: Int) {
        when (sliderId) {
            R.id.seekBarCloudDensity -> {
                val count = when (value) {
                    0 -> 0
                    25 -> 2
                    50 -> 5
                    75 -> 10
                    90 -> 13
                    100 -> 15
                    else -> (value / 100f * 15).toInt()
                }.coerceIn(0, 15)
                textView.text = "$value% • $count nubes"
            }
            R.id.seekBarLightningFrequency -> {
                val desc = when {
                    value <= 0 -> "Nunca"
                    value < 25 -> "Muy raro (20–60s)"
                    value == 25 -> "Cada 20s"
                    value < 50 -> "Frecuente (5–20s)"
                    value == 50 -> "Cada 5s"
                    value < 75 -> "Tormenta (2–5s)"
                    value == 75 -> "Cada 2s"
                    value < 90 -> "Tormenta eléctrica (0.8–2s)"
                    value < 100 -> "Tempestad extrema (0.25–0.8s)"
                    else -> "Máximo caos"
                }
                textView.text = "$value% • $desc"
            }
            R.id.seekBarWindIntensity -> {
                textView.text = "$value%"
            }
            R.id.seekBarCloudDynamicsSpeed -> {
                textView.text = if (value == 0) "Desactivado" else "$value%"
            }
            R.id.seekBarRainSpeed -> {
                textView.text = "$value%"
            }
            R.id.seekBarLightningDuration -> {
                val desc = when {
                    value <= 20 -> "Corto"
                    value <= 60 -> "Normal"
                    value <= 85 -> "Largo"
                    else -> "Extremo"
                }
                textView.text = "$value% • $desc"
            }
            R.id.seekBarCloudFlashFrequency -> {
                val desc = when {
                    value <= 0 -> "Nunca"
                    value < 25 -> "Muy raro"
                    value == 25 -> "Cada 20s"
                    value < 50 -> "Frecuente"
                    value == 50 -> "Cada 5s"
                    value < 75 -> "Tormenta"
                    value == 75 -> "Cada 2s"
                    value < 90 -> "Tormenta eléctrica"
                    value < 100 -> "Tempestad extrema"
                    else -> "Máximo caos"
                }
                textView.text = "$value% • $desc"
            }
            R.id.seekBarSunSize -> {
                val desc = when {
                    value <= 20 -> "Pequeño"
                    value <= 50 -> "Normal"
                    value <= 85 -> "Grande"
                    else -> "Gigante"
                }
                textView.text = "$value% • $desc"
            }
            R.id.seekBarSunSpeed -> {
                val desc = when {
                    value <= 20 -> "Lento"
                    value <= 50 -> "Normal"
                    value <= 85 -> "Rápido"
                    else -> "Extremo"
                }
                textView.text = "$value% • $desc"
            }
        }
    }

    private fun updateSummaries() {
        val summaryCloudsWind = findViewById<TextView>(R.id.summaryCloudsWind) ?: return
        val summaryRain = findViewById<TextView>(R.id.summaryRain) ?: return
        val summaryLightning = findViewById<TextView>(R.id.summaryLightning) ?: return
        val summaryBackground = findViewById<TextView>(R.id.summaryBackground) ?: return
        val summarySunny = findViewById<TextView>(R.id.summarySunny) ?: return

        val accentColor = "#00E5FF"

        // 1. Nubes y Viento summary
        val cloudDensity = configManager.getCloudDensity()
        val windIntensity = configManager.getWindIntensity()
        val windDirText = when (configManager.getWindDirection()) {
            0 -> "Izquierda"
            1 -> "Vertical"
            2 -> "Derecha"
            else -> "Izquierda"
        }
        summaryCloudsWind.text = Html.fromHtml(
            "Densidad: <font color='$accentColor'>$cloudDensity%</font> • Viento: <font color='$accentColor'>$windDirText</font> ($windIntensity%)",
            Html.FROM_HTML_MODE_LEGACY
        )

        // 2. Lluvia summary
        val rainIntName = when (configManager.getRainIntensity() / 25) {
            0 -> "Nada"
            1 -> "Pocas"
            2 -> "Media"
            3 -> "Alta"
            4 -> "Muy alta"
            else -> "Media"
        }
        val rainColorText = when (configManager.getRainColorIndex()) {
            0 -> "Azul"
            1 -> "Blanco"
            2 -> "Rojo"
            3 -> "Verde"
            4 -> "Amarillo"
            5 -> "Morado"
            else -> "Azul"
        }
        val rainSpeed = configManager.getRainSpeed()
        summaryRain.text = Html.fromHtml(
            "Intensidad: <font color='$accentColor'>$rainIntName</font> • Color: <font color='$accentColor'>$rainColorText</font> • Vel: <font color='$accentColor'>$rainSpeed%</font>",
            Html.FROM_HTML_MODE_LEGACY
        )

        // 3. Rayos summary
        val lightningFreq = configManager.getLightningFrequency()
        val lightningColorText = when (configManager.getLightningColorIndex()) {
            0 -> "Blanco"
            1 -> "Azul"
            2 -> "Amarillo"
            3 -> "Rojo"
            4 -> "Verde"
            5 -> "Morado"
            6 -> "Aleatorio"
            else -> "Blanco"
        }
        val cloudFlashFreq = configManager.getCloudFlashFrequency()
        summaryLightning.text = Html.fromHtml(
            "Rayos: <font color='$accentColor'>$lightningFreq%</font> • Nubes: <font color='$accentColor'>$cloudFlashFreq%</font> • Color: <font color='$accentColor'>$lightningColorText</font>",
            Html.FROM_HTML_MODE_LEGACY
        )

        // 4. Fondo summary
        val bgModeText = when (configManager.getBackgroundIndex()) {
            0 -> "Color Oscuro"
            1 -> "Fondo 1"
            2 -> "Fondo 2"
            3 -> "Fondo 3"
            4 -> "Fondo 4"
            5 -> "Fondo 5"
            else -> "Color Oscuro"
        }
        summaryBackground.text = Html.fromHtml(
            "Fondo: <font color='$accentColor'>$bgModeText</font>",
            Html.FROM_HTML_MODE_LEGACY
        )

        // 5. Sunny summary
        val sunSize = configManager.getSunSize()
        val sunSpeed = configManager.getSunSpeed()
        val sunnyThemeText = when (configManager.getSunnyTheme()) {
            0 -> "Mediodía Celeste"
            1 -> "Atardecer Dorado"
            2 -> "Anochecer Púrpura"
            else -> "Mediodía Celeste"
        }
        summarySunny.text = Html.fromHtml(
            "Tamaño: <font color='$accentColor'>$sunSize%</font> • Pulso: <font color='$accentColor'>$sunSpeed%</font> • Tema: <font color='$accentColor'>$sunnyThemeText</font>",
            Html.FROM_HTML_MODE_LEGACY
        )
    }
}
