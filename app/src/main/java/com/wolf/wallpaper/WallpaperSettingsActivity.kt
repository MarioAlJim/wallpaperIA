package com.wolf.wallpaper

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

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

        // 2. Setup Nubes y Viento Controls
        setupSlider(
            R.id.seekBarCloudDensity,
            R.id.textViewCloudDensityValue,
            configManager.getCloudDensity()
        ) { value ->
            configManager.setCloudDensity(value)
        }

        val windDirections = arrayOf("Izquierda", "Vertical", "Derecha")
        setupSpinner(
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

        // 3. Setup Lluvia Controls
        val rainIntensities = arrayOf("Nada (0%)", "Pocas (25%)", "Media (50%)", "Alta (75%)", "Muy alta (100%)")
        val initialRainValue = configManager.getRainIntensity()
        val initialRainProgress = (initialRainValue / 25).coerceIn(0, 4)
        setupSpinner(
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
        setupSpinner(
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
        setupSpinner(
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

        val backgroundModes = arrayOf("Color Oscuro (Original)", "Imagen de Fondo")
        setupSpinner(
            R.id.spinnerBackgroundMode,
            backgroundModes,
            if (configManager.getShowBackground()) 1 else 0
        ) { position ->
            configManager.setShowBackground(position == 1)
        }

        // 5. Initialize dynamic summaries in headers
        updateSummaries()

        val buttonApply = findViewById<Button>(R.id.buttonApplyWallpaper)
        buttonApply.setOnClickListener {
            val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                putExtra(
                    WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                    ComponentName(this@WallpaperSettingsActivity, StormWallpaperService::class.java)
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
            if (content.visibility == View.VISIBLE) {
                content.visibility = View.GONE
                divider.visibility = View.GONE
                arrow.animate().rotation(0f).setDuration(200).start()
            } else {
                content.visibility = View.VISIBLE
                divider.visibility = View.VISIBLE
                arrow.animate().rotation(180f).setDuration(200).start()
            }
        }
    }

    private fun setupSpinner(
        spinnerId: Int,
        options: Array<String>,
        initialIndex: Int,
        onSelected: (Int) -> Unit
    ) {
        val spinner = findViewById<Spinner>(spinnerId)
        val adapter = ArrayAdapter(
            this,
            R.layout.spinner_item,
            options
        )
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        spinner.adapter = adapter
        spinner.setSelection(initialIndex)

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                onSelected(position)
                updateSummaries()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupSlider(
        seekBarId: Int,
        valueTextViewId: Int,
        initialValue: Int,
        onValueChanged: (Int) -> Unit
    ) {
        val seekBar = findViewById<SeekBar>(seekBarId)
        val valueTextView = findViewById<TextView>(valueTextViewId)

        seekBar.progress = initialValue
        updateTextView(seekBarId, valueTextView, initialValue)

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    onValueChanged(progress)
                    updateTextView(seekBarId, valueTextView, progress)
                    updateSummaries()
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun updateTextView(seekBarId: Int, textView: TextView, value: Int) {
        when (seekBarId) {
            R.id.seekBarCloudDensity -> {
                val count = (value / 100f * 20).toInt()
                textView.text = "$value% ($count nubes)"
            }
            R.id.seekBarLightningFrequency -> {
                val desc = when {
                    value <= 0 -> "Nunca"
                    value < 25 -> "Muy raro (20–60s)"
                    value == 25 -> "Cada 20 segundos"
                    value < 50 -> "Frecuente (5–20s)"
                    value == 50 -> "Cada 5 segundos"
                    value < 75 -> "Tormenta (2–5s)"
                    value == 75 -> "Cada 2 segundos"
                    value < 90 -> "Tormenta eléctrica (0.8–2s)"
                    value < 100 -> "Tempestad extrema (0.25–0.8s)"
                    else -> "Máximo caos (rayos rápidos)"
                }
                textView.text = "$value% ($desc)"
            }
            R.id.seekBarWindIntensity -> {
                textView.text = "$value%"
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
                textView.text = "$value% ($desc)"
            }
        }
    }

    private fun updateSummaries() {
        val summaryCloudsWind = findViewById<TextView>(R.id.summaryCloudsWind) ?: return
        val summaryRain = findViewById<TextView>(R.id.summaryRain) ?: return
        val summaryLightning = findViewById<TextView>(R.id.summaryLightning) ?: return
        val summaryBackground = findViewById<TextView>(R.id.summaryBackground) ?: return

        // 1. Nubes y Viento summary
        val cloudDensity = configManager.getCloudDensity()
        val windIntensity = configManager.getWindIntensity()
        val windDirText = when (configManager.getWindDirection()) {
            0 -> "Izquierda"
            1 -> "Vertical"
            2 -> "Derecha"
            else -> "Izquierda"
        }
        summaryCloudsWind.text = "Densidad: $cloudDensity% | Viento: $windDirText ($windIntensity%)"

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
        summaryRain.text = "Lluvia: $rainIntName | Color: $rainColorText | Vel: $rainSpeed%"

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
        val lightningDuration = configManager.getLightningDuration()
        val durationText = when {
            lightningDuration <= 20 -> "Corto"
            lightningDuration <= 60 -> "Normal"
            lightningDuration <= 85 -> "Largo"
            else -> "Extremo"
        }
        summaryLightning.text = "Freq: $lightningFreq% | Color: $lightningColorText | Dur: $durationText ($lightningDuration%)"

        // 4. Fondo summary
        val bgModeText = if (configManager.getShowBackground()) "Imagen de Fondo" else "Color Oscuro"
        summaryBackground.text = "Fondo: $bgModeText"
    }
}
