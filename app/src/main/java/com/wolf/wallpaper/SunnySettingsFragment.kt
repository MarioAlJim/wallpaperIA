package com.wolf.wallpaper

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
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
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.card.MaterialCardView
import com.google.android.material.slider.Slider
import com.google.android.material.switchmaterial.SwitchMaterial
import com.wolf.wallpaper.core.ConfigManager
import java.io.File
import java.io.FileOutputStream

class SunnySettingsFragment : Fragment() {

    companion object {
        /** Cache of decoded background thumbnails (index -> bitmap), reused across settings sessions. */
        private val backgroundAssetThumbnailCache = mutableMapOf<Int, android.graphics.Bitmap>()
    }

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
        SettingsAccordionHelper.setupAccordionGroup(
            view,
            SettingsAccordionHelper.AccordionSpec(R.id.headerSunnyEnvironment, R.id.contentSunnyEnvironment, R.id.dividerSunnyEnvironment, R.id.arrowSunnyEnvironment),
            SettingsAccordionHelper.AccordionSpec(R.id.headerSunnySun, R.id.contentSunnySun, R.id.dividerSunnySun, R.id.arrowSunnySun),
            SettingsAccordionHelper.AccordionSpec(R.id.headerSunnyMoon, R.id.contentSunnyMoon, R.id.dividerSunnyMoon, R.id.arrowSunnyMoon),
            SettingsAccordionHelper.AccordionSpec(R.id.headerSunnyCombined, R.id.contentSunnyCombined, R.id.dividerSunnyCombined, R.id.arrowSunnyCombined)
        )

        // 2. Setup Cloud Density Cards (Sunny clouds)
        setupCloudDensityCards(view)

        // 3. Setup Sunny Main controls
        setupValueCardSelector(
            view, R.id.spinnerSunSize, R.id.textViewSunSizeValue,
            resources.getStringArray(R.array.sun_size_options), intArrayOf(0, 25, 50, 75, 100),
            configManager.getSunSize()
        ) { value -> configManager.setSunSize(value) }

        setupValueCardSelector(
            view, R.id.spinnerSunSpeed, R.id.textViewSunSpeedValue,
            resources.getStringArray(R.array.sun_speed_options), intArrayOf(0, 25, 50, 75, 100),
            configManager.getSunSpeed()
        ) { value -> configManager.setSunSpeed(value) }

        setupSunnyThemeCards(view)
        setupSunDirectionCards(view)

        setupValueCardSelector(
            view, R.id.spinnerSunMoveSpeed, R.id.textViewSunMoveSpeedValue,
            resources.getStringArray(R.array.sun_move_speed_options), intArrayOf(0, 25, 50, 75, 100),
            configManager.getSunMoveSpeed()
        ) { value -> configManager.setSunMoveSpeed(value) }
        setupSunStationaryCards(view)
        updateSunMoveSpeedSliderVisibility(view, configManager.getSunPathDirection())

        setupSunnyBackgroundCards(view)

        view.findViewById<Button>(R.id.btnSelectCustomSunnyBg)?.setOnClickListener {
            pickSunnyImageLauncher.launch("image/*")
        }
        updateCustomSunnyBackgroundViewsVisibility(view, configManager.getSunnyBackgroundIndex())

