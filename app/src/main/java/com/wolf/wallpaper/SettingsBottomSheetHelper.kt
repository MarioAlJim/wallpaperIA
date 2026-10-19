package com.wolf.wallpaper

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.radiobutton.MaterialRadioButton

/**
 * Helper centralizado para desplegar Modal Bottom Sheets con estilo Material 3 / WhatsApp & Cash App.
 * Proporciona:
 * - Modal informativo y de ayuda contextual (Hero Badge circular, ítems con leading icon, CTA pill y botón de tutorial).
 * - Modal interactivo para selección y mezcla de color HSV con previsualización en vivo, presets y controles ergonómicos.
 * - Modal dinámico para selectores de color con Hero preview interactivo que reacciona en tiempo real al tocar cada tono.
 */
object SettingsBottomSheetHelper {

    private data class HelpFeatureItem(
        val iconRes: Int,
        val title: String,
        val description: String
    )

    /**
     * Crea y configura un BottomSheetDialog con esquinas superiores redondeadas a 28dp
     * y apertura en estado expandido completo.
     */
    fun createBottomSheetDialog(context: Context, layoutResId: Int): Pair<BottomSheetDialog, View> {
        val dialog = BottomSheetDialog(context, R.style.Theme_Wallpaper_BottomSheetDialog)
        val view = LayoutInflater.from(context).inflate(layoutResId, null)
        dialog.setContentView(view)

        dialog.setOnShowListener {
            val bottomSheet =
                dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            if (bottomSheet != null) {
                val behavior = BottomSheetBehavior.from(bottomSheet)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true
            }
        }

        return Pair(dialog, view)
    }

    /**
     * Muestra el modal informativo de ayuda estilo WhatsApp Android.
     */
    fun showHelpSheet(
        context: Context,
        isStorm: Boolean,
        onTutorialRequested: (() -> Unit)? = null
    ) {
        val (dialog, view) = createBottomSheetDialog(context, R.layout.bottom_sheet_help)

        val closeBtn = view.findViewById<ImageView>(R.id.btnHelpClose)
        val heroImage = view.findViewById<ImageView>(R.id.imageHelpHero)
        val titleView = view.findViewById<TextView>(R.id.textHelpTitle)
        val featuresContainer = view.findViewById<LinearLayout>(R.id.containerHelpFeatures)
        val dismissBtn = view.findViewById<MaterialButton>(R.id.btnHelpDismiss)
        val tutorialBtn = view.findViewById<MaterialButton>(R.id.btnHelpTutorial)

        val inflater = LayoutInflater.from(context)

        val features = if (isStorm) {
            heroImage.setImageResource(R.drawable.ic_storm)
            titleView.setText(R.string.storm_help_dialog_title)
            listOf(
                HelpFeatureItem(
                    R.drawable.ic_storm,
                    "Tormenta Variable",
                    "Ciclos automáticos que alternan calma y ráfagas. Bloquea controles manuales mientras ella misma modula los parámetros climáticos."
                ),
                HelpFeatureItem(
                    R.drawable.ic_sun,
                    "Brillo Global",
                    "Ajusta el nivel base de iluminación ambiental. Los relámpagos iluminan dinámicamente toda la escena."
                ),
                HelpFeatureItem(
                    R.drawable.ic_cloud,
                    "Nubes y Viento",
                    "Controla la densidad y desplazamiento de las nubes, así como la intensidad del viento que inclina lluvia y líneas de aire."
                ),
                HelpFeatureItem(
                    R.drawable.ic_rain,
                    "Lluvia y Gotas en Pantalla",
                    "Densidad, velocidad cromática y tamaño de las gotas simuladas con efecto lente sobre el cristal."
                ),
                HelpFeatureItem(
                    R.drawable.ic_lightning,
                    "Rayos y Relámpagos",
                    "Frecuencia, duración, tono de los arcos eléctricos, destellos internos en nubes y resplandor a pantalla completa."
                ),
                HelpFeatureItem(
                    R.drawable.ic_background,
                    "Lienzo de Fondo",
                    "Permite elegir entre paisaje predeterminado, fondo oscuro sólido o una fotografía personalizada."
                )
            )
        } else {
            heroImage.setImageResource(R.drawable.ic_sun)
            titleView.setText(R.string.sunny_help_dialog_title)
            listOf(
                HelpFeatureItem(
                    R.drawable.ic_clock,
                    "Modo de Tiempo",
                    "Alterna entre Día, Noche, Ciclo Combinado automático o Reloj en tiempo real según la hora del sistema."
                ),
                HelpFeatureItem(
                    R.drawable.ic_cloud,
                    "Ajuste de Entorno",
                    "Densidad de nubes diurnas, siluetas de fondo y efecto 3D Parallax mediante el giroscopio del móvil."
                ),
                HelpFeatureItem(
                    R.drawable.ic_sun,
                    "Configuración del Sol",
                    "Tamaño, pulsaciones, rayos de luz divinos (God Rays), destello de lente (Lens Flare) y degradado celeste."
                ),
                HelpFeatureItem(
                    R.drawable.ic_moon,
                    "Luna y Estrellas",
                    "Fases lunares con avance opcional, estrellas fugaces, luciérnagas titilantes y nebulosas espaciales."
                ),
                HelpFeatureItem(
                    R.drawable.ic_combined,
                    "Ciclo Combinado",
                    "Velocidad de transición entre sol y luna con trayectorias compartidas y sincronizadas."
                )
            )
        }

        featuresContainer.removeAllViews()
        features.forEach { item ->
            val itemView = inflater.inflate(R.layout.item_help_feature, featuresContainer, false)
            itemView.findViewById<ImageView>(R.id.imageFeatureIcon).setImageResource(item.iconRes)
            itemView.findViewById<TextView>(R.id.textFeatureTitle).text = item.title
            itemView.findViewById<TextView>(R.id.textFeatureDesc).text = item.description
            featuresContainer.addView(itemView)
        }

        closeBtn.setOnClickListener { dialog.dismiss() }
        dismissBtn.setOnClickListener { dialog.dismiss() }

        if (onTutorialRequested != null) {
            tutorialBtn.visibility = View.VISIBLE
            tutorialBtn.setOnClickListener {
                dialog.dismiss()
                onTutorialRequested()
            }
        } else {
            tutorialBtn.visibility = View.GONE
        }

        dialog.show()
    }

