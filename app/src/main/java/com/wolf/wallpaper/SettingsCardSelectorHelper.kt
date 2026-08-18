package com.wolf.wallpaper

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView

/**
 * Shared builder for the "row of 92dp cards" selector used by both settings fragments
 * (Storm / Sunny) in place of a Slider, extracted so both use a single implementation instead
 * of duplicated copies. Mirrors [SettingsAccordionHelper]'s role as a stateless, Context-aware
 * sibling helper.
 *
 * This only builds/wires the cards themselves - [SettingsAccordionHelper.scrollToSelected] is
 * reused as-is for centering the initially-selected card, and callers remain responsible for
 * anything fragment-specific (reading ConfigManager, calling updateSummaries, etc.) via the
 * [onSelected] callback.
 */
object SettingsCardSelectorHelper {

    /**
     * Clears [container] and repopulates it with one 92dp [MaterialCardView] per entry in
     * [options], each showing a small preview (from [previewFactory], if provided) above its
     * title. [selectedIndex] starts highlighted and centered in its scroll view (if any).
     *
     * [previewFrameBackgroundRes] lets a caller reuse an existing drawable (e.g. Sunny's
     * `R.drawable.mini_screen_bg`) for the preview frame's background instead of the default
     * programmatic [GradientDrawable] (Storm's existing look) used when left as 0.
     *
     * [previewFactory] returns `null` for a bare frame with no preview child (already the
     * existing behavior for a couple of Storm's frequency selectors).
     */
    fun populate(
        context: Context,
        container: LinearLayout,
        options: Array<String>,
        selectedIndex: Int,
        previewFactory: ((optionIndex: Int) -> View?)? = null,
        previewFrameBackgroundRes: Int = 0,
        onSelected: (optionIndex: Int) -> Unit
    ) {
        container.removeAllViews()
        val cardViews = ArrayList<MaterialCardView>(options.size)
        val titleViews = ArrayList<TextView>(options.size)

        val density = context.resources.displayMetrics.density
        fun Int.px() = Math.round(this * density)

        val activeColor = ContextCompat.getColor(context, R.color.settings_accent_text)
        val inactiveColor = ContextCompat.getColor(context, R.color.settings_text_primary)
        val activeBgColor = ContextCompat.getColor(context, R.color.settings_chip_bg_active)
        val inactiveBgColor = ContextCompat.getColor(context, R.color.settings_card_bg_alt)
        val strokeActiveColor = ContextCompat.getColor(context, R.color.settings_accent)
        val strokeInactiveColor = ContextCompat.getColor(context, R.color.settings_chip_bg_inactive)

        fun applySelection(index: Int) {
            cardViews.forEachIndexed { i, card ->
                val titleView = titleViews[i]
                if (i == index) {
                    card.strokeColor = strokeActiveColor
                    card.strokeWidth = 3.px()
                    card.setCardBackgroundColor(activeBgColor)
                    titleView.setTextColor(activeColor)
                } else {
                    card.strokeColor = strokeInactiveColor
                    card.strokeWidth = 1.px()
                    card.setCardBackgroundColor(inactiveBgColor)
                    titleView.setTextColor(inactiveColor)
                }
            }
        }

        for (i in options.indices) {
            val card = MaterialCardView(context).apply {
                layoutParams = LinearLayout.LayoutParams(92.px(), 92.px()).apply {
                    setMargins(0, 0, 10.px(), 0)
                }
                radius = 12.px().toFloat()
                strokeWidth = 1.px()
                strokeColor = strokeInactiveColor
                cardElevation = 0f
                setCardBackgroundColor(inactiveBgColor)
                isClickable = true
                isFocusable = true
            }

            val innerLayout = LinearLayout(context).apply {
                layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(6.px(), 6.px(), 6.px(), 6.px())
            }

            val previewFrame = FrameLayout(context).apply {
                layoutParams = LinearLayout.LayoutParams(38.px(), 50.px()).apply {
                    setMargins(0, 0, 0, 4.px())
                }
                background = if (previewFrameBackgroundRes != 0) {
                    ContextCompat.getDrawable(context, previewFrameBackgroundRes)
                } else {
                    GradientDrawable().apply {
                        shape = GradientDrawable.RECTANGLE
                        cornerRadius = 6 * density
                        setColor(ContextCompat.getColor(context, R.color.settings_card_bg_alt2))
                        setStroke(1.px(), strokeInactiveColor)
                    }
                }
            }

            previewFactory?.invoke(i)?.let { preview ->
                if (preview.layoutParams == null) {
                    preview.layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
                }
                previewFrame.addView(preview)
            }

            val titleView = TextView(context).apply {
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                text = options[i]
                setTextColor(if (i == selectedIndex) activeColor else inactiveColor)
                textSize = 10f
                gravity = Gravity.CENTER
                setTypeface(null, android.graphics.Typeface.BOLD)
            }

            innerLayout.addView(previewFrame)
            innerLayout.addView(titleView)
            card.addView(innerLayout)

            if (i == selectedIndex) {
                card.strokeColor = strokeActiveColor
                card.strokeWidth = 3.px()
                card.setCardBackgroundColor(activeBgColor)
            }

            card.setOnClickListener {
                onSelected(i)
                applySelection(i)
            }

            container.addView(card)
            cardViews.add(card)
            titleViews.add(titleView)
        }

        cardViews.getOrNull(selectedIndex)?.let { selectedCard ->
            (container.parent as? HorizontalScrollView)?.let { scrollView ->
                SettingsAccordionHelper.scrollToSelected(scrollView, selectedCard)
            }
        }
    }

    /** Index into [values] whose entry is numerically closest to [current] (ties favor the lower index). */
    fun closestValueIndex(values: IntArray, current: Int): Int =
        values.indices.minByOrNull { kotlin.math.abs(values[it] - current) } ?: 0
}
