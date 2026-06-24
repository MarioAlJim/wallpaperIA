package com.wolf.wallpaper

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Html
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.card.MaterialCardView
import com.google.android.material.slider.Slider
import com.google.android.material.switchmaterial.SwitchMaterial
import com.wolf.wallpaper.core.ConfigManager
import java.io.File
import java.io.FileOutputStream

class SunnySettingsFragment : Fragment() {

    private lateinit var configManager: ConfigManager

    private val pickSunnyImageLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            saveCustomSunnyBackground(uri)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_sunny_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configManager = (requireActivity() as WallpaperSettingsActivity).configManager

        // 1. Setup Accordions
        setupAccordion(view, R.id.headerSunnyClouds, R.id.contentSunnyClouds, R.id.dividerSunnyClouds, R.id.arrowSunnyClouds)
        setupAccordion(view, R.id.headerSunnySun, R.id.contentSunnySun, R.id.dividerSunnySun, R.id.arrowSunnySun)
        setupAccordion(view, R.id.headerSunnyMoon, R.id.contentSunnyMoon, R.id.dividerSunnyMoon, R.id.arrowSunnyMoon)
        setupAccordion(view, R.id.headerSunnyCombined, R.id.contentSunnyCombined, R.id.dividerSunnyCombined, R.id.arrowSunnyCombined)

        // 2. Setup Cloud Density Slider (Sunny clouds)
        setupSlider(view, R.id.seekBarCloudDensity, R.id.textViewCloudDensityValue, configManager.getCloudDensity()) { value ->
            configManager.setCloudDensity(value)
        }

        // 3. Setup Sunny Main controls
        setupSlider(view, R.id.seekBarSunSize, R.id.textViewSunSizeValue, configManager.getSunSize()) { value ->
            configManager.setSunSize(value)
        }

        setupSlider(view, R.id.seekBarSunSpeed, R.id.textViewSunSpeedValue, configManager.getSunSpeed()) { value ->
            configManager.setSunSpeed(value)
        }

        setupSunnyThemeCards(view)
        setupSunDirectionCards(view)

        setupSlider(view, R.id.seekBarSunMoveSpeed, R.id.textViewSunMoveSpeedValue, configManager.getSunMoveSpeed()) { value ->
            configManager.setSunMoveSpeed(value)
        }
        setupSunStationaryCards(view)
        updateSunMoveSpeedSliderVisibility(view, configManager.getSunPathDirection())

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
        setupDropdown(view, R.id.spinnerSunnyBackground, sunnyBackgrounds, configManager.getSunnyBackgroundIndex()) { position ->
            configManager.setSunnyBackgroundIndex(position)
            updateCustomSunnyBackgroundViewsVisibility(view, position)
        }

        view.findViewById<Button>(R.id.btnSelectCustomSunnyBg)?.setOnClickListener {
            pickSunnyImageLauncher.launch("image/*")
        }
        updateCustomSunnyBackgroundViewsVisibility(view, configManager.getSunnyBackgroundIndex())

        // Setup Sunny God Rays
        val switchSunnyGodRays = view.findViewById<SwitchMaterial>(R.id.switchSunnyGodRays)
        val layoutSunnyGodRaysIntensity = view.findViewById<View>(R.id.layoutSunnyGodRaysIntensity)
        if (switchSunnyGodRays != null) {
            val isEnabled = configManager.isSunnyGodRaysEnabled()
            switchSunnyGodRays.isChecked = isEnabled
            layoutSunnyGodRaysIntensity?.visibility = if (isEnabled) View.VISIBLE else View.GONE
            switchSunnyGodRays.setOnCheckedChangeListener { _, isChecked ->
                configManager.setSunnyGodRaysEnabled(isChecked)
                layoutSunnyGodRaysIntensity?.visibility = if (isChecked) View.VISIBLE else View.GONE
                updateSummaries(view)
            }
        }
        setupSlider(view, R.id.seekBarSunnyGodRaysIntensity, R.id.textViewSunnyGodRaysIntensityValue, configManager.getSunnyGodRaysIntensity()) { value ->
            configManager.setSunnyGodRaysIntensity(value)
        }