    /**
     * Muestra el modal interactivo de selección de color HSV estilo Cash App / Material 3.
     */
    fun showColorMixerSheet(
        context: Context,
        initialColorHex: Int,
        presets: List<Int>? = null,
        onColorSelected: (Int) -> Unit
    ) {
        val density = context.resources.displayMetrics.density
        fun Int.px() = Math.round(this * density)

        val (dialog, view) = createBottomSheetDialog(context, R.layout.bottom_sheet_color_mixer)

        val preview = view.findViewById<View>(R.id.colorMixerPreview)
        val hexBadge = view.findViewById<TextView>(R.id.textColorHexBadge)
        val presetsContainer = view.findViewById<LinearLayout>(R.id.colorMixerPresets)
        val hueSeekBar = view.findViewById<SeekBar>(R.id.colorMixerHue)
        val satSeekBar = view.findViewById<SeekBar>(R.id.colorMixerSaturation)
        val valSeekBar = view.findViewById<SeekBar>(R.id.colorMixerBrightness)
        val textHueValue = view.findViewById<TextView>(R.id.textHueValue)
        val textSatValue = view.findViewById<TextView>(R.id.textSatValue)
        val textValValue = view.findViewById<TextView>(R.id.textValValue)
        val closeBtn = view.findViewById<ImageView>(R.id.btnMixerClose)
        val cancelBtn = view.findViewById<MaterialButton>(R.id.btnMixerCancel)
        val applyBtn = view.findViewById<MaterialButton>(R.id.btnMixerApply)

        fun currentColorHex(): Int {
            val hsv = floatArrayOf(
                hueSeekBar.progress.toFloat(),
                satSeekBar.progress / 100f,
                valSeekBar.progress / 100f
            )
            return Color.HSVToColor(hsv) and 0xFFFFFF
        }

        fun updateUI() {
            val hex = currentColorHex()
            val colorWithAlpha = hex or 0xFF000000.toInt()

            (preview.background as? GradientDrawable
                ?: GradientDrawable().also { preview.background = it })
                .apply {
                    cornerRadius = 14f * density
                    setColor(colorWithAlpha)
                }

            hexBadge.text = String.format("#%06X", hex)

            textHueValue.text = "${hueSeekBar.progress}°"
            textSatValue.text = "${satSeekBar.progress}%"
            textValValue.text = "${valSeekBar.progress}%"
        }

        fun setHsv(hsv: FloatArray) {
            hueSeekBar.progress = hsv[0].toInt().coerceIn(0, 360)
            satSeekBar.progress = (hsv[1] * 100).toInt().coerceIn(0, 100)
            valSeekBar.progress = (hsv[2] * 100).toInt().coerceIn(0, 100)
            updateUI()
        }

        val initialHsv = FloatArray(3)
        Color.colorToHSV(initialColorHex or 0xFF000000.toInt(), initialHsv)
        setHsv(initialHsv)

        val seekListener = object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                updateUI()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        }
        hueSeekBar.setOnSeekBarChangeListener(seekListener)
        satSeekBar.setOnSeekBarChangeListener(seekListener)
        valSeekBar.setOnSeekBarChangeListener(seekListener)

