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
import android.widget.EditText
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.content.res.ColorStateList
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

    private val pickImageLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            saveCustomBackground(uri)
        }
    }

    private val pickSunnyImageLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            saveCustomSunnyBackground(uri)
        }
    }

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
            "Color Oscuro (Sin Imagen)",
            "Montaña",
            "Valle",
            "Bosque",
            "Pico Rocoso",
            "Lago Nebloso",
            "Picos y Pinos",
            "Acantilado Costero",
            "Imagen de la Galería"
        )
        setupDropdown(
            R.id.spinnerBackgroundMode,
            backgroundModes,
            configManager.getBackgroundIndex()
        ) { position ->
            configManager.setBackgroundIndex(position)
            updateCustomBackgroundViewsVisibility(position)
        }

        findViewById<Button>(R.id.btnSelectCustomBg)?.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        updateCustomBackgroundViewsVisibility(configManager.getBackgroundIndex())

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

        setupSunnyThemeCards()

        setupSunDirectionCards()

        setupSlider(
            R.id.seekBarSunMoveSpeed,
            R.id.textViewSunMoveSpeedValue,
            configManager.getSunMoveSpeed()
        ) { value ->
            configManager.setSunMoveSpeed(value)
        }
        setupSunStationaryCards()
        updateSunMoveSpeedSliderVisibility(configManager.getSunPathDirection())

        val sunnyBackgrounds = arrayOf(
            "Solo degradado",
            "Montañas",
            "Campos de Trigo",
            "Lago y Bosque",
            "Desierto y Dunas",
            "Silueta de Ciudad",
            "Playa y Palmeras",
            "Imagen de la Galería"
        )
        setupDropdown(
            R.id.spinnerSunnyBackground,
            sunnyBackgrounds,
            configManager.getSunnyBackgroundIndex()
        ) { position ->
            configManager.setSunnyBackgroundIndex(position)
            updateCustomSunnyBackgroundViewsVisibility(position)
        }

        findViewById<Button>(R.id.btnSelectCustomSunnyBg)?.setOnClickListener {
            pickSunnyImageLauncher.launch("image/*")
        }
        updateCustomSunnyBackgroundViewsVisibility(configManager.getSunnyBackgroundIndex())

        // Setup Sunny God Rays
        val switchSunnyGodRays = findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.switchSunnyGodRays)
        val layoutSunnyGodRaysIntensity = findViewById<android.view.View>(R.id.layoutSunnyGodRaysIntensity)
        if (switchSunnyGodRays != null) {
            val isEnabled = configManager.isSunnyGodRaysEnabled()
            switchSunnyGodRays.isChecked = isEnabled
            layoutSunnyGodRaysIntensity?.visibility = if (isEnabled) android.view.View.VISIBLE else android.view.View.GONE
            switchSunnyGodRays.setOnCheckedChangeListener { _, isChecked ->
                configManager.setSunnyGodRaysEnabled(isChecked)
                layoutSunnyGodRaysIntensity?.visibility = if (isChecked) android.view.View.VISIBLE else android.view.View.GONE
                updateSummaries()
            }
        }
        setupSlider(
            R.id.seekBarSunnyGodRaysIntensity,
            R.id.textViewSunnyGodRaysIntensityValue,
            configManager.getSunnyGodRaysIntensity()
        ) { value ->
            configManager.setSunnyGodRaysIntensity(value)
        }

        // Setup Sunny Lens Flare
        val switchSunnyLensFlare = findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.switchSunnyLensFlare)
        val layoutSunnyLensFlareIntensity = findViewById<android.view.View>(R.id.layoutSunnyLensFlareIntensity)
        if (switchSunnyLensFlare != null) {
            val isEnabled = configManager.isSunnyLensFlareEnabled()
            switchSunnyLensFlare.isChecked = isEnabled
            layoutSunnyLensFlareIntensity?.visibility = if (isEnabled) android.view.View.VISIBLE else android.view.View.GONE
            switchSunnyLensFlare.setOnCheckedChangeListener { _, isChecked ->
                configManager.setSunnyLensFlareEnabled(isChecked)
                layoutSunnyLensFlareIntensity?.visibility = if (isChecked) android.view.View.VISIBLE else android.view.View.GONE
                updateSummaries()
            }
        }
        setupSlider(
            R.id.seekBarSunnyLensFlareIntensity,
            R.id.textViewSunnyLensFlareIntensityValue,
            configManager.getSunnyLensFlareIntensity()
        ) { value ->
            configManager.setSunnyLensFlareIntensity(value)
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
            cardCloudsWind.visibility = View.VISIBLE
            cardRain.visibility = View.GONE
            cardLightning.visibility = View.GONE
            cardBackground.visibility = View.GONE
            cardSunny.visibility = View.VISIBLE
        }

        // Refresh mode-dependent labels
        val slider = findViewById<Slider>(R.id.seekBarCloudDensity)
        val label = findViewById<TextView>(R.id.textViewCloudDensityValue)
        if (slider != null && label != null) {
            updateTextView(R.id.seekBarCloudDensity, label, slider.value.toInt())
        }
    }

    private fun saveCustomBackground(uri: android.net.Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri) ?: return
            val outputFile = java.io.File(filesDir, "custom_background.png")
            inputStream.use { input ->
                java.io.FileOutputStream(outputFile).use { output ->
                    input.copyTo(output)
                }
            }
            configManager.setBackgroundIndex(8)
            updateCustomBackgroundViewsVisibility(8)
            updateSummaries()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateCustomBackgroundViewsVisibility(position: Int) {
        val btnSelect = findViewById<Button>(R.id.btnSelectCustomBg) ?: return
        val tvStatus = findViewById<TextView>(R.id.tvCustomBgStatus) ?: return

        val parentView = btnSelect.parent as? ViewGroup
        if (parentView != null) {
            TransitionManager.beginDelayedTransition(parentView, AutoTransition().apply {
                duration = 200
            })
        }

        if (position == 8) {
            btnSelect.visibility = View.VISIBLE
            tvStatus.visibility = View.VISIBLE
            val file = java.io.File(filesDir, "custom_background.png")
            if (file.exists()) {
                tvStatus.text = "Imagen de galería cargada correctamente"
            } else {
                tvStatus.text = "Ninguna imagen seleccionada"
            }
        } else {
            btnSelect.visibility = View.GONE
            tvStatus.visibility = View.GONE
        }
    }

    private fun saveCustomSunnyBackground(uri: android.net.Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri) ?: return
            val outputFile = java.io.File(filesDir, "custom_sunny_background.png")
            inputStream.use { input ->
                java.io.FileOutputStream(outputFile).use { output ->
                    input.copyTo(output)
                }
            }
            configManager.setSunnyBackgroundIndex(7)
            updateCustomSunnyBackgroundViewsVisibility(7)
            updateSummaries()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateCustomSunnyBackgroundViewsVisibility(position: Int) {
        val btnSelect = findViewById<Button>(R.id.btnSelectCustomSunnyBg) ?: return
        val tvStatus = findViewById<TextView>(R.id.tvCustomSunnyBgStatus) ?: return

        val parentView = btnSelect.parent as? ViewGroup
        if (parentView != null) {
            TransitionManager.beginDelayedTransition(parentView, AutoTransition().apply {
                duration = 200
            })
        }

        if (position == 7) {
            btnSelect.visibility = View.VISIBLE
            tvStatus.visibility = View.VISIBLE
            val file = java.io.File(filesDir, "custom_sunny_background.png")
            if (file.exists()) {
                tvStatus.text = "Imagen de galería cargada correctamente"
            } else {
                tvStatus.text = "Ninguna imagen seleccionada"
            }
        } else {
            btnSelect.visibility = View.GONE
            tvStatus.visibility = View.GONE
        }
    }

    private fun updateSunMoveSpeedSliderVisibility(direction: Int) {
        val layout = findViewById<View>(R.id.layoutSunMoveSpeed) ?: return
        val slider = findViewById<View>(R.id.seekBarSunMoveSpeed) ?: return
        val stationaryLayout = findViewById<View>(R.id.layoutSunStationaryPosition) ?: return
        val customLayout = findViewById<View>(R.id.layoutSunCustomXY)
        val parent = layout.parent as? ViewGroup
        if (parent != null) {
            TransitionManager.beginDelayedTransition(parent, AutoTransition().apply {
                duration = 200
            })
        }
        if (direction == 2) { // Stationary
            layout.visibility = View.GONE
            slider.visibility = View.GONE
            stationaryLayout.visibility = View.VISIBLE
            customLayout?.visibility = if (configManager.getSunStationaryPosition() == 5) View.VISIBLE else View.GONE
        } else {
            layout.visibility = View.VISIBLE
            slider.visibility = View.VISIBLE
            stationaryLayout.visibility = View.GONE
            customLayout?.visibility = View.GONE
        }
    }

    private fun setupSunDirectionCards() {
        val cardL2R = findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardSunDirL2R)
        val cardR2L = findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardSunDirR2L)
        val cardStationary = findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardSunDirStationary)
        val cardRandom = findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardSunDirRandom)

        val cards = listOf(cardL2R, cardR2L, cardStationary, cardRandom)

        fun updateCardSelection(selectedDir: Int) {
            cards.forEachIndexed { index, card ->
                if (card != null) {
                    if (index == selectedDir) {
                        card.strokeColor = Color.parseColor("#00E5FF")
                        card.strokeWidth = (3 * resources.displayMetrics.density).toInt()
                        card.setCardBackgroundColor(Color.parseColor("#2D2D3D"))
                    } else {
                        card.strokeColor = Color.parseColor("#3A3A4A")
                        card.strokeWidth = (1 * resources.displayMetrics.density).toInt()
                        card.setCardBackgroundColor(Color.parseColor("#21212A"))
                    }
                }
            }
            updateSunMoveSpeedSliderVisibility(selectedDir)
        }

        // Set initial selection
        val initialDir = configManager.getSunPathDirection()
        updateCardSelection(initialDir)

        // Set click listeners
        cards.forEachIndexed { index, card ->
            card?.setOnClickListener {
                configManager.setSunPathDirection(index)
                updateCardSelection(index)
                updateSummaries()
            }
        }
    }

    private fun setupSunStationaryCards() {
        val cardTopLeft = findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardSunPosTopLeft)
        val cardTopRight = findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardSunPosTopRight)
        val cardCenter = findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardSunPosCenter)
        val cardLeft = findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardSunPosLeft)
        val cardRight = findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardSunPosRight)
        val cardCustom = findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardSunPosCustom)

        val cards = listOf(cardTopLeft, cardTopRight, cardCenter, cardLeft, cardRight, cardCustom)
        val customLayout = findViewById<View>(R.id.layoutSunCustomXY)

        fun updateCardSelection(selectedPosition: Int) {
            cards.forEachIndexed { index, card ->
                if (card != null) {
                    if (index == selectedPosition) {
                        card.strokeColor = android.graphics.Color.parseColor("#00E5FF")
                        card.strokeWidth = (3 * resources.displayMetrics.density).toInt()
                        card.setCardBackgroundColor(android.graphics.Color.parseColor("#2D2D3D"))
                    } else {
                        card.strokeColor = android.graphics.Color.parseColor("#3A3A4A")
                        card.strokeWidth = (1 * resources.displayMetrics.density).toInt()
                        card.setCardBackgroundColor(android.graphics.Color.parseColor("#21212A"))
                    }
                }
            }
            if (customLayout != null) {
                val parent = customLayout.parent as? ViewGroup
                if (parent != null) {
                    TransitionManager.beginDelayedTransition(parent, AutoTransition().apply {
                        duration = 200
                    })
                }
                customLayout.visibility = if (selectedPosition == 5) View.VISIBLE else View.GONE
            }
        }

        // Set initial selection
        val initialPosition = configManager.getSunStationaryPosition()
        updateCardSelection(initialPosition)

        // Set click listeners
        cards.forEachIndexed { index, card ->
            card?.setOnClickListener {
                configManager.setSunStationaryPosition(index)
                updateCardSelection(index)
                updateSummaries()
            }
        }

        // Setup Custom X / Y sliders
        val sliderX = findViewById<com.google.android.material.slider.Slider>(R.id.seekBarSunCustomX)
        val textX = findViewById<TextView>(R.id.textViewSunCustomXValue)
        val initialX = configManager.getSunCustomX()
        sliderX?.value = initialX.toFloat()
        textX?.text = "$initialX%"
        sliderX?.addOnChangeListener { _, value, _ ->
            val intVal = value.toInt()
            configManager.setSunCustomX(intVal)
            textX?.text = "$intVal%"
            updateSummaries()
        }

        val sliderY = findViewById<com.google.android.material.slider.Slider>(R.id.seekBarSunCustomY)
        val textY = findViewById<TextView>(R.id.textViewSunCustomYValue)
        val initialY = configManager.getSunCustomY()
        sliderY?.value = initialY.toFloat()
        textY?.text = "$initialY%"
        sliderY?.addOnChangeListener { _, value, _ ->
            val intVal = value.toInt()
            configManager.setSunCustomY(intVal)
            textY?.text = "$intVal%"
            updateSummaries()
        }
    }

    private fun setCardGradientPreview(view: View?, topColor: Int, bottomColor: Int) {
        view ?: return
        val drawable = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(topColor, bottomColor)
        ).apply {
            cornerRadius = 8 * resources.displayMetrics.density
        }
        view.background = drawable
    }

    private fun setCircleColorPreview(view: View?, color: Int) {
        view ?: return
        val drawable = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(color)
        }
        view.background = drawable
    }

    private fun setupSunnyThemeCards() {
        val cardNoon = findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardSunnyThemeNoon)
        val cardSunset = findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardSunnyThemeSunset)
        val cardDusk = findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardSunnyThemeDusk)
        val cardCustom = findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardSunnyThemeCustom)

        val cards = listOf(cardNoon, cardSunset, cardDusk, cardCustom)
        val customLayout = findViewById<View>(R.id.layoutSunnyCustomGradient)

        // Set static preview gradients for the 3 presets
        setCardGradientPreview(findViewById(R.id.viewThemeNoonPreview), 0xFF0566D9.toInt(), 0xFF8DCCEF.toInt())
        setCardGradientPreview(findViewById(R.id.viewThemeSunsetPreview), 0xFF1A0D40.toInt(), 0xFFF2731A.toInt())
        setCardGradientPreview(findViewById(R.id.viewThemeDuskPreview), 0xFF261A59.toInt(), 0xFFE6808C.toInt())

        fun updateThemeSelection(selectedTheme: Int) {
            cards.forEachIndexed { index, card ->
                if (card != null) {
                    if (index == selectedTheme) {
                        card.strokeColor = Color.parseColor("#00E5FF")
                        card.strokeWidth = (3 * resources.displayMetrics.density).toInt()
                        card.setCardBackgroundColor(Color.parseColor("#2D2D3D"))
                    } else {
                        card.strokeColor = Color.parseColor("#3A3A4A")
                        card.strokeWidth = (1 * resources.displayMetrics.density).toInt()
                        card.setCardBackgroundColor(Color.parseColor("#21212A"))
                    }
                }
            }
            if (customLayout != null) {
                val parent = customLayout.parent as? ViewGroup
                if (parent != null) {
                    TransitionManager.beginDelayedTransition(parent, AutoTransition().apply {
                        duration = 200
                    })
                }
                customLayout.visibility = if (selectedTheme == 3) View.VISIBLE else View.GONE
            }
            updateSunnyThemePreviews()
        }

        // Set initial theme selection
        val initialTheme = configManager.getSunnyTheme()
        updateThemeSelection(initialTheme)

        // Set click listeners for the theme cards
        cards.forEachIndexed { index, card ->
            card?.setOnClickListener {
                configManager.setSunnyTheme(index)
                updateThemeSelection(index)
                updateSummaries()
            }
        }

        // Set click listeners for custom color picker buttons
        findViewById<View>(R.id.btnCustomSkyTopColor)?.setOnClickListener {
            showColorPickerDialog(true)
        }
        findViewById<View>(R.id.btnCustomSkyBottomColor)?.setOnClickListener {
            showColorPickerDialog(false)
        }
    }

    private fun updateSunnyThemePreviews() {
        val top = configManager.getSunnyCustomSkyTopColor()
        val bottom = configManager.getSunnyCustomSkyBottomColor()
        
        // Custom preview card gradient
        setCardGradientPreview(findViewById(R.id.viewThemeCustomPreview), top, bottom)
        
        // Circle color pickers previews
        setCircleColorPreview(findViewById(R.id.viewCustomSkyTopColorPreview), top)
        setCircleColorPreview(findViewById(R.id.viewCustomSkyBottomColorPreview), bottom)
        
        // Hex text codes
        findViewById<TextView>(R.id.textViewCustomSkyTopColorCode)?.text = String.format("#%06X", 0xFFFFFF and top)
        findViewById<TextView>(R.id.textViewCustomSkyBottomColorCode)?.text = String.format("#%06X", 0xFFFFFF and bottom)
    }

    private fun showColorPickerDialog(isTopColor: Boolean) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_color_picker, null)
        
        val previewView = dialogView.findViewById<View>(R.id.dialogColorPreview)
        val hexInput = dialogView.findViewById<EditText>(R.id.dialogColorHexInput)
        
        val sliderR = dialogView.findViewById<Slider>(R.id.sliderR)
        val sliderG = dialogView.findViewById<Slider>(R.id.sliderG)
        val sliderB = dialogView.findViewById<Slider>(R.id.sliderB)
        
        val labelR = dialogView.findViewById<TextView>(R.id.labelR)
        val labelG = dialogView.findViewById<TextView>(R.id.labelG)
        val labelB = dialogView.findViewById<TextView>(R.id.labelB)
        
        val initialColor = if (isTopColor) {
            configManager.getSunnyCustomSkyTopColor()
        } else {
            configManager.getSunnyCustomSkyBottomColor()
        }
        
        var currentColor = initialColor
        
        fun updateDialogColor(color: Int, updateHexText: Boolean = true, updateSliders: Boolean = true) {
            currentColor = color
            
            val previewDrawable = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(color)
            }
            previewView?.background = previewDrawable
            
            val r = Color.red(color)
            val g = Color.green(color)
            val b = Color.blue(color)
            
            if (updateHexText) {
                val hexStr = String.format("#%06X", 0xFFFFFF and color)
                hexInput?.setText(hexStr)
            }
            
            if (updateSliders) {
                sliderR?.value = r.toFloat()
                sliderG?.value = g.toFloat()
                sliderB?.value = b.toFloat()
            }
            
            labelR?.text = "R: $r"
            labelG?.text = "G: $g"
            labelB?.text = "B: $b"
        }
        
        val sliderListener = {
            val r = sliderR?.value?.toInt() ?: 0
            val g = sliderG?.value?.toInt() ?: 0
            val b = sliderB?.value?.toInt() ?: 0
            val color = Color.rgb(r, g, b)
            updateDialogColor(color, updateHexText = true, updateSliders = false)
        }
        
        sliderR?.addOnChangeListener { _, _, _ -> sliderListener() }
        sliderG?.addOnChangeListener { _, _, _ -> sliderListener() }
        sliderB?.addOnChangeListener { _, _, _ -> sliderListener() }
        
        hexInput?.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val str = s?.toString() ?: ""
                if (str.length == 7 && str.startsWith("#")) {
                    try {
                        val parsed = Color.parseColor(str)
                        updateDialogColor(parsed, updateHexText = false, updateSliders = true)
                    } catch (e: Exception) {
                        // Ignore
                    }
                }
            }
        })
        
        val presets = intArrayOf(
            0xFF0566D9.toInt(),
            0xFF8DCCEF.toInt(),
            0xFF1A0D40.toInt(),
            0xFFF2731A.toInt(),
            0xFFE6808C.toInt(),
            0xFF261A59.toInt(),
            0xFFE53935.toInt(),
            0xFF8E24AA.toInt(),
            0xFF00ACC1.toInt(),
            0xFF43A047.toInt(),
            0xFFFFB300.toInt(),
            0xFFFFFFFF.toInt()
        )
        
        for (i in 0..11) {
            val presetId = resources.getIdentifier("presetColor$i", "id", packageName)
            val presetView = dialogView.findViewById<View>(presetId)
            if (presetView != null) {
                val circle = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(presets[i])
                    setStroke(
                        (1 * resources.displayMetrics.density).toInt(),
                        Color.parseColor("#3A3A4A")
                    )
                }
                presetView.background = circle
                presetView.setOnClickListener {
                    updateDialogColor(presets[i], updateHexText = true, updateSliders = true)
                }
            }
        }
        
        updateDialogColor(initialColor, updateHexText = true, updateSliders = true)
        
        val builder = com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setPositiveButton("Aceptar") { _, _ ->
                if (isTopColor) {
                    configManager.setSunnyCustomSkyTopColor(currentColor)
                } else {
                    configManager.setSunnyCustomSkyBottomColor(currentColor)
                }
                updateSunnyThemePreviews()
                updateSummaries()
            }
            .setNegativeButton("Cancelar", null)
            
        builder.show()
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
                val isSunny = configManager.getActiveEffect() == 1
                val maxClouds = if (isSunny) 10 else 15
                val count = if (isSunny) {
                    when (value) {
                        0 -> 0
                        25 -> 1
                        50 -> 3
                        75 -> 6
                        90 -> 8
                        100 -> 10
                        else -> (value / 100f * 10).toInt()
                    }
                } else {
                    when (value) {
                        0 -> 0
                        25 -> 2
                        50 -> 5
                        75 -> 10
                        90 -> 13
                        100 -> 15
                        else -> (value / 100f * 15).toInt()
                    }
                }.coerceIn(0, maxClouds)
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
            R.id.seekBarSunMoveSpeed -> {
                val desc = when {
                    value <= 20 -> "Lento"
                    value <= 50 -> "Normal"
                    value <= 85 -> "Rápido"
                    else -> "Extremo"
                }
                textView.text = "$value% • $desc"
            }
            R.id.seekBarSunnyGodRaysIntensity -> {
                val desc = when {
                    value <= 20 -> "Sutil"
                    value <= 50 -> "Normal"
                    value <= 85 -> "Intenso"
                    else -> "Cegador"
                }
                textView.text = "$value% • $desc"
            }
            R.id.seekBarSunnyLensFlareIntensity -> {
                val desc = when {
                    value <= 20 -> "Tenue"
                    value <= 50 -> "Normal"
                    value <= 85 -> "Brillante"
                    else -> "Cinematográfico"
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
            1 -> "Montaña"
            2 -> "Valle"
            3 -> "Bosque"
            4 -> "Pico Rocoso"
            5 -> "Lago Nebloso"
            6 -> "Picos y Pinos"
            7 -> "Acantilado Costero"
            8 -> "Imagen de la Galería"
            else -> "Color Oscuro"
        }
        summaryBackground.text = Html.fromHtml(
            "Fondo: <font color='$accentColor'>$bgModeText</font>",
            Html.FROM_HTML_MODE_LEGACY
        )

        // 5. Sunny summary
        val sunnyThemeText = when (configManager.getSunnyTheme()) {
            0 -> "Celeste"
            1 -> "Dorado"
            2 -> "Púrpura"
            3 -> "Personalizado"
            else -> "Celeste"
        }
        val sunDirText = when (configManager.getSunPathDirection()) {
            0 -> "Izq a Der"
            1 -> "Der a Izq"
            3 -> "Aleatorio"
            2 -> {
                val posText = when (configManager.getSunStationaryPosition()) {
                    0 -> "Sup. Izq."
                    1 -> "Sup. Der."
                    2 -> "Centro"
                    3 -> "Borde Izq."
                    4 -> "Borde Der."
                    5 -> "Libre (${configManager.getSunCustomX()}, ${configManager.getSunCustomY()})"
                    else -> "Centro"
                }
                "Fijo ($posText)"
            }
            else -> "Estático"
        }
        val sunnyBgText = when (configManager.getSunnyBackgroundIndex()) {
            0 -> "Degradado"
            1 -> "Montañas"
            2 -> "Campos"
            3 -> "Lago/Bosque"
            4 -> "Desierto"
            5 -> "Ciudad"
            6 -> "Playa"
            7 -> "Galería"
            else -> "Degradado"
        }
        val godRaysStatus = if (configManager.isSunnyGodRaysEnabled()) "Activo" else "Inactivo"
        val lensFlareStatus = if (configManager.isSunnyLensFlareEnabled()) "Activo" else "Inactivo"
        summarySunny.text = Html.fromHtml(
            "Tema: <font color='$accentColor'>$sunnyThemeText</font> • Sol: <font color='$accentColor'>$sunDirText</font> • Fondo: <font color='$accentColor'>$sunnyBgText</font><br/>Rayos: <font color='$accentColor'>$godRaysStatus</font> • Destellos: <font color='$accentColor'>$lensFlareStatus</font>",
            Html.FROM_HTML_MODE_LEGACY
        )
    }
}
