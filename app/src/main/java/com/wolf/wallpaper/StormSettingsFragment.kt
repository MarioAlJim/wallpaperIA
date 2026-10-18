package com.wolf.wallpaper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.LinearGradient
import android.graphics.Shader
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.card.MaterialCardView
import com.google.android.material.switchmaterial.SwitchMaterial
import com.wolf.wallpaper.core.ConfigManager
import java.io.File
import java.io.FileOutputStream

class StormSettingsFragment : Fragment() {

    private lateinit var configManager: ConfigManager
    private val backgroundBitmapCache = HashMap<String, Bitmap>()

    private val pickImageLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            saveCustomBackground(uri)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_storm_settings, container, false)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        backgroundBitmapCache.forEach { (_, bitmap) ->
            if (!bitmap.isRecycled) {
                bitmap.recycle()
            }
        }
        backgroundBitmapCache.clear()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configManager = (requireActivity() as WallpaperSettingsActivity).configManager

        // 1. Setup Accordions
        SettingsAccordionHelper.setupAccordionGroup(
            view,
            SettingsAccordionHelper.AccordionSpec(R.id.headerCloudsWind, R.id.contentCloudsWind, R.id.dividerCloudsWind, R.id.arrowCloudsWind),
            SettingsAccordionHelper.AccordionSpec(R.id.headerRain, R.id.contentRain, R.id.dividerRain, R.id.arrowRain),
            SettingsAccordionHelper.AccordionSpec(R.id.headerLightning, R.id.contentLightning, R.id.dividerLightning, R.id.arrowLightning),
            SettingsAccordionHelper.AccordionSpec(R.id.headerBackground, R.id.contentBackground, R.id.dividerBackground, R.id.arrowBackground)
        )

        // 2. Setup Nubes y Viento Controls
        val cloudOptions = resources.getStringArray(R.array.cloud_options)
        val cloudCounts = intArrayOf(0, 1, 2, 4, 6, 8, 10)
        val initialCloudIndex = getClosestCloudOptionIndex(configManager.getCloudDensity())

        val textViewCloudDensityValue = view.findViewById<TextView>(R.id.textViewCloudDensityValue)
        textViewCloudDensityValue?.text = cloudOptions.getOrNull(initialCloudIndex) ?: "5 nubes"

        setupDropdown(view, R.id.spinnerCloudDensity, cloudOptions, initialCloudIndex) { position ->
            val count = cloudCounts[position]
            configManager.setCloudDensity(count)
            textViewCloudDensityValue?.text = cloudOptions[position]
            updateSummaries(view)
        }



        val cloudDynamicsOptions = resources.getStringArray(R.array.cloud_dynamics_options)
        val cloudDynamicsValues = intArrayOf(0, 25, 50, 75, 100)
        val initialCloudDynamicsIndex = SettingsCardSelectorHelper.closestValueIndex(cloudDynamicsValues, configManager.getCloudDynamicsSpeed())
        val textViewCloudDynamicsSpeedValue = view.findViewById<TextView>(R.id.textViewCloudDynamicsSpeedValue)
        textViewCloudDynamicsSpeedValue?.text = cloudDynamicsOptions.getOrNull(initialCloudDynamicsIndex) ?: "Extremo"
        setupDropdown(view, R.id.spinnerCloudDynamicsSpeed, cloudDynamicsOptions, initialCloudDynamicsIndex) { position ->
            configManager.setCloudDynamicsSpeed(cloudDynamicsValues[position])
            textViewCloudDynamicsSpeedValue?.text = cloudDynamicsOptions[position]
            updateSummaries(view)
        }

        val switchWindLines = view.findViewById<SwitchMaterial>(R.id.switchWindLines)
        val layoutWindLinesIntensity = view.findViewById<View>(R.id.layoutWindLinesIntensity)
        if (switchWindLines != null) {
            val isEnabled = configManager.isWindLinesEnabled()
            switchWindLines.isChecked = isEnabled
            layoutWindLinesIntensity?.visibility = if (isEnabled) View.VISIBLE else View.GONE
            switchWindLines.setOnCheckedChangeListener { _, isChecked ->
                configManager.setWindLinesEnabled(isChecked)
                layoutWindLinesIntensity?.visibility = if (isChecked) View.VISIBLE else View.GONE
                updateSummaries(view)
            }
        }

        val windLinesOptions = resources.getStringArray(R.array.wind_lines_options)
        val windLinesValues = intArrayOf(0, 25, 50, 75, 100)
        val initialWindLinesIndex = SettingsCardSelectorHelper.closestValueIndex(windLinesValues, configManager.getWindLinesIntensity())
        val textViewWindLinesIntensityValue = view.findViewById<TextView>(R.id.textViewWindLinesIntensityValue)
        textViewWindLinesIntensityValue?.text = windLinesOptions.getOrNull(initialWindLinesIndex) ?: "Normal"
        setupDropdown(view, R.id.spinnerWindLinesIntensity, windLinesOptions, initialWindLinesIndex) { position ->
            configManager.setWindLinesIntensity(windLinesValues[position])
            textViewWindLinesIntensityValue?.text = windLinesOptions[position]
            updateSummaries(view)
        }

        // 3. Setup Lluvia Controls

        val switchScreenDroplets = view.findViewById<SwitchMaterial>(R.id.switchScreenDroplets)
        val layoutScreenDropletsSize = view.findViewById<View>(R.id.layoutScreenDropletsSize)
        if (switchScreenDroplets != null) {
            val isEnabled = configManager.isScreenDropletsEnabled()
            switchScreenDroplets.isChecked = isEnabled
            layoutScreenDropletsSize?.visibility = if (isEnabled) View.VISIBLE else View.GONE
            switchScreenDroplets.setOnCheckedChangeListener { _, isChecked ->
                configManager.setScreenDropletsEnabled(isChecked)
                layoutScreenDropletsSize?.visibility = if (isChecked) View.VISIBLE else View.GONE
                updateSummaries(view)
            }
        }

        val dropletsSizeOptions = resources.getStringArray(R.array.droplets_size_options)
        val dropletsSizeValues = intArrayOf(10, 55, 100, 150, 200)
        val initialDropletsSizeIndex = SettingsCardSelectorHelper.closestValueIndex(dropletsSizeValues, configManager.getScreenDropletsSize())
        val textViewScreenDropletsSizeValue = view.findViewById<TextView>(R.id.textViewScreenDropletsSizeValue)
        textViewScreenDropletsSizeValue?.text = dropletsSizeOptions.getOrNull(initialDropletsSizeIndex) ?: "Medianas"
        setupDropdown(view, R.id.spinnerScreenDropletsSize, dropletsSizeOptions, initialDropletsSizeIndex) { position ->
            configManager.setScreenDropletsSize(dropletsSizeValues[position])
            textViewScreenDropletsSizeValue?.text = dropletsSizeOptions[position]
            updateSummaries(view)
        }

        // 4. Setup Rayos Controls
        val freqOptions = resources.getStringArray(R.array.frequency_options)
        val freqValues = intArrayOf(12, 31, 62, 87, 100)
        
        val initialLightningFreqIndex = getClosestFrequencyOptionIndex(configManager.getLightningFrequency())
        val textViewLightningFrequencyValue = view.findViewById<TextView>(R.id.textViewLightningFrequencyValue)
        textViewLightningFrequencyValue?.text = freqOptions.getOrNull(initialLightningFreqIndex) ?: "Medio"
        
        setupDropdown(view, R.id.spinnerLightningFrequency, freqOptions, initialLightningFreqIndex) { position ->
            val value = freqValues[position]
            configManager.setLightningFrequency(value)
            textViewLightningFrequencyValue?.text = freqOptions[position]
            updateSummaries(view)
        }

        val lightningDurationOptions = resources.getStringArray(R.array.lightning_duration_options)
        val lightningDurationValues = intArrayOf(0, 25, 50, 75, 100)
        val initialLightningDurationIndex = SettingsCardSelectorHelper.closestValueIndex(lightningDurationValues, configManager.getLightningDuration())
        val textViewLightningDurationValue = view.findViewById<TextView>(R.id.textViewLightningDurationValue)
        textViewLightningDurationValue?.text = lightningDurationOptions.getOrNull(initialLightningDurationIndex) ?: "Normal"
        setupDropdown(view, R.id.spinnerLightningDuration, lightningDurationOptions, initialLightningDurationIndex) { position ->
            configManager.setLightningDuration(lightningDurationValues[position])
            textViewLightningDurationValue?.text = lightningDurationOptions[position]
            updateSummaries(view)
        }

        // 4b. Setup Destellos en Nubes Controls
        val initialCloudFlashIndex = getClosestFrequencyOptionIndex(configManager.getCloudFlashFrequency())
        val textViewCloudFlashFrequencyValue = view.findViewById<TextView>(R.id.textViewCloudFlashFrequencyValue)
        textViewCloudFlashFrequencyValue?.text = freqOptions.getOrNull(initialCloudFlashIndex) ?: "Medio"
        
        setupDropdown(view, R.id.spinnerCloudFlashFrequency, freqOptions, initialCloudFlashIndex) { position ->
            val value = freqValues[position]
            configManager.setCloudFlashFrequency(value)
            textViewCloudFlashFrequencyValue?.text = freqOptions[position]
            updateSummaries(view)
        }

        val switchCloudFlash = view.findViewById<SwitchMaterial>(R.id.switchCloudFlash)
        if (switchCloudFlash != null) {
            switchCloudFlash.isChecked = configManager.isCloudFlashEnabled()
            switchCloudFlash.setOnCheckedChangeListener { _, isChecked ->
                configManager.setCloudFlashEnabled(isChecked)
                updateSummaries(view)
            }
        }

        val switchLightningFlash = view.findViewById<SwitchMaterial>(R.id.switchLightningFlash)
        if (switchLightningFlash != null) {
            switchLightningFlash.isChecked = configManager.isLightningFlashEnabled()
            switchLightningFlash.setOnCheckedChangeListener { _, isChecked ->
                configManager.setLightningFlashEnabled(isChecked)
                updateSummaries(view)
            }
        }

        val switchInteractiveLightning = view.findViewById<SwitchMaterial>(R.id.switchInteractiveLightning)
        if (switchInteractiveLightning != null) {
            switchInteractiveLightning.isChecked = configManager.isInteractiveLightningEnabled()
            switchInteractiveLightning.setOnCheckedChangeListener { _, isChecked ->
                configManager.setInteractiveLightningEnabled(isChecked)
                updateSummaries(view)
            }
        }

        val backgroundModes = resources.getStringArray(R.array.background_modes)
        setupDropdown(view, R.id.spinnerBackgroundMode, backgroundModes, configManager.getBackgroundIndex()) { position ->
            configManager.setBackgroundIndex(position)
            updateCustomBackgroundViewsVisibility(view, position)
        }

        val bgBrightnessOptions = resources.getStringArray(R.array.bg_brightness_options)
        val bgBrightnessValues = intArrayOf(30, 60, 100, 150)
        val initialBgBrightnessIndex = SettingsCardSelectorHelper.closestValueIndex(bgBrightnessValues, configManager.getBgBrightness())
        val textViewBgBrightnessValue = view.findViewById<TextView>(R.id.textViewBgBrightnessValue)
        textViewBgBrightnessValue?.text = bgBrightnessOptions.getOrNull(initialBgBrightnessIndex) ?: "Normal"
        setupDropdown(view, R.id.spinnerBgBrightness, bgBrightnessOptions, initialBgBrightnessIndex) { position ->
            configManager.setBgBrightness(bgBrightnessValues[position])
            textViewBgBrightnessValue?.text = bgBrightnessOptions[position]
            updateSummaries(view)
        }

        view.findViewById<Button>(R.id.btnSelectCustomBg)?.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        val switchStormVariable = view.findViewById<SwitchMaterial>(R.id.switchStormVariable)
        if (switchStormVariable != null) {
            val isEnabled = configManager.isStormVariableEnabled()
            switchStormVariable.isChecked = isEnabled
            updateControlsLockState(view, isEnabled)
            switchStormVariable.setOnCheckedChangeListener { _, isChecked ->
                configManager.setStormVariableEnabled(isChecked)
                updateControlsLockState(view, isChecked)
                updateSummaries(view)
            }
        }

        view.findViewById<ImageView>(R.id.btnStormHelp)?.setOnClickListener {
            showHelpDialog()
        }

        updateCustomBackgroundViewsVisibility(view, configManager.getBackgroundIndex())
        refreshStormCardPreviews(view)
        updateSummaries(view)
    }

    private fun showHelpDialog() {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.storm_help_dialog_title)
            .setMessage(Html.fromHtml(getString(R.string.storm_help_dialog_message), Html.FROM_HTML_MODE_LEGACY))
            .setNeutralButton("Ver tutorial") { _, _ ->
                (requireActivity() as WallpaperSettingsActivity).startTutorial(0)
            }
            .setPositiveButton("Entendido", null)
            .show()
    }

    private fun getClosestCloudOptionIndex(density: Int): Int {
        val targets = intArrayOf(0, 1, 2, 4, 6, 8, 10)
        val actualClouds = if (density > 10) {
            when (density) {
                25 -> 2
                50 -> 5
                75 -> 8
                100 -> 10
                else -> (density / 100f * 10).toInt()
            }
        } else {
            density
        }
        var closestIndex = 0
        var minDiff = Int.MAX_VALUE
        for (i in targets.indices) {
            val diff = kotlin.math.abs(actualClouds - targets[i])
            if (diff < minDiff) {
                minDiff = diff
                closestIndex = i
            }
        }
        return closestIndex
    }

    private fun getClosestFrequencyOptionIndex(value: Int): Int {
        return when {
            value <= 21 -> 0 // Muy raro (12)
            value <= 46 -> 1 // Raro (31)
            value <= 74 -> 2 // Medio (62)
            value <= 93 -> 3 // Frecuente (87)
            else -> 4        // Muy frecuente (100)
        }
    }

    private fun setupDropdown(parent: View, viewId: Int, options: Array<String>, initialIndex: Int, onSelected: (Int) -> Unit) {
        val view = parent.findViewById<View>(viewId)
        if (view is LinearLayout) {
            populateCardSelector(parent, view, options, initialIndex, onSelected)
        } else if (view is AutoCompleteTextView) {
            val adapter = ArrayAdapter(requireContext(), R.layout.spinner_dropdown_item, options)
            view.setAdapter(adapter)
            view.setText(options.getOrNull(initialIndex), false)
            view.setOnItemClickListener { _, _, position, _ ->
                onSelected(position)
                updateSummaries(parent)
            }
        }
    }

    private fun rainColorHex(colorIndex: Int): String = when (colorIndex) {
        0 -> "#1E90FF"
        1 -> "#FFFFFF"
        2 -> "#FF3030"
        3 -> "#00FF7F"
        4 -> "#FFD700"
        5 -> "#8A2BE2"
        else -> "#1E90FF"
    }

    private fun lightningColorHex(colorIndex: Int): String = when (colorIndex) {
        0 -> "#FFFFFF"
        1 -> "#6699FF"
        2 -> "#FFE533"
        3 -> "#FF3333"
        4 -> "#33FF33"
        5 -> "#CC4DFF"
        6 -> "#FFE533"
        else -> "#FFFFFF"
    }

    /** Builds the `spinnerBackgroundMode` preview (thumbnail ImageView instead of a Canvas-drawn StormCardPreviewView). */
    private fun buildBackgroundPreviewImage(optionIndex: Int): ImageView {
        val previewImage = ImageView(requireContext()).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            clipToOutline = true
        }

        val bgPath = when (optionIndex) {
            0 -> "black"
            1 -> "background/background.jpg"
            2 -> "background/background_02.png"
            3 -> "background/background_03.png"
            4 -> "background/background_04.png"
            5 -> "background/background_05.png"
            6 -> "background/background_06.png"
            7 -> "background/background_07.png"
            8 -> "custom"
            else -> "black"
        }

        if (bgPath == "black") {
            previewImage.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.settings_card_bg_alt3))
        } else if (bgPath == "custom") {
            val customBgFile = File(requireContext().filesDir, "custom_background.png")
            if (customBgFile.exists()) {
                val path = customBgFile.absolutePath
                var bitmap = backgroundBitmapCache[path]
                if (bitmap == null) {
                    bitmap = loadThumbnailFromAsset(requireContext(), path, 38.dpToPx(), 50.dpToPx())
                    if (bitmap != null) {
                        backgroundBitmapCache[path] = bitmap
                    }
                }
                previewImage.setImageBitmap(bitmap)
            } else {
                previewImage.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.settings_card_bg_alt))
            }
        } else {
            var bitmap = backgroundBitmapCache[bgPath]
            if (bitmap == null) {
                bitmap = loadThumbnailFromAsset(requireContext(), bgPath, 38.dpToPx(), 50.dpToPx())
                if (bitmap != null) {
                    backgroundBitmapCache[bgPath] = bitmap
                }
            }
            previewImage.setImageBitmap(bitmap)
        }
        return previewImage
    }

    private fun populateCardSelector(parent: View, container: LinearLayout, options: Array<String>, initialIndex: Int, onSelected: (Int) -> Unit) {
        val containerId = container.id
        val activeRainColor = Color.parseColor(rainColorHex(configManager.getRainColorIndex()))
        val activeLightningColor = Color.parseColor(lightningColorHex(configManager.getLightningColorIndex()))
        val activeWindDir = configManager.getWindDirection()

        val isTextOnly = containerId == R.id.spinnerLightningFrequency || containerId == R.id.spinnerCloudFlashFrequency

        SettingsCardSelectorHelper.populate(
            context = requireContext(),
            container = container,
            options = options,
            selectedIndex = initialIndex,
            previewFactory = if (isTextOnly) null else { index ->
                if (containerId != R.id.spinnerBackgroundMode) {
                    StormCardPreviewView(requireContext(), containerId, index, activeRainColor, activeWindDir, activeLightningColor)
                } else {
                    buildBackgroundPreviewImage(index)
                }
            }
        ) { position ->
            onSelected(position)
            updateSummaries(parent)
        }
    }

    private fun refreshStormCardPreviews(view: View) {
        val windDirections = resources.getStringArray(R.array.wind_directions)
        val initialWindDirIndex = if (configManager.getWindDirection() == 2) 1 else 0
        setupDropdown(view, R.id.spinnerWindDirection, windDirections, initialWindDirIndex) { position ->
            val dir = if (position == 1) 2 else 0
            configManager.setWindDirection(dir)
            refreshStormCardPreviews(view)
        }

        val windIntensities = resources.getStringArray(R.array.wind_intensities)
        val currentWindIntensity = configManager.getWindIntensity()
        val initialWindIntensityIndex = when (currentWindIntensity) {
            0 -> 0
            25 -> 1
            50 -> 2
            75 -> 3
            100 -> 4
            else -> {
                when {
                    currentWindIntensity < 12 -> 0
                    currentWindIntensity < 37 -> 1
                    currentWindIntensity < 62 -> 2
                    currentWindIntensity < 87 -> 3
                    else -> 4
                }
            }
        }
        setupDropdown(view, R.id.spinnerWindIntensity, windIntensities, initialWindIntensityIndex) { position ->
            val selectedIntensity = when (position) {
                0 -> 0
                1 -> 25
                2 -> 50
                3 -> 75
                4 -> 100
                else -> 50
            }
            configManager.setWindIntensity(selectedIntensity)
            refreshStormCardPreviews(view)
        }

        val rainIntensities = resources.getStringArray(R.array.rain_intensities)
        val initialRainValue = configManager.getRainIntensity()
        val initialRainProgress = (initialRainValue / 25).coerceIn(0, 4)
        setupDropdown(view, R.id.spinnerRainIntensity, rainIntensities, initialRainProgress) { position ->
            configManager.setRainIntensity(position * 25)
        }

        val rainSpeeds = resources.getStringArray(R.array.rain_speeds)
        val currentSpeed = configManager.getRainSpeed()
        val initialSpeedIndex = when (currentSpeed) {
            0 -> 0
            25 -> 1
            50 -> 2
            else -> {
                when {
                    currentSpeed < 12 -> 0
                    currentSpeed < 37 -> 1
                    else -> 2
                }
            }
        }
        setupDropdown(view, R.id.spinnerRainSpeed, rainSpeeds, initialSpeedIndex) { position ->
            val selectedSpeed = when (position) {
                0 -> 0
                1 -> 25
                2 -> 50
                else -> 25
            }
            configManager.setRainSpeed(selectedSpeed)
            refreshStormCardPreviews(view)
        }

        val rainColors = resources.getStringArray(R.array.rain_colors)
        setupDropdown(view, R.id.spinnerRainColor, rainColors, configManager.getRainColorIndex()) { position ->
            configManager.setRainColorIndex(position)
            refreshStormCardPreviews(view)
        }

        val rainSpawnModes = resources.getStringArray(R.array.rain_spawn_modes)
        setupDropdown(view, R.id.spinnerRainSpawnMode, rainSpawnModes, configManager.getRainSpawnMode()) { position ->
            configManager.setRainSpawnMode(position)
        }

        val lightningColors = resources.getStringArray(R.array.lightning_colors)
        setupDropdown(view, R.id.spinnerLightningColor, lightningColors, configManager.getLightningColorIndex()) { position ->
            configManager.setLightningColorIndex(position)
            refreshStormCardPreviews(view)
        }

        val lightningSpawnModes = resources.getStringArray(R.array.lightning_spawn_modes)
        setupDropdown(view, R.id.spinnerLightningSpawnMode, lightningSpawnModes, configManager.getLightningSpawnMode()) { position ->
            configManager.setLightningSpawnMode(position)
        }

        setupDropdown(view, R.id.spinnerCloudFlashColor, lightningColors, configManager.getCloudFlashColorIndex()) { position ->
            configManager.setCloudFlashColorIndex(position)
        }

        updateControlsLockState(view, configManager.isStormVariableEnabled())
    }

    private fun saveCustomBackground(uri: android.net.Uri) {
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
            val boundsOpts = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
                inScaled = false
            }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOpts)
            }
            val origW = boundsOpts.outWidth
            val origH = boundsOpts.outHeight
            
            val maxSize = 2048
            var sampleSize = 1
            while (origW / (sampleSize * 2) >= maxSize && origH / (sampleSize * 2) >= maxSize) {
                sampleSize *= 2
            }
            
            val decodeOpts = BitmapFactory.Options().apply {
                inScaled = false
                inSampleSize = sampleSize
            }
            
            // 3. Decode the bitmap safely
            var bitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOpts)
            } ?: return

            // 4. Scale exactly to maxSize if it's still larger on either dimension
            val maxDim = maxOf(bitmap.width, bitmap.height)
            if (maxDim > maxSize) {
                val scale = maxSize.toFloat() / maxDim
                val newW = (bitmap.width * scale).toInt().coerceAtLeast(1)
                val newH = (bitmap.height * scale).toInt().coerceAtLeast(1)
                val scaled = Bitmap.createScaledBitmap(bitmap, newW, newH, true)
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
                val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                if (rotatedBitmap != bitmap) {
                    bitmap.recycle()
                    bitmap = rotatedBitmap
                }
            }

            // 6. Save as PNG to internal storage
            val outputFile = File(context.filesDir, "custom_background.png")
            FileOutputStream(outputFile).use { output ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 90, output)
            }
            bitmap.recycle()

            configManager.setBackgroundIndex(8)
            view?.let {
                updateCustomBackgroundViewsVisibility(it, 8)
                refreshStormCardPreviews(it)
                updateSummaries(it)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateCustomBackgroundViewsVisibility(parent: View, position: Int) {
        val btnSelect = parent.findViewById<View>(R.id.btnSelectCustomBg)
        val container = parent.findViewById<View>(R.id.tvCustomBgStatus)
        if (position == 8) {
            btnSelect?.visibility = View.VISIBLE
            container?.visibility = View.VISIBLE
        } else {
            btnSelect?.visibility = View.GONE
            container?.visibility = View.GONE
        }
    }

    private fun updateSummaries(parent: View) {
        val summaryCloudsWind = parent.findViewById<TextView>(R.id.summaryCloudsWind) ?: return
        val summaryRain = parent.findViewById<TextView>(R.id.summaryRain) ?: return
        val summaryLightning = parent.findViewById<TextView>(R.id.summaryLightning) ?: return
        val summaryBackground = parent.findViewById<TextView>(R.id.summaryBackground) ?: return

        val accentColor = String.format(
            "#%06X",
            0xFFFFFF and ContextCompat.getColor(requireContext(), R.color.settings_accent_text)
        )

        // Preload arrays for summaries
        val freqOptions = resources.getStringArray(R.array.frequency_options)
        val rainColors = resources.getStringArray(R.array.rain_colors)
        val rainSpeeds = resources.getStringArray(R.array.rain_speeds)
        val backgroundModes = resources.getStringArray(R.array.background_modes)

        val density = configManager.getCloudDensity()
        val densityText = if (density <= 15) {
            when (density) {
                0 -> getString(R.string.summary_cloud_clear)
                1 -> getString(R.string.summary_cloud_one_cloud)
                else -> getString(R.string.summary_cloud_n_clouds, density)
            }
        } else {
            when {
                density <= 0 -> getString(R.string.summary_cloud_clear)
                density <= 25 -> getString(R.string.summary_cloud_few)
                density <= 50 -> getString(R.string.summary_cloud_cloudy)
                density <= 75 -> getString(R.string.summary_cloud_very_cloudy)
                else -> getString(R.string.summary_cloud_overcast)
            }
        }
        val windDir = when (configManager.getWindDirection()) {
            0 -> getString(R.string.summary_wind_left)
            2 -> getString(R.string.summary_wind_right)
            else -> getString(R.string.summary_wind_calm)
        }
        val windSpeed = configManager.getWindIntensity()
        val isVariable = configManager.isStormVariableEnabled()
        val densityLabel = if (isVariable) getString(R.string.summary_active_cycle) else densityText
        val windDirLabel = if (isVariable) getString(R.string.summary_active_cycle) else windDir
        
        val windSpeedText = when (windSpeed) {
            0 -> getString(R.string.summary_wind_none)
            25 -> getString(R.string.summary_wind_slow)
            50 -> getString(R.string.summary_wind_medium)
            75 -> getString(R.string.summary_wind_fast)
            100 -> getString(R.string.summary_wind_very_fast)
            else -> {
                when {
                    windSpeed < 12 -> getString(R.string.summary_wind_none)
                    windSpeed < 37 -> getString(R.string.summary_wind_slow)
                    windSpeed < 62 -> getString(R.string.summary_wind_medium)
                    windSpeed < 87 -> getString(R.string.summary_wind_fast)
                    else -> getString(R.string.summary_wind_very_fast)
                }
            }
        }
        val windSpeedLabel = if (isVariable) getString(R.string.summary_active_cycle) else windSpeedText

        summaryCloudsWind.text = Html.fromHtml(
            getString(R.string.summary_clouds_wind_format, accentColor, densityLabel, windDirLabel, windSpeedLabel),
            Html.FROM_HTML_MODE_LEGACY
        )

        val rainIntensity = configManager.getRainIntensity()
        val rainText = when {
            rainIntensity <= 0 -> getString(R.string.summary_rain_none)
            rainIntensity <= 25 -> getString(R.string.summary_rain_drizzle)
            rainIntensity <= 50 -> getString(R.string.summary_rain_medium)
            rainIntensity <= 75 -> getString(R.string.summary_rain_heavy)
            else -> getString(R.string.summary_rain_torrential)
        }
        val rainColorText = rainColors.getOrNull(configManager.getRainColorIndex()) ?: (rainColors.getOrNull(0) ?: "")
        val rainSpeedVal = configManager.getRainSpeed()
        val rainSpeedIndex = when {
            rainSpeedVal < 12 -> 0
            rainSpeedVal < 37 -> 1
            else -> 2
        }
        val rainSpeedText = rainSpeeds.getOrNull(rainSpeedIndex) ?: ""

        val rainLabel = if (isVariable) getString(R.string.summary_active_cycle) else rainText
        val rainSpeedLabel = if (isVariable) getString(R.string.summary_active_cycle) else rainSpeedText

        summaryRain.text = Html.fromHtml(
            getString(R.string.summary_rain_format, accentColor, rainLabel, rainSpeedLabel, rainColorText),
            Html.FROM_HTML_MODE_LEGACY
        )

        val lightningFreq = configManager.getLightningFrequency()
        val lightningText = when {
            lightningFreq <= 21 -> freqOptions.getOrNull(0) ?: ""
            lightningFreq <= 46 -> freqOptions.getOrNull(1) ?: ""
            lightningFreq <= 74 -> freqOptions.getOrNull(2) ?: ""
            lightningFreq <= 93 -> freqOptions.getOrNull(3) ?: ""
            else -> freqOptions.getOrNull(4) ?: ""
        }
        val flashStatus = if (configManager.isCloudFlashEnabled()) {
            val cloudFreq = configManager.getCloudFlashFrequency()
            val cloudFreqText = when {
                cloudFreq <= 21 -> freqOptions.getOrNull(0) ?: ""
                cloudFreq <= 46 -> freqOptions.getOrNull(1) ?: ""
                cloudFreq <= 74 -> freqOptions.getOrNull(2) ?: ""
                cloudFreq <= 93 -> freqOptions.getOrNull(3) ?: ""
                else -> freqOptions.getOrNull(4) ?: ""
            }
            getString(R.string.summary_active_with_val, cloudFreqText)
        } else {
            getString(R.string.summary_inactive)
        }
        val lightningLabel = if (isVariable) getString(R.string.summary_active_cycle) else lightningText
        summaryLightning.text = Html.fromHtml(
            getString(R.string.summary_lightning_format, accentColor, lightningLabel, flashStatus),
            Html.FROM_HTML_MODE_LEGACY
        )

        val bgIndex = configManager.getBackgroundIndex()
        val bgText = backgroundModes.getOrNull(bgIndex) ?: ""
        summaryBackground.text = Html.fromHtml(
            getString(R.string.summary_background_format, accentColor, bgText),
            Html.FROM_HTML_MODE_LEGACY
        )

        val bgBrightnessOptions = resources.getStringArray(R.array.bg_brightness_options)
        val bgBrightnessValues = intArrayOf(30, 60, 100, 150)
        val currentBgBrightnessIndex = SettingsCardSelectorHelper.closestValueIndex(bgBrightnessValues, configManager.getBgBrightness())
        parent?.findViewById<TextView>(R.id.textViewBgBrightnessValue)?.text = bgBrightnessOptions.getOrNull(currentBgBrightnessIndex) ?: "Normal"
    }

    private fun Int.dpToPx(): Int {
        val density = resources.displayMetrics.density
        return Math.round(this * density)
    }

    private fun Float.dpToPx(): Float {
        val density = resources.displayMetrics.density
        return this * density
    }

    private fun loadThumbnailFromAsset(context: Context, path: String, reqWidth: Int, reqHeight: Int): Bitmap? {
        val isCustom = path.contains("/") && !path.startsWith("background/")
        try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            if (isCustom) {
                BitmapFactory.decodeFile(path, options)
            } else {
                context.assets.open(path).use { inputStream ->
                    BitmapFactory.decodeStream(inputStream, null, options)
                }
            }
            
            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
            options.inJustDecodeBounds = false
            
            return if (isCustom) {
                BitmapFactory.decodeFile(path, options)
            } else {
                context.assets.open(path).use { inputStream ->
                    BitmapFactory.decodeStream(inputStream, null, options)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    private fun updateControlsLockState(view: View, isVariableEnabled: Boolean) {
        val enabled = !isVariableEnabled
        val alpha = if (enabled) 1.0f else 0.4f

        val idsToLock = arrayOf(
            R.id.spinnerCloudDensity,
            R.id.spinnerWindDirection,
            R.id.spinnerWindIntensity,
            R.id.spinnerCloudDynamicsSpeed,
            R.id.spinnerRainIntensity,
            R.id.spinnerRainSpeed,
            R.id.spinnerLightningFrequency,
            R.id.spinnerCloudFlashFrequency
        )

        for (id in idsToLock) {
            val controlView = view.findViewById<View>(id)
            if (controlView != null) {
                var controlEnabled = enabled
                if (id == R.id.spinnerWindDirection && configManager.getWindIntensity() == 0) {
                    controlEnabled = false
                }
                val controlAlpha = if (controlEnabled) 1.0f else 0.4f
                controlView.isEnabled = controlEnabled
                controlView.alpha = controlAlpha
                if (controlView is ViewGroup) {
                    for (i in 0 until controlView.childCount) {
                        val child = controlView.getChildAt(i)
                        child.isEnabled = controlEnabled
                        child.alpha = controlAlpha
                        if (child is ViewGroup) {
                            for (j in 0 until child.childCount) {
                                child.getChildAt(j).isEnabled = controlEnabled
                                child.getChildAt(j).alpha = controlAlpha
                            }
                        }
                    }
                }
            }
        }
    }

    private inner class StormCardPreviewView(
        context: Context,
        val containerId: Int,
        val optionIndex: Int,
        var activeRainColor: Int,
        var activeWindDir: Int,
        var activeLightningColor: Int
    ) : View(context) {

        fun updateConfig(rainColor: Int, windDir: Int, lightningColor: Int) {
            this.activeRainColor = rainColor
            this.activeWindDir = windDir
            this.activeLightningColor = lightningColor
            invalidate()
        }

        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val path = Path()

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val w = width.toFloat()
            val h = height.toFloat()
            if (w <= 0 || h <= 0) return

            when (containerId) {
                R.id.spinnerCloudDensity -> {
                    val cloudCounts = intArrayOf(0, 1, 2, 4, 6, 8, 10)
                    val count = cloudCounts.getOrElse(optionIndex) { 0 }
                    if (count > 0) {
                        drawStylizedClouds(canvas, w, h, count)
                    } else {
                        paint.color = ContextCompat.getColor(context, R.color.settings_text_secondary)
                        paint.style = Paint.Style.STROKE
                        paint.strokeWidth = 1.dpToPx().toFloat()
                        canvas.drawCircle(w * 0.5f, h * 0.5f, 10.dpToPx().toFloat(), paint)
                    }
                }

                R.id.spinnerWindDirection -> {
                    paint.color = Color.parseColor("#8A2BE2")
                    paint.strokeWidth = 2.dpToPx().toFloat()
                    paint.style = Paint.Style.STROKE
                    paint.strokeCap = Paint.Cap.ROUND

                    when (optionIndex) {
                        0 -> { // Left
                            drawWindLine(canvas, w * 0.8f, h * 0.3f, w * 0.2f, h * 0.3f)
                            drawWindLine(canvas, w * 0.9f, h * 0.5f, w * 0.1f, h * 0.5f)
                            drawWindLine(canvas, w * 0.7f, h * 0.7f, w * 0.3f, h * 0.7f)
                        }
                        1 -> { // Right
                            drawWindLine(canvas, w * 0.2f, h * 0.3f, w * 0.8f, h * 0.3f)
                            drawWindLine(canvas, w * 0.1f, h * 0.5f, w * 0.9f, h * 0.5f)
                            drawWindLine(canvas, w * 0.3f, h * 0.7f, w * 0.7f, h * 0.7f)
                        }
                    }
                }

                R.id.spinnerWindIntensity -> {
                    paint.color = Color.parseColor("#8A2BE2")
                    paint.strokeWidth = 2.dpToPx().toFloat()
                    paint.style = Paint.Style.STROKE
                    paint.strokeCap = Paint.Cap.ROUND

                    val dirFactor = if (activeWindDir == 0) -1 else 1
                    when (optionIndex) {
                        0 -> {
                            // Sin viento: draw a small center circle
                            paint.style = Paint.Style.FILL
                            canvas.drawCircle(w * 0.5f, h * 0.5f, 4.dpToPx().toFloat(), paint)
                        }
                        1 -> {
                            // Lento: draw 1 wind line
                            if (dirFactor < 0) {
                                drawWindLine(canvas, w * 0.8f, h * 0.5f, w * 0.2f, h * 0.5f)
                            } else {
                                drawWindLine(canvas, w * 0.2f, h * 0.5f, w * 0.8f, h * 0.5f)
                            }
                        }
                        2 -> {
                            // Medio: draw 2 wind lines
                            if (dirFactor < 0) {
                                drawWindLine(canvas, w * 0.8f, h * 0.3f, w * 0.3f, h * 0.3f)
                                drawWindLine(canvas, w * 0.7f, h * 0.7f, w * 0.2f, h * 0.7f)
                            } else {
                                drawWindLine(canvas, w * 0.2f, h * 0.3f, w * 0.7f, h * 0.3f)
                                drawWindLine(canvas, w * 0.3f, h * 0.7f, w * 0.8f, h * 0.7f)
                            }
                        }
                        3 -> {
                            // Rápido: draw 3 wind lines
                            if (dirFactor < 0) {
                                drawWindLine(canvas, w * 0.8f, h * 0.3f, w * 0.2f, h * 0.3f)
                                drawWindLine(canvas, w * 0.9f, h * 0.5f, w * 0.1f, h * 0.5f)
                                drawWindLine(canvas, w * 0.7f, h * 0.7f, w * 0.3f, h * 0.7f)
                            } else {
                                drawWindLine(canvas, w * 0.2f, h * 0.3f, w * 0.8f, h * 0.3f)
                                drawWindLine(canvas, w * 0.1f, h * 0.5f, w * 0.9f, h * 0.5f)
                                drawWindLine(canvas, w * 0.3f, h * 0.7f, w * 0.7f, h * 0.7f)
                            }
                        }
                        4 -> {
                            // Muy rápido: draw 4 wind lines
                            if (dirFactor < 0) {
                                drawWindLine(canvas, w * 0.8f, h * 0.2f, w * 0.2f, h * 0.2f)
                                drawWindLine(canvas, w * 0.9f, h * 0.4f, w * 0.1f, h * 0.4f)
                                drawWindLine(canvas, w * 0.7f, h * 0.6f, w * 0.3f, h * 0.6f)
                                drawWindLine(canvas, w * 0.8f, h * 0.8f, w * 0.2f, h * 0.8f)
                            } else {
                                drawWindLine(canvas, w * 0.2f, h * 0.2f, w * 0.8f, h * 0.2f)
                                drawWindLine(canvas, w * 0.1f, h * 0.4f, w * 0.9f, h * 0.4f)
                                drawWindLine(canvas, w * 0.3f, h * 0.6f, w * 0.7f, h * 0.6f)
                                drawWindLine(canvas, w * 0.2f, h * 0.8f, w * 0.8f, h * 0.8f)
                            }
                        }
                    }
                }

                R.id.spinnerRainSpeed -> {
                    val speedCount = 12
                    drawRaindropsForSpeed(canvas, w, h, speedCount, activeRainColor, activeWindDir, optionIndex)
                }

                R.id.spinnerRainIntensity -> {
                    val density = when (optionIndex) {
                        0 -> 0
                        1 -> 4
                        2 -> 9
                        3 -> 16
                        4 -> 25
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
                        0 -> "#1E90FF"
                        1 -> "#FFFFFF"
                        2 -> "#FF3030"
                        3 -> "#00FF7F"
                        4 -> "#FFD700"
                        5 -> "#8A2BE2"
                        else -> "#1E90FF"
                    }
                    val optionColor = Color.parseColor(colorHex)
                    drawRaindrops(canvas, w, h, 12, optionColor, activeWindDir)
                }

                R.id.spinnerBgBrightness -> {
                    val bgColors = arrayOf("#0A0A0E", "#15151F", "#2A2A38", "#4A4A5F")
                    val colorHex = bgColors.getOrElse(optionIndex) { bgColors[2] }
                    paint.color = Color.parseColor(colorHex)
                    paint.style = Paint.Style.FILL
                    canvas.drawRoundRect(0f, 0f, w, h, 8.dpToPx().toFloat(), 8.dpToPx().toFloat(), paint)
                    drawLightningBolt(canvas, w, h, Color.parseColor("#FFE533"))
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
                        0 -> {
                            for (i in 0..4) {
                                val rx = w * 0.15f + w * 0.17f * i
                                canvas.drawLine(rx, 4.dpToPx().toFloat(), rx + dx, 24.dpToPx().toFloat(), paint)
                            }
                        }
                        1 -> {
                            for (i in 0..3) {
                                val rx = w * 0.25f + w * 0.16f * i
                                canvas.drawLine(rx, 15.dpToPx().toFloat(), rx + dx, 35.dpToPx().toFloat(), paint)
                            }
                        }
                        2 -> {
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
                        0 -> "#FFFFFF"
                        1 -> "#6699FF"
                        2 -> "#FFE533"
                        3 -> "#FF3333"
                        4 -> "#33FF33"
                        5 -> "#CC4DFF"
                        6 -> "#FFE533"
                        else -> "#FFFFFF"
                    }
                    val lColor = Color.parseColor(colorHex)
                    drawStylizedLightning(canvas, w, h, lColor, isRainbow = (optionIndex == 6))
                }

                R.id.spinnerCloudFlashColor -> {
                    val colorHex = when (optionIndex) {
                        0 -> "#FFFFFF"
                        1 -> "#6699FF"
                        2 -> "#FFE533"
                        3 -> "#FF3333"
                        4 -> "#33FF33"
                        5 -> "#CC4DFF"
                        6 -> "#FFE533"
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

                R.id.spinnerLightningSpawnMode -> {
                    when (optionIndex) {
                        0 -> {
                            paint.color = activeLightningColor
                            paint.style = Paint.Style.STROKE
                            paint.strokeWidth = 2.dpToPx().toFloat()
                            paint.strokeJoin = Paint.Join.ROUND
                            paint.strokeCap = Paint.Cap.ROUND
                            val lPath = Path().apply {
                                moveTo(w * 0.5f, 4.dpToPx().toFloat())
                                lineTo(w * 0.45f, h * 0.45f)
                                lineTo(w * 0.55f, h * 0.45f)
                                lineTo(w * 0.48f, h * 0.85f)
                            }
                            canvas.drawPath(lPath, paint)
                        }
                        1 -> {
                            drawCloudShape(canvas, w, h)
                            paint.color = activeLightningColor
                            paint.style = Paint.Style.STROKE
                            paint.strokeWidth = 1.5f.dpToPx().toFloat()
                            paint.strokeJoin = Paint.Join.ROUND
                            paint.strokeCap = Paint.Cap.ROUND
                            val lPath = Path().apply {
                                moveTo(w * 0.5f, 12.dpToPx().toFloat())
                                lineTo(w * 0.45f, h * 0.5f)
                                lineTo(w * 0.55f, h * 0.5f)
                                lineTo(w * 0.48f, h * 0.85f)
                            }
                            canvas.drawPath(lPath, paint)
                        }
                        2 -> {
                            drawCloudShape(canvas, w, h)
                            paint.color = activeLightningColor
                            paint.style = Paint.Style.STROKE
                            paint.strokeWidth = 1.5f.dpToPx().toFloat()
                            paint.strokeJoin = Paint.Join.ROUND
                            paint.strokeCap = Paint.Cap.ROUND
                            val path1 = Path().apply {
                                moveTo(w * 0.5f, 12.dpToPx().toFloat())
                                lineTo(w * 0.46f, h * 0.45f)
                                lineTo(w * 0.54f, h * 0.45f)
                                lineTo(w * 0.48f, h * 0.8f)
                            }
                            canvas.drawPath(path1, paint)
                            val path2 = Path().apply {
                                moveTo(w * 0.8f, 4.dpToPx().toFloat())
                                lineTo(w * 0.74f, h * 0.35f)
                                lineTo(w * 0.82f, h * 0.35f)
                                lineTo(w * 0.76f, h * 0.7f)
                            }
                            canvas.drawPath(path2, paint)
                        }
                    }
                }

                R.id.spinnerWindLinesIntensity -> {
                    val lineColor = Color.parseColor("#8A2BE2")
                    paint.style = Paint.Style.STROKE
                    paint.strokeCap = Paint.Cap.ROUND
                    if (optionIndex == 0) {
                        paint.color = ContextCompat.getColor(context, R.color.settings_text_secondary)
                        paint.strokeWidth = 1.dpToPx().toFloat()
                        canvas.drawCircle(w * 0.5f, h * 0.5f, 10.dpToPx().toFloat(), paint)
                    } else {
                        val alphas = floatArrayOf(0f, 0.35f, 0.55f, 0.8f, 1.0f)
                        val widths = floatArrayOf(1f, 1f, 1f, 1.5f, 2f)
                        paint.color = adjustAlpha(lineColor, alphas[optionIndex])
                        paint.strokeWidth = widths[optionIndex].dpToPx()
                        val ys = when (optionIndex) {
                            1 -> floatArrayOf(0.5f)
                            2 -> floatArrayOf(0.35f, 0.65f)
                            3 -> floatArrayOf(0.25f, 0.5f, 0.75f)
                            else -> floatArrayOf(0.15f, 0.35f, 0.55f, 0.75f, 0.9f)
                        }
                        for (yFrac in ys) {
                            canvas.drawLine(w * 0.85f, h * yFrac, w * 0.15f, h * yFrac, paint)
                        }
                    }
                }

                R.id.spinnerCloudDynamicsSpeed -> {
                    drawCloudShapeCentered(canvas, w, h)
                    val ringCount = intArrayOf(0, 1, 2, 3, 4).getOrElse(optionIndex) { 0 }
                    if (ringCount > 0) {
                        paint.style = Paint.Style.STROKE
                        paint.strokeWidth = 1.dpToPx().toFloat()
                        paint.color = adjustAlpha(ContextCompat.getColor(context, R.color.settings_text_primary), 0.4f)
                        val baseRadius = 11.dpToPx()
                        val gap = 3.5f.dpToPx()
                        for (i in 1..ringCount) {
                            canvas.drawCircle(w * 0.5f, h * 0.5f, baseRadius + gap * i, paint)
                        }
                    }
                }

                R.id.spinnerScreenDropletsSize -> {
                    val radii = floatArrayOf(2f, 3.5f, 5f, 7f, 9f)
                    val r = radii.getOrElse(optionIndex) { 5f }.dpToPx()
                    paint.style = Paint.Style.FILL
                    paint.color = adjustAlpha(activeRainColor, 0.55f)
                    canvas.drawCircle(w * 0.5f, h * 0.55f, r, paint)
                    paint.color = adjustAlpha(Color.WHITE, 0.8f)
                    canvas.drawCircle(w * 0.5f - r * 0.3f, h * 0.55f - r * 0.3f, (r * 0.25f).coerceAtLeast(1.dpToPx().toFloat()), paint)
                }

                R.id.spinnerLightningDuration -> {
                    val haloAlphas = floatArrayOf(0f, 0.15f, 0.30f, 0.45f, 0.60f)
                    val haloAlpha = haloAlphas.getOrElse(optionIndex) { 0f }
                    if (haloAlpha > 0f) {
                        val glowColor2 = adjustAlpha(activeLightningColor, haloAlpha)
                        val glowColors = intArrayOf(glowColor2, adjustAlpha(activeLightningColor, 0f))
                        val glowRadius2 = w * 0.45f
                        paint.shader = RadialGradient(w / 2f, h / 2f, glowRadius2, glowColors, null, Shader.TileMode.CLAMP)
                        paint.style = Paint.Style.FILL
                        canvas.drawCircle(w / 2f, h / 2f, glowRadius2, paint)
                        paint.shader = null
                    }
                    drawStylizedLightning(canvas, w, h, activeLightningColor, isRainbow = false)
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

        private fun drawRaindropsForSpeed(canvas: Canvas, w: Float, h: Float, count: Int, color: Int, windDir: Int, speedIndex: Int) {
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

            val lengthFactor = when (speedIndex) {
                0 -> 0.5f
                1 -> 1.0f
                2 -> 1.8f
                else -> 1.0f
            }

            for (i in 0 until count) {
                val rx = random.nextFloat() * (w + Math.abs(dx)) - (if (dx > 0) dx else 0f)
                val ry = random.nextFloat() * (h - 14.dpToPx())
                val length = (8.dpToPx() + random.nextFloat() * 8.dpToPx()) * lengthFactor
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
            paint.color = ContextCompat.getColor(context, R.color.settings_text_secondary)
            paint.style = Paint.Style.FILL
            
            canvas.drawCircle(w * 0.35f, 10.dpToPx().toFloat(), 8.dpToPx().toFloat(), paint)
            canvas.drawCircle(w * 0.65f, 10.dpToPx().toFloat(), 8.dpToPx().toFloat(), paint)
            canvas.drawCircle(w * 0.5f, 8.dpToPx().toFloat(), 10.dpToPx().toFloat(), paint)
            canvas.drawRect(w * 0.25f, 10.dpToPx().toFloat(), w * 0.75f, 16.dpToPx().toFloat(), paint)
        }

        private fun drawCloudShapeCentered(canvas: Canvas, w: Float, h: Float) {
            paint.color = ContextCompat.getColor(context, R.color.settings_text_primary)
            paint.style = Paint.Style.FILL
            
            val cy = h / 2f
            canvas.drawCircle(w * 0.35f, cy + 2.dpToPx(), 7.dpToPx().toFloat(), paint)
            canvas.drawCircle(w * 0.65f, cy + 2.dpToPx(), 7.dpToPx().toFloat(), paint)
            canvas.drawCircle(w * 0.5f, cy - 1.dpToPx(), 9.dpToPx().toFloat(), paint)
            canvas.drawRect(w * 0.3f, cy + 1.dpToPx(), w * 0.7f, cy + 9.dpToPx(), paint)
        }

        private fun drawStylizedClouds(canvas: Canvas, w: Float, h: Float, count: Int) {
            paint.color = ContextCompat.getColor(context, R.color.settings_text_secondary)
            paint.style = Paint.Style.FILL
            
            val random = java.util.Random(count * 100L)
            for (i in 0 until count.coerceAtMost(6)) {
                val cx = w * 0.2f + random.nextFloat() * w * 0.6f
                val cy = h * 0.25f + random.nextFloat() * h * 0.5f
                val r = 5.dpToPx().toFloat() + random.nextFloat() * 4.dpToPx()
                canvas.drawCircle(cx, cy, r, paint)
                canvas.drawCircle(cx - r * 0.6f, cy + r * 0.2f, r * 0.7f, paint)
                canvas.drawCircle(cx + r * 0.6f, cy + r * 0.2f, r * 0.7f, paint)
                canvas.drawRect(cx - r * 0.6f, cy + r * 0.2f, cx + r * 0.6f, cy + r * 0.9f, paint)
            }
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