        val defaultPresets = presets ?: listOf(
            0x00E5FF, 0x8E4FDB, 0x4F7FDB, 0xDB4FA9, 0x4FDBC4,
            0xDB914F, 0x6A3093, 0xE53935, 0x8E24AA, 0x00ACC1,
            0x43A047, 0xFFB300, 0xFFFFFF
        )

        presetsContainer.removeAllViews()
        defaultPresets.forEach { colorIntRaw ->
            val colorWithAlpha = colorIntRaw or 0xFF000000.toInt()
            val swatchCard = MaterialCardView(context).apply {
                layoutParams = LinearLayout.LayoutParams(44.px(), 44.px()).apply {
                    marginEnd = 12.px()
                }
                radius = 12f * density
                cardElevation = 0f
                strokeWidth = (1.5f * density).toInt()
                strokeColor = ContextCompat.getColor(context, R.color.settings_stroke)
                setCardBackgroundColor(colorWithAlpha)
                isClickable = true
                isFocusable = true
                foreground = RippleDrawable(
                    ColorStateList.valueOf(0x33FFFFFF),
                    null,
                    null
                )
                setOnClickListener {
                    val presetHsv = FloatArray(3)
                    Color.colorToHSV(colorWithAlpha, presetHsv)
                    setHsv(presetHsv)
                }
            }
            presetsContainer.addView(swatchCard)
        }

        closeBtn.setOnClickListener { dialog.dismiss() }
        cancelBtn.setOnClickListener { dialog.dismiss() }
        applyBtn.setOnClickListener {
            dialog.dismiss()
            onColorSelected(currentColorHex())
        }

