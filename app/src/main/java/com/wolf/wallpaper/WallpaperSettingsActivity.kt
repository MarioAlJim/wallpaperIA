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
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.LinearGradient
import android.graphics.Shader
import android.content.Context
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
    private val backgroundBitmapCache = HashMap<String, Bitmap>()

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

        val windDirections = arrayOf("Izquierda", "Neutro", "Derecha")
        setupDropdown(
            R.id.spinnerWindDirection,
            windDirections,
            configManager.getWindDirection()
        ) { position ->
            configManager.setWindDirection(position)
            refreshStormCardPreviews()
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

        val switchWindLines = findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.switchWindLines)
        val layoutWindLinesIntensity = findViewById<android.view.View>(R.id.layoutWindLinesIntensity)
        if (switchWindLines != null) {
            val isEnabled = configManager.isWindLinesEnabled()
            switchWindLines.isChecked = isEnabled
            layoutWindLinesIntensity?.visibility = if (isEnabled) android.view.View.VISIBLE else android.view.View.GONE
            switchWindLines.setOnCheckedChangeListener { _, isChecked ->
                configManager.setWindLinesEnabled(isChecked)
                layoutWindLinesIntensity?.visibility = if (isChecked) android.view.View.VISIBLE else android.view.View.GONE
                updateSummaries()
            }
        }

        setupSlider(
            R.id.seekBarWindLinesIntensity,
            R.id.textViewWindLinesIntensityValue,
            configManager.getWindLinesIntensity()
        ) { value ->
            configManager.setWindLinesIntensity(value)
        }

        // 3. Setup Lluvia Controls
        // Rain intensity is set up dynamically in refreshStormCardPreviews()

        setupSlider(
            R.id.seekBarRainSpeed,
            R.id.textViewRainSpeedValue,
            configManager.getRainSpeed()
        ) { value ->
            configManager.setRainSpeed(value)
        }

        // Rain color & spawn mode are set up dynamically in refreshStormCardPreviews()

        val switchScreenDroplets = findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.switchScreenDroplets)
        val layoutScreenDropletsSize = findViewById<android.view.View>(R.id.layoutScreenDropletsSize)
        if (switchScreenDroplets != null) {
            val isEnabled = configManager.isScreenDropletsEnabled()
            switchScreenDroplets.isChecked = isEnabled
            layoutScreenDropletsSize?.visibility = if (isEnabled) android.view.View.VISIBLE else android.view.View.GONE
            switchScreenDroplets.setOnCheckedChangeListener { _, isChecked ->
                configManager.setScreenDropletsEnabled(isChecked)
                layoutScreenDropletsSize?.visibility = if (isChecked) android.view.View.VISIBLE else android.view.View.GONE
                updateSummaries()
            }
        }

        setupSlider(
            R.id.seekBarScreenDropletsSize,
            R.id.textViewScreenDropletsSizeValue,
            configManager.getScreenDropletsSize()
        ) { value ->
            configManager.setScreenDropletsSize(value)
        }

        // 4. Setup Rayos Controls
        setupSlider(
            R.id.seekBarLightningFrequency,
            R.id.textViewLightningFrequencyValue,
            configManager.getLightningFrequency()
        ) { value ->
            configManager.setLightningFrequency(value)
        }

        // Lightning color is set up dynamically in refreshStormCardPreviews()

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

        // Cloud flash color is set up dynamically in refreshStormCardPreviews()

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
        refreshStormCardPreviews()

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
            "Bosque y Río (Color)",
            "Silueta de Ciudad",
            "Valle de Flores (Color)",
            "Cascada y Acantilado (Color)",
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

        // Setup Sunny Gyroscope Parallax
        val switchSunnyGyro = findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.switchSunnyGyro)
        if (switchSunnyGyro != null) {
            switchSunnyGyro.isChecked = configManager.isSunnyGyroEnabled()
            switchSunnyGyro.setOnCheckedChangeListener { _, isChecked ->
                configManager.setSunnyGyroEnabled(isChecked)
                updateSummaries()
            }
        }

        // Setup Sunny Touch Burst
        val switchSunnyTouchBurst = findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.switchSunnyTouchBurst)
        if (switchSunnyTouchBurst != null) {
            switchSunnyTouchBurst.isChecked = configManager.isSunnyTouchBurstEnabled()
            switchSunnyTouchBurst.setOnCheckedChangeListener { _, isChecked ->
                configManager.setSunnyTouchBurstEnabled(isChecked)
                updateSummaries()
            }
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
            configManager.setSunnyBackgroundIndex(8)
            updateCustomSunnyBackgroundViewsVisibility(8)
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

        if (position == 8) {
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

    private fun refreshStormCardPreviews() {
        // Rain intensity
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

        // Rain color
        val rainColors = arrayOf("Azul", "Blanco", "Rojo", "Verde", "Amarillo", "Morado")
        setupDropdown(
            R.id.spinnerRainColor,
            rainColors,
            configManager.getRainColorIndex()
        ) { position ->
            configManager.setRainColorIndex(position)
            refreshStormCardPreviews() // Refresh rain intensity & spawn mode previews with new color!
        }

        // Rain spawn mode
        val rainSpawnModes = arrayOf("Borde Superior", "Debajo de las Nubes", "Todos Lados")
        setupDropdown(
            R.id.spinnerRainSpawnMode,
            rainSpawnModes,
            configManager.getRainSpawnMode()
        ) { position ->
            configManager.setRainSpawnMode(position)
        }

        // Lightning color (White, Blue, Yellow, Red, Green, Purple, Random)
        val lightningColors = arrayOf("Blanco", "Azul", "Amarillo", "Rojo", "Verde", "Morado", "Aleatorio")
        setupDropdown(
            R.id.spinnerLightningColor,
            lightningColors,
            configManager.getLightningColorIndex()
        ) { position ->
            configManager.setLightningColorIndex(position)
            refreshStormCardPreviews() // Refresh cloud flash color preview with new lightning color!
        }

        // Cloud flash color
        setupDropdown(
            R.id.spinnerCloudFlashColor,
            lightningColors,
            configManager.getCloudFlashColorIndex()
        ) { position ->
            configManager.setCloudFlashColorIndex(position)
        }
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
        viewId: Int,
        options: Array<String>,
        initialIndex: Int,
        onSelected: (Int) -> Unit
    ) {
        val view = findViewById<View>(viewId)
        if (view is LinearLayout) {
            populateCardSelector(view, options, initialIndex, onSelected)
        } else if (view is AutoCompleteTextView) {
            view.setup(options, initialIndex) { position ->
                onSelected(position)
                updateSummaries()
            }
        }
    }

    private fun populateCardSelector(
        container: LinearLayout,
        options: Array<String>,
        initialIndex: Int,
        onSelected: (Int) -> Unit
    ) {
        container.removeAllViews()

        val cardViews = ArrayList<com.google.android.material.card.MaterialCardView>()

        val activeColor = Color.parseColor("#00E5FF")
        val inactiveColor = Color.parseColor("#E0E0E6")
        
        val activeBgColor = Color.parseColor("#2D2D3D")
        val inactiveBgColor = Color.parseColor("#21212A")
        
        val strokeActiveColor = Color.parseColor("#00E5FF")
        val strokeInactiveColor = Color.parseColor("#3A3A4A")

        for (i in options.indices) {
            val optionText = options[i]

            val card = com.google.android.material.card.MaterialCardView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    92.dpToPx(),
                    115.dpToPx()
                ).apply {
                    setMargins(0, 0, 10.dpToPx(), 0)
                }
                radius = 12.dpToPx().toFloat()
                strokeWidth = (1 * resources.displayMetrics.density).toInt()
                strokeColor = strokeInactiveColor
                cardElevation = 0f
                setCardBackgroundColor(inactiveBgColor)
                isClickable = true
                isFocusable = true
            }

            val innerLayout = LinearLayout(this).apply {
                layoutParams = android.widget.FrameLayout.LayoutParams(
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT
                )
                orientation = LinearLayout.VERTICAL
                gravity = android.view.Gravity.CENTER
                setPadding(8.dpToPx(), 8.dpToPx(), 8.dpToPx(), 8.dpToPx())
            }

            val previewFrame = android.widget.FrameLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    48.dpToPx(),
                    64.dpToPx()
                ).apply {
                    setMargins(0, 0, 0, 6.dpToPx())
                }
                val drawable = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = 6 * resources.displayMetrics.density
                    setColor(Color.parseColor("#15151A"))
                    setStroke((1 * resources.displayMetrics.density).toInt(), Color.parseColor("#3A3A4A"))
                }
                background = drawable
            }

            // Customize preview Frame based on the parent container setting ID
            val activeRainColorHex = when (configManager.getRainColorIndex()) {
                0 -> "#1E90FF" // Azul
                1 -> "#FFFFFF" // Blanco
                2 -> "#FF3030" // Rojo
                3 -> "#00FF7F" // Verde
                4 -> "#FFD700" // Amarillo
                5 -> "#8A2BE2" // Morado
                else -> "#1E90FF"
            }
            val activeRainColor = Color.parseColor(activeRainColorHex)

            val activeLightningColorHex = when (configManager.getLightningColorIndex()) {
                0 -> "#FFFFFF" // Blanco
                1 -> "#6699FF" // Azul
                2 -> "#FFE533" // Amarillo
                3 -> "#FF3333" // Rojo
                4 -> "#33FF33" // Verde
                5 -> "#CC4DFF" // Morado
                6 -> "#FFE533" // Random
                else -> "#FFFFFF"
            }
            val activeLightningColor = Color.parseColor(activeLightningColorHex)
            val activeWindDir = configManager.getWindDirection()

            if (container.id != R.id.spinnerBackgroundMode) {
                val stormPreview = StormCardPreviewView(
                    this,
                    container.id,
                    i,
                    activeRainColor,
                    activeWindDir,
                    activeLightningColor
                ).apply {
                    layoutParams = android.widget.FrameLayout.LayoutParams(
                        android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                        android.widget.FrameLayout.LayoutParams.MATCH_PARENT
                    )
                }
                previewFrame.addView(stormPreview)
            } else {
                if (i == 0) {
                    val colors = intArrayOf(Color.parseColor("#08080C"), Color.parseColor("#12121A"))
                    val gradient = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, colors).apply {
                        cornerRadius = 6 * resources.displayMetrics.density
                    }
                    previewFrame.background = gradient
                } else if (i == 8) {
                    val file = java.io.File(filesDir, "custom_background.png")
                    val imageView = ImageView(this).apply {
                        layoutParams = android.widget.FrameLayout.LayoutParams(
                            android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                            android.widget.FrameLayout.LayoutParams.MATCH_PARENT
                        )
                        scaleType = ImageView.ScaleType.CENTER_CROP
                    }
                    if (file.exists()) {
                        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                        if (bitmap != null) {
                            imageView.setImageBitmap(bitmap)
                        } else {
                            imageView.setImageResource(android.R.drawable.ic_menu_gallery)
                        }
                    } else {
                        imageView.setImageResource(android.R.drawable.ic_menu_gallery)
                        imageView.setColorFilter(Color.parseColor("#6C6C75"))
                        imageView.scaleType = ImageView.ScaleType.CENTER
                    }
                    previewFrame.addView(imageView)
                } else {
                    val assetPaths = arrayOf(
                        "background/background.jpg",
                        "background/background_02.png",
                        "background/background_03.png",
                        "background/background_04.png",
                        "background/background_05.png",
                        "background/background_06.png",
                        "background/background_07.png"
                    )
                    val assetPath = assetPaths.getOrNull(i - 1) ?: "background/background.jpg"
                    
                    val imageView = ImageView(this).apply {
                        layoutParams = android.widget.FrameLayout.LayoutParams(
                            android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                            android.widget.FrameLayout.LayoutParams.MATCH_PARENT
                        )
                        scaleType = ImageView.ScaleType.CENTER_CROP
                    }
                    
                    val cached = backgroundBitmapCache[assetPath]
                    if (cached != null) {
                        imageView.setImageBitmap(cached)
                    } else {
                        val bitmap = loadThumbnailFromAsset(this, assetPath, 48.dpToPx(), 64.dpToPx())
                        if (bitmap != null) {
                            backgroundBitmapCache[assetPath] = bitmap
                            imageView.setImageBitmap(bitmap)
                        }
                    }
                    previewFrame.addView(imageView)
                }
            }

            val tv = TextView(this).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                text = optionText
                setTextColor(inactiveColor)
                textSize = 11f
                typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
                gravity = android.view.Gravity.CENTER
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
            }

            innerLayout.addView(previewFrame)
            innerLayout.addView(tv)
            card.addView(innerLayout)
            container.addView(card)
            cardViews.add(card)

            card.setOnClickListener {
                for (j in cardViews.indices) {
                    val isSelected = (j == i)
                    val c = cardViews[j]
                    val inner = c.getChildAt(0) as LinearLayout
                    val t = inner.getChildAt(1) as TextView
                    if (isSelected) {
                        c.strokeWidth = (3 * resources.displayMetrics.density).toInt()
                        c.strokeColor = strokeActiveColor
                        c.setCardBackgroundColor(activeBgColor)
                        t.setTextColor(activeColor)
                    } else {
                        c.strokeWidth = (1 * resources.displayMetrics.density).toInt()
                        c.strokeColor = strokeInactiveColor
                        c.setCardBackgroundColor(inactiveBgColor)
                        t.setTextColor(inactiveColor)
                    }
                }
                onSelected(i)
                updateSummaries()
            }
        }

        if (initialIndex in options.indices) {
            val selectedCard = cardViews[initialIndex]
            val inner = selectedCard.getChildAt(0) as LinearLayout
            val selectedText = inner.getChildAt(1) as TextView
            selectedCard.strokeWidth = (3 * resources.displayMetrics.density).toInt()
            selectedCard.strokeColor = strokeActiveColor
            selectedCard.setCardBackgroundColor(activeBgColor)
            selectedText.setTextColor(activeColor)
        }
    }

    private fun Int.dpToPx(): Int {
        val density = resources.displayMetrics.density
        return (this * density).toInt()
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
            R.id.seekBarWindLinesIntensity -> {
                textView.text = "$value%"
            }
            R.id.seekBarCloudDynamicsSpeed -> {
                textView.text = if (value == 0) "Desactivado" else "$value%"
            }
            R.id.seekBarRainSpeed -> {
                textView.text = "$value%"
            }
            R.id.seekBarScreenDropletsSize -> {
                val desc = when {
                    value < 50 -> "Pequeñas"
                    value < 90 -> "Medianas"
                    value <= 110 -> "Estándar"
                    value <= 160 -> "Grandes"
                    else -> "Gigantes"
                }
                textView.text = "$value% • $desc"
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
            1 -> "Neutro"
            2 -> "Derecha"
            else -> "Izquierda"
        }
        val windLinesEnabled = configManager.isWindLinesEnabled()
        val windLinesText = if (windLinesEnabled) "Sí (${configManager.getWindLinesIntensity()}%)" else "No"
        summaryCloudsWind.text = Html.fromHtml(
            "Densidad: <font color='$accentColor'>$cloudDensity%</font> • Viento: <font color='$accentColor'>$windDirText</font> ($windIntensity%) • Líneas: <font color='$accentColor'>$windLinesText</font>",
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
        val rainSpawnModeText = when (configManager.getRainSpawnMode()) {
            0 -> "Borde"
            1 -> "Nubes"
            2 -> "Todos Lados"
            else -> "Borde"
        }
        val screenDropletsText = if (configManager.isScreenDropletsEnabled()) {
            "Activas (${configManager.getScreenDropletsSize()}%)"
        } else {
            "Desactivadas"
        }
        summaryRain.text = Html.fromHtml(
            "Intensidad: <font color='$accentColor'>$rainIntName</font> • Origen: <font color='$accentColor'>$rainSpawnModeText</font> • Color: <font color='$accentColor'>$rainColorText</font> • Vel: <font color='$accentColor'>$rainSpeed%</font> • Gotas Pantalla: <font color='$accentColor'>$screenDropletsText</font>",
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
            4 -> "Bosque/Río"
            5 -> "Ciudad"
            6 -> "Valle Flores"
            7 -> "Cascada"
            8 -> "Galería"
            else -> "Degradado"
        }
        val godRaysStatus = if (configManager.isSunnyGodRaysEnabled()) "Activo" else "Inactivo"
        val lensFlareStatus = if (configManager.isSunnyLensFlareEnabled()) "Activo" else "Inactivo"
        val gyroStatus = if (configManager.isSunnyGyroEnabled()) "Activo" else "Inactivo"
        val touchBurstStatus = if (configManager.isSunnyTouchBurstEnabled()) "Activo" else "Inactivo"
        summarySunny.text = Html.fromHtml(
            "Tema: <font color='$accentColor'>$sunnyThemeText</font> • Sol: <font color='$accentColor'>$sunDirText</font> • Fondo: <font color='$accentColor'>$sunnyBgText</font><br/>Rayos: <font color='$accentColor'>$godRaysStatus</font> • Destellos: <font color='$accentColor'>$lensFlareStatus</font> • Giroscopio: <font color='$accentColor'>$gyroStatus</font> • Ráfaga: <font color='$accentColor'>$touchBurstStatus</font>",
            Html.FROM_HTML_MODE_LEGACY
        )
    }

    private fun loadThumbnailFromAsset(context: Context, path: String, reqWidth: Int, reqHeight: Int): Bitmap? {
        return try {
            context.assets.open(path).use { inputStream ->
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeStream(inputStream, null, options)
                inputStream.close()
                
                options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
                options.inJustDecodeBounds = false
                
                context.assets.open(path).use { stream ->
                    BitmapFactory.decodeStream(stream, null, options)
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    private fun Float.dpToPx(): Float {
        val density = resources.displayMetrics.density
        return this * density
    }

    private inner class StormCardPreviewView(
        context: Context,
        val containerId: Int,
        val optionIndex: Int,
        val activeRainColor: Int,
        val activeWindDir: Int,
        val activeLightningColor: Int
    ) : View(context) {

        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val path = Path()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return

        when (containerId) {
            R.id.spinnerWindDirection -> {
                paint.color = Color.parseColor("#8A2BE2")
                paint.strokeWidth = 2.dpToPx().toFloat()
                paint.style = Paint.Style.STROKE
                paint.strokeCap = Paint.Cap.ROUND
                
                when (optionIndex) {
                    0 -> { // Left (blowing left)
                        drawWindLine(canvas, w * 0.8f, h * 0.3f, w * 0.2f, h * 0.3f)
                        drawWindLine(canvas, w * 0.9f, h * 0.5f, w * 0.1f, h * 0.5f)
                        drawWindLine(canvas, w * 0.7f, h * 0.7f, w * 0.3f, h * 0.7f)
                    }
                    1 -> { // Neutro (vertical/down)
                        drawWindLine(canvas, w * 0.3f, h * 0.2f, w * 0.3f, h * 0.8f)
                        drawWindLine(canvas, w * 0.5f, h * 0.1f, w * 0.5f, h * 0.9f)
                        drawWindLine(canvas, w * 0.7f, h * 0.2f, w * 0.7f, h * 0.8f)
                    }
                    2 -> { // Right (blowing right)
                        drawWindLine(canvas, w * 0.2f, h * 0.3f, w * 0.8f, h * 0.3f)
                        drawWindLine(canvas, w * 0.1f, h * 0.5f, w * 0.9f, h * 0.5f)
                        drawWindLine(canvas, w * 0.3f, h * 0.7f, w * 0.7f, h * 0.7f)
                    }
                }
            }
            
            R.id.spinnerRainIntensity -> {
                val density = when (optionIndex) {
                    0 -> 0 // Nada
                    1 -> 4 // Pocas
                    2 -> 9 // Media
                    3 -> 16 // Alta
                    4 -> 25 // Muy alta
                    else -> 0
                }
                if (density > 0) {
                    drawRaindrops(canvas, w, h, density, activeRainColor, activeWindDir)
                }
                
                if (optionIndex == 4) {
                    drawLightningBolt(canvas, w, h, activeLightningColor)
                } else if (optionIndex == 0) {
                    paint.color = Color.parseColor("#FFD700")
                    paint.style = Paint.Style.FILL
                    canvas.drawCircle(w * 0.5f, h * 0.5f, 10.dpToPx().toFloat(), paint)
                }
            }

            R.id.spinnerRainColor -> {
                val colorHex = when (optionIndex) {
                    0 -> "#1E90FF" // Azul
                    1 -> "#FFFFFF" // Blanco
                    2 -> "#FF3030" // Rojo
                    3 -> "#00FF7F" // Verde
                    4 -> "#FFD700" // Amarillo
                    5 -> "#8A2BE2" // Morado
                    else -> "#1E90FF"
                }
                val optionColor = Color.parseColor(colorHex)
                drawRaindrops(canvas, w, h, 12, optionColor, activeWindDir)
            }

            R.id.spinnerRainSpawnMode -> {
                if (optionIndex == 1 || optionIndex == 2) {
                    drawCloudShape(canvas, w, h)
                }
                
                paint.color = activeRainColor
                paint.strokeWidth = 1.5f.dpToPx().toFloat()
                paint.style = Paint.Style.STROKE
                paint.strokeCap = Paint.Cap.ROUND
                
                val dx = when (activeWindDir) {
                    0 -> -4.dpToPx().toFloat()
                    2 -> 4.dpToPx().toFloat()
                    else -> 0f
                }
                
                when (optionIndex) {
                    0 -> { // Borde superior (rain from top)
                        for (i in 0..4) {
                            val rx = w * 0.15f + w * 0.17f * i
                            canvas.drawLine(rx, 4.dpToPx().toFloat(), rx + dx, 24.dpToPx().toFloat(), paint)
                        }
                    }
                    1 -> { // Nubes (rain only below clouds)
                        for (i in 0..3) {
                            val rx = w * 0.25f + w * 0.16f * i
                            canvas.drawLine(rx, 15.dpToPx().toFloat(), rx + dx, 35.dpToPx().toFloat(), paint)
                        }
                    }
                    2 -> { // Todos lados (rain from top and clouds)
                        for (i in 0..4) {
                            val rx = w * 0.15f + w * 0.17f * i
                            val startY = if (i % 2 == 0) 4.dpToPx().toFloat() else 15.dpToPx().toFloat()
                            canvas.drawLine(rx, startY, rx + dx, startY + 20.dpToPx(), paint)
                        }
                    }
                }
            }

            R.id.spinnerLightningColor -> {
                val colorHex = when (optionIndex) {
                    0 -> "#FFFFFF" // Blanco
                    1 -> "#6699FF" // Azul
                    2 -> "#FFE533" // Amarillo
                    3 -> "#FF3333" // Rojo
                    4 -> "#33FF33" // Verde
                    5 -> "#CC4DFF" // Morado
                    6 -> "#FFE533" // Random
                    else -> "#FFFFFF"
                }
                val lColor = Color.parseColor(colorHex)
                drawStylizedLightning(canvas, w, h, lColor, isRainbow = (optionIndex == 6))
            }

            R.id.spinnerCloudFlashColor -> {
                val colorHex = when (optionIndex) {
                    0 -> "#FFFFFF" // Blanco
                    1 -> "#6699FF" // Azul
                    2 -> "#FFE533" // Amarillo
                    3 -> "#FF3333" // Rojo
                    4 -> "#33FF33" // Verde
                    5 -> "#CC4DFF" // Morado
                    6 -> "#FFE533" // Random
                    else -> "#FFFFFF"
                }
                val glowColor = Color.parseColor(colorHex)
                
                val colors = intArrayOf(
                    adjustAlpha(glowColor, 0.7f),
                    adjustAlpha(glowColor, 0.0f)
                )
                val glowRadius = w * 0.35f
                paint.shader = RadialGradient(w/2f, h/2f, glowRadius, colors, null, Shader.TileMode.CLAMP)
                paint.style = Paint.Style.FILL
                canvas.drawCircle(w/2f, h/2f, glowRadius, paint)
                paint.shader = null
                
                drawCloudShapeCentered(canvas, w, h)
            }
        }
    }

    private fun drawWindLine(canvas: Canvas, sx: Float, sy: Float, ex: Float, ey: Float) {
        canvas.drawLine(sx, sy, ex, ey, paint)
        val angle = Math.atan2((ey - sy).toDouble(), (ex - sx).toDouble())
        val arrowSize = 4.dpToPx()
        val path = Path().apply {
            moveTo(ex, ey)
            lineTo(
                (ex - arrowSize * Math.cos(angle - Math.PI / 6)).toFloat(),
                (ey - arrowSize * Math.sin(angle - Math.PI / 6)).toFloat()
            )
            moveTo(ex, ey)
            lineTo(
                (ex - arrowSize * Math.cos(angle + Math.PI / 6)).toFloat(),
                (ey - arrowSize * Math.sin(angle + Math.PI / 6)).toFloat()
            )
        }
        canvas.drawPath(path, paint)
    }

    private fun drawRaindrops(canvas: Canvas, w: Float, h: Float, count: Int, color: Int, windDir: Int) {
        paint.color = color
        paint.strokeWidth = 1.5f.dpToPx().toFloat()
        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND

        val random = java.util.Random(optionIndex * 100L + 42)
        val dx = when (windDir) {
            0 -> -6.dpToPx().toFloat()
            2 -> 6.dpToPx().toFloat()
            else -> 0f
        }

        for (i in 0 until count) {
            val rx = random.nextFloat() * (w + Math.abs(dx)) - (if (dx > 0) dx else 0f)
            val ry = random.nextFloat() * (h - 14.dpToPx())
            val length = 8.dpToPx() + random.nextFloat() * 8.dpToPx()
            canvas.drawLine(rx, ry, rx + dx * (length / h), ry + length, paint)
        }
    }

    private fun drawLightningBolt(canvas: Canvas, w: Float, h: Float, color: Int) {
        paint.color = color
        paint.style = Paint.Style.FILL
        paint.strokeCap = Paint.Cap.ROUND
        
        val lPath = Path().apply {
            val cx = w * 0.75f
            val cy = h * 0.15f
            moveTo(cx + 2.dpToPx(), cy)
            lineTo(cx - 3.dpToPx(), cy + 6.dpToPx())
            lineTo(cx, cy + 6.dpToPx())
            lineTo(cx - 2.dpToPx(), cy + 12.dpToPx())
            lineTo(cx + 3.dpToPx(), cy + 4.dpToPx())
            lineTo(cx - 1.dpToPx(), cy + 4.dpToPx())
            close()
        }
        canvas.drawPath(lPath, paint)
    }

    private fun drawStylizedLightning(canvas: Canvas, w: Float, h: Float, color: Int, isRainbow: Boolean) {
        if (isRainbow) {
            val shader = LinearGradient(0f, 0f, 0f, h, 
                intArrayOf(Color.RED, Color.YELLOW, Color.GREEN, Color.CYAN, Color.BLUE, Color.MAGENTA),
                null, Shader.TileMode.CLAMP
            )
            paint.shader = shader
        } else {
            paint.color = color
        }
        
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2.dpToPx().toFloat()
        paint.strokeJoin = Paint.Join.ROUND
        paint.strokeCap = Paint.Cap.ROUND

        val lPath = Path().apply {
            moveTo(w * 0.5f, h * 0.1f)
            lineTo(w * 0.35f, h * 0.45f)
            lineTo(w * 0.6f, h * 0.45f)
            lineTo(w * 0.4f, h * 0.9f)
        }
        
        if (!isRainbow) {
            paint.color = adjustAlpha(color, 0.3f)
            paint.strokeWidth = 5.dpToPx().toFloat()
            canvas.drawPath(lPath, paint)
            
            paint.color = color
            paint.strokeWidth = 2.dpToPx().toFloat()
        }
        
        canvas.drawPath(lPath, paint)
        paint.shader = null
    }

    private fun drawCloudShape(canvas: Canvas, w: Float, h: Float) {
        paint.color = Color.parseColor("#4A4A5A")
        paint.style = Paint.Style.FILL
        
        canvas.drawCircle(w * 0.35f, 10.dpToPx().toFloat(), 8.dpToPx().toFloat(), paint)
        canvas.drawCircle(w * 0.65f, 10.dpToPx().toFloat(), 8.dpToPx().toFloat(), paint)
        canvas.drawCircle(w * 0.5f, 8.dpToPx().toFloat(), 10.dpToPx().toFloat(), paint)
        canvas.drawRect(w * 0.25f, 10.dpToPx().toFloat(), w * 0.75f, 16.dpToPx().toFloat(), paint)
    }

    private fun drawCloudShapeCentered(canvas: Canvas, w: Float, h: Float) {
        paint.color = Color.parseColor("#E0E0E6")
        paint.style = Paint.Style.FILL
        
        val cy = h / 2f
        canvas.drawCircle(w * 0.35f, cy + 2.dpToPx(), 7.dpToPx().toFloat(), paint)
        canvas.drawCircle(w * 0.65f, cy + 2.dpToPx(), 7.dpToPx().toFloat(), paint)
        canvas.drawCircle(w * 0.5f, cy - 1.dpToPx(), 9.dpToPx().toFloat(), paint)
        canvas.drawRect(w * 0.3f, cy + 1.dpToPx(), w * 0.7f, cy + 9.dpToPx(), paint)
    }

    private fun adjustAlpha(color: Int, factor: Float): Int {
        val alpha = Math.round(Color.alpha(color) * factor)
        val red = Color.red(color)
        val green = Color.green(color)
        val blue = Color.blue(color)
        return Color.argb(alpha, red, green, blue)
    }
}
}
