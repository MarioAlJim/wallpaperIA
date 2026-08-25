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

        // 1. Accordions removed (flat inline design)

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
        setupValueCardSelector(
            view, R.id.spinnerSunnyGodRaysIntensity, R.id.textViewSunnyGodRaysIntensityValue,
            resources.getStringArray(R.array.god_rays_intensity_options), intArrayOf(0, 25, 50, 75, 100),
            configManager.getSunnyGodRaysIntensity()
        ) { value -> configManager.setSunnyGodRaysIntensity(value) }

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
        val switchSunnyLensFlareChromatic = view.findViewById<SwitchMaterial>(R.id.switchSunnyLensFlareChromatic)
        if (switchSunnyLensFlareChromatic != null) {
            switchSunnyLensFlareChromatic.isChecked = configManager.isSunnyLensFlareChromaticEnabled()
            switchSunnyLensFlareChromatic.setOnCheckedChangeListener { _, isChecked ->
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

        // Setup Sunny Rainbow Always
        val switchSunnyRainbowAlways = view.findViewById<SwitchMaterial>(R.id.switchSunnyRainbowAlways)
        if (switchSunnyRainbowAlways != null) {
            switchSunnyRainbowAlways.isChecked = configManager.isRainbowAlwaysEnabled()
            switchSunnyRainbowAlways.setOnCheckedChangeListener { _, isChecked ->
                configManager.setRainbowAlwaysEnabled(isChecked)
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
        setupCombinedMoonPhaseCards(view)
        setupCombinedStarColorCards(view)
        setupCombinedTrajectory(view)
        setupStarColorPreviews(view)

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
                        card.strokeColor = ContextCompat.getColor(requireContext(), R.color.settings_accent)
                        card.strokeWidth = (3 * resources.displayMetrics.density).toInt()
                        card.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.settings_chip_bg_active))
                    } else {
                        card.strokeColor = ContextCompat.getColor(requireContext(), R.color.settings_chip_bg_inactive)
                        card.strokeWidth = (1 * resources.displayMetrics.density).toInt()
                        card.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.settings_card_bg_alt))
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
        scrollCardIntoView(cards.getOrNull(initialTheme))

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
            setStroke((1 * resources.displayMetrics.density).toInt(), ContextCompat.getColor(requireContext(), R.color.settings_chip_bg_inactive))
        }
        view.background = gd
    }

    private fun setupSunDirectionCards(parent: View) {
        val cardL2R = parent.findViewById<MaterialCardView>(R.id.cardSunDirL2R)
        val cardR2L = parent.findViewById<MaterialCardView>(R.id.cardSunDirR2L)
        val cardStationary = parent.findViewById<MaterialCardView>(R.id.cardSunDirStationary)
        val cardRandom = parent.findViewById<MaterialCardView>(R.id.cardSunDirRandom)
        val cardChaotic = parent.findViewById<MaterialCardView>(R.id.cardSunDirChaotic)
        val cards = listOf(cardL2R, cardR2L, cardStationary, cardRandom, cardChaotic)

        fun updateCardSelection(selectedDir: Int) {
            cards.forEachIndexed { index, card ->
                if (card != null) {
                    if (index == selectedDir) {
                        card.strokeColor = ContextCompat.getColor(requireContext(), R.color.settings_accent)
                        card.strokeWidth = (3 * resources.displayMetrics.density).toInt()
                        card.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.settings_chip_bg_active))
                    } else {
                        card.strokeColor = ContextCompat.getColor(requireContext(), R.color.settings_chip_bg_inactive)
                        card.strokeWidth = (1 * resources.displayMetrics.density).toInt()
                        card.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.settings_card_bg_alt))
                    }
                }
            }
            updateSunMoveSpeedSliderVisibility(parent, selectedDir)
        }

        updateCardSelection(configManager.getSunPathDirection())
        scrollCardIntoView(cards.getOrNull(configManager.getSunPathDirection()))

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
                        card.strokeColor = ContextCompat.getColor(requireContext(), R.color.settings_accent)
                        card.strokeWidth = (3 * resources.displayMetrics.density).toInt()
                        card.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.settings_chip_bg_active))
                    } else {
                        card.strokeColor = ContextCompat.getColor(requireContext(), R.color.settings_chip_bg_inactive)
                        card.strokeWidth = (1 * resources.displayMetrics.density).toInt()
                        card.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.settings_card_bg_alt))
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
        scrollCardIntoView(cards.getOrNull(configManager.getSunStationaryPosition()))

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
        val cardPoco = parent.findViewById<MaterialCardView>(R.id.cardCloudDensityPoco) ?: return
        val cardMedio = parent.findViewById<MaterialCardView>(R.id.cardCloudDensityMedio) ?: return
        val cardAlto = parent.findViewById<MaterialCardView>(R.id.cardCloudDensityAlto) ?: return
        val cardMuyAlto = parent.findViewById<MaterialCardView>(R.id.cardCloudDensityMuyAlto) ?: return
        val cards = listOf(cardPoco, cardMedio, cardAlto, cardMuyAlto)

        val textPoco = cardPoco.getChildAt(0) as? TextView
        val textMedio = cardMedio.getChildAt(0) as? TextView
        val textAlto = cardAlto.getChildAt(0) as? TextView
        val textMuyAlto = cardMuyAlto.getChildAt(0) as? TextView
        val texts = listOf(textPoco, textMedio, textAlto, textMuyAlto)

        val activeColor = ContextCompat.getColor(requireContext(), R.color.settings_accent_text)
        val inactiveColor = ContextCompat.getColor(requireContext(), R.color.settings_text_muted)
        val activeBgColor = ContextCompat.getColor(requireContext(), R.color.settings_chip_bg_active)
        val inactiveBgColor = ContextCompat.getColor(requireContext(), R.color.settings_card_bg_alt)
        val strokeActiveColor = ContextCompat.getColor(requireContext(), R.color.settings_accent)
        val strokeInactiveColor = ContextCompat.getColor(requireContext(), R.color.settings_chip_bg_inactive)

        fun selectDensity(densityValue: Int) {
            val index = when (densityValue) {
                25 -> 0
                50 -> 1
                75 -> 2
                100 -> 3
                else -> 1 // Default to Medio (50)
            }
            cards.forEachIndexed { i, card ->
                val text = texts[i]
                if (i == index) {
                    card.strokeColor = strokeActiveColor
                    card.strokeWidth = (3 * resources.displayMetrics.density).toInt()
                    card.setCardBackgroundColor(activeBgColor)
                    text?.setTextColor(activeColor)
                } else {
                    card.strokeColor = strokeInactiveColor
                    card.strokeWidth = (1 * resources.displayMetrics.density).toInt()
                    card.setCardBackgroundColor(inactiveBgColor)
                    text?.setTextColor(inactiveColor)
                }
            }
            val valueText = when (index) {
                0 -> "Poco"
                1 -> "Medio"
                2 -> "Alto"
                3 -> "Muy Alto"
                else -> "Medio"
            }
            parent.findViewById<TextView>(R.id.textViewCloudDensityValue)?.text = valueText
            updateSummaries(parent)
        }

        cardPoco.setOnClickListener {
            configManager.setCloudDensity(25)
            selectDensity(25)
        }
        cardMedio.setOnClickListener {
            configManager.setCloudDensity(50)
            selectDensity(50)
        }
        cardAlto.setOnClickListener {
            configManager.setCloudDensity(75)
            selectDensity(75)
        }
        cardMuyAlto.setOnClickListener {
            configManager.setCloudDensity(100)
            selectDensity(100)
        }

        selectDensity(configManager.getCloudDensity())
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
        cardSun.visibility = if (mode == 0 || mode == 2 || mode == 3) View.VISIBLE else View.GONE
        cardMoon.visibility = if (mode == 1 || mode == 2 || mode == 3) View.VISIBLE else View.GONE
        cardCombined.visibility = if (mode == 2) View.VISIBLE else View.GONE

        parent.findViewById<View>(R.id.layoutCombinedSunSizeGroup)?.visibility = if (mode == 2) View.VISIBLE else View.GONE
        parent.findViewById<View>(R.id.layoutCombinedMoonSettingsGroup)?.visibility = if (mode == 2) View.VISIBLE else View.GONE

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
        val ids = listOf(
            R.id.cardMoonPhase0, R.id.cardMoonPhase1, R.id.cardMoonPhase2, R.id.cardMoonPhase3,
            R.id.cardMoonPhase4, R.id.cardMoonPhase5, R.id.cardMoonPhase6, R.id.cardMoonPhase7
        )
        val cards = ids.map { id -> parent.findViewById<MaterialCardView>(id) }

        fun selectPhase(phase: Int) {
            cards.forEachIndexed { i, card ->
                card ?: return@forEachIndexed
                if (i == phase) {
                    card.strokeColor = ContextCompat.getColor(requireContext(), R.color.settings_accent)
                    card.strokeWidth = (3 * resources.displayMetrics.density).toInt()
                    card.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.settings_chip_bg_active))
                } else {
                    card.strokeColor = ContextCompat.getColor(requireContext(), R.color.settings_chip_bg_inactive)
                    card.strokeWidth = (1 * resources.displayMetrics.density).toInt()
                    card.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.settings_card_bg_alt))
                }
            }
        }

        selectPhase(configManager.getMoonPhase())
        scrollCardIntoView(cards.getOrNull(configManager.getMoonPhase()))

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
        val cardChaotic = parent.findViewById<MaterialCardView>(R.id.cardMoonDirChaotic)
        val cards = listOf(cardL2R, cardR2L, cardStatic, cardRandom, cardChaotic)

        fun selectDir(dir: Int) {
            cards.forEachIndexed { i, card ->
                card ?: return@forEachIndexed
                if (i == dir) {
                    card.strokeColor = ContextCompat.getColor(requireContext(), R.color.settings_accent)
                    card.strokeWidth = (3 * resources.displayMetrics.density).toInt()
                    card.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.settings_chip_bg_active))
                } else {
                    card.strokeColor = ContextCompat.getColor(requireContext(), R.color.settings_chip_bg_inactive)
                    card.strokeWidth = (1 * resources.displayMetrics.density).toInt()
                    card.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.settings_card_bg_alt))
                }
            }
            updateMoonMoveSpeedVisibility(parent, dir)
        }

        selectDir(configManager.getMoonPathDirection())
        scrollCardIntoView(cards.getOrNull(configManager.getMoonPathDirection()))

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
        val customLayout = parent.findViewById<View>(R.id.layoutMoonCustomXY)

        fun selectPos(pos: Int) {
            cards.forEachIndexed { i, card ->
                card ?: return@forEachIndexed
                if (i == pos) {
                    card.strokeColor = ContextCompat.getColor(requireContext(), R.color.settings_accent)
                    card.strokeWidth = (3 * resources.displayMetrics.density).toInt()
                    card.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.settings_chip_bg_active))
                } else {
                    card.strokeColor = ContextCompat.getColor(requireContext(), R.color.settings_chip_bg_inactive)
                    card.strokeWidth = (1 * resources.displayMetrics.density).toInt()
                    card.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.settings_card_bg_alt))
                }
            }
            if (customLayout != null) {
                val p = customLayout.parent as? ViewGroup
                p?.let { TransitionManager.beginDelayedTransition(it, AutoTransition().apply { duration = 200 }) }
                customLayout.visibility = if (pos == 5) View.VISIBLE else View.GONE
            }
        }

        selectPos(configManager.getMoonStationaryPosition())
        scrollCardIntoView(cards.getOrNull(configManager.getMoonStationaryPosition()))

        // Wire sliders for custom X/Y
        setupSlider(parent, R.id.seekBarMoonCustomX, R.id.textViewMoonCustomXValue, configManager.getMoonCustomX()) { value ->
            configManager.setMoonCustomX(value)
        }
        setupSlider(parent, R.id.seekBarMoonCustomY, R.id.textViewMoonCustomYValue, configManager.getMoonCustomY()) { value ->
            configManager.setMoonCustomY(value)
        }

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
                    card.strokeColor = ContextCompat.getColor(requireContext(), R.color.settings_accent)
                    card.strokeWidth = (3 * resources.displayMetrics.density).toInt()
                    card.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.settings_chip_bg_active))
                } else {
                    card.strokeColor = ContextCompat.getColor(requireContext(), R.color.settings_chip_bg_inactive)
                    card.strokeWidth = (1 * resources.displayMetrics.density).toInt()
                    card.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.settings_card_bg_alt))
                }
            }
        }

        selectColor(configManager.getStarColorIndex())
        scrollCardIntoView(cards.getOrNull(configManager.getStarColorIndex()))

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
                    card.strokeColor = ContextCompat.getColor(requireContext(), R.color.settings_accent)
                    card.strokeWidth = (3 * resources.displayMetrics.density).toInt()
                    card.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.settings_chip_bg_active))
                } else {
                    card.strokeColor = ContextCompat.getColor(requireContext(), R.color.settings_chip_bg_inactive)
                    card.strokeWidth = (1 * resources.displayMetrics.density).toInt()
                    card.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.settings_card_bg_alt))
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
        val switchShootingStars = parent.findViewById<SwitchMaterial>(R.id.switchShootingStars)
        val layoutDensityGroup = parent.findViewById<View>(R.id.layoutShootingStarsDensityGroup)
        if (switchShootingStars != null) {
            val enabled = configManager.isShootingStarsEnabled()
            switchShootingStars.isChecked = enabled
            layoutDensityGroup?.visibility = if (enabled) View.VISIBLE else View.GONE
            switchShootingStars.setOnCheckedChangeListener { _, isChecked ->
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
        val switchFireflies = parent.findViewById<SwitchMaterial>(R.id.switchFireflies)
        if (switchFireflies != null) {
            switchFireflies.isChecked = configManager.isFirefliesEnabled()
            switchFireflies.setOnCheckedChangeListener { _, isChecked ->
                configManager.setFirefliesEnabled(isChecked)
                updateSummaries(parent)
            }
        }

        setupNebulaSection(parent)
    }

    private fun setupNebulaSection(parent: View) {
        val switchNebula = parent.findViewById<SwitchMaterial>(R.id.switchNebula)
        val layoutOptionsGroup = parent.findViewById<View>(R.id.layoutNebulaOptionsGroup)
        if (switchNebula != null) {
            val enabled = configManager.isNebulaEnabled()
            switchNebula.isChecked = enabled
            layoutOptionsGroup?.visibility = if (enabled) View.VISIBLE else View.GONE
            switchNebula.setOnCheckedChangeListener { _, isChecked ->
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
            showNebulaColorPickerDialog(parent)
        }
    }

    private fun updateNebulaColorPreview(parent: View) {
        val color = configManager.getNebulaColor()
        setCircleColorPreview(parent.findViewById(R.id.viewNebulaColorPreview), color)
        parent.findViewById<TextView>(R.id.textViewNebulaColorCode)?.text = String.format("#%06X", 0xFFFFFF and color)
    }

    private fun showNebulaColorPickerDialog(parent: View) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_color_picker, null)
        val previewView = dialogView.findViewById<View>(R.id.dialogColorPreview)
        val hexInput = dialogView.findViewById<EditText>(R.id.dialogColorHexInput)

        val sliderR = dialogView.findViewById<Slider>(R.id.sliderR)
        val sliderG = dialogView.findViewById<Slider>(R.id.sliderG)
        val sliderB = dialogView.findViewById<Slider>(R.id.sliderB)

        val labelR = dialogView.findViewById<TextView>(R.id.labelR)
        val labelG = dialogView.findViewById<TextView>(R.id.labelG)
        val labelB = dialogView.findViewById<TextView>(R.id.labelB)

        val initialColor = configManager.getNebulaColor()
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
            0xFF8E4FDB.toInt(), 0xFF4F7FDB.toInt(), 0xFFDB4FA9.toInt(), 0xFF4FDBC4.toInt(),
            0xFFDB914F.toInt(), 0xFF6A3093.toInt(), 0xFFE53935.toInt(), 0xFF8E24AA.toInt(),
            0xFF00ACC1.toInt(), 0xFF43A047.toInt(), 0xFFFFB300.toInt(), 0xFFFFFFFF.toInt()
        )

        for (i in 0..11) {
            val presetId = resources.getIdentifier("presetColor$i", "id", requireContext().packageName)
            val presetView = dialogView.findViewById<View>(presetId)
            if (presetView != null) {
                presetView.background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(presets[i])
                    setStroke((1 * resources.displayMetrics.density).toInt(), ContextCompat.getColor(requireContext(), R.color.settings_chip_bg_inactive))
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
                configManager.setNebulaColor(currentColor)
                updateNebulaColorPreview(parent)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun setupCombinedMoonPhaseCards(parent: View) {
        val ids = listOf(
            R.id.cardCombinedMoonPhase0, R.id.cardCombinedMoonPhase1, R.id.cardCombinedMoonPhase2, R.id.cardCombinedMoonPhase3,
            R.id.cardCombinedMoonPhase4, R.id.cardCombinedMoonPhase5, R.id.cardCombinedMoonPhase6, R.id.cardCombinedMoonPhase7
        )
        val cards = ids.map { id -> parent.findViewById<MaterialCardView>(id) }

        fun selectPhase(phase: Int) {
            cards.forEachIndexed { i, card ->
                card ?: return@forEachIndexed
                if (i == phase) {
                    card.strokeColor = ContextCompat.getColor(requireContext(), R.color.settings_accent)
                    card.strokeWidth = (3 * resources.displayMetrics.density).toInt()
                    card.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.settings_chip_bg_active))
                } else {
                    card.strokeColor = ContextCompat.getColor(requireContext(), R.color.settings_chip_bg_inactive)
                    card.strokeWidth = (1 * resources.displayMetrics.density).toInt()
                    card.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.settings_card_bg_alt))
                }
            }
        }

        // When auto-advance is on, the phase is controlled entirely by the renderer (see
        // SunnyRenderer.effectiveCombinedMoonPhase) - the manual cards become inert, dimmed
        // pickers reflecting whatever phase was last saved, matching the pattern already used
        // for Storm's variable-mode lock (StormSettingsFragment.updateControlsLockState).
        fun updateLockState(autoAdvanceEnabled: Boolean) {
            cards.forEach { card ->
                card ?: return@forEach
                card.isEnabled = !autoAdvanceEnabled
                card.alpha = if (autoAdvanceEnabled) 0.4f else 1.0f
            }
        }

        selectPhase(configManager.getCombinedMoonPhase())
        scrollCardIntoView(cards.getOrNull(configManager.getCombinedMoonPhase()))
        updateLockState(configManager.isCombinedMoonPhaseAutoAdvanceEnabled())

        cards.forEachIndexed { i, card ->
            card?.setOnClickListener {
                if (configManager.isCombinedMoonPhaseAutoAdvanceEnabled()) return@setOnClickListener
                configManager.setCombinedMoonPhase(i)
                selectPhase(i)
                updateSummaries(parent)
            }
        }

        val switchAutoAdvance = parent.findViewById<SwitchMaterial>(R.id.switchCombinedMoonPhaseAutoAdvance)
        if (switchAutoAdvance != null) {
            switchAutoAdvance.isChecked = configManager.isCombinedMoonPhaseAutoAdvanceEnabled()
            switchAutoAdvance.setOnCheckedChangeListener { _, isChecked ->
                configManager.setCombinedMoonPhaseAutoAdvanceEnabled(isChecked)
                updateLockState(isChecked)
                updateSummaries(parent)
            }
        }
    }

    private fun setupCombinedStarColorCards(parent: View) {
        val ids = listOf(
            R.id.cardCombinedStarColorWhite, R.id.cardCombinedStarColorBlue, R.id.cardCombinedStarColorWarm,
            R.id.cardCombinedStarColorPink, R.id.cardCombinedStarColorGreen, R.id.cardCombinedStarColorMixed
        )
        val cards = ids.map { id -> parent.findViewById<MaterialCardView>(id) }

        fun selectColor(index: Int) {
            cards.forEachIndexed { i, card ->
                card ?: return@forEachIndexed
                if (i == index) {
                    card.strokeColor = ContextCompat.getColor(requireContext(), R.color.settings_accent)
                    card.strokeWidth = (3 * resources.displayMetrics.density).toInt()
                    card.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.settings_chip_bg_active))
                } else {
                    card.strokeColor = ContextCompat.getColor(requireContext(), R.color.settings_chip_bg_inactive)
                    card.strokeWidth = (1 * resources.displayMetrics.density).toInt()
                    card.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.settings_card_bg_alt))
                }
            }
        }

        selectColor(configManager.getCombinedStarColorIndex())
        scrollCardIntoView(cards.getOrNull(configManager.getCombinedStarColorIndex()))

        cards.forEachIndexed { i, card ->
            card?.setOnClickListener {
                configManager.setCombinedStarColorIndex(i)
                selectColor(i)
                updateSummaries(parent)
            }
        }
    }

    private fun setupStarColorPreviews(parent: View) {
        val whiteView = parent.findViewById<View>(R.id.viewStarColorWhitePreview)
        val blueView = parent.findViewById<View>(R.id.viewStarColorBluePreview)
        val warmView = parent.findViewById<View>(R.id.viewStarColorWarmPreview)
        val pinkView = parent.findViewById<View>(R.id.viewStarColorPinkPreview)
        val greenView = parent.findViewById<View>(R.id.viewStarColorGreenPreview)
        val mixedView = parent.findViewById<View>(R.id.viewStarColorMixedPreview)

        setCircleColorPreview(whiteView, 0xFFFFFFFF.toInt())
        setCircleColorPreview(blueView, 0xFF99CCFF.toInt())
        setCircleColorPreview(warmView, 0xFFFFF2B3.toInt())
        setCircleColorPreview(pinkView, 0xFFFFB3D9.toInt())
        setCircleColorPreview(greenView, 0xFF80FFB3.toInt())
        setCardGradientPreview(mixedView, 0xFFD0B0FF.toInt(), 0xFFFFA0D0.toInt())

        val combinedWhiteView = parent.findViewById<View>(R.id.viewCombinedStarColorWhitePreview)
        val combinedBlueView = parent.findViewById<View>(R.id.viewCombinedStarColorBluePreview)
        val combinedWarmView = parent.findViewById<View>(R.id.viewCombinedStarColorWarmPreview)
        val combinedPinkView = parent.findViewById<View>(R.id.viewCombinedStarColorPinkPreview)
        val combinedGreenView = parent.findViewById<View>(R.id.viewCombinedStarColorGreenPreview)
        val combinedMixedView = parent.findViewById<View>(R.id.viewCombinedStarColorMixedPreview)

        setCircleColorPreview(combinedWhiteView, 0xFFFFFFFF.toInt())
        setCircleColorPreview(combinedBlueView, 0xFF99CCFF.toInt())
        setCircleColorPreview(combinedWarmView, 0xFFFFF2B3.toInt())
        setCircleColorPreview(combinedPinkView, 0xFFFFB3D9.toInt())
        setCircleColorPreview(combinedGreenView, 0xFF80FFB3.toInt())
        setCardGradientPreview(combinedMixedView, 0xFFD0B0FF.toInt(), 0xFFFFA0D0.toInt())
    }

    private fun setupCombinedTrajectory(parent: View) {
        val combinedTrajectories = resources.getStringArray(R.array.combined_trajectories)
        setupDropdown(parent, R.id.spinnerCombinedTrajectory, combinedTrajectories, configManager.getCombinedPathDirection()) { position ->
            configManager.setCombinedPathDirection(position)
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
                    setStroke((1 * resources.displayMetrics.density).toInt(), ContextCompat.getColor(requireContext(), R.color.settings_chip_bg_inactive))
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
            val speedText = when {
                speed <= 25 -> getString(R.string.summary_speed_very_slow)
                speed <= 50 -> getString(R.string.summary_speed_normal)
                speed <= 75 -> getString(R.string.summary_speed_fast)
                else -> getString(R.string.summary_speed_very_fast)
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