        // Setup Sunny Lens Flare
        val switchSunnyLensFlare = view.findViewById<SwitchMaterial>(R.id.switchSunnyLensFlare)
        val layoutSunnyLensFlareIntensity = view.findViewById<View>(R.id.layoutSunnyLensFlareIntensity)
        if (switchSunnyLensFlare != null) {
            val isEnabled = configManager.isSunnyLensFlareEnabled()
            switchSunnyLensFlare.isChecked = isEnabled
            layoutSunnyLensFlareIntensity?.visibility = if (isEnabled) View.VISIBLE else View.GONE
            switchSunnyLensFlare.setOnCheckedChangeListener { _, isChecked ->
                configManager.setSunnyLensFlareEnabled(isChecked)
                layoutSunnyLensFlareIntensity?.visibility = if (isChecked) View.VISIBLE else View.GONE
                updateSummaries(view)
            }
        }
        setupSlider(view, R.id.seekBarSunnyLensFlareIntensity, R.id.textViewSunnyLensFlareIntensityValue, configManager.getSunnyLensFlareIntensity()) { value ->
            configManager.setSunnyLensFlareIntensity(value)
        }

        // Setup Sunny Gyroscope Parallax
        val switchSunnyGyro = view.findViewById<SwitchMaterial>(R.id.switchSunnyGyro)
        if (switchSunnyGyro != null) {
            switchSunnyGyro.isChecked = configManager.isSunnyGyroEnabled()
            switchSunnyGyro.setOnCheckedChangeListener { _, isChecked ->
                configManager.setSunnyGyroEnabled(isChecked)
                updateSummaries(view)
            }
        }

        // Setup Sunny Touch Burst
        val switchSunnyTouchBurst = view.findViewById<SwitchMaterial>(R.id.switchSunnyTouchBurst)
        if (switchSunnyTouchBurst != null) {
            switchSunnyTouchBurst.isChecked = configManager.isSunnyTouchBurstEnabled()
            switchSunnyTouchBurst.setOnCheckedChangeListener { _, isChecked ->
                configManager.setSunnyTouchBurstEnabled(isChecked)
                updateSummaries(view)
            }
        }

        // Setup Night/Combined Mode Controls
        setupTimeModeCards(view)
        setupMoonPhaseCards(view)
        setupMoonDirectionCards(view)
        setupMoonStationaryCards(view)
        setupStarColorCards(view)
        setupStarModeCards(view)
        setupNightAndCombinedSliders(view)