        // Setup Sunny God Rays
        val rowSunnyGodRays = view.findViewById<View>(R.id.rowSunnyGodRays)
        val switchSunnyGodRays = view.findViewById<SwitchMaterial>(R.id.switchSunnyGodRays)
        val statusSunnyGodRays = view.findViewById<TextView>(R.id.statusSunnyGodRays)
        val iconSunnyGodRays = view.findViewById<ImageView>(R.id.iconSunnyGodRays)
        val cardIconSunnyGodRays = view.findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardIconSunnyGodRays)
        val layoutSunnyGodRaysIntensity = view.findViewById<View>(R.id.layoutSunnyGodRaysIntensity)
        if (rowSunnyGodRays != null || switchSunnyGodRays != null) {
            val isEnabled = configManager.isSunnyGodRaysEnabled()
            layoutSunnyGodRaysIntensity?.visibility = if (isEnabled) View.VISIBLE else View.GONE
            SettingsSwitchHelper.bind(
                row = rowSunnyGodRays ?: switchSunnyGodRays!!,
                switch = switchSunnyGodRays,
                isChecked = isEnabled,
                statusView = statusSunnyGodRays,
                iconView = iconSunnyGodRays,
                iconCard = cardIconSunnyGodRays
            ) { isChecked ->
                configManager.setSunnyGodRaysEnabled(isChecked)
                layoutSunnyGodRaysIntensity?.visibility = if (isChecked) View.VISIBLE else View.GONE
                updateSummaries(view)
            }
        }
        setupValueCardSelector(
            view, R.id.spinnerSunnyGodRaysIntensity, R.id.textViewSunnyGodRaysIntensityValue,
            resources.getStringArray(R.array.god_rays_intensity_options), intArrayOf(0, 25, 50, 75, 100),
            configManager.getSunnyGodRaysIntensity()
        ) { value -> configManager.setSunnyGodRaysIntensity(value) }

        // Setup Sunny Lens Flare
        val rowSunnyLensFlare = view.findViewById<View>(R.id.rowSunnyLensFlare)
        val switchSunnyLensFlare = view.findViewById<SwitchMaterial>(R.id.switchSunnyLensFlare)
        val statusSunnyLensFlare = view.findViewById<TextView>(R.id.statusSunnyLensFlare)
        val iconSunnyLensFlare = view.findViewById<ImageView>(R.id.iconSunnyLensFlare)
        val cardIconSunnyLensFlare = view.findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardIconSunnyLensFlare)
        val layoutSunnyLensFlareIntensity = view.findViewById<View>(R.id.layoutSunnyLensFlareIntensity)
        if (rowSunnyLensFlare != null || switchSunnyLensFlare != null) {
            val isEnabled = configManager.isSunnyLensFlareEnabled()
            layoutSunnyLensFlareIntensity?.visibility = if (isEnabled) View.VISIBLE else View.GONE
            SettingsSwitchHelper.bind(
                row = rowSunnyLensFlare ?: switchSunnyLensFlare!!,
                switch = switchSunnyLensFlare,
                isChecked = isEnabled,
                statusView = statusSunnyLensFlare,
                iconView = iconSunnyLensFlare,
                iconCard = cardIconSunnyLensFlare
            ) { isChecked ->
                configManager.setSunnyLensFlareEnabled(isChecked)
                layoutSunnyLensFlareIntensity?.visibility = if (isChecked) View.VISIBLE else View.GONE
                updateSummaries(view)
            }
        }
        val rowSunnyLensFlareChromatic = view.findViewById<View>(R.id.rowSunnyLensFlareChromatic)
        val switchSunnyLensFlareChromatic = view.findViewById<SwitchMaterial>(R.id.switchSunnyLensFlareChromatic)
        val statusSunnyLensFlareChromatic = view.findViewById<TextView>(R.id.statusSunnyLensFlareChromatic)
        val iconSunnyLensFlareChromatic = view.findViewById<ImageView>(R.id.iconSunnyLensFlareChromatic)
        val cardIconSunnyLensFlareChromatic = view.findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardIconSunnyLensFlareChromatic)
        if (rowSunnyLensFlareChromatic != null || switchSunnyLensFlareChromatic != null) {
            SettingsSwitchHelper.bind(
                row = rowSunnyLensFlareChromatic ?: switchSunnyLensFlareChromatic!!,
                switch = switchSunnyLensFlareChromatic,
                isChecked = configManager.isSunnyLensFlareChromaticEnabled(),
                statusView = statusSunnyLensFlareChromatic,
                iconView = iconSunnyLensFlareChromatic,
                iconCard = cardIconSunnyLensFlareChromatic
            ) { isChecked ->
                configManager.setSunnyLensFlareChromaticEnabled(isChecked)
                updateSummaries(view)
            }
        }
        setupValueCardSelector(
            view, R.id.spinnerSunnyLensFlareIntensity, R.id.textViewSunnyLensFlareIntensityValue,
            resources.getStringArray(R.array.lens_flare_intensity_options), intArrayOf(0, 25, 50, 75, 100),
            configManager.getSunnyLensFlareIntensity()
        ) { value -> configManager.setSunnyLensFlareIntensity(value) }

        // Setup Sunny Gyroscope Parallax
        val rowSunnyGyro = view.findViewById<View>(R.id.rowSunnyGyro)
        val switchSunnyGyro = view.findViewById<SwitchMaterial>(R.id.switchSunnyGyro)
        val statusSunnyGyro = view.findViewById<TextView>(R.id.statusSunnyGyro)
        val iconSunnyGyro = view.findViewById<ImageView>(R.id.iconSunnyGyro)
        val cardIconSunnyGyro = view.findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardIconSunnyGyro)
        if (rowSunnyGyro != null || switchSunnyGyro != null) {
            SettingsSwitchHelper.bind(
                row = rowSunnyGyro ?: switchSunnyGyro!!,
                switch = switchSunnyGyro,
                isChecked = configManager.isSunnyGyroEnabled(),
                statusView = statusSunnyGyro,
                iconView = iconSunnyGyro,
                iconCard = cardIconSunnyGyro
            ) { isChecked ->
                configManager.setSunnyGyroEnabled(isChecked)
                updateSummaries(view)
            }
        }

        // Setup Sunny Touch Burst
        val rowSunnyTouchBurst = view.findViewById<View>(R.id.rowSunnyTouchBurst)
        val switchSunnyTouchBurst = view.findViewById<SwitchMaterial>(R.id.switchSunnyTouchBurst)
        val statusSunnyTouchBurst = view.findViewById<TextView>(R.id.statusSunnyTouchBurst)
        val iconSunnyTouchBurst = view.findViewById<ImageView>(R.id.iconSunnyTouchBurst)
        val cardIconSunnyTouchBurst = view.findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardIconSunnyTouchBurst)
        if (rowSunnyTouchBurst != null || switchSunnyTouchBurst != null) {
            SettingsSwitchHelper.bind(
                row = rowSunnyTouchBurst ?: switchSunnyTouchBurst!!,
                switch = switchSunnyTouchBurst,
                isChecked = configManager.isSunnyTouchBurstEnabled(),
                statusView = statusSunnyTouchBurst,
                iconView = iconSunnyTouchBurst,
                iconCard = cardIconSunnyTouchBurst
            ) { isChecked ->
                configManager.setSunnyTouchBurstEnabled(isChecked)
                updateSummaries(view)
            }
        }

        // Setup Sunny Rainbow Always
        val rowSunnyRainbowAlways = view.findViewById<View>(R.id.rowSunnyRainbowAlways)
        val switchSunnyRainbowAlways = view.findViewById<SwitchMaterial>(R.id.switchSunnyRainbowAlways)
        val statusSunnyRainbowAlways = view.findViewById<TextView>(R.id.statusSunnyRainbowAlways)
        val iconSunnyRainbowAlways = view.findViewById<ImageView>(R.id.iconSunnyRainbowAlways)
        val cardIconSunnyRainbowAlways = view.findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardIconSunnyRainbowAlways)
        if (rowSunnyRainbowAlways != null || switchSunnyRainbowAlways != null) {
            SettingsSwitchHelper.bind(
                row = rowSunnyRainbowAlways ?: switchSunnyRainbowAlways!!,
                switch = switchSunnyRainbowAlways,
                isChecked = configManager.isRainbowAlwaysEnabled(),
                statusView = statusSunnyRainbowAlways,
                iconView = iconSunnyRainbowAlways,
                iconCard = cardIconSunnyRainbowAlways
            ) { isChecked ->
                configManager.setRainbowAlwaysEnabled(isChecked)
                updateSummaries(view)
            }
        }

        // Setup Night/Combined Mode Controls
        setupTimeModeCards(view)
        setupMoonPhaseCards(view)
        setupMoonDirectionCards(view)
        setupMoonStationaryCards(view)
        setupStarModeCards(view)
        setupNightAndCombinedSliders(view)
        setupCombinedMoonPhaseCards(view)
        setupCombinedTrajectory(view)
        setupStarColorRows(view)

        // Init summaries and views
        updateSummaries(view)
    }

    /** Centers the currently-selected card of a horizontally-scrolling chip row, if any. */
    private fun scrollCardIntoView(card: View?) {
        card ?: return
        val scrollView = card.parent?.parent as? HorizontalScrollView ?: return
        SettingsAccordionHelper.scrollToSelected(scrollView, card)
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
            R.id.seekBarSunCustomX, R.id.seekBarSunCustomY,
            R.id.seekBarMoonCustomX, R.id.seekBarMoonCustomY -> {
                textView.text = "$value%"
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

    /**
     * Card-selector replacement for [setupSlider], used by the 12 controls converted from
     * sliders to 5-card rows (see SettingsCardSelectorHelper). Not built on top of
     * [setupDropdown] because that one targets `AutoCompleteTextView` (spinnerCombinedTrajectory);
     * this targets an empty `LinearLayout` container instead.
     */
    private fun setupValueCardSelector(
        parent: View,
        containerId: Int,
        valueTextViewId: Int,
        options: Array<String>,
        values: IntArray,
        currentValue: Int,
        onValueSelected: (Int) -> Unit
    ) {
        val container = parent.findViewById<LinearLayout>(containerId) ?: return
        val valueTv = parent.findViewById<TextView>(valueTextViewId)
        val initialIndex = SettingsCardSelectorHelper.closestValueIndex(values, currentValue)
        valueTv?.text = options.getOrNull(initialIndex)
        SettingsCardSelectorHelper.populate(
            context = requireContext(),
            container = container,
            options = options,
            selectedIndex = initialIndex,
            previewFactory = { index -> SunnyCardPreviewView(requireContext(), containerId, index) },
            previewFrameBackgroundRes = R.drawable.mini_screen_bg
        ) { position ->
            onValueSelected(values[position])
            valueTv?.text = options[position]
            updateSummaries(parent)
        }
    }

    /**
     * Card-selector for the sunny background, showing real thumbnails (decoded from the
     * `background/sunny_background_0N.png` assets, the user's custom gallery image, or a small
     * sky-gradient swatch for the "solo degradado" option) instead of a plain text dropdown.
     */
    private fun setupSunnyBackgroundCards(parent: View) {
        val container = parent.findViewById<LinearLayout>(R.id.spinnerSunnyBackground) ?: return
        val options = resources.getStringArray(R.array.sunny_backgrounds)
        val initialIndex = configManager.getSunnyBackgroundIndex().coerceIn(0, options.size - 1)
        SettingsCardSelectorHelper.populate(
            context = requireContext(),
            container = container,
            options = options,
            selectedIndex = initialIndex,
            previewFactory = { index -> createSunnyBackgroundPreview(index) },
            previewFrameBackgroundRes = R.drawable.mini_screen_bg
        ) { position ->
            configManager.setSunnyBackgroundIndex(position)
            updateCustomSunnyBackgroundViewsVisibility(parent, position)
            updateSummaries(parent)
        }
    }

    private fun createSunnyBackgroundPreview(index: Int): View {
        val context = requireContext()
        val corner = 4 * resources.displayMetrics.density
        return when (index) {
            0 -> {
                val (top, bottom) = currentSkyGradientColors()
                View(context).apply {
                    background = GradientDrawable(
                        GradientDrawable.Orientation.TOP_BOTTOM,
                        intArrayOf(top, bottom)
                    ).apply { cornerRadius = corner }
                }
            }
            8 -> ImageView(context).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
                val bitmap = loadCustomSunnyBackgroundThumbnail(context)
                if (bitmap != null) {
                    setImageBitmap(bitmap)
                    background = GradientDrawable().apply { cornerRadius = corner }
                } else {
                    setImageResource(R.drawable.ic_background)
                    scaleType = ImageView.ScaleType.CENTER_INSIDE
                    setColorFilter(ContextCompat.getColor(context, R.color.settings_text_secondary))
                    background = GradientDrawable().apply {
                        setColor(ContextCompat.getColor(context, R.color.settings_card_bg_alt2))
                        cornerRadius = corner
                    }
                }
                clipToOutline = true
                outlineProvider = android.view.ViewOutlineProvider.BACKGROUND
            }
            else -> ImageView(context).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
                loadSunnyBackgroundAssetThumbnail(context, index)?.let { setImageBitmap(it) }
                background = GradientDrawable().apply { cornerRadius = corner }
                clipToOutline = true
                outlineProvider = android.view.ViewOutlineProvider.BACKGROUND
            }
        }
    }

    /** Sky colors used for the "solo degradado" (no image) thumbnail, mirroring [setupSunnyThemeCards]. */
    private fun currentSkyGradientColors(): Pair<Int, Int> = when (configManager.getSunnyTheme()) {
        0 -> 0xFF0566D9.toInt() to 0xFF8DCCEF.toInt()
        1 -> 0xFF1A0D40.toInt() to 0xFFF2731A.toInt()
        2 -> 0xFF261A59.toInt() to 0xFFE6808C.toInt()
        else -> configManager.getSunnyCustomSkyTopColor() to configManager.getSunnyCustomSkyBottomColor()
    }

    /** Small decoded thumbnail for `background/sunny_background_0{index}.png`, cached across calls. */
    private fun loadSunnyBackgroundAssetThumbnail(context: Context, index: Int): android.graphics.Bitmap? {
        backgroundAssetThumbnailCache[index]?.let { return it }
        val assetPath = "background/sunny_background_%02d.png".format(index)
        return try {
            context.assets.open(assetPath).use { stream ->
                android.graphics.BitmapFactory.decodeStream(
                    stream,
                    null,
                    android.graphics.BitmapFactory.Options().apply { inSampleSize = 8 }
                )
            }?.also { backgroundAssetThumbnailCache[index] = it }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /** Decoded (and downsampled) thumbnail for the user's custom gallery background, if one was saved. */
    private fun loadCustomSunnyBackgroundThumbnail(context: Context): android.graphics.Bitmap? {
        val file = File(context.filesDir, "custom_sunny_background.png")
        if (!file.exists()) return null
        return try {
            val boundsOpts = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
            android.graphics.BitmapFactory.decodeFile(file.absolutePath, boundsOpts)
            var sampleSize = 1
            while (boundsOpts.outWidth / (sampleSize * 2) >= 200 && boundsOpts.outHeight / (sampleSize * 2) >= 200) {
                sampleSize *= 2
            }
            android.graphics.BitmapFactory.decodeFile(
                file.absolutePath,
                android.graphics.BitmapFactory.Options().apply { inSampleSize = sampleSize }
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun setupSunnyThemeCards(parent: View) {
        val container = parent.findViewById<LinearLayout>(R.id.layoutSunnyThemeCards) ?: return
        val customLayout = parent.findViewById<View>(R.id.layoutSunnyCustomGradient)
        val options = arrayOf(
            getString(R.string.summary_theme_noon),
            getString(R.string.summary_theme_sunset),
            getString(R.string.summary_theme_dusk),
            getString(R.string.summary_theme_custom)
        )

        fun updateCustomVisibility(selectedTheme: Int) {
            if (customLayout != null) {
                val p = customLayout.parent as? ViewGroup
                p?.let {
                    TransitionManager.beginDelayedTransition(it, AutoTransition().apply { duration = 200 })
                }
                customLayout.visibility = if (selectedTheme == 3) View.VISIBLE else View.GONE
            }
            updateSunnyThemePreviews(parent)
        }

        val initialTheme = configManager.getSunnyTheme().coerceIn(0, options.size - 1)
        updateCustomVisibility(initialTheme)

        SettingsCardSelectorHelper.populate(
            context = requireContext(),
            container = container,
            options = options,
            selectedIndex = initialTheme,
            previewFactory = { index ->
                val (topColor, bottomColor) = when (index) {
                    0 -> Pair(0xFF0566D9.toInt(), 0xFF8DCCEF.toInt())
                    1 -> Pair(0xFF1A0D40.toInt(), 0xFFF2731A.toInt())
                    2 -> Pair(0xFF261A59.toInt(), 0xFFE6808C.toInt())
                    else -> Pair(configManager.getSunnyCustomSkyTopColor(), configManager.getSunnyCustomSkyBottomColor())
                }
                View(requireContext()).apply {
                    val gd = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(topColor, bottomColor)).apply {
                        cornerRadius = 8 * resources.displayMetrics.density
                    }
                    background = gd
                }
            },
            title = getString(R.string.sunny_tema_del_cielo)
        ) { index ->
            configManager.setSunnyTheme(index)
            updateCustomVisibility(index)
            updateSummaries(parent)
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
            setStroke((1 * resources.displayMetrics.density).toInt(), ContextCompat.getColor(requireContext(), R.color.settings_chip_bg_inactive))
        }
        view.background = gd
    }

    private fun setupSunDirectionCards(parent: View) {
        val container = parent.findViewById<LinearLayout>(R.id.layoutSunDirectionCards) ?: return
        val options = arrayOf(
            getString(R.string.sunny_izq_a_der),
            getString(R.string.sunny_der_a_izq),
            getString(R.string.sunny_esttico),
            getString(R.string.sunny_aleatorio),
            getString(R.string.sunny_catico)
        )
        val initialDir = configManager.getSunPathDirection().coerceIn(0, options.size - 1)
        updateSunMoveSpeedSliderVisibility(parent, initialDir)

        SettingsCardSelectorHelper.populate(
            context = requireContext(),
            container = container,
            options = options,
            selectedIndex = initialDir,
            previewFactory = { index ->
                val symbol = when (index) {
                    0 -> "→"
                    1 -> "←"
                    2 -> "⚫"
                    3 -> "?"
                    else -> "⚡"
                }
                SettingsCardSelectorHelper.previewTextBadge(requireContext(), symbol, "#FFD700", 18f)
            },
            previewFrameBackgroundRes = R.drawable.mini_screen_bg,
            title = getString(R.string.sunny_trayectoriamovimiento_del_sol)
        ) { index ->
            configManager.setSunPathDirection(index)
            updateSunMoveSpeedSliderVisibility(parent, index)
            updateSummaries(parent)
        }
    }

    private fun setupSunStationaryCards(parent: View) {
        val container = parent.findViewById<LinearLayout>(R.id.layoutSunStationaryCards) ?: return
        val customLayout = parent.findViewById<View>(R.id.layoutSunCustomXY)
        val options = arrayOf(
            getString(R.string.sunny_sup_izq),
            getString(R.string.sunny_sup_der),
            getString(R.string.sunny_centro),
            getString(R.string.sunny_borde_izq),
            getString(R.string.sunny_borde_der),
            getString(R.string.sunny_personalizado)
        )
        val initialPos = configManager.getSunStationaryPosition().coerceIn(0, options.size - 1)

        fun updateCustomVisibility(pos: Int) {
            if (customLayout != null) {
                val p = customLayout.parent as? ViewGroup
                p?.let { TransitionManager.beginDelayedTransition(it, AutoTransition().apply { duration = 200 }) }
                customLayout.visibility = if (pos == 5) View.VISIBLE else View.GONE
            }
        }

        updateCustomVisibility(initialPos)

        // Wire sliders for custom X/Y
        setupSlider(parent, R.id.seekBarSunCustomX, R.id.textViewSunCustomXValue, configManager.getSunCustomX()) { value ->
            configManager.setSunCustomX(value)
        }
        setupSlider(parent, R.id.seekBarSunCustomY, R.id.textViewSunCustomYValue, configManager.getSunCustomY()) { value ->
            configManager.setSunCustomY(value)
        }

        SettingsCardSelectorHelper.populate(
            context = requireContext(),
            container = container,
            options = options,
            selectedIndex = initialPos,
            previewFactory = { index ->
                val symbol = when (index) {
                    0 -> "↖"
                    1 -> "↗"
                    2 -> "⊙"
                    3 -> "←"
                    4 -> "→"
                    else -> "⚙"
                }
                SettingsCardSelectorHelper.previewTextBadge(requireContext(), symbol, "#FFD700", 18f)
            },
            previewFrameBackgroundRes = R.drawable.mini_screen_bg,
            title = getString(R.string.sunny_posicin_del_sol_esttico)
        ) { index ->
            configManager.setSunStationaryPosition(index)
            updateCustomVisibility(index)
            updateSummaries(parent)
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

    private fun showHelpDialog() {
        SettingsBottomSheetHelper.showHelpSheet(requireContext(), isStorm = false) {
            (requireActivity() as? WallpaperSettingsActivity)?.startTutorial(1)
        }
    }

    private fun saveCustomSunnyBackground(uri: android.net.Uri) {
        try {
            val context = requireContext()
            
            // 1. Get EXIF orientation
            var orientation = android.media.ExifInterface.ORIENTATION_NORMAL
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val exif = android.media.ExifInterface(stream)
                    orientation = exif.getAttributeInt(
                        android.media.ExifInterface.TAG_ORIENTATION,
                        android.media.ExifInterface.ORIENTATION_NORMAL
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // 2. Determine scale/sample size to avoid OOM
            val boundsOpts = android.graphics.BitmapFactory.Options().apply {
                inJustDecodeBounds = true
                inScaled = false
            }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                android.graphics.BitmapFactory.decodeStream(stream, null, boundsOpts)
            }
            val origW = boundsOpts.outWidth
            val origH = boundsOpts.outHeight
            
            val maxSize = 2048
            var sampleSize = 1
            while (origW / (sampleSize * 2) >= maxSize && origH / (sampleSize * 2) >= maxSize) {
                sampleSize *= 2
            }
            
            val decodeOpts = android.graphics.BitmapFactory.Options().apply {
                inScaled = false
                inSampleSize = sampleSize
            }
            
            // 3. Decode the bitmap safely using fully qualified name
            var bitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                android.graphics.BitmapFactory.decodeStream(stream, null, decodeOpts)
            } ?: return

            // 4. Scale exactly to maxSize if it's still larger on either dimension
            val maxDim = maxOf(bitmap.width, bitmap.height)
            if (maxDim > maxSize) {
                val scale = maxSize.toFloat() / maxDim
                val newW = (bitmap.width * scale).toInt().coerceAtLeast(1)
                val newH = (bitmap.height * scale).toInt().coerceAtLeast(1)
                val scaled = android.graphics.Bitmap.createScaledBitmap(bitmap, newW, newH, true)
                bitmap.recycle()
                bitmap = scaled
            }

            // 5. Rotate based on EXIF orientation
            val rotationDegrees = when (orientation) {
                android.media.ExifInterface.ORIENTATION_ROTATE_90 -> 90
                android.media.ExifInterface.ORIENTATION_ROTATE_180 -> 180
                android.media.ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }

            if (rotationDegrees != 0) {
                val matrix = android.graphics.Matrix().apply { postRotate(rotationDegrees.toFloat()) }
                val rotatedBitmap = android.graphics.Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                if (rotatedBitmap != bitmap) {
                    bitmap.recycle()
                    bitmap = rotatedBitmap
                }
            }

            // 6. Save as PNG to internal storage
            val outputFile = File(context.filesDir, "custom_sunny_background.png")
            FileOutputStream(outputFile).use { output ->
                bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 90, output)
            }
            bitmap.recycle()

            configManager.setSunnyBackgroundIndex(8)
            view?.let {
                updateCustomSunnyBackgroundViewsVisibility(it, 8)
                setupSunnyBackgroundCards(it)
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

    private fun setupCloudDensityCards(parent: View) {
        val container = parent.findViewById<LinearLayout>(R.id.layoutCloudDensityCards) ?: return
        val options = arrayOf(
            getString(R.string.sunny_poco),
            getString(R.string.sunny_medio),
            getString(R.string.sunny_alto),
            getString(R.string.sunny_muy_alto)
        )
        val values = intArrayOf(25, 50, 75, 100)
        val initialIndex = when (configManager.getCloudDensity()) {
            25 -> 0
            50 -> 1
            75 -> 2
            100 -> 3
            else -> 1
        }
        parent.findViewById<TextView>(R.id.textViewCloudDensityValue)?.text = options[initialIndex]

        SettingsCardSelectorHelper.populate(
            context = requireContext(),
            container = container,
            options = options,
            selectedIndex = initialIndex,
            previewFactory = { index ->
                SettingsCardSelectorHelper.previewIcon(
                    requireContext(),
                    R.drawable.ic_cloud,
                    "#80D8FF"
                )
            },
            previewFrameBackgroundRes = R.drawable.mini_screen_bg,
            title = getString(R.string.cloud_density_label)
        ) { index ->
            val value = values[index]
            configManager.setCloudDensity(value)
            parent.findViewById<TextView>(R.id.textViewCloudDensityValue)?.text = options[index]
            updateSummaries(parent)
        }
    }

    private fun setupTimeModeCards(parent: View) {
        val cardDay = parent.findViewById<MaterialCardView>(R.id.cardTimeModeDay) ?: return
        val cardNight = parent.findViewById<MaterialCardView>(R.id.cardTimeModeNight) ?: return
        val cardCombined = parent.findViewById<MaterialCardView>(R.id.cardTimeModeCombined) ?: return
        val cardClock = parent.findViewById<MaterialCardView>(R.id.cardTimeModeClock) ?: return
        val cards = listOf(cardDay, cardNight, cardCombined, cardClock)

        val imageDay = parent.findViewById<ImageView>(R.id.imageTimeModeDay)
        val imageNight = parent.findViewById<ImageView>(R.id.imageTimeModeNight)
        val imageCombined = parent.findViewById<ImageView>(R.id.imageTimeModeCombined)
        val imageClock = parent.findViewById<ImageView>(R.id.imageTimeModeClock)
        val images = listOf(imageDay, imageNight, imageCombined, imageClock)

        val textDay = parent.findViewById<TextView>(R.id.textTimeModeDay)
        val textNight = parent.findViewById<TextView>(R.id.textTimeModeNight)
        val textCombined = parent.findViewById<TextView>(R.id.textTimeModeCombined)
        val textClock = parent.findViewById<TextView>(R.id.textTimeModeClock)
        val texts = listOf(textDay, textNight, textCombined, textClock)

        val activeColor = ContextCompat.getColor(requireContext(), R.color.settings_accent_text)
        val inactiveColor = ContextCompat.getColor(requireContext(), R.color.settings_text_muted)
        val activeBgColor = ContextCompat.getColor(requireContext(), R.color.settings_chip_bg_active)
        val inactiveBgColor = ContextCompat.getColor(requireContext(), R.color.settings_card_bg_alt)
        val strokeActiveColor = ContextCompat.getColor(requireContext(), R.color.settings_accent)
        val strokeInactiveColor = ContextCompat.getColor(requireContext(), R.color.settings_chip_bg_inactive)

        val switchClockMode = parent.findViewById<SwitchMaterial>(R.id.switchCombinedClockMode)
        val rowCombinedClockMode = parent.findViewById<View>(R.id.rowCombinedClockMode)
        val statusCombinedClockMode = parent.findViewById<TextView>(R.id.statusCombinedClockMode)
        val iconCombinedClockMode = parent.findViewById<ImageView>(R.id.iconCombinedClockMode)
        val cardIconCombinedClockMode = parent.findViewById<MaterialCardView>(R.id.cardIconCombinedClockMode)

        fun selectMode(mode: Int) {
            val highlightIndex = when (mode) {
                0 -> 0
                1 -> 1
                2, 3 -> 2
                else -> 0
            }
            cards.forEachIndexed { i, card ->
                val image = images[i]
                val text = texts[i]
                if (i == highlightIndex) {
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
            // Update switch & illuminating icon state
            if (rowCombinedClockMode != null || switchClockMode != null) {
                SettingsSwitchHelper.bind(
                    row = rowCombinedClockMode ?: switchClockMode!!,
                    switch = switchClockMode,
                    isChecked = mode == 3,
                    statusView = statusCombinedClockMode,
                    iconView = iconCombinedClockMode,
                    iconCard = cardIconCombinedClockMode
                ) { isChecked ->
                    val newMode = if (isChecked) 3 else 2
                    configManager.setTimeMode(newMode)
                    selectMode(newMode)
                    updateSummaries(parent)
                }
            }
            updateTimeModeVisibility(parent, mode)
        }

        val initial = configManager.getTimeMode()
        selectMode(initial)

        cards.forEachIndexed { i, card ->
            card.setOnClickListener {
                if (i == 2) {
                    val isClockChecked = configManager.getTimeMode() == 3
                    val selectedMode = if (isClockChecked) 3 else 2
                    configManager.setTimeMode(selectedMode)
                    selectMode(selectedMode)
                } else {
                    configManager.setTimeMode(i)
                    selectMode(i)
                }
                updateSummaries(parent)
            }
        }
    }

    private fun updateTimeModeVisibility(parent: View, mode: Int) {
        val cardSun = parent.findViewById<View>(R.id.cardSunnySun) ?: return
        val cardMoon = parent.findViewById<View>(R.id.cardSunnyMoon) ?: return
        val cardCombined = parent.findViewById<View>(R.id.cardSunnyCombined) ?: return
        cardSun.visibility = if (mode == 0 || mode == 2 || mode == 3) View.VISIBLE else View.GONE
        cardMoon.visibility = if (mode == 1 || mode == 2 || mode == 3) View.VISIBLE else View.GONE
        cardCombined.visibility = if (mode == 2 || mode == 3) View.VISIBLE else View.GONE

        parent.findViewById<View>(R.id.layoutCombinedSunSizeGroup)?.visibility = if (mode == 2 || mode == 3) View.VISIBLE else View.GONE
        parent.findViewById<View>(R.id.layoutCombinedMoonSettingsGroup)?.visibility = if (mode == 2 || mode == 3) View.VISIBLE else View.GONE
        parent.findViewById<View>(R.id.layoutGradientCycleSpeedContainer)?.visibility = if (mode == 2) View.VISIBLE else View.GONE

        // Hide redundant individual day/night controls when Combined/Clock mode is active
        val layoutSunSizeAndSpeed = parent.findViewById<View>(R.id.layoutSunSizeAndSpeed)
        val layoutSunTrajectoryGroup = parent.findViewById<View>(R.id.layoutSunTrajectoryGroup)
        val layoutMoonTrajectoryGroup = parent.findViewById<View>(R.id.layoutMoonTrajectoryGroup)
        val layoutStarColorAndDensityGroup = parent.findViewById<View>(R.id.layoutStarColorAndDensityGroup)

        when (mode) {
            2 -> { // Combined Cycle
                layoutSunSizeAndSpeed?.visibility = View.GONE
                layoutSunTrajectoryGroup?.visibility = View.GONE
                layoutMoonTrajectoryGroup?.visibility = View.GONE
                layoutStarColorAndDensityGroup?.visibility = View.GONE
            }
            3 -> { // Clock Real-time
                layoutSunSizeAndSpeed?.visibility = View.VISIBLE
                layoutSunTrajectoryGroup?.visibility = View.GONE
                layoutMoonTrajectoryGroup?.visibility = View.GONE
                layoutStarColorAndDensityGroup?.visibility = View.VISIBLE
            }
            else -> { // Day/Night Manual
                layoutSunSizeAndSpeed?.visibility = View.VISIBLE
                layoutSunTrajectoryGroup?.visibility = View.VISIBLE
                layoutMoonTrajectoryGroup?.visibility = View.VISIBLE
                layoutStarColorAndDensityGroup?.visibility = View.VISIBLE
            }
        }
    }

    private fun setupMoonPhaseCards(parent: View) {
        val container = parent.findViewById<LinearLayout>(R.id.layoutMoonPhaseCards) ?: return
        val options = resources.getStringArray(R.array.moon_phases)
        val moonDrawables = intArrayOf(
            R.drawable.ic_moon_phase_0,
            R.drawable.ic_moon_phase_1,
            R.drawable.ic_moon_phase_2,
            R.drawable.ic_moon_phase_3,
            R.drawable.ic_moon_phase_4,
            R.drawable.ic_moon_phase_5,
            R.drawable.ic_moon_phase_6,
            R.drawable.ic_moon_phase_7
        )
        val initialPhase = configManager.getMoonPhase().coerceIn(0, options.size - 1)

        SettingsCardSelectorHelper.populate(
            context = requireContext(),
            container = container,
            options = options,
            selectedIndex = initialPhase,
            previewFactory = { index ->
                SettingsCardSelectorHelper.previewIcon(
                    requireContext(),
                    moonDrawables.getOrElse(index) { R.drawable.ic_moon_phase_4 },
                    "#FFFFFF"
                )
            },
            previewFrameBackgroundRes = R.drawable.mini_screen_bg,
            title = getString(R.string.sunny_fase_lunar)
        ) { phase ->
            configManager.setMoonPhase(phase)
            updateSummaries(parent)
        }
    }

    private fun setupMoonDirectionCards(parent: View) {
        val container = parent.findViewById<LinearLayout>(R.id.layoutMoonDirectionCards) ?: return
        val options = arrayOf(
            getString(R.string.sunny_izq_a_der),
            getString(R.string.sunny_der_a_izq),
            getString(R.string.sunny_esttico),
            getString(R.string.sunny_aleatorio),
            getString(R.string.sunny_catico)
        )
        val initialDir = configManager.getMoonPathDirection().coerceIn(0, options.size - 1)
        updateMoonMoveSpeedVisibility(parent, initialDir)

        SettingsCardSelectorHelper.populate(
            context = requireContext(),
            container = container,
            options = options,
            selectedIndex = initialDir,
            previewFactory = { index ->
                val symbol = when (index) {
                    0 -> "→"
                    1 -> "←"
                    2 -> "⚫"
                    3 -> "?"
                    else -> "⚡"
                }
                SettingsCardSelectorHelper.previewTextBadge(requireContext(), symbol, "#90CAF9", 18f)
            },
            previewFrameBackgroundRes = R.drawable.mini_screen_bg,
            title = getString(R.string.sunny_trayectoria_de_la_luna)
        ) { index ->
            configManager.setMoonPathDirection(index)
            updateMoonMoveSpeedVisibility(parent, index)
            updateSummaries(parent)
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
        val container = parent.findViewById<LinearLayout>(R.id.layoutMoonStationaryCards) ?: return
        val customLayout = parent.findViewById<View>(R.id.layoutMoonCustomXY)
        val options = arrayOf(
            getString(R.string.sunny_sup_izq),
            getString(R.string.sunny_sup_der),
            getString(R.string.sunny_centro),
            getString(R.string.sunny_borde_izq),
            getString(R.string.sunny_borde_der),
            getString(R.string.sunny_personalizado)
        )
        val initialPos = configManager.getMoonStationaryPosition().coerceIn(0, options.size - 1)

        fun updateCustomVisibility(pos: Int) {
            if (customLayout != null) {
                val p = customLayout.parent as? ViewGroup
                p?.let { TransitionManager.beginDelayedTransition(it, AutoTransition().apply { duration = 200 }) }
                customLayout.visibility = if (pos == 5) View.VISIBLE else View.GONE
            }
        }

        updateCustomVisibility(initialPos)

        // Wire sliders for custom X/Y
        setupSlider(parent, R.id.seekBarMoonCustomX, R.id.textViewMoonCustomXValue, configManager.getMoonCustomX()) { value ->
            configManager.setMoonCustomX(value)
        }
        setupSlider(parent, R.id.seekBarMoonCustomY, R.id.textViewMoonCustomYValue, configManager.getMoonCustomY()) { value ->
            configManager.setMoonCustomY(value)
        }

        SettingsCardSelectorHelper.populate(
            context = requireContext(),
            container = container,
            options = options,
            selectedIndex = initialPos,
            previewFactory = { index ->
                val symbol = when (index) {
                    0 -> "↖"
                    1 -> "↗"
                    2 -> "⊙"
                    3 -> "←"
                    4 -> "→"
                    else -> "⚙"
                }
                SettingsCardSelectorHelper.previewTextBadge(requireContext(), symbol, "#90CAF9", 18f)
            },
            previewFrameBackgroundRes = R.drawable.mini_screen_bg,
            title = getString(R.string.sunny_posicin_de_la_luna_esttica)
        ) { index ->
            configManager.setMoonStationaryPosition(index)
            updateCustomVisibility(index)
            updateSummaries(parent)
        }
    }

    private fun setupStarModeCards(parent: View) {
        val container = parent.findViewById<LinearLayout>(R.id.layoutStarModeCards) ?: return
        val options = arrayOf(
            getString(R.string.sunny_esttico),
            getString(R.string.sunny_aleatorio)
        )
        val initialMode = configManager.getStarMode().coerceIn(0, options.size - 1)

        SettingsCardSelectorHelper.populate(
            context = requireContext(),
            container = container,
            options = options,
            selectedIndex = initialMode,
            previewFactory = { index ->
                SettingsCardSelectorHelper.previewIcon(
                    requireContext(),
                    R.drawable.ic_star,
                    if (index == 0) "#B0BEC5" else "#FFD54F"
                )
            },
            previewFrameBackgroundRes = R.drawable.mini_screen_bg,
            title = getString(R.string.sunny_modo_de_estrellas)
        ) { index ->
            configManager.setStarMode(index)
            updateSummaries(parent)
        }
    }

    private fun setupNightAndCombinedSliders(parent: View) {
        setupValueCardSelector(
            parent, R.id.spinnerMoonMoveSpeed, R.id.textViewMoonMoveSpeedValue,
            resources.getStringArray(R.array.moon_move_speed_options), intArrayOf(0, 25, 50, 75, 100),
            configManager.getMoonMoveSpeed()
        ) { value -> configManager.setMoonMoveSpeed(value) }
        setupValueCardSelector(
            parent, R.id.spinnerStarDensity, R.id.textViewStarDensityValue,
            resources.getStringArray(R.array.star_density_options), intArrayOf(0, 25, 50, 75, 100),
            configManager.getStarDensity()
        ) { value -> configManager.setStarDensity(value) }

        setupValueCardSelector(
            parent, R.id.spinnerGradientCycleSpeed, R.id.textViewGradientCycleSpeedValue,
            resources.getStringArray(R.array.gradient_cycle_speed_options), intArrayOf(25, 50, 75, 100),
            configManager.getGradientCycleSpeed()
        ) { value -> configManager.setGradientCycleSpeed(value) }

        setupValueCardSelector(
            parent, R.id.spinnerCombinedSunSize, R.id.textViewCombinedSunSizeValue,
            resources.getStringArray(R.array.sun_size_options), intArrayOf(0, 25, 50, 75, 100),
            configManager.getCombinedSunSize()
        ) { value -> configManager.setCombinedSunSize(value) }
        setupValueCardSelector(
            parent, R.id.spinnerCombinedMoonSize, R.id.textViewCombinedMoonSizeValue,
            resources.getStringArray(R.array.night_cloud_density_options), intArrayOf(0, 25, 50, 75, 100),
            configManager.getCombinedMoonSize()
        ) { value -> configManager.setCombinedMoonSize(value) }
        setupValueCardSelector(
            parent, R.id.spinnerCombinedStarDensity, R.id.textViewCombinedStarDensityValue,
            resources.getStringArray(R.array.star_density_options), intArrayOf(0, 25, 50, 75, 100),
            configManager.getCombinedStarDensity()
        ) { value -> configManager.setCombinedStarDensity(value) }

        // Setup Shooting Stars switch and slider
        val rowShootingStars = parent.findViewById<View>(R.id.rowShootingStars)
        val switchShootingStars = parent.findViewById<SwitchMaterial>(R.id.switchShootingStars)
        val statusShootingStars = parent.findViewById<TextView>(R.id.statusShootingStars)
        val iconShootingStars = parent.findViewById<ImageView>(R.id.iconShootingStars)
        val cardIconShootingStars = parent.findViewById<MaterialCardView>(R.id.cardIconShootingStars)
        val layoutDensityGroup = parent.findViewById<View>(R.id.layoutShootingStarsDensityGroup)
        if (rowShootingStars != null || switchShootingStars != null) {
            val enabled = configManager.isShootingStarsEnabled()
            layoutDensityGroup?.visibility = if (enabled) View.VISIBLE else View.GONE
            SettingsSwitchHelper.bind(
                row = rowShootingStars ?: switchShootingStars!!,
                switch = switchShootingStars,
                isChecked = enabled,
                statusView = statusShootingStars,
                iconView = iconShootingStars,
                iconCard = cardIconShootingStars
            ) { isChecked ->
                configManager.setShootingStarsEnabled(isChecked)
                layoutDensityGroup?.visibility = if (isChecked) View.VISIBLE else View.GONE
                updateSummaries(parent)
            }
        }

        setupValueCardSelector(
            parent, R.id.spinnerShootingStarsDensity, R.id.textViewShootingStarsDensityValue,
            resources.getStringArray(R.array.combined_star_density_options), intArrayOf(0, 25, 50, 75, 100),
            configManager.getShootingStarsDensity()
        ) { value -> configManager.setShootingStarsDensity(value) }

        // Setup Fireflies switch
        val rowFireflies = parent.findViewById<View>(R.id.rowFireflies)
        val switchFireflies = parent.findViewById<SwitchMaterial>(R.id.switchFireflies)
        val statusFireflies = parent.findViewById<TextView>(R.id.statusFireflies)
        val iconFireflies = parent.findViewById<ImageView>(R.id.iconFireflies)
        val cardIconFireflies = parent.findViewById<MaterialCardView>(R.id.cardIconFireflies)
        if (rowFireflies != null || switchFireflies != null) {
            SettingsSwitchHelper.bind(
                row = rowFireflies ?: switchFireflies!!,
                switch = switchFireflies,
                isChecked = configManager.isFirefliesEnabled(),
                statusView = statusFireflies,
                iconView = iconFireflies,
                iconCard = cardIconFireflies
            ) { isChecked ->
                configManager.setFirefliesEnabled(isChecked)
                updateSummaries(parent)
            }
        }

        setupNebulaSection(parent)
    }

    private fun setupNebulaSection(parent: View) {
        val rowNebula = parent.findViewById<View>(R.id.rowNebula)
        val switchNebula = parent.findViewById<SwitchMaterial>(R.id.switchNebula)
        val statusNebula = parent.findViewById<TextView>(R.id.statusNebula)
        val iconNebula = parent.findViewById<ImageView>(R.id.iconNebula)
        val cardIconNebula = parent.findViewById<MaterialCardView>(R.id.cardIconNebula)
        val layoutOptionsGroup = parent.findViewById<View>(R.id.layoutNebulaOptionsGroup)
        if (rowNebula != null || switchNebula != null) {
            val enabled = configManager.isNebulaEnabled()
            layoutOptionsGroup?.visibility = if (enabled) View.VISIBLE else View.GONE
            SettingsSwitchHelper.bind(
                row = rowNebula ?: switchNebula!!,
                switch = switchNebula,
                isChecked = enabled,
                statusView = statusNebula,
                iconView = iconNebula,
                iconCard = cardIconNebula
            ) { isChecked ->
                configManager.setNebulaEnabled(isChecked)
                layoutOptionsGroup?.visibility = if (isChecked) View.VISIBLE else View.GONE
            }
        }

        setupValueCardSelector(
            parent, R.id.spinnerNebulaIntensity, R.id.textViewNebulaIntensityValue,
            resources.getStringArray(R.array.combined_star_density_options), intArrayOf(0, 25, 50, 75, 100),
            configManager.getNebulaIntensity()
        ) { value -> configManager.setNebulaIntensity(value) }

        updateNebulaColorPreview(parent)
        parent.findViewById<View>(R.id.btnNebulaColor)?.setOnClickListener {
            showNebulaColorPickerDialog(parent, isColor2 = false)
        }
        parent.findViewById<View>(R.id.btnNebulaColor2)?.setOnClickListener {
            showNebulaColorPickerDialog(parent, isColor2 = true)
        }
    }

    private fun updateNebulaColorPreview(parent: View) {
        val color = configManager.getNebulaColor()
        setCircleColorPreview(parent.findViewById(R.id.viewNebulaColorPreview), color)
        parent.findViewById<TextView>(R.id.textViewNebulaColorCode)?.text = String.format("#%06X", 0xFFFFFF and color)

        val color2 = configManager.getNebulaColor2()
        setCircleColorPreview(parent.findViewById(R.id.viewNebulaColorPreview2), color2)
        parent.findViewById<TextView>(R.id.textViewNebulaColorCode2)?.text = String.format("#%06X", 0xFFFFFF and color2)
    }

    private fun showNebulaColorPickerDialog(parent: View, isColor2: Boolean) {
        val initialColor = if (isColor2) configManager.getNebulaColor2() else configManager.getNebulaColor()
        val presets = listOf(
            0x8E4FDB, 0x4F7FDB, 0xDB4FA9, 0x4FDBC4,
            0xDB914F, 0x6A3093, 0xE53935, 0x8E24AA,
            0x00ACC1, 0x43A047, 0xFFB300, 0xFFFFFF
        )
        SettingsBottomSheetHelper.showColorMixerSheet(requireContext(), initialColor, presets) { selectedColor ->
            if (isColor2) {
                configManager.setNebulaColor2(selectedColor)
            } else {
                configManager.setNebulaColor(selectedColor)
            }
            updateNebulaColorPreview(parent)
        }
    }

    private fun setupCombinedMoonPhaseCards(parent: View) {
        val container = parent.findViewById<LinearLayout>(R.id.layoutCombinedMoonPhaseCards) ?: return
        val options = resources.getStringArray(R.array.moon_phases)
        val moonDrawables = intArrayOf(
            R.drawable.ic_moon_phase_0,
            R.drawable.ic_moon_phase_1,
            R.drawable.ic_moon_phase_2,
            R.drawable.ic_moon_phase_3,
            R.drawable.ic_moon_phase_4,
            R.drawable.ic_moon_phase_5,
            R.drawable.ic_moon_phase_6,
            R.drawable.ic_moon_phase_7
        )
        val initialPhase = configManager.getCombinedMoonPhase().coerceIn(0, options.size - 1)

        SettingsCardSelectorHelper.populate(
            context = requireContext(),
            container = container,
            options = options,
            selectedIndex = initialPhase,
            previewFactory = { index ->
                SettingsCardSelectorHelper.previewIcon(
                    requireContext(),
                    moonDrawables.getOrElse(index) { R.drawable.ic_moon_phase_4 },
                    "#FFFFFF"
                )
            },
            previewFrameBackgroundRes = R.drawable.mini_screen_bg,
            title = getString(R.string.sunny_fase_lunar)
        ) { phase ->
            if (configManager.isCombinedMoonPhaseAutoAdvanceEnabled()) return@populate
            configManager.setCombinedMoonPhase(phase)
            updateSummaries(parent)
        }

        SettingsCardSelectorHelper.setLocked(container, configManager.isCombinedMoonPhaseAutoAdvanceEnabled())

        val rowAutoAdvance = parent.findViewById<View>(R.id.rowCombinedMoonPhaseAutoAdvance)
        val switchAutoAdvance = parent.findViewById<SwitchMaterial>(R.id.switchCombinedMoonPhaseAutoAdvance)
        val statusAutoAdvance = parent.findViewById<TextView>(R.id.statusCombinedMoonPhaseAutoAdvance)
        val iconAutoAdvance = parent.findViewById<ImageView>(R.id.iconCombinedMoonPhaseAutoAdvance)
        val cardIconAutoAdvance = parent.findViewById<MaterialCardView>(R.id.cardIconCombinedMoonPhaseAutoAdvance)
        if (rowAutoAdvance != null || switchAutoAdvance != null) {
            SettingsSwitchHelper.bind(
                row = rowAutoAdvance ?: switchAutoAdvance!!,
                switch = switchAutoAdvance,
                isChecked = configManager.isCombinedMoonPhaseAutoAdvanceEnabled(),
                statusView = statusAutoAdvance,
                iconView = iconAutoAdvance,
                iconCard = cardIconAutoAdvance
            ) { isChecked ->
                configManager.setCombinedMoonPhaseAutoAdvanceEnabled(isChecked)
                SettingsCardSelectorHelper.setLocked(container, isChecked)
                updateSummaries(parent)
            }
        }
    }

    private fun setupStarColorRows(parent: View) {
        val starColorNames = resources.getStringArray(R.array.star_color_index)
        val starColorValues = listOf(
            0xFFFFFFFF.toInt(),
            0xFF99CCFF.toInt(),
            0xFFFFF2B3.toInt(),
            0xFFFFB3D9.toInt(),
            0xFF80FFB3.toInt(),
            0xFFD0B0FF.toInt()
        )
        val starOptions = starColorNames.mapIndexed { i, name ->
            SettingsBottomSheetHelper.DynamicColorOption(
                name = name,
                colorInt = starColorValues.getOrElse(i) { 0xFFFFFFFF.toInt() },
                isRainbow = (i == 5)
            )
        }

        fun updateMoonStarColorRow() {
            val idx = configManager.getStarColorIndex().coerceIn(0, starOptions.size - 1)
            val opt = starOptions[idx]
            parent.findViewById<TextView>(R.id.textViewStarColorValue)?.text = opt.name
            val swatch = parent.findViewById<View>(R.id.viewStarColorSwatch)
            if (opt.isRainbow) {
                swatch?.background = GradientDrawable(
                    GradientDrawable.Orientation.LEFT_RIGHT,
                    intArrayOf(0xFFD0B0FF.toInt(), 0xFFFFA0D0.toInt(), 0xFF80FFB3.toInt(), 0xFF99CCFF.toInt())
                )
            } else {
                swatch?.background = null
                swatch?.setBackgroundColor(opt.colorInt)
            }
        }

        fun updateCombinedStarColorRow() {
            val idx = configManager.getCombinedStarColorIndex().coerceIn(0, starOptions.size - 1)
            val opt = starOptions[idx]
            parent.findViewById<TextView>(R.id.textViewCombinedStarColorValue)?.text = opt.name
            val swatch = parent.findViewById<View>(R.id.viewCombinedStarColorSwatch)
            if (opt.isRainbow) {
                swatch?.background = GradientDrawable(
                    GradientDrawable.Orientation.LEFT_RIGHT,
                    intArrayOf(0xFFD0B0FF.toInt(), 0xFFFFA0D0.toInt(), 0xFF80FFB3.toInt(), 0xFF99CCFF.toInt())
                )
            } else {
                swatch?.background = null
                swatch?.setBackgroundColor(opt.colorInt)
            }
        }

        updateMoonStarColorRow()
        updateCombinedStarColorRow()

        parent.findViewById<View>(R.id.cardStarColorSelector)?.setOnClickListener {
            SettingsBottomSheetHelper.showDynamicColorPickerSheet(
                context = requireContext(),
                title = getString(R.string.sunny_color_de_estrellas),
                subtitle = "Selecciona una tonalidad para las estrellas",
                elementType = SettingsBottomSheetHelper.ElementType.STARS,
                options = starOptions,
                selectedIndex = configManager.getStarColorIndex()
            ) { selectedPosition ->
                configManager.setStarColorIndex(selectedPosition)
                updateMoonStarColorRow()
                updateSummaries(parent)
            }
        }

        parent.findViewById<View>(R.id.cardCombinedStarColorSelector)?.setOnClickListener {
            SettingsBottomSheetHelper.showDynamicColorPickerSheet(
                context = requireContext(),
                title = getString(R.string.sunny_color_de_estrellas),
                subtitle = "Selecciona una tonalidad para las estrellas",
                elementType = SettingsBottomSheetHelper.ElementType.STARS,
                options = starOptions,
                selectedIndex = configManager.getCombinedStarColorIndex()
            ) { selectedPosition ->
                configManager.setCombinedStarColorIndex(selectedPosition)
                updateCombinedStarColorRow()
                updateSummaries(parent)
            }
        }
    }

    private fun setupCombinedTrajectory(parent: View) {
        val combinedTrajectories = resources.getStringArray(R.array.combined_trajectories)
        setupDropdown(parent, R.id.spinnerCombinedTrajectory, combinedTrajectories, configManager.getCombinedPathDirection()) { position ->
            configManager.setCombinedPathDirection(position)
        }
    }

    private fun showColorPickerDialog(parent: View, isTopColor: Boolean) {
        val initialColor = if (isTopColor) {
            configManager.getSunnyCustomSkyTopColor()
        } else {
            configManager.getSunnyCustomSkyBottomColor()
        }
        val presets = listOf(
            0x0566D9, 0x8DCCEF, 0x1A0D40, 0xF2731A,
            0xE6808C, 0x261A59, 0xE53935, 0x8E24AA,
            0x00ACC1, 0x43A047, 0xFFB300, 0xFFFFFF
        )
        SettingsBottomSheetHelper.showColorMixerSheet(requireContext(), initialColor, presets) { selectedColor ->
            if (isTopColor) {
                configManager.setSunnyCustomSkyTopColor(selectedColor)
            } else {
                configManager.setSunnyCustomSkyBottomColor(selectedColor)
            }
            updateSunnyThemePreviews(parent)
            updateSummaries(parent)
        }
    }

    private fun updateSummaries(parent: View) {
        val summarySunnyEnvironment = parent.findViewById<TextView>(R.id.summarySunnyEnvironment)
        val summarySunnySun = parent.findViewById<TextView>(R.id.summarySunnySun)
        val summarySunnyMoon = parent.findViewById<TextView>(R.id.summarySunnyMoon)
        val summarySunnyCombined = parent.findViewById<TextView>(R.id.summarySunnyCombined)

        val accentColor = String.format(
            "#%06X",
            0xFFFFFF and ContextCompat.getColor(requireContext(), R.color.settings_accent_text)
        )

        val sunnyBackgrounds = resources.getStringArray(R.array.sunny_backgrounds)
        val moonPhases = resources.getStringArray(R.array.moon_phases)
        val starColors = resources.getStringArray(R.array.star_color_index)
        val combinedTrajectories = resources.getStringArray(R.array.combined_trajectories)

        if (summarySunnyEnvironment != null) {
            val density = configManager.getCloudDensity()
            val densityText = when (density) {
                25 -> getString(R.string.summary_density_low)
                50 -> getString(R.string.summary_density_medium)
                75 -> getString(R.string.summary_density_high)
                100 -> getString(R.string.summary_density_very_high)
                else -> getString(R.string.summary_density_medium)
            }
            val sunBg = sunnyBackgrounds.getOrNull(configManager.getSunnyBackgroundIndex()) ?: ""
            val gyroStatus = if (configManager.isSunnyGyroEnabled()) getString(R.string.summary_yes) else getString(R.string.summary_no)
            summarySunnyEnvironment.text = Html.fromHtml(
                getString(R.string.summary_sunny_env_format, accentColor, densityText, sunBg, gyroStatus),
                Html.FROM_HTML_MODE_LEGACY
            )
        }

        if (summarySunnySun != null) {
            val theme = configManager.getSunnyTheme()
            val themeText = when (theme) {
                0 -> getString(R.string.summary_theme_noon)
                1 -> getString(R.string.summary_theme_sunset)
                2 -> getString(R.string.summary_theme_dusk)
                3 -> getString(R.string.summary_theme_custom)
                else -> getString(R.string.summary_theme_noon)
            }
            val sunDir = when (configManager.getSunPathDirection()) {
                0 -> getString(R.string.summary_sun_l2r)
                1 -> getString(R.string.summary_sun_r2l)
                2 -> getString(R.string.summary_sun_static)
                3 -> getString(R.string.summary_sun_random)
                else -> getString(R.string.summary_sun_l2r)
            }
            val godRaysStatus = if (configManager.isSunnyGodRaysEnabled()) getString(R.string.summary_active) else getString(R.string.summary_inactive)
            val lensFlareStatus = if (configManager.isSunnyLensFlareEnabled()) getString(R.string.summary_active) else getString(R.string.summary_inactive)

            summarySunnySun.text = Html.fromHtml(
                getString(R.string.summary_sunny_sun_format, accentColor, themeText, sunDir, godRaysStatus, lensFlareStatus),
                Html.FROM_HTML_MODE_LEGACY
            )
        }

        if (summarySunnyMoon != null) {
            val phaseText = moonPhases.getOrNull(configManager.getMoonPhase()) ?: ""
            val starDensity = configManager.getStarDensity()
            val starColorText = starColors.getOrNull(configManager.getStarColorIndex()) ?: ""
            val starModeText = if (configManager.getStarMode() == 1) getString(R.string.summary_star_random) else getString(R.string.summary_star_static)
            val starDensityText = when {
                starDensity <= 25 -> getString(R.string.summary_stars_few)
                starDensity <= 50 -> getString(R.string.summary_stars_medium)
                starDensity <= 75 -> getString(R.string.summary_stars_many)
                else -> getString(R.string.summary_stars_full_sky)
            }
            val shootingStarsText = if (configManager.isShootingStarsEnabled()) {
                val densityVal = configManager.getShootingStarsDensity()
                val densityLabel = when {
                    densityVal <= 25 -> getString(R.string.summary_stars_few)
                    densityVal <= 50 -> getString(R.string.summary_stars_medium)
                    densityVal <= 75 -> getString(R.string.summary_stars_many)
                    else -> getString(R.string.summary_shooting_stars_frequent)
                }
                getString(R.string.summary_shooting_stars_active, densityLabel)
            } else {
                getString(R.string.summary_no)
            }
            val firefliesText = if (configManager.isFirefliesEnabled()) getString(R.string.summary_yes) else getString(R.string.summary_no)
            summarySunnyMoon.text = Html.fromHtml(
                getString(R.string.summary_sunny_moon_format, accentColor, phaseText, starColorText, starDensityText, starModeText, shootingStarsText, firefliesText),
                Html.FROM_HTML_MODE_LEGACY
            )
        }

        if (summarySunnyCombined != null) {
            val speed = configManager.getGradientCycleSpeed()
            val speedText = if (configManager.getTimeMode() == 3) {
                getString(R.string.sunny_modo_reloj_summary_text)
            } else {
                when {
                    speed <= 25 -> getString(R.string.summary_speed_very_slow)
                    speed <= 50 -> getString(R.string.summary_speed_normal)
                    speed <= 75 -> getString(R.string.summary_speed_fast)
                    else -> getString(R.string.summary_speed_very_fast)
                }
            }
            val sunSize = configManager.getCombinedSunSize()
            val moonSize = configManager.getCombinedMoonSize()
            val phaseText = if (configManager.isCombinedMoonPhaseAutoAdvanceEnabled()) {
                getString(R.string.summary_combined_phase_auto)
            } else {
                moonPhases.getOrNull(configManager.getCombinedMoonPhase()) ?: ""
            }
            val starDensity = configManager.getCombinedStarDensity()
            val starColorText = starColors.getOrNull(configManager.getCombinedStarColorIndex()) ?: ""
            val trajectoryText = combinedTrajectories.getOrNull(configManager.getCombinedPathDirection()) ?: ""
            val sunSizeVal = configManager.getCombinedSunSize()
            val sunSizeText = when {
                sunSizeVal <= 30 -> getString(R.string.summary_size_small)
                sunSizeVal <= 65 -> getString(R.string.summary_size_medium)
                else -> getString(R.string.summary_size_large)
            }
            val moonSizeVal = configManager.getCombinedMoonSize()
            val moonSizeText = when {
                moonSizeVal <= 30 -> getString(R.string.summary_size_small_fem)
                moonSizeVal <= 65 -> getString(R.string.summary_size_medium_fem)
                else -> getString(R.string.summary_size_large_fem)
            }
            val combStarDensityText = when {
                starDensity <= 25 -> getString(R.string.summary_stars_few)
                starDensity <= 50 -> getString(R.string.summary_stars_medium)
                starDensity <= 75 -> getString(R.string.summary_stars_many)
                else -> getString(R.string.summary_stars_full_sky)
            }
            summarySunnyCombined.text = Html.fromHtml(
                getString(R.string.summary_sunny_combined_format, accentColor, speedText, sunSizeText, moonSizeText, phaseText, starColorText, combStarDensityText, trajectoryText),
                Html.FROM_HTML_MODE_LEGACY
            )
        }
    }


}

private class SunnyCardPreviewView(
    context: Context,
    private val containerId: Int,
    private val optionIndex: Int
) : View(context) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val density = context.resources.displayMetrics.density
    private fun Int.px(): Float = this * density
    private fun Float.px(): Float = this * density

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return

        when (containerId) {
            R.id.spinnerSunSize, R.id.spinnerCombinedSunSize -> {
                val radii = floatArrayOf(3f, 5f, 7f, 9f, 11f)
                drawSunDisc(canvas, w * 0.5f, h * 0.5f, radii.getOrElse(optionIndex) { 7f }.px())
            }
            R.id.spinnerCombinedMoonSize -> {
                val radii = floatArrayOf(3f, 5f, 7f, 9f, 11f)
                drawMoonDisc(canvas, w * 0.5f, h * 0.5f, radii.getOrElse(optionIndex) { 7f }.px())
            }
            R.id.spinnerSunSpeed -> {
                drawSunDisc(canvas, w * 0.5f, h * 0.5f, 5f.px())
                drawPulseRings(canvas, w * 0.5f, h * 0.5f, 5f.px())
            }
            R.id.spinnerSunMoveSpeed -> drawTrajectoryPreview(canvas, w, h, isSun = true)
            R.id.spinnerMoonMoveSpeed -> drawTrajectoryPreview(canvas, w, h, isSun = false)
            R.id.spinnerSunnyGodRaysIntensity -> drawGodRaysPreview(canvas, w, h)
            R.id.spinnerSunnyLensFlareIntensity -> drawLensFlarePreview(canvas, w, h)
            R.id.spinnerStarDensity, R.id.spinnerCombinedStarDensity -> drawStarField(canvas, w, h)
            R.id.spinnerShootingStarsDensity -> drawShootingStarsPreview(canvas, w, h)
            R.id.spinnerGradientCycleSpeed -> drawCyclePreview(canvas, w, h)
        }
    }

    private fun drawSunDisc(canvas: Canvas, cx: Float, cy: Float, radius: Float) {
        paint.shader = null
        val glowColors = intArrayOf(adjustAlpha(SUN_GLOW, 0.35f), adjustAlpha(SUN_GLOW, 0f))
        paint.style = Paint.Style.FILL
        paint.shader = RadialGradient(cx, cy, radius * 2.2f, glowColors, null, Shader.TileMode.CLAMP)
        canvas.drawCircle(cx, cy, radius * 2.2f, paint)
        paint.shader = null
        paint.color = SUN_COLOR
        canvas.drawCircle(cx, cy, radius, paint)
    }

    /** A disc with a bite taken out of it (in the frame's own background color) to read as a crescent. */
    private fun drawMoonDisc(canvas: Canvas, cx: Float, cy: Float, radius: Float) {
        paint.shader = null
        paint.style = Paint.Style.FILL
        paint.color = MOON_COLOR
        canvas.drawCircle(cx, cy, radius, paint)
        paint.color = FRAME_BG
        canvas.drawCircle(cx + radius * 0.55f, cy - radius * 0.3f, radius * 0.85f, paint)
    }

    private fun drawPulseRings(canvas: Canvas, cx: Float, cy: Float, baseRadius: Float) {
        val ringCount = intArrayOf(0, 1, 2, 3, 4).getOrElse(optionIndex) { 0 }
        if (ringCount <= 0) return
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f.px()
        paint.color = adjustAlpha(SUN_GLOW, 0.5f)
        val gaps = floatArrayOf(0f, 5f, 4f, 3.2f, 2.6f)
        val gap = gaps.getOrElse(optionIndex) { 4f }.px()
        for (i in 1..ringCount) {
            canvas.drawCircle(cx, cy, baseRadius + gap * i, paint)
        }
    }

    /** Sun/moon body with trailing motion-dashes behind it; dash count grows with speed. */
    private fun drawTrajectoryPreview(canvas: Canvas, w: Float, h: Float, isSun: Boolean) {
        val bodyR = 5f.px()
        val bodyCx = w * 0.68f
        val bodyCy = h * 0.4f
        if (isSun) drawSunDisc(canvas, bodyCx, bodyCy, bodyR) else drawMoonDisc(canvas, bodyCx, bodyCy, bodyR)

        val dashCount = intArrayOf(0, 1, 2, 3, 4).getOrElse(optionIndex) { 0 }
        if (dashCount > 0) {
            paint.shader = null
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1.3f.px()
            paint.strokeCap = Paint.Cap.ROUND
            paint.color = adjustAlpha(if (isSun) SUN_COLOR else MOON_COLOR, 0.55f)
            var x = bodyCx - bodyR - 3f.px()
            for (i in 0 until dashCount) {
                val dashLen = (3 + i).px()
                canvas.drawLine(x, bodyCy, x - dashLen, bodyCy, paint)
                x -= dashLen + 2.5f.px()
            }
        } else {
            paint.shader = null
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1f.px()
            paint.color = adjustAlpha(Color.WHITE, 0.25f)
            canvas.drawLine(w * 0.2f, h * 0.85f, w * 0.8f, h * 0.85f, paint)
        }
    }

    private fun drawGodRaysPreview(canvas: Canvas, w: Float, h: Float) {
        val cx = w * 0.5f
        val cy = h * 0.32f
        val rayCounts = intArrayOf(0, 3, 5, 7, 9)
        val alphas = floatArrayOf(0f, 0.35f, 0.55f, 0.8f, 1.0f)
        val count = rayCounts.getOrElse(optionIndex) { 0 }
        if (count > 0) {
            paint.shader = null
            paint.style = Paint.Style.STROKE
            paint.strokeCap = Paint.Cap.ROUND
            paint.strokeWidth = 1.3f.px()
            paint.color = adjustAlpha(SUN_RAY, alphas.getOrElse(optionIndex) { 1f })
            val len = (6 + optionIndex * 2).px()
            val innerR = 7f.px()
            for (i in 0 until count) {
                val t = if (count == 1) 0.5 else i.toDouble() / (count - 1)
                val angle = Math.PI * (0.15 + 0.7 * t)
                val sx = cx + (innerR * Math.cos(angle)).toFloat()
                val sy = cy + (innerR * Math.sin(angle)).toFloat()
                val ex = cx + ((innerR + len) * Math.cos(angle)).toFloat()
                val ey = cy + ((innerR + len) * Math.sin(angle)).toFloat()
                canvas.drawLine(sx, sy, ex, ey, paint)
            }
        }
        drawSunDisc(canvas, cx, cy, 6f.px())
    }

    private fun drawLensFlarePreview(canvas: Canvas, w: Float, h: Float) {
        val alphas = floatArrayOf(0f, 0.25f, 0.45f, 0.65f, 0.85f)
        val ghostCounts = intArrayOf(0, 1, 2, 3, 3)
        val alpha = alphas.getOrElse(optionIndex) { 0f }
        val cx = w * 0.3f
        val cy = h * 0.3f
        if (alpha > 0f) {
            paint.style = Paint.Style.FILL
            val glowColors = intArrayOf(adjustAlpha(FLARE_COLOR, alpha), adjustAlpha(FLARE_COLOR, 0f))
            val glowRadius = w * 0.42f
            paint.shader = RadialGradient(cx, cy, glowRadius, glowColors, null, Shader.TileMode.CLAMP)
            canvas.drawCircle(cx, cy, glowRadius, paint)
            paint.shader = null
        }
        paint.style = Paint.Style.FILL
        paint.color = adjustAlpha(FLARE_COLOR, alpha * 0.7f)
        val ghosts = ghostCounts.getOrElse(optionIndex) { 0 }
        for (i in 1..ghosts) {
            val t = i / 4f
            canvas.drawCircle(cx + (w * 0.5f) * t, cy + (h * 0.5f) * t, (5 - i).coerceAtLeast(1).px(), paint)
        }
    }

    private fun drawStarField(canvas: Canvas, w: Float, h: Float) {
        val counts = intArrayOf(0, 4, 9, 18, 30)
        val count = counts.getOrElse(optionIndex) { 9 }
        paint.shader = null
        if (count <= 0) return
        val random = java.util.Random(optionIndex * 100L + 11)
        paint.style = Paint.Style.FILL
        for (i in 0 until count) {
            paint.color = adjustAlpha(STAR_COLOR, 0.6f + random.nextFloat() * 0.4f)
            val r = (0.8f + random.nextFloat() * 0.8f).px()
            canvas.drawCircle(random.nextFloat() * w, random.nextFloat() * h, r, paint)
        }
        if (optionIndex >= 3) {
            paint.color = STAR_COLOR
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1f.px()
            val cx = w * 0.5f
            val cy = h * 0.3f
            val s = 5f.px()
            canvas.drawLine(cx - s, cy, cx + s, cy, paint)
            canvas.drawLine(cx, cy - s, cx, cy + s, paint)
        }
    }

    private fun drawShootingStarsPreview(canvas: Canvas, w: Float, h: Float) {
        paint.shader = null
        // A handful of faint static stars so index 0 (no shooting stars) isn't a bare frame.
        val bgRandom = java.util.Random(7)
        paint.style = Paint.Style.FILL
        for (i in 0 until 5) {
            paint.color = adjustAlpha(STAR_COLOR, 0.25f)
            canvas.drawCircle(bgRandom.nextFloat() * w, bgRandom.nextFloat() * h * 0.6f, 0.8f.px(), paint)
        }
        val counts = intArrayOf(0, 1, 2, 3, 5)
        val count = counts.getOrElse(optionIndex) { 0 }
        if (count <= 0) return
        val random = java.util.Random(optionIndex * 100L + 21)
        for (i in 0 until count) {
            val headX = random.nextFloat() * w * 0.6f + w * 0.3f
            val headY = random.nextFloat() * h * 0.4f
            val length = 10f.px() + random.nextFloat() * 6f.px()
            val tailX = headX - length
            val tailY = headY + length * 0.55f
            paint.style = Paint.Style.STROKE
            paint.strokeCap = Paint.Cap.ROUND
            paint.strokeWidth = 1.2f.px()
            paint.shader = LinearGradient(headX, headY, tailX, tailY, STAR_COLOR, adjustAlpha(STAR_COLOR, 0f), Shader.TileMode.CLAMP)
            canvas.drawLine(headX, headY, tailX, tailY, paint)
            paint.shader = null
            paint.style = Paint.Style.FILL
            paint.color = STAR_COLOR
            canvas.drawCircle(headX, headY, 1.3f.px(), paint)
        }
    }

    private fun drawCyclePreview(canvas: Canvas, w: Float, h: Float) {
        paint.style = Paint.Style.FILL
        paint.shader = LinearGradient(0f, 0f, 0f, h, CYCLE_NIGHT, CYCLE_DAY, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        val count = intArrayOf(0, 1, 2, 3, 4).getOrElse(optionIndex) { 0 }
        if (count == 0) {
            paint.style = Paint.Style.FILL
            paint.color = adjustAlpha(Color.WHITE, 0.85f)
            canvas.drawRect(w * 0.42f, h * 0.42f, w * 0.47f, h * 0.58f, paint)
            canvas.drawRect(w * 0.53f, h * 0.42f, w * 0.58f, h * 0.58f, paint)
        } else {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1.3f.px()
            paint.strokeCap = Paint.Cap.ROUND
            paint.color = adjustAlpha(Color.WHITE, 0.85f)
            val cy = h * 0.5f
            val startX = w * 0.5f - (count - 1) * 4f.px()
            for (i in 0 until count) {
                val cx = startX + i * 8f.px()
                canvas.drawLine(cx - 3f.px(), cy - 4f.px(), cx + 3f.px(), cy, paint)
                canvas.drawLine(cx + 3f.px(), cy, cx - 3f.px(), cy + 4f.px(), paint)
            }
        }
    }

    private fun adjustAlpha(color: Int, factor: Float): Int {
        val alpha = Math.round(Color.alpha(color) * factor)
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))
    }

    companion object {
        private val SUN_COLOR = Color.parseColor("#FFC107")
        private val SUN_GLOW = Color.parseColor("#FFD54F")
        private val SUN_RAY = Color.parseColor("#FFD54F")
        private val MOON_COLOR = Color.parseColor("#ECEFF1")
        private val FRAME_BG = Color.parseColor("#1A1A24")
        private val STAR_COLOR = Color.WHITE
        private val FLARE_COLOR = Color.parseColor("#FFF3E0")
        private val CYCLE_NIGHT = Color.parseColor("#1A0D40")
        private val CYCLE_DAY = Color.parseColor("#F2731A")
    }
}
