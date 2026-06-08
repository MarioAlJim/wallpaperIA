package com.wolf.wallpaper

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class WallpaperSettingsActivity : AppCompatActivity() {

    private lateinit var configManager: ConfigManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        configManager = ConfigManager(this)

        setupSlider(
            R.id.seekBarCloudDensity,
            R.id.textViewCloudDensityValue,
            configManager.getCloudDensity()
        ) { value ->
            configManager.setCloudDensity(value)
        }

        // Configuración especial para la densidad de lluvia en 5 niveles
        val rainSeek = findViewById<SeekBar>(R.id.seekBarRainIntensity)
        val rainText = findViewById<TextView>(R.id.textViewRainIntensityValue)
        val initialRainValue = configManager.getRainIntensity()
        val initialProgress = (initialRainValue / 25).coerceIn(0, 4)
        rainSeek.progress = initialProgress
        updateRainTextView(initialProgress, rainText)

        rainSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val value = progress * 25
                    configManager.setRainIntensity(value)
                    updateRainTextView(progress, rainText)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Configuración para la dirección de la lluvia
        val windSeek = findViewById<SeekBar>(R.id.seekBarWindDirection)
        val windText = findViewById<TextView>(R.id.textViewWindDirectionValue)
        val initialWindValue = configManager.getWindDirection()
        windSeek.progress = initialWindValue
        updateWindTextView(initialWindValue, windText)

        windSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    configManager.setWindDirection(progress)
                    updateWindTextView(progress, windText)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Configuración para el color de la lluvia
        val colorSeek = findViewById<SeekBar>(R.id.seekBarRainColor)
        val colorText = findViewById<TextView>(R.id.textViewRainColorValue)
        val initialColorValue = configManager.getRainColorIndex()
        colorSeek.progress = initialColorValue
        updateColorTextView(initialColorValue, colorText)

        colorSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    configManager.setRainColorIndex(progress)
                    updateColorTextView(progress, colorText)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        setupSlider(
            R.id.seekBarLightningFrequency,
            R.id.textViewLightningFrequencyValue,
            configManager.getLightningFrequency()
        ) { value ->
            configManager.setLightningFrequency(value)
        }

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
            R.id.seekBarRainIntensity -> {
                // No-op here since it is handled by custom seekbar listener
            }
            R.id.seekBarLightningFrequency -> {
                val desc = when {
                    value <= 0 -> "Nunca"
                    value < 25 -> "Muy raro (>60s)"
                    value == 25 -> "Cada 60 segundos"
                    value < 50 -> "Cada 30–60s"
                    value == 50 -> "Cada 30 segundos"
                    value < 75 -> "Cada 15–30s"
                    value == 75 -> "Cada 15 segundos"
                    value < 100 -> "Cada 5–15s"
                    else -> "Cada 5 segundos"
                }
                textView.text = "$value% ($desc)"
            }
        }
    }

    private fun updateRainTextView(progress: Int, textView: TextView) {
        val percentage = progress * 25
        val levelName = when (progress) {
            0 -> "Nada"
            1 -> "Pocas"
            2 -> "Media"
            3 -> "Alta"
            4 -> "Muy alta"
            else -> "Media"
        }
        val particles = when (progress) {
            0 -> 0
            1 -> 50
            2 -> 150
            3 -> 300
            4 -> 500
            else -> 150
        }
        textView.text = "$levelName ($percentage% - $particles gotas)"
    }

    private fun updateWindTextView(progress: Int, textView: TextView) {
        val directionName = when (progress) {
            0 -> "Izquierda"
            1 -> "Vertical"
            2 -> "Derecha"
            else -> "Izquierda"
        }
        textView.text = directionName
    }

    private fun updateColorTextView(progress: Int, textView: TextView) {
        val colorName = when (progress) {
            0 -> "Azul"
            1 -> "Blanco"
            2 -> "Rojo"
            3 -> "Verde"
            4 -> "Amarillo"
            5 -> "Morado"
            else -> "Azul"
        }
        textView.text = colorName
    }
}