        // Init summaries and views
        updateSummaries(view)
    }

    private fun setupAccordion(parent: View, headerId: Int, contentId: Int, dividerId: Int, arrowId: Int) {
        val header = parent.findViewById<RelativeLayout>(headerId)
        val content = parent.findViewById<LinearLayout>(contentId)
        val divider = parent.findViewById<View>(dividerId)
        val arrow = parent.findViewById<ImageView>(arrowId)

        header.setOnClickListener {
            val isVisible = content.visibility == View.VISIBLE
            val parentViewGroup = content.parent as? ViewGroup ?: return@setOnClickListener
            TransitionManager.beginDelayedTransition(parentViewGroup, AutoTransition().apply {
                duration = 250
            })
            content.visibility = if (isVisible) View.GONE else View.VISIBLE
            divider?.visibility = if (isVisible) View.GONE else View.VISIBLE
            arrow.animate().rotation(if (isVisible) 0f else 180f).setDuration(250).start()
        }
    }

    private fun setupSlider(parent: View, viewId: Int, textViewId: Int, initialValue: Int, onProgressChanged: (Int) -> Unit) {
        val slider = parent.findViewById<Slider>(viewId)
        val textView = parent.findViewById<TextView>(textViewId)
        if (slider != null) {
            slider.value = initialValue.toFloat()
            slider.addOnChangeListener { _, value, fromUser ->
                if (fromUser) {
                    onProgressChanged(value.toInt())
                }
                if (textView != null) {
                    updateTextView(viewId, textView, value.toInt())
                    updateSummaries(parent)
                }
            }
            if (textView != null) {
                updateTextView(viewId, textView, initialValue)
            }
        }
    }

    private fun updateTextView(sliderId: Int, textView: TextView, value: Int) {
        when (sliderId) {
            R.id.seekBarCloudDensity -> {
                val desc = when {
                    value <= 0 -> "Cielo despejado"
                    value <= 25 -> "Pocas nubes"
                    value <= 50 -> "Nublado"
                    value <= 75 -> "Muy nublado"
                    else -> "Cubierto"
                }
                textView.text = "$value% • $desc"
            }
            R.id.seekBarSunSize -> {
                val desc = when {
                    value <= 20 -> "Pequeño"
                    value <= 50 -> "Normal"
                    value <= 80 -> "Grande"
                    else -> "Gigante"
                }
                textView.text = "$value% • $desc"
            }
            R.id.seekBarSunSpeed -> {
                val desc = when {
                    value <= 20 -> "Lento"
                    value <= 50 -> "Normal"
                    value <= 80 -> "Rápido"
                    else -> "Pulsación extrema"
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
            R.id.seekBarSunCustomX -> {
                textView.text = "$value%"
            }
            R.id.seekBarSunCustomY -> {
                textView.text = "$value%"
            }
            R.id.seekBarSunnyGodRaysIntensity -> {
                val desc = when {
                    value <= 20 -> "Tenue"
                    value <= 50 -> "Normal"
                    value <= 80 -> "Brillante"
                    else -> "Resplandor total"
                }
                textView.text = "$value% • $desc"
            }
            R.id.seekBarSunnyLensFlareIntensity -> {
                val desc = when {
                    value <= 20 -> "Sutil"
                    value <= 50 -> "Normal"
                    value <= 80 -> "Llamativo"
                    else -> "Destello total"
                }
                textView.text = "$value% • $desc"
            }
            R.id.seekBarMoonMoveSpeed -> {
                val desc = when {
                    value <= 20 -> "Lento"
                    value <= 50 -> "Normal"
                    value <= 85 -> "Rápido"
                    else -> "Extremo"
                }
                textView.text = "$value% • $desc"
            }
            R.id.seekBarStarDensity -> {
                val desc = when {
                    value <= 20 -> "Pocas"
                    value <= 50 -> "Normal"
                    value <= 85 -> "Muchas"
                    else -> "Cielo lleno"
                }
                textView.text = "$value% • $desc"
            }
            R.id.seekBarNightCloudDensity -> {
                val desc = when {
                    value <= 0 -> "Sin nubes"
                    value <= 30 -> "Pocas"
                    value <= 60 -> "Normal"
                    else -> "Cubierto"
                }
                textView.text = "$value% • $desc"
            }
            R.id.seekBarGradientCycleSpeed -> {
                val desc = when {
                    value <= 20 -> "Muy lento"
                    value <= 50 -> "Normal"
                    value <= 85 -> "Rápido"
                    else -> "Muy rápido"
                }
                textView.text = "$value% • $desc"
            }
        }
    }

    private fun setupDropdown(parent: View, viewId: Int, options: Array<String>, initialIndex: Int, onSelected: (Int) -> Unit) {
        val view = parent.findViewById<AutoCompleteTextView>(viewId)
        if (view != null) {
            val adapter = ArrayAdapter(requireContext(), R.layout.spinner_dropdown_item, options)
            view.setAdapter(adapter)
            view.setText(options.getOrNull(initialIndex), false)
            view.setOnItemClickListener { _, _, position, _ ->
                onSelected(position)
                updateSummaries(parent)
            }
        }
    }

    private fun setupSunnyThemeCards(parent: View) {
        val cardNoon = parent.findViewById<MaterialCardView>(R.id.cardSunnyThemeNoon)
        val cardSunset = parent.findViewById<MaterialCardView>(R.id.cardSunnyThemeSunset)
        val cardDusk = parent.findViewById<MaterialCardView>(R.id.cardSunnyThemeDusk)
        val cardCustom = parent.findViewById<MaterialCardView>(R.id.cardSunnyThemeCustom)

        val cards = listOf(cardNoon, cardSunset, cardDusk, cardCustom)
        val customLayout = parent.findViewById<View>(R.id.layoutSunnyCustomGradient)

        setCardGradientPreview(parent.findViewById(R.id.viewThemeNoonPreview), 0xFF0566D9.toInt(), 0xFF8DCCEF.toInt())
        setCardGradientPreview(parent.findViewById(R.id.viewThemeSunsetPreview), 0xFF1A0D40.toInt(), 0xFFF2731A.toInt())
        setCardGradientPreview(parent.findViewById(R.id.viewThemeDuskPreview), 0xFF261A59.toInt(), 0xFFE6808C.toInt())

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
                val p = customLayout.parent as? ViewGroup
                p?.let {
                    TransitionManager.beginDelayedTransition(it, AutoTransition().apply { duration = 200 })
                }
                customLayout.visibility = if (selectedTheme == 3) View.VISIBLE else View.GONE
            }
            updateSunnyThemePreviews(parent)
        }

        val initialTheme = configManager.getSunnyTheme()
        updateThemeSelection(initialTheme)

        cards.forEachIndexed { index, card ->
            card?.setOnClickListener {
                configManager.setSunnyTheme(index)
                updateThemeSelection(index)
                updateSummaries(parent)
            }
        }

        parent.findViewById<View>(R.id.btnCustomSkyTopColor)?.setOnClickListener {
            showColorPickerDialog(parent, true)
        }
        parent.findViewById<View>(R.id.btnCustomSkyBottomColor)?.setOnClickListener {
            showColorPickerDialog(parent, false)
        }
    }

    private fun updateSunnyThemePreviews(parent: View) {
        val top = configManager.getSunnyCustomSkyTopColor()
        val bottom = configManager.getSunnyCustomSkyBottomColor()
        setCardGradientPreview(parent.findViewById(R.id.viewThemeCustomPreview), top, bottom)
        setCircleColorPreview(parent.findViewById(R.id.viewCustomSkyTopColorPreview), top)
        setCircleColorPreview(parent.findViewById(R.id.viewCustomSkyBottomColorPreview), bottom)
        
        parent.findViewById<TextView>(R.id.textViewCustomSkyTopColorCode)?.text = String.format("#%06X", 0xFFFFFF and top)
        parent.findViewById<TextView>(R.id.textViewCustomSkyBottomColorCode)?.text = String.format("#%06X", 0xFFFFFF and bottom)
    }

    private fun setCardGradientPreview(view: View?, topColor: Int, bottomColor: Int) {
        if (view == null) return
        val gd = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(topColor, bottomColor)
        ).apply {
            cornerRadius = 6 * resources.displayMetrics.density
        }
        view.background = gd
    }

    private fun setCircleColorPreview(view: View?, color: Int) {
        if (view == null) return
        val gd = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(color)
            setStroke((1 * resources.displayMetrics.density).toInt(), Color.parseColor("#3A3A4A"))
        }
        view.background = gd
    }

    private fun setupSunDirectionCards(parent: View) {
        val cardL2R = parent.findViewById<MaterialCardView>(R.id.cardSunDirL2R)
        val cardR2L = parent.findViewById<MaterialCardView>(R.id.cardSunDirR2L)
        val cardStationary = parent.findViewById<MaterialCardView>(R.id.cardSunDirStationary)
        val cardRandom = parent.findViewById<MaterialCardView>(R.id.cardSunDirRandom)
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
            updateSunMoveSpeedSliderVisibility(parent, selectedDir)
        }

        updateCardSelection(configManager.getSunPathDirection())

        cards.forEachIndexed { index, card ->
            card?.setOnClickListener {
                configManager.setSunPathDirection(index)
                updateCardSelection(index)
                updateSummaries(parent)
            }
        }
    }

    private fun setupSunStationaryCards(parent: View) {
        val cardTopLeft = parent.findViewById<MaterialCardView>(R.id.cardSunPosTopLeft)
        val cardTopRight = parent.findViewById<MaterialCardView>(R.id.cardSunPosTopRight)
        val cardCenter = parent.findViewById<MaterialCardView>(R.id.cardSunPosCenter)
        val cardLeft = parent.findViewById<MaterialCardView>(R.id.cardSunPosLeft)
        val cardRight = parent.findViewById<MaterialCardView>(R.id.cardSunPosRight)
        val cardCustom = parent.findViewById<MaterialCardView>(R.id.cardSunPosCustom)
        val cards = listOf(cardTopLeft, cardTopRight, cardCenter, cardLeft, cardRight, cardCustom)
        val customLayout = parent.findViewById<View>(R.id.layoutSunCustomXY)

        fun updateCardSelection(selectedPosition: Int) {
            cards.forEachIndexed { index, card ->
                if (card != null) {
                    if (index == selectedPosition) {
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
                val p = customLayout.parent as? ViewGroup
                p?.let { TransitionManager.beginDelayedTransition(it, AutoTransition().apply { duration = 200 }) }
                customLayout.visibility = if (selectedPosition == 5) View.VISIBLE else View.GONE
            }
        }

        updateCardSelection(configManager.getSunStationaryPosition())

        // Wire sliders for custom X/Y
        setupSlider(parent, R.id.seekBarSunCustomX, R.id.textViewSunCustomXValue, configManager.getSunCustomX()) { value ->
            configManager.setSunCustomX(value)
        }
        setupSlider(parent, R.id.seekBarSunCustomY, R.id.textViewSunCustomYValue, configManager.getSunCustomY()) { value ->
            configManager.setSunCustomY(value)
        }

        cards.forEachIndexed { index, card ->
            card?.setOnClickListener {
                configManager.setSunStationaryPosition(index)
                updateCardSelection(index)
                updateSummaries(parent)
            }
        }
    }

    private fun updateSunMoveSpeedSliderVisibility(parent: View, direction: Int) {
        val layoutSpeed = parent.findViewById<View>(R.id.layoutSunMoveSpeed)
        val layoutStationary = parent.findViewById<View>(R.id.layoutSunStationaryPosition)
        if (direction == 2) {
            layoutSpeed?.visibility = View.GONE
            layoutStationary?.visibility = View.VISIBLE
        } else {
            layoutSpeed?.visibility = View.VISIBLE
            layoutStationary?.visibility = View.GONE
        }
    }

    private fun saveCustomSunnyBackground(uri: android.net.Uri) {
        try {
            val inputStream = requireContext().contentResolver.openInputStream(uri) ?: return
            val outputFile = File(requireContext().filesDir, "custom_sunny_background.png")
            inputStream.use { input ->
                FileOutputStream(outputFile).use { output ->
                    input.copyTo(output)
                }
            }
            configManager.setSunnyBackgroundIndex(8)
            view?.let {
                updateCustomSunnyBackgroundViewsVisibility(it, 8)
                updateSummaries(it)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateCustomSunnyBackgroundViewsVisibility(parent: View, position: Int) {
        val btnSelect = parent.findViewById<View>(R.id.btnSelectCustomSunnyBg)
        val container = parent.findViewById<View>(R.id.tvCustomSunnyBgStatus)
        if (position == 8) {
            btnSelect?.visibility = View.VISIBLE
            container?.visibility = View.VISIBLE
        } else {
            btnSelect?.visibility = View.GONE
            container?.visibility = View.GONE
        }
    }

    private fun setupTimeModeCards(parent: View) {
        val cardDay = parent.findViewById<MaterialCardView>(R.id.cardTimeModeDay) ?: return
        val cardNight = parent.findViewById<MaterialCardView>(R.id.cardTimeModeNight) ?: return
        val cardCombined = parent.findViewById<MaterialCardView>(R.id.cardTimeModeCombined) ?: return
        val cards = listOf(cardDay, cardNight, cardCombined)

        val imageDay = parent.findViewById<ImageView>(R.id.imageTimeModeDay)
        val imageNight = parent.findViewById<ImageView>(R.id.imageTimeModeNight)
        val imageCombined = parent.findViewById<ImageView>(R.id.imageTimeModeCombined)
        val images = listOf(imageDay, imageNight, imageCombined)

        val textDay = parent.findViewById<TextView>(R.id.textTimeModeDay)
        val textNight = parent.findViewById<TextView>(R.id.textTimeModeNight)
        val textCombined = parent.findViewById<TextView>(R.id.textTimeModeCombined)
        val texts = listOf(textDay, textNight, textCombined)

        val activeColor = Color.parseColor("#00E5FF")
        val inactiveColor = Color.parseColor("#B0B0BA")
        val activeBgColor = Color.parseColor("#2D2D3D")
        val inactiveBgColor = Color.parseColor("#21212A")
        val strokeActiveColor = Color.parseColor("#00E5FF")
        val strokeInactiveColor = Color.parseColor("#3A3A4A")

        fun selectMode(mode: Int) {
            cards.forEachIndexed { i, card ->
                val image = images[i]
                val text = texts[i]
                if (i == mode) {
                    card.strokeColor = strokeActiveColor
                    card.strokeWidth = (3 * resources.displayMetrics.density).toInt()
                    card.setCardBackgroundColor(activeBgColor)
                    text?.setTextColor(activeColor)
                    image?.setColorFilter(activeColor)
                } else {
                    card.strokeColor = strokeInactiveColor
                    card.strokeWidth = (1 * resources.displayMetrics.density).toInt()
                    card.setCardBackgroundColor(inactiveBgColor)
                    text?.setTextColor(inactiveColor)
                    image?.setColorFilter(inactiveColor)
                }
            }
            updateTimeModeVisibility(parent, mode)
        }

        val initial = configManager.getTimeMode()
        selectMode(initial)

        cards.forEachIndexed { i, card ->
            card.setOnClickListener {
                configManager.setTimeMode(i)
                selectMode(i)
                updateSummaries(parent)
            }
        }
    }

    private fun updateTimeModeVisibility(parent: View, mode: Int) {
        val cardSun = parent.findViewById<View>(R.id.cardSunnySun) ?: return
        val cardMoon = parent.findViewById<View>(R.id.cardSunnyMoon) ?: return
        val cardCombined = parent.findViewById<View>(R.id.cardSunnyCombined) ?: return
        cardSun.visibility = if (mode == 0 || mode == 2) View.VISIBLE else View.GONE
        cardMoon.visibility = if (mode == 1 || mode == 2) View.VISIBLE else View.GONE
        cardCombined.visibility = if (mode == 2) View.VISIBLE else View.GONE
    }

    private fun setupMoonPhaseCards(parent: View) {
        val ids = listOf(
            R.id.cardMoonPhase0, R.id.cardMoonPhase1, R.id.cardMoonPhase2, R.id.cardMoonPhase3,
            R.id.cardMoonPhase4, R.id.cardMoonPhase5, R.id.cardMoonPhase6, R.id.cardMoonPhase7
        )
        val cards = ids.map { id -> parent.findViewById<MaterialCardView>(id) }

        fun selectPhase(phase: Int) {
            cards.forEachIndexed { i, card ->
                card ?: return@forEachIndexed
                if (i == phase) {
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

        selectPhase(configManager.getMoonPhase())

        cards.forEachIndexed { i, card ->
            card?.setOnClickListener {
                configManager.setMoonPhase(i)
                selectPhase(i)
                updateSummaries(parent)
            }
        }
    }

    private fun setupMoonDirectionCards(parent: View) {
        val cardL2R = parent.findViewById<MaterialCardView>(R.id.cardMoonDirL2R)
        val cardR2L = parent.findViewById<MaterialCardView>(R.id.cardMoonDirR2L)
        val cardStatic = parent.findViewById<MaterialCardView>(R.id.cardMoonDirStatic)
        val cardRandom = parent.findViewById<MaterialCardView>(R.id.cardMoonDirRandom)
        val cards = listOf(cardL2R, cardR2L, cardStatic, cardRandom)

        fun selectDir(dir: Int) {
            cards.forEachIndexed { i, card ->
                card ?: return@forEachIndexed
                if (i == dir) {
                    card.strokeColor = Color.parseColor("#00E5FF")
                    card.strokeWidth = (3 * resources.displayMetrics.density).toInt()
                    card.setCardBackgroundColor(Color.parseColor("#2D2D3D"))
                } else {
                    card.strokeColor = Color.parseColor("#3A3A4A")
                    card.strokeWidth = (1 * resources.displayMetrics.density).toInt()
                    card.setCardBackgroundColor(Color.parseColor("#21212A"))
                }
            }
            updateMoonMoveSpeedVisibility(parent, dir)
        }

        selectDir(configManager.getMoonPathDirection())

        cards.forEachIndexed { i, card ->
            card?.setOnClickListener {
                configManager.setMoonPathDirection(i)
                selectDir(i)
                updateSummaries(parent)
            }
        }
    }

    private fun updateMoonMoveSpeedVisibility(parent: View, dir: Int) {
        val speedLayout = parent.findViewById<View>(R.id.layoutMoonMoveSpeed)
        val stationaryLayout = parent.findViewById<View>(R.id.layoutMoonStationaryPosition)
        if (dir == 2) {
            speedLayout?.visibility = View.GONE
            stationaryLayout?.visibility = View.VISIBLE
        } else {
            speedLayout?.visibility = View.VISIBLE
            stationaryLayout?.visibility = View.GONE
        }
    }

    private fun setupMoonStationaryCards(parent: View) {
        val ids = listOf(
            R.id.cardMoonPosTopLeft, R.id.cardMoonPosTopRight, R.id.cardMoonPosCenter,
            R.id.cardMoonPosMidLeft, R.id.cardMoonPosMidRight, R.id.cardMoonPosCustom
        )
        val cards = ids.map { id -> parent.findViewById<MaterialCardView>(id) }

        fun selectPos(pos: Int) {
            cards.forEachIndexed { i, card ->
                card ?: return@forEachIndexed
                if (i == pos) {
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

        selectPos(configManager.getMoonStationaryPosition())

        cards.forEachIndexed { i, card ->
            card?.setOnClickListener {
                configManager.setMoonStationaryPosition(i)
                selectPos(i)
                updateSummaries(parent)
            }
        }
    }

    private fun setupStarColorCards(parent: View) {
        val ids = listOf(
            R.id.cardStarColorWhite, R.id.cardStarColorBlue, R.id.cardStarColorWarm,
            R.id.cardStarColorPink, R.id.cardStarColorGreen, R.id.cardStarColorMixed
        )
        val cards = ids.map { id -> parent.findViewById<MaterialCardView>(id) }

        fun selectColor(index: Int) {
            cards.forEachIndexed { i, card ->
                card ?: return@forEachIndexed
                if (i == index) {
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

        selectColor(configManager.getStarColorIndex())

        cards.forEachIndexed { i, card ->
            card?.setOnClickListener {
                configManager.setStarColorIndex(i)
                selectColor(i)
                updateSummaries(parent)
            }
        }
    }

    private fun setupStarModeCards(parent: View) {
        val cardStatic = parent.findViewById<MaterialCardView>(R.id.cardStarModeStatic)
        val cardRandom = parent.findViewById<MaterialCardView>(R.id.cardStarModeRandom)
        val cards = listOf(cardStatic, cardRandom)

        fun selectMode(mode: Int) {
            cards.forEachIndexed { i, card ->
                card ?: return@forEachIndexed
                if (i == mode) {
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

        selectMode(configManager.getStarMode())

        cards.forEachIndexed { i, card ->
            card?.setOnClickListener {
                configManager.setStarMode(i)
                selectMode(i)
                updateSummaries(parent)
            }
        }
    }

    private fun setupNightAndCombinedSliders(parent: View) {
        setupSlider(parent, R.id.seekBarMoonMoveSpeed, R.id.textViewMoonMoveSpeedValue, configManager.getMoonMoveSpeed()) { value ->
            configManager.setMoonMoveSpeed(value)
        }
        setupSlider(parent, R.id.seekBarStarDensity, R.id.textViewStarDensityValue, configManager.getStarDensity()) { value ->
            configManager.setStarDensity(value)
        }
        setupSlider(parent, R.id.seekBarNightCloudDensity, R.id.textViewNightCloudDensityValue, configManager.getNightCloudDensity()) { value ->
            configManager.setNightCloudDensity(value)
        }
        setupSlider(parent, R.id.seekBarGradientCycleSpeed, R.id.textViewGradientCycleSpeedValue, configManager.getGradientCycleSpeed()) { value ->
            configManager.setGradientCycleSpeed(value)
        }
    }

    private fun showColorPickerDialog(parent: View, isTopColor: Boolean) {
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
                hexInput?.setText(String.format("#%06X", 0xFFFFFF and color))
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
            updateDialogColor(Color.rgb(r, g, b), updateHexText = true, updateSliders = false)
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
                        updateDialogColor(Color.parseColor(str), updateHexText = false, updateSliders = true)
                    } catch (e: Exception) {}
                }
            }
        })
        
        val presets = intArrayOf(
            0xFF0566D9.toInt(), 0xFF8DCCEF.toInt(), 0xFF1A0D40.toInt(), 0xFFF2731A.toInt(),
            0xFFE6808C.toInt(), 0xFF261A59.toInt(), 0xFFE53935.toInt(), 0xFF8E24AA.toInt(),
            0xFF00ACC1.toInt(), 0xFF43A047.toInt(), 0xFFFFB300.toInt(), 0xFFFFFFFF.toInt()
        )
        
        for (i in 0..11) {
            val presetId = resources.getIdentifier("presetColor$i", "id", requireContext().packageName)
            val presetView = dialogView.findViewById<View>(presetId)
            if (presetView != null) {
                presetView.background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(presets[i])
                    setStroke((1 * resources.displayMetrics.density).toInt(), Color.parseColor("#3A3A4A"))
                }
                presetView.setOnClickListener {
                    updateDialogColor(presets[i], updateHexText = true, updateSliders = true)
                }
            }
        }
        
        updateDialogColor(initialColor, updateHexText = true, updateSliders = true)
        
        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .setPositiveButton("Aceptar") { _, _ ->
                if (isTopColor) {
                    configManager.setSunnyCustomSkyTopColor(currentColor)
                } else {
                    configManager.setSunnyCustomSkyBottomColor(currentColor)
                }
                updateSunnyThemePreviews(parent)
                updateSummaries(parent)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun updateSummaries(parent: View) {
        val summarySunnyClouds = parent.findViewById<TextView>(R.id.summarySunnyClouds)
        val summarySunnySun = parent.findViewById<TextView>(R.id.summarySunnySun)
        val summarySunnyMoon = parent.findViewById<TextView>(R.id.summarySunnyMoon)
        val summarySunnyCombined = parent.findViewById<TextView>(R.id.summarySunnyCombined)

        val accentColor = "#00E5FF"

        if (summarySunnyClouds != null) {
            val density = configManager.getCloudDensity()
            val densityText = when {
                density <= 0 -> "Cielo despejado"
                density <= 25 -> "Pocas nubes"
                density <= 50 -> "Nublado"
                density <= 75 -> "Muy nublado"
                else -> "Cielo Cubierto"
            }
            summarySunnyClouds.text = Html.fromHtml(
                "Nubes: <font color='$accentColor'>$densityText ($density%)</font>",
                Html.FROM_HTML_MODE_LEGACY
            )
        }

        if (summarySunnySun != null) {
            val theme = configManager.getSunnyTheme()
            val themeText = when (theme) {
                0 -> "Mediodía"
                1 -> "Atardecer"
                2 -> "Anochecer"
                3 -> "Personalizado"
                else -> "Mediodía"
            }
            val sunDir = when (configManager.getSunPathDirection()) {
                0 -> "Izquierda a Derecha"
                1 -> "Derecha a Izquierda"
                2 -> "Estático"
                3 -> "Aleatorio"
                else -> "Izquierda a Derecha"
            }
            val sunBg = when (configManager.getSunnyBackgroundIndex()) {
                0 -> "Solo degradado"
                1 -> "Montañas"
                2 -> "Campos"
                3 -> "Lago y Bosque"
                4 -> "Bosque y Río"
                5 -> "Silueta de Ciudad"
                6 -> "Valle de Flores"
                7 -> "Cascada"
                8 -> "Galería"
                else -> "Degradado"
            }
            val godRaysStatus = if (configManager.isSunnyGodRaysEnabled()) "Activo" else "Inactivo"
            val lensFlareStatus = if (configManager.isSunnyLensFlareEnabled()) "Activo" else "Inactivo"

            summarySunnySun.text = Html.fromHtml(
                "Tema: <font color='$accentColor'>$themeText</font> • Sol: <font color='$accentColor'>$sunDir</font><br/>Fondo: <font color='$accentColor'>$sunBg</font> • Rayos: <font color='$accentColor'>$godRaysStatus</font> • Destellos: <font color='$accentColor'>$lensFlareStatus</font>",
                Html.FROM_HTML_MODE_LEGACY
            )
        }

        if (summarySunnyMoon != null) {
            val phaseText = when (configManager.getMoonPhase()) {
                0 -> "Luna Nueva"
                1 -> "Creciente Cóncava"
                2 -> "Cuarto Creciente"
                3 -> "Creciente Convexa"
                4 -> "Luna Llena"
                5 -> "Menguante Convexa"
                6 -> "Cuarto Menguante"
                7 -> "Menguante Cóncava"
                else -> "Luna Llena"
            }
            val starDensity = configManager.getStarDensity()
            val starColorText = when (configManager.getStarColorIndex()) {
                0 -> "Blanco"
                1 -> "Azul"
                2 -> "Cálido"
                3 -> "Rosa"
                4 -> "Verde"
                5 -> "Mixto"
                else -> "Blanco"
            }
            val starModeText = if (configManager.getStarMode() == 1) "Aleatorio" else "Estático"
            summarySunnyMoon.text = Html.fromHtml(
                "Fase: <font color='$accentColor'>$phaseText</font> • Estrellas: <font color='$accentColor'>$starColorText ($starDensity%)</font> • Modo: <font color='$accentColor'>$starModeText</font>",
                Html.FROM_HTML_MODE_LEGACY
            )
        }

        if (summarySunnyCombined != null) {
            val speed = configManager.getGradientCycleSpeed()
            val speedText = when {
                speed <= 20 -> "Muy lento"
                speed <= 50 -> "Normal"
                speed <= 85 -> "Rápido"
                else -> "Muy rápido"
            }
            summarySunnyCombined.text = Html.fromHtml(
                "Ciclo: <font color='$accentColor'>$speedText ($speed%)</font>",
                Html.FROM_HTML_MODE_LEGACY
            )
        }
    }

    private fun Int.dpToPx(): Int {
        val density = resources.displayMetrics.density
        return Math.round(this * density)
    }

    private fun Float.dpToPx(): Float {
        val density = resources.displayMetrics.density
        return this * density
    }
}
