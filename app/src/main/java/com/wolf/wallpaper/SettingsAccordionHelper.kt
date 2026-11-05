package com.wolf.wallpaper

import android.transition.AutoTransition
import android.transition.TransitionManager
import android.view.View
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.ScrollView
import androidx.core.widget.NestedScrollView

/**
 * Shared UI helpers for the settings screen fragments (Storm / Sunny), extracted so both
 * fragments use a single implementation instead of duplicated copies.
 */
object SettingsAccordionHelper {

    /** Identifies the views that make up a single top-level accordion section. */
    data class AccordionSpec(val headerId: Int, val contentId: Int, val dividerId: Int, val arrowId: Int)

    private class AccordionEntry(val header: View, val content: View, val divider: View?, val arrow: ImageView)

    /**
     * Wires up a group of top-level accordions living under [root]. Tapping a header
     * expands/collapses its own content and, when expanding, automatically collapses any other
     * accordion in the same group that was open (non-exclusive accordions become exclusive).
     *
     * When expanding a section, it automatically centers the section within the visible viewport
     * so it doesn't expand off-screen.
     */
    fun setupAccordionGroup(root: View, vararg specs: AccordionSpec) {
        val entries = specs.map { spec ->
            AccordionEntry(
                header = root.findViewById(spec.headerId),
                content = root.findViewById(spec.contentId),
                divider = root.findViewById(spec.dividerId),
                arrow = root.findViewById(spec.arrowId)
            )
        }

        specs.forEachIndexed { index, spec ->
            val entry = entries[index]
            entry.header.setOnClickListener {
                val isVisible = entry.content.visibility == View.VISIBLE
                if (isVisible) {
                    setExpanded(entry, false)
                } else {
                    entries.forEach { other ->
                        if (other !== entry && other.content.visibility == View.VISIBLE) {
                            setExpanded(other, false)
                        }
                    }
                    setExpanded(entry, true)
                    centerSectionOnScreen(entry.header)
                }
            }
        }
    }

    private fun setExpanded(entry: AccordionEntry, expand: Boolean) {
        val parentViewGroup = entry.content.parent as? ViewGroup
        if (parentViewGroup != null) {
            TransitionManager.beginDelayedTransition(parentViewGroup, AutoTransition().apply { duration = 250 })
        }
        entry.content.visibility = if (expand) View.VISIBLE else View.GONE
        entry.divider?.visibility = if (expand) View.VISIBLE else View.GONE
        entry.arrow.animate().rotation(if (expand) 180f else 0f).setDuration(250).start()
    }

    private fun centerSectionOnScreen(header: View) {
        val container = (header.parent?.parent as? View) ?: (header.parent as? View) ?: header
        val scrollParent = findScrollParent(container) ?: return

        val onLayoutListener = object : View.OnLayoutChangeListener {
            override fun onLayoutChange(
                v: View, left: Int, top: Int, right: Int, bottom: Int,
                oldLeft: Int, oldTop: Int, oldRight: Int, oldBottom: Int
            ) {
                v.removeOnLayoutChangeListener(this)
                v.post {
                    scrollToCenter(scrollParent, v)
                }
            }
        }
        container.addOnLayoutChangeListener(onLayoutListener)
        container.postDelayed({
            container.removeOnLayoutChangeListener(onLayoutListener)
            scrollToCenter(scrollParent, container)
        }, 150)
    }

    private fun findScrollParent(view: View): View? {
        var current: View? = view
        var activeScrollView: View? = null
        while (current != null) {
            if (current is NestedScrollView || current is ScrollView) {
                val child = (current as ViewGroup).getChildAt(0)
                if (child != null && child.height > current.height) {
                    return current
                }
                activeScrollView = current
            }
            val p = current.parent
            current = if (p is View) p else null
        }
        return activeScrollView
    }

    private fun getRelativeTop(target: View, parent: View): Int {
        var top = 0
        var current: View? = target
        while (current != null && current !== parent) {
            top += current.top
            val p = current.parent
            current = if (p is View) p else null
        }
        return top
    }

    private fun scrollToCenter(scrollContainer: View, target: View) {
        val scrollChild = (scrollContainer as? ViewGroup)?.getChildAt(0) ?: return
        val relativeTop = getRelativeTop(target, scrollChild)
        val targetHeight = target.height
        val viewportHeight = scrollContainer.height - scrollContainer.paddingTop - scrollContainer.paddingBottom
        if (viewportHeight <= 0) return

        val density = target.resources.displayMetrics.density
        val topMargin = (12f * density).toInt()

        val targetScrollY = if (targetHeight in 1 until viewportHeight) {
            relativeTop - (viewportHeight - targetHeight) / 2
        } else {
            relativeTop - topMargin
        }

        val maxScrollY = (scrollChild.height - scrollContainer.height).coerceAtLeast(0)
        val clampedScrollY = targetScrollY.coerceIn(0, maxScrollY)

        when (scrollContainer) {
            is NestedScrollView -> scrollContainer.smoothScrollTo(0, clampedScrollY)
            is ScrollView -> scrollContainer.smoothScrollTo(0, clampedScrollY)
        }
    }

    /** Centers [target] (a currently-selected chip/card) within [scrollView]'s viewport. */
    fun scrollToSelected(scrollView: HorizontalScrollView, target: View) {
        scrollView.post {
            scrollView.smoothScrollTo(
                (target.left - scrollView.width / 2 + target.width / 2).coerceAtLeast(0),
                0
            )
        }
    }
}
