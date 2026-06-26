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
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configManager = (requireActivity() as WallpaperSettingsActivity).configManager

        // 1. Setup Accordions
        setupAccordion(view, R.id.headerCloudsWind, R.id.contentCloudsWind, R.id.dividerCloudsWind, R.id.arrowCloudsWind)
        setupAccordion(view, R.id.headerRain, R.id.contentRain, R.id.dividerRain, R.id.arrowRain)
        setupAccordion(view, R.id.headerLightning, R.id.contentLightning, R.id.dividerLightning, R.id.arrowLightning)
        setupAccordion(view, R.id.headerBackground, R.id.contentBackground, R.id.dividerBackground, R.id.arrowBackground)

        // 2. Setup Nubes y Viento Controls
        val cloudOptions = arrayOf("0 nubes", "1 nube", "3 nubes", "5 nubes", "7 nubes", "9 nubes", "11 nubes", "13 nubes", "15 nubes")
        val cloudCounts = intArrayOf(0, 1, 3, 5, 7, 9, 11, 13, 15)
        val initialCloudIndex = getClosestCloudOptionIndex(configManager.getCloudDensity())

        val textViewCloudDensityValue = view.findViewById<TextView>(R.id.textViewCloudDensityValue)
        textViewCloudDensityValue?.text = cloudOptions.getOrNull(initialCloudIndex) ?: "5 nubes"

        setupDropdown(view, R.id.spinnerCloudDensity, cloudOptions, initialCloudIndex) { position ->
            val count = cloudCounts[position]
            configManager.setCloudDensity(count)
            textViewCloudDensityValue?.text = cloudOptions[position]
            updateSummaries(view)
        }

        val windDirections = arrayOf("Izquierda", "Neutro", "Derecha")
        setupDropdown(view, R.id.spinnerWindDirection, windDirections, configManager.getWindDirection()) { position ->
            configManager.setWindDirection(position)
            refreshStormCardPreviews(view)
        }

        setupSlider(view, R.id.seekBarWindIntensity, R.id.textViewWindIntensityValue, configManager.getWindIntensity()) { value ->
            configManager.setWindIntensity(value)
        }

        setupSlider(view, R.id.seekBarCloudDynamicsSpeed, R.id.textViewCloudDynamicsSpeedValue, configManager.getCloudDynamicsSpeed()) { value ->
            configManager.setCloudDynamicsSpeed(value)
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

        setupSlider(view, R.id.seekBarWindLinesIntensity, R.id.textViewWindLinesIntensityValue, configManager.getWindLinesIntensity()) { value ->
            configManager.setWindLinesIntensity(value)
        }

        // 3. Setup Lluvia Controls
        setupSlider(view, R.id.seekBarRainSpeed, R.id.textViewRainSpeedValue, configManager.getRainSpeed()) { value ->
            configManager.setRainSpeed(value)
        }

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

        setupSlider(view, R.id.seekBarScreenDropletsSize, R.id.textViewScreenDropletsSizeValue, configManager.getScreenDropletsSize()) { value ->
            configManager.setScreenDropletsSize(value)
        }

        // 4. Setup Rayos Controls
        setupSlider(view, R.id.seekBarLightningFrequency, R.id.textViewLightningFrequencyValue, configManager.getLightningFrequency()) { value ->
            configManager.setLightningFrequency(value)
        }

        setupSlider(view, R.id.seekBarLightningDuration, R.id.textViewLightningDurationValue, configManager.getLightningDuration()) { value ->
            configManager.setLightningDuration(value)
        }

        // 4b. Setup Destellos en Nubes Controls
        setupSlider(view, R.id.seekBarCloudFlashFrequency, R.id.textViewCloudFlashFrequencyValue, configManager.getCloudFlashFrequency()) { value ->
            configManager.setCloudFlashFrequency(value)
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
        setupDropdown(view, R.id.spinnerBackgroundMode, backgroundModes, configManager.getBackgroundIndex()) { position ->
            configManager.setBackgroundIndex(position)
            updateCustomBackgroundViewsVisibility(view, position)
        }

        view.findViewById<Button>(R.id.btnSelectCustomBg)?.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        updateCustomBackgroundViewsVisibility(view, configManager.getBackgroundIndex())
        refreshStormCardPreviews(view)
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

            R.id.seekBarWindIntensity -> {
                val desc = when {
                    value <= 10 -> "Calma"
                    value <= 35 -> "Brisa"
                    value <= 65 -> "Moderado"
                    value <= 85 -> "Fuerte"
                    else -> "Vendaval"
                }
                textView.text = "$value% • $desc"
            }
            R.id.seekBarCloudDynamicsSpeed -> {
                val desc = when {
                    value <= 20 -> "Lento"
                    value <= 50 -> "Normal"
                    value <= 80 -> "Rápido"
                    else -> "Extremo"
                }
                textView.text = "$value% • $desc"
            }
            R.id.seekBarWindLinesIntensity -> {
                val desc = when {
                    value <= 20 -> "Sutil"
                    value <= 50 -> "Normal"
                    value <= 80 -> "Llamativo"
                    else -> "Muy denso"
                }
                textView.text = "$value% • $desc"
            }
            R.id.seekBarRainSpeed -> {
                val desc = when {
                    value <= 20 -> "Lenta"
                    value <= 50 -> "Normal"
                    value <= 80 -> "Rápida"
                    else -> "Torrencial"
                }
                textView.text = "$value% • $desc"
            }
            R.id.seekBarScreenDropletsSize -> {
                val desc = when {
                    value <= 50 -> "Finas"
                    value <= 100 -> "Medianas"
                    value <= 150 -> "Grandes"
                    else -> "Gigantes"
                }
                textView.text = "$value% • $desc"
            }
            R.id.seekBarLightningFrequency -> {
                val desc = when {
                    value <= 20 -> "Raro"
                    value <= 50 -> "Frecuente"
                    value <= 80 -> "Constante"
                    else -> "Tormenta eléctrica"
                }
                textView.text = "$value% • $desc"
            }
            R.id.seekBarLightningDuration -> {
                val desc = when {
                    value <= 20 -> "Instantáneo"
                    value <= 50 -> "Normal"
                    value <= 80 -> "Prolongado"
                    else -> "Destello persistente"
                }
                textView.text = "$value% • $desc"
            }
            R.id.seekBarCloudFlashFrequency -> {
                val desc = when {
                    value <= 10 -> "Desactivado"
                    value <= 40 -> "Ocasional"
                    value <= 70 -> "Frecuente"
                    else -> "Incesante"
                }
                textView.text = "$value% • $desc"
            }
        }
    }

    private fun getClosestCloudOptionIndex(density: Int): Int {
        val targets = intArrayOf(0, 1, 3, 5, 7, 9, 11, 13, 15)
        val actualClouds = if (density > 15) {
            when (density) {
                25 -> 2
                50 -> 5
                75 -> 10
                90 -> 13
                100 -> 15
                else -> (density / 100f * 15).toInt()
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

    private fun populateCardSelector(parent: View, container: LinearLayout, options: Array<String>, initialIndex: Int, onSelected: (Int) -> Unit) {
        container.removeAllViews()
        val cardViews = ArrayList<MaterialCardView>()

        val activeColor = Color.parseColor("#00E5FF")
        val inactiveColor = Color.parseColor("#E0E0E6")
        val activeBgColor = Color.parseColor("#2D2D3D")
        val inactiveBgColor = Color.parseColor("#21212A")
        val strokeActiveColor = Color.parseColor("#00E5FF")
        val strokeInactiveColor = Color.parseColor("#3A3A4A")

        for (i in options.indices) {
            val optionText = options[i]
            val card = MaterialCardView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(92.dpToPx(), 115.dpToPx()).apply {
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

            val innerLayout = LinearLayout(requireContext()).apply {
                layoutParams = android.widget.FrameLayout.LayoutParams(
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT
                )
                orientation = LinearLayout.VERTICAL
                gravity = android.view.Gravity.CENTER
                setPadding(8.dpToPx(), 8.dpToPx(), 8.dpToPx(), 8.dpToPx())
            }

            val previewFrame = android.widget.FrameLayout(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(48.dpToPx(), 64.dpToPx()).apply {
                    setMargins(0, 0, 0, 6.dpToPx())
                }
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = 6 * resources.displayMetrics.density
                    setColor(Color.parseColor("#15151A"))
                    setStroke((1 * resources.displayMetrics.density).toInt(), Color.parseColor("#3A3A4A"))
                }
            }

            val activeRainColorHex = when (configManager.getRainColorIndex()) {
                0 -> "#1E90FF"
                1 -> "#FFFFFF"
                2 -> "#FF3030"
                3 -> "#00FF7F"
                4 -> "#FFD700"
                5 -> "#8A2BE2"
                else -> "#1E90FF"
            }
            val activeRainColor = Color.parseColor(activeRainColorHex)

            val activeLightningColorHex = when (configManager.getLightningColorIndex()) {
                0 -> "#FFFFFF"
                1 -> "#6699FF"
                2 -> "#FFE533"
                3 -> "#FF3333"
                4 -> "#33FF33"
                5 -> "#CC4DFF"
                6 -> "#FFE533"
                else -> "#FFFFFF"
            }
            val activeLightningColor = Color.parseColor(activeLightningColorHex)
            val activeWindDir = configManager.getWindDirection()

            if (container.id != R.id.spinnerBackgroundMode) {
                val stormPreview = StormCardPreviewView(
                    requireContext(),
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
                val previewImage = ImageView(requireContext()).apply {
                    layoutParams = android.widget.FrameLayout.LayoutParams(
                        android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                        android.widget.FrameLayout.LayoutParams.MATCH_PARENT
                    )
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    clipToOutline = true
                }
                
                val bgPath = when (i) {
                    0 -> "black"
                    1 -> "background/background_01.png"
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
                    previewImage.setBackgroundColor(Color.parseColor("#08080C"))
                } else if (bgPath == "custom") {
                    val customBgFile = File(requireContext().filesDir, "custom_background.png")
                    if (customBgFile.exists()) {
                        val path = customBgFile.absolutePath
                        var bitmap = backgroundBitmapCache[path]
                        if (bitmap == null) {
                            bitmap = loadThumbnailFromAsset(requireContext(), path, 48.dpToPx(), 64.dpToPx())
                            if (bitmap != null) {
                                backgroundBitmapCache[path] = bitmap
                            }
                        }
                        previewImage.setImageBitmap(bitmap)
                    } else {
                        previewImage.setBackgroundColor(Color.parseColor("#1C1A24"))
                    }
                } else {
                    var bitmap = backgroundBitmapCache[bgPath]
                    if (bitmap == null) {
                        bitmap = loadThumbnailFromAsset(requireContext(), bgPath, 48.dpToPx(), 64.dpToPx())
                        if (bitmap != null) {
                            backgroundBitmapCache[bgPath] = bitmap
                        }
                    }
                    previewImage.setImageBitmap(bitmap)
                }
                previewFrame.addView(previewImage)
            }

            val titleText = TextView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                text = optionText
                setTextColor(if (i == initialIndex) activeColor else inactiveColor)
                textSize = 10f
                gravity = android.view.Gravity.CENTER
                setTypeface(null, android.graphics.Typeface.BOLD)
            }

            innerLayout.addView(previewFrame)
            innerLayout.addView(titleText)
            card.addView(innerLayout)

            if (i == initialIndex) {
                card.strokeColor = strokeActiveColor
                card.strokeWidth = (3 * resources.displayMetrics.density).toInt()
                card.setCardBackgroundColor(activeBgColor)
            }

            card.setOnClickListener {
                onSelected(i)
                updateSummaries(parent)
                
                for (j in cardViews.indices) {
                    val c = cardViews[j]
                    val childLayout = c.getChildAt(0) as LinearLayout
                    val titleTv = childLayout.getChildAt(1) as TextView
                    if (j == i) {
                        c.strokeColor = strokeActiveColor
                        c.strokeWidth = (3 * resources.displayMetrics.density).toInt()
                        c.setCardBackgroundColor(activeBgColor)
                        titleTv.setTextColor(activeColor)
                    } else {
                        c.strokeColor = strokeInactiveColor
                        c.strokeWidth = (1 * resources.displayMetrics.density).toInt()
                        c.setCardBackgroundColor(inactiveBgColor)
                        titleTv.setTextColor(inactiveColor)
                    }
                }
            }

            container.addView(card)
            cardViews.add(card)
        }
    }

    private fun refreshStormCardPreviews(view: View) {
        val rainIntensities = arrayOf("Nada (0%)", "Pocas (25%)", "Media (50%)", "Alta (75%)", "Muy alta (100%)")
        val initialRainValue = configManager.getRainIntensity()
        val initialRainProgress = (initialRainValue / 25).coerceIn(0, 4)
        setupDropdown(view, R.id.spinnerRainIntensity, rainIntensities, initialRainProgress) { position ->
            configManager.setRainIntensity(position * 25)
        }

        val rainColors = arrayOf("Azul", "Blanco", "Rojo", "Verde", "Amarillo", "Morado")
        setupDropdown(view, R.id.spinnerRainColor, rainColors, configManager.getRainColorIndex()) { position ->
            configManager.setRainColorIndex(position)
            refreshStormCardPreviews(view)
        }

        val rainSpawnModes = arrayOf("Borde Superior", "Debajo de las Nubes", "Todos Lados")
        setupDropdown(view, R.id.spinnerRainSpawnMode, rainSpawnModes, configManager.getRainSpawnMode()) { position ->
            configManager.setRainSpawnMode(position)
        }

        val lightningColors = arrayOf("Blanco", "Azul", "Amarillo", "Rojo", "Verde", "Morado", "Aleatorio")
        setupDropdown(view, R.id.spinnerLightningColor, lightningColors, configManager.getLightningColorIndex()) { position ->
            configManager.setLightningColorIndex(position)
            refreshStormCardPreviews(view)
        }

        setupDropdown(view, R.id.spinnerCloudFlashColor, lightningColors, configManager.getCloudFlashColorIndex()) { position ->
            configManager.setCloudFlashColorIndex(position)
        }
    }

    private fun saveCustomBackground(uri: android.net.Uri) {
        try {
            val inputStream = requireContext().contentResolver.openInputStream(uri) ?: return
            val outputFile = File(requireContext().filesDir, "custom_background.png")
            inputStream.use { input ->
                FileOutputStream(outputFile).use { output ->
                    input.copyTo(output)
                }
            }
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

        val accentColor = "#00E5FF"

        val density = configManager.getCloudDensity()
        val densityText = if (density <= 15) {
            when (density) {
                0 -> "Despejado"
                1 -> "1 nube"
                else -> "$density nubes"
            }
        } else {
            when {
                density <= 0 -> "Despejado"
                density <= 25 -> "Pocas nubes"
                density <= 50 -> "Nublado"
                density <= 75 -> "Muy nublado"
                else -> "Cielo Cubierto"
            }
        }
        val windDir = when (configManager.getWindDirection()) {
            0 -> "Izquierda"
            2 -> "Derecha"
            else -> "Calma"
        }
        val windSpeed = configManager.getWindIntensity()
        val densityLabel = if (density <= 15) densityText else "$densityText ($density%)"
        summaryCloudsWind.text = Html.fromHtml(
            "Nubes: <font color='$accentColor'>$densityLabel</font> • Viento: <font color='$accentColor'>$windDir (${windSpeed}%)</font>",
            Html.FROM_HTML_MODE_LEGACY
        )

        val rainIntensity = configManager.getRainIntensity()
        val rainText = when {
            rainIntensity <= 0 -> "Sin lluvia"
            rainIntensity <= 25 -> "Llovizna"
            rainIntensity <= 50 -> "Lluvia media"
            rainIntensity <= 75 -> "Lluvia fuerte"
            else -> "Torrencial"
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
        summaryRain.text = Html.fromHtml(
            "Intensidad: <font color='$accentColor'>$rainText ($rainIntensity%)</font> • Color: <font color='$accentColor'>$rainColorText</font>",
            Html.FROM_HTML_MODE_LEGACY
        )

        val lightningFreq = configManager.getLightningFrequency()
        val lightningText = when {
            lightningFreq <= 0 -> "Inactivo"
            lightningFreq <= 20 -> "Ocasional"
            lightningFreq <= 50 -> "Frecuente"
            lightningFreq <= 80 -> "Constante"
            else -> "Tormenta activa"
        }
        val flashStatus = if (configManager.isCloudFlashEnabled()) "Activo" else "Inactivo"
        summaryLightning.text = Html.fromHtml(
            "Frecuencia: <font color='$accentColor'>$lightningText ($lightningFreq%)</font> • Destellos: <font color='$accentColor'>$flashStatus</font>",
            Html.FROM_HTML_MODE_LEGACY
        )

        val bgIndex = configManager.getBackgroundIndex()
        val bgText = when (bgIndex) {
            0 -> "Color Oscuro (Sin Imagen)"
            1 -> "Montaña"
            2 -> "Valle"
            3 -> "Bosque"
            4 -> "Pico Rocoso"
            5 -> "Lago Nebloso"
            6 -> "Picos y Pinos"
            7 -> "Acantilado Costero"
            8 -> "Galería de Fotos"
            else -> "Color Oscuro"
        }
        summaryBackground.text = Html.fromHtml(
            "Fondo: <font color='$accentColor'>$bgText</font>",
            Html.FROM_HTML_MODE_LEGACY
        )
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
                R.id.spinnerCloudDensity -> {
                    val cloudCounts = intArrayOf(0, 1, 3, 5, 7, 9, 11, 13, 15)
                    val count = cloudCounts.getOrElse(optionIndex) { 0 }
                    if (count > 0) {
                        drawStylizedClouds(canvas, w, h, count)
                    } else {
                        paint.color = Color.parseColor("#444455")
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
                        0 -> {
                            drawWindLine(canvas, w * 0.8f, h * 0.3f, w * 0.2f, h * 0.3f)
                            drawWindLine(canvas, w * 0.9f, h * 0.5f, w * 0.1f, h * 0.5f)
                            drawWindLine(canvas, w * 0.7f, h * 0.7f, w * 0.3f, h * 0.7f)
                        }
                        1 -> {
                            drawWindLine(canvas, w * 0.3f, h * 0.2f, w * 0.3f, h * 0.8f)
                            drawWindLine(canvas, w * 0.5f, h * 0.1f, w * 0.5f, h * 0.9f)
                            drawWindLine(canvas, w * 0.7f, h * 0.2f, w * 0.7f, h * 0.8f)
                        }
                        2 -> {
                            drawWindLine(canvas, w * 0.2f, h * 0.3f, w * 0.8f, h * 0.3f)
                            drawWindLine(canvas, w * 0.1f, h * 0.5f, w * 0.9f, h * 0.5f)
                            drawWindLine(canvas, w * 0.3f, h * 0.7f, w * 0.7f, h * 0.7f)
                        }
                    }
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

        private fun drawStylizedClouds(canvas: Canvas, w: Float, h: Float, count: Int) {
            paint.color = Color.parseColor("#4A4A5A")
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