        dialog.show()
    }

    enum class ElementType {
        RAIN,
        LIGHTNING,
        CLOUD_FLASH,
        STARS
    }

    data class DynamicColorOption(
        val name: String,
        val colorInt: Int,
        val isRainbow: Boolean = false
    )

    /**
     * Muestra un Modal Bottom Sheet para selección de color con un Hero Preview dinámico.
     * Al tocar cualquier color de la lista, el elemento (lluvia, relámpagos, destellos, estrellas)
     * cambia de color en tiempo real sin requerir tarjetas estáticas repetitivas.
     */
    fun showDynamicColorPickerSheet(
        context: Context,
        title: String,
        subtitle: String = "Selecciona un color para ver el efecto en tiempo real",
        elementType: ElementType,
        options: List<DynamicColorOption>,
        selectedIndex: Int,
        onCustomColorSelected: ((Int) -> Unit)? = null,
        onColorSelected: (Int) -> Unit
    ) {
        val (dialog, view) = createBottomSheetDialog(context, R.layout.bottom_sheet_dynamic_color_picker)

        val titleView = view.findViewById<TextView>(R.id.textBottomSheetTitle)
        val subtitleView = view.findViewById<TextView>(R.id.textBottomSheetSubtitle)
        val closeBtn = view.findViewById<ImageView>(R.id.btnBottomSheetClose)
        val previewHost = view.findViewById<FrameLayout>(R.id.dynamicPreviewContainer)
        val heroBadge = view.findViewById<TextView>(R.id.heroColorBadge)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerColorList)
        val rowCustomColor = view.findViewById<View>(R.id.rowCustomColorPicker)
        val cardCustomColorSwatch = view.findViewById<MaterialCardView>(R.id.cardCustomColorSwatch)
        val textCustomColorSubtitle = view.findViewById<TextView>(R.id.textCustomColorSubtitle)
        val applyBtn = view.findViewById<MaterialButton>(R.id.btnColorApply)

        titleView.text = title
        subtitleView.text = subtitle

        var currentSelected = selectedIndex.coerceIn(0, options.size - 1)
        val initialOption = options[currentSelected]

        // Crear e inyectar el canvas dinámico del elemento en el Hero Preview
        val previewView = DynamicElementPreviewView(
            context,
            elementType,
            initialOption.colorInt,
            initialOption.isRainbow
        )
        previewHost.removeAllViews()
        previewHost.addView(
            previewView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        fun updateBadge(option: DynamicColorOption) {
            heroBadge.text = option.name
            if (option.isRainbow) {
                heroBadge.backgroundTintList = ColorStateList.valueOf(0xCC7B1FA2.toInt())
            } else {
                heroBadge.backgroundTintList = ColorStateList.valueOf(
                    Color.argb(0xDD, Color.red(option.colorInt), Color.green(option.colorInt), Color.blue(option.colorInt))
                )
            }
        }

        updateBadge(initialOption)

        val adapter = object : RecyclerView.Adapter<ColorOptionViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColorOptionViewHolder {
                val itemView = LayoutInflater.from(parent.context).inflate(
                    R.layout.item_color_choice,
                    parent,
                    false
                )
                return ColorOptionViewHolder(itemView)
            }

            override fun onBindViewHolder(holder: ColorOptionViewHolder, position: Int) {
                val item = options[position]
                holder.nameView.text = item.name

                if (item.isRainbow) {
                    val rainbowGradient = GradientDrawable(
                        GradientDrawable.Orientation.LEFT_RIGHT,
                        intArrayOf(Color.RED, Color.YELLOW, Color.GREEN, Color.CYAN, Color.BLUE, Color.MAGENTA)
                    ).apply {
                        cornerRadius = 8 * context.resources.displayMetrics.density
                    }
                    holder.swatchView.background = rainbowGradient
                } else {
                    holder.swatchView.background = null
                    holder.swatchView.setBackgroundColor(item.colorInt)
                }

                holder.radioView.isChecked = (position == currentSelected)

                holder.itemView.setOnClickListener {
                    val prev = currentSelected
                    currentSelected = position
                    notifyItemChanged(prev)
                    notifyItemChanged(currentSelected)

                    // Cambiar el color del elemento en tiempo real
                    previewView.updateColor(item.colorInt, item.isRainbow)
                    updateBadge(item)

                    onColorSelected(position)
                }
            }

            override fun getItemCount(): Int = options.size
        }

        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter

        // Setup selector de color personalizado
        if (rowCustomColor != null) {
            val presets = options.filter { !it.isRainbow }.map { it.colorInt }
            rowCustomColor.setOnClickListener {
                val baseColor = options.getOrNull(currentSelected)?.takeIf { !it.isRainbow }?.colorInt
                    ?: 0x00E5FF

                showColorMixerSheet(context, baseColor, presets) { customColorHex ->
                    val customColorInt = customColorHex or 0xFF000000.toInt()
                    previewView.updateColor(customColorInt, false)
                    val customOpt = DynamicColorOption(
                        name = context.getString(R.string.color_custom),
                        colorInt = customColorInt,
                        isRainbow = false
                    )
                    updateBadge(customOpt)
                    cardCustomColorSwatch?.setCardBackgroundColor(customColorInt)
                    val hexStr = String.format("#%06X", customColorHex and 0xFFFFFF)
                    textCustomColorSubtitle?.text = hexStr

                    val prev = currentSelected
                    currentSelected = -1
                    if (prev >= 0) adapter.notifyItemChanged(prev)

                    if (onCustomColorSelected != null) {
                        onCustomColorSelected(customColorHex)
                    } else {
                        // Si no hay callback específico para custom hex, mapea al preset más cercano
                        val closestIndex = options.indices.minByOrNull { i ->
                            val c = options[i].colorInt
                            val dr = Color.red(c) - Color.red(customColorInt)
                            val dg = Color.green(c) - Color.green(customColorInt)
                            val db = Color.blue(c) - Color.blue(customColorInt)
                            dr * dr + dg * dg + db * db
                        } ?: 0
                        currentSelected = closestIndex
                        adapter.notifyItemChanged(closestIndex)
                        onColorSelected(closestIndex)
                    }
                }
            }
        }

        closeBtn.setOnClickListener { dialog.dismiss() }
        applyBtn.setOnClickListener { dialog.dismiss() }

        dialog.show()
    }

    private class ColorOptionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val swatchView: View = itemView.findViewById(R.id.viewColorSwatch)
        val nameView: TextView = itemView.findViewById(R.id.textColorName)
        val radioView: MaterialRadioButton = itemView.findViewById(R.id.radioSelected)
    }

    /**
     * Muestra un Modal Bottom Sheet para cualquier selector de opciones con Hero Preview reactivo en vivo.
     * Al tocar cualquier opción de la lista, el Hero Preview actualiza su renderizado dinámico en tiempo real.
     */
    fun showDynamicOptionPickerSheet(
        context: Context,
        title: String,
        subtitle: String = "Selecciona una opción para previsualizar en tiempo real",
        options: Array<String>,
        selectedIndex: Int,
        previewFactory: ((optionIndex: Int) -> View?)? = null,
        onOptionSelected: (Int) -> Unit
    ) {
        val (dialog, view) = createBottomSheetDialog(context, R.layout.bottom_sheet_dynamic_option_picker)

        val titleView = view.findViewById<TextView>(R.id.textOptionSheetTitle)
        val subtitleView = view.findViewById<TextView>(R.id.textOptionSheetSubtitle)
        val closeBtn = view.findViewById<ImageView>(R.id.btnOptionSheetClose)
        val heroCard = view.findViewById<MaterialCardView>(R.id.cardOptionHeroPreview)
        val heroContainer = view.findViewById<FrameLayout>(R.id.frameOptionHeroContainer)
        val heroBadge = view.findViewById<TextView>(R.id.badgeOptionHero)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerOptionSheetList)
        val applyBtn = view.findViewById<MaterialButton>(R.id.btnOptionSheetApply)

        titleView.text = title
        subtitleView.text = subtitle

        var currentSelected = selectedIndex.coerceIn(0, options.size - 1)

        fun updateHeroPreview(index: Int) {
            if (previewFactory != null) {
                heroCard.visibility = View.VISIBLE
                heroContainer.removeAllViews()
                val preview = previewFactory(index)
                if (preview != null) {
                    (preview.parent as? ViewGroup)?.removeView(preview)
                    heroContainer.addView(
                        preview,
                        FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.MATCH_PARENT
                        )
                    )
                }
                heroBadge.text = options.getOrNull(index) ?: ""
                heroBadge.visibility = View.VISIBLE
            } else {
                heroCard.visibility = View.GONE
            }
        }

        updateHeroPreview(currentSelected)

        val adapter = object : RecyclerView.Adapter<OptionChoiceViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OptionChoiceViewHolder {
                val itemView = LayoutInflater.from(parent.context).inflate(
                    R.layout.item_option_choice,
                    parent,
                    false
                )
                return OptionChoiceViewHolder(itemView)
            }

            override fun onBindViewHolder(holder: OptionChoiceViewHolder, position: Int) {
                holder.titleView.text = options[position]
                holder.radioView.isChecked = (position == currentSelected)

                if (previewFactory != null) {
                    holder.leadingCard.visibility = View.VISIBLE
                    holder.leadingFrame.removeAllViews()
                    val miniPreview = previewFactory(position)
                    if (miniPreview != null) {
                        (miniPreview.parent as? ViewGroup)?.removeView(miniPreview)
                        holder.leadingFrame.addView(
                            miniPreview,
                            FrameLayout.LayoutParams(
                                FrameLayout.LayoutParams.MATCH_PARENT,
                                FrameLayout.LayoutParams.MATCH_PARENT
                            )
                        )
                    }
                } else {
                    holder.leadingCard.visibility = View.GONE
                }

                holder.itemView.setOnClickListener {
                    val prev = currentSelected
                    currentSelected = position
                    notifyItemChanged(prev)
                    notifyItemChanged(currentSelected)

                    updateHeroPreview(position)
                    onOptionSelected(position)
                }
            }

            override fun getItemCount(): Int = options.size
        }

        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter

        closeBtn.setOnClickListener { dialog.dismiss() }
        applyBtn.setOnClickListener { dialog.dismiss() }

        dialog.show()
    }

    private class OptionChoiceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val leadingCard: MaterialCardView = itemView.findViewById(R.id.cardOptionLeading)
        val leadingFrame: FrameLayout = itemView.findViewById(R.id.frameOptionLeading)
        val titleView: TextView = itemView.findViewById(R.id.textOptionTitle)
        val subtitleView: TextView = itemView.findViewById(R.id.textOptionSubtitle)
        val radioView: MaterialRadioButton = itemView.findViewById(R.id.radioOptionSelected)
    }

    /**
     * Vista interactiva que dibuja el elemento en vivo (lluvia, rayos, destellos de nubes o estrellas)
     * respondiendo dinámicamente al cambio de color seleccionado por el usuario en el modal.
     */
    class DynamicElementPreviewView(
        context: Context,
        private val elementType: ElementType,
        private var activeColor: Int,
        private var isRainbow: Boolean = false
    ) : View(context) {

        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val density = context.resources.displayMetrics.density
        private fun Float.dp(): Float = this * density

        fun updateColor(color: Int, rainbow: Boolean = false) {
            this.activeColor = color
            this.isRainbow = rainbow
            invalidate()
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val w = width.toFloat()
            val h = height.toFloat()
            if (w <= 0 || h <= 0) return

            when (elementType) {
                ElementType.RAIN -> drawRain(canvas, w, h)
                ElementType.LIGHTNING -> drawLightning(canvas, w, h)
                ElementType.CLOUD_FLASH -> drawCloudFlash(canvas, w, h)
                ElementType.STARS -> drawStars(canvas, w, h)
            }
        }

        private fun drawRain(canvas: Canvas, w: Float, h: Float) {
            // Fondo atmosférico sutil
            paint.shader = LinearGradient(0f, 0f, 0f, h, 0xFF0D131A.toInt(), 0xFF141E28.toInt(), Shader.TileMode.CLAMP)
            paint.style = Paint.Style.FILL
            canvas.drawRect(0f, 0f, w, h, paint)
            paint.shader = null

            // Nube estilizada en la parte superior
            paint.color = 0x5537474F.toInt()
            paint.style = Paint.Style.FILL
            canvas.drawCircle(w * 0.35f, 15f.dp(), 22f.dp(), paint)
            canvas.drawCircle(w * 0.50f, 10f.dp(), 28f.dp(), paint)
            canvas.drawCircle(w * 0.65f, 16f.dp(), 24f.dp(), paint)

            // Gotas de lluvia dinámicas
            paint.style = Paint.Style.STROKE
            paint.strokeCap = Paint.Cap.ROUND

            val rainColors = if (isRainbow) {
                intArrayOf(Color.RED, Color.YELLOW, Color.GREEN, Color.CYAN, Color.BLUE, Color.MAGENTA)
            } else {
                intArrayOf(activeColor)
            }

            val random = java.util.Random(1337)
            val count = 26
            val dx = -6f.dp()

            for (i in 0 until count) {
                val rx = random.nextFloat() * (w - 30f.dp()) + 15f.dp()
                val ry = random.nextFloat() * (h - 45f.dp()) + 30f.dp()
                val length = random.nextFloat() * 14f.dp() + 10f.dp()
                val alpha = random.nextFloat() * 0.55f + 0.45f
                val c = rainColors[i % rainColors.size]

                paint.color = Color.argb(
                    (alpha * 255).toInt(),
                    Color.red(c),
                    Color.green(c),
                    Color.blue(c)
                )
                paint.strokeWidth = if (i % 3 == 0) 2.2f.dp() else 1.5f.dp()
                canvas.drawLine(rx, ry, rx + dx, ry + length, paint)
            }
        }

        private fun drawLightning(canvas: Canvas, w: Float, h: Float) {
            // Fondo atmosférico de tormenta profunda
            paint.shader = RadialGradient(
                w * 0.5f, h * 0.4f, w * 0.6f,
                if (isRainbow) 0x339C27B0.toInt() else Color.argb(0x33, Color.red(activeColor), Color.green(activeColor), Color.blue(activeColor)),
                0xFF080C12.toInt(),
                Shader.TileMode.CLAMP
            )
            paint.style = Paint.Style.FILL
            canvas.drawRect(0f, 0f, w, h, paint)
            paint.shader = null

            // Trazado del rayo principal
            val path = Path()
            path.moveTo(w * 0.54f, 10f.dp())
            path.lineTo(w * 0.46f, h * 0.35f)
            path.lineTo(w * 0.56f, h * 0.42f)
            path.lineTo(w * 0.42f, h * 0.70f)
            path.lineTo(w * 0.48f, h * 0.74f)
            path.lineTo(w * 0.36f, h * 0.95f)

            // Resplandor exterior (Halo)
            paint.style = Paint.Style.STROKE
            paint.strokeCap = Paint.Cap.ROUND
            paint.strokeJoin = Paint.Join.ROUND

            if (isRainbow) {
                paint.shader = LinearGradient(
                    0f, 0f, 0f, h,
                    intArrayOf(Color.RED, Color.YELLOW, Color.GREEN, Color.CYAN, Color.BLUE, Color.MAGENTA),
                    null, Shader.TileMode.CLAMP
                )
                paint.strokeWidth = 14f.dp()
                paint.alpha = 80
                canvas.drawPath(path, paint)

                paint.strokeWidth = 3.5f.dp()
                paint.alpha = 255
                canvas.drawPath(path, paint)
                paint.shader = null
            } else {
                paint.color = Color.argb(0x40, Color.red(activeColor), Color.green(activeColor), Color.blue(activeColor))
                paint.strokeWidth = 16f.dp()
                canvas.drawPath(path, paint)

                paint.color = Color.argb(0x80, Color.red(activeColor), Color.green(activeColor), Color.blue(activeColor))
                paint.strokeWidth = 8f.dp()
                canvas.drawPath(path, paint)

                // Núcleo blanco brillante con halo del color
                paint.color = Color.WHITE
                paint.strokeWidth = 2.5f.dp()
                canvas.drawPath(path, paint)
            }
        }

        private fun drawCloudFlash(canvas: Canvas, w: Float, h: Float) {
            // Fondo oscuro
            paint.color = 0xFF0D1117.toInt()
            paint.style = Paint.Style.FILL
            canvas.drawRect(0f, 0f, w, h, paint)

            // Resplandor radial interno
            val glowRadius = w * 0.40f
            val flashColor = if (isRainbow) Color.MAGENTA else activeColor
            paint.shader = RadialGradient(
                w * 0.5f, h * 0.5f, glowRadius,
                intArrayOf(
                    Color.argb(0xB0, Color.red(flashColor), Color.green(flashColor), Color.blue(flashColor)),
                    0x00000000
                ),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP
            )
            paint.style = Paint.Style.FILL
            canvas.drawCircle(w * 0.5f, h * 0.5f, glowRadius, paint)
            paint.shader = null

            // Nube estilizada en primer plano
            paint.color = 0xAA263238.toInt()
            paint.style = Paint.Style.FILL
            canvas.drawCircle(w * 0.40f, h * 0.52f, 26f.dp(), paint)
            canvas.drawCircle(w * 0.52f, h * 0.46f, 32f.dp(), paint)
            canvas.drawCircle(w * 0.64f, h * 0.54f, 24f.dp(), paint)
        }

        private fun drawStars(canvas: Canvas, w: Float, h: Float) {
            // Cielo nocturno profundo
            paint.shader = LinearGradient(0f, 0f, 0f, h, 0xFF060913.toInt(), 0xFF0F172A.toInt(), Shader.TileMode.CLAMP)
            paint.style = Paint.Style.FILL
            canvas.drawRect(0f, 0f, w, h, paint)
            paint.shader = null

            // Nebulosa suave con el color seleccionado
            paint.shader = RadialGradient(
                w * 0.55f, h * 0.45f, w * 0.45f,
                Color.argb(0x28, Color.red(activeColor), Color.green(activeColor), Color.blue(activeColor)),
                0x00000000,
                Shader.TileMode.CLAMP
            )
            paint.style = Paint.Style.FILL
            canvas.drawCircle(w * 0.55f, h * 0.45f, w * 0.45f, paint)
            paint.shader = null

            // Cúmulo de estrellas
            paint.style = Paint.Style.FILL
            val random = java.util.Random(999)
            val mixedColors = intArrayOf(Color.WHITE, 0xFF80D8FF.toInt(), 0xFFFFE082.toInt(), 0xFFF48FB1.toInt(), 0xFFA5D6A7.toInt())

            for (i in 0 until 38) {
                val rx = random.nextFloat() * (w - 24f.dp()) + 12f.dp()
                val ry = random.nextFloat() * (h - 24f.dp()) + 12f.dp()
                val radius = random.nextFloat() * 1.8f.dp() + 0.8f.dp()
                val c = if (isRainbow) mixedColors[i % mixedColors.size] else activeColor
                val alpha = random.nextFloat() * 0.6f + 0.4f

                paint.color = Color.argb((alpha * 255).toInt(), Color.red(c), Color.green(c), Color.blue(c))
                canvas.drawCircle(rx, ry, radius, paint)
            }
        }
    }
}
