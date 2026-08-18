package com.tanzir.diabo.preview

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.view.setPadding

sealed class ViewRenderResult {
    data class Success(val view: View, val warnings: List<String>) : ViewRenderResult()
    data class Error(val message: String, val lineNumber: Int? = null) : ViewRenderResult()
}

/**
 * Builds a real Android View tree from a parsed [XmlNode], WITHOUT going through
 * LayoutInflater/compiled resources — this is what makes Instant Preview possible
 * from a raw string the user is still typing.
 *
 * Supported tags per the PRD: LinearLayout, RelativeLayout, FrameLayout, ScrollView,
 * TextView, Button, ImageView, EditText, CheckBox, Switch, ProgressBar, View (divider).
 * ConstraintLayout / CardView / RecyclerView render as a labeled placeholder box —
 * full support needs their real layout engines, which is out of scope for an
 * instant/approximate preview (Real Build gives 100% accuracy for these).
 */
class XmlViewRenderer(
    private val context: Context,
    /** Called when a rendered view's `android:onClick="methodName"` fires in the preview. */
    private val onViewClicked: (methodName: String) -> Unit = {}
) {

    /** Tag attached to every rendered view so the click-simulator can resolve `id`-based references. */
    data class ViewMeta(val id: String?, val onClickMethod: String?)

    private val warnings = mutableListOf<String>()

    fun render(node: XmlNode): ViewRenderResult {
        warnings.clear()
        return try {
            val view = buildView(node, parent = null)
            ViewRenderResult.Success(view, warnings.toList())
        } catch (e: UnsupportedTagException) {
            ViewRenderResult.Error("Unsupported tag <${e.tag}> at line ${e.line}", e.line)
        } catch (e: Exception) {
            ViewRenderResult.Error(e.message ?: "Couldn't render this layout", node.lineNumber)
        }
    }

    private class UnsupportedTagException(val tag: String, val line: Int) : Exception()

    private fun buildView(node: XmlNode, parent: ViewGroup?): View {
        val simpleTag = node.tag.substringAfterLast('.')

        val view: View = when (simpleTag) {
            "LinearLayout" -> LinearLayout(context).apply {
                orientation = if (node.attributes["orientation"] == "horizontal")
                    LinearLayout.HORIZONTAL else LinearLayout.VERTICAL
                applyGravity(node.attributes["gravity"])
                node.children.forEach { addView(buildView(it, this)) }
            }
            "RelativeLayout" -> RelativeLayout(context).apply {
                node.children.forEach { addView(buildView(it, this)) }
            }
            "FrameLayout" -> FrameLayout(context).apply {
                node.children.forEach { addView(buildView(it, this)) }
            }
            "ScrollView" -> ScrollView(context).apply {
                node.children.firstOrNull()?.let { addView(buildView(it, this)) }
                if (node.children.size > 1) {
                    warnings.add("ScrollView at line ${node.lineNumber} only shows its first child in preview")
                }
            }
            "TextView" -> TextView(context).applyTextAttrs(node)
            "Button" -> Button(context).applyTextAttrs(node)
            "EditText" -> EditText(context).apply {
                hint = node.attributes["hint"]
                setText(node.attributes["text"])
                applyCommonTextSize(node)
            }
            "ImageView" -> ImageView(context).apply {
                // No resource table at instant-preview time, so custom drawables can't resolve —
                // render a neutral placeholder box instead of crashing.
                setBackgroundColor(Color.parseColor("#DDDDDD"))
                if (node.attributes.containsKey("src")) {
                    warnings.add("ImageView at line ${node.lineNumber}: custom image not shown in Instant Preview")
                }
            }
            "CheckBox" -> CheckBox(context).applyTextAttrs(node)
            "Switch" -> Switch(context).applyTextAttrs(node)
            "ProgressBar" -> ProgressBar(context)
            "View" -> View(context).apply {
                setBackgroundColor(parseColorOrNull(node.attributes["background"]) ?: Color.LTGRAY)
            }
            "androidx.constraintlayout.widget.ConstraintLayout", "ConstraintLayout",
            "androidx.cardview.widget.CardView", "CardView",
            "androidx.recyclerview.widget.RecyclerView", "RecyclerView" ->
                placeholderView(simpleTag, node)
            else -> throw UnsupportedTagException(simpleTag, node.lineNumber)
        }

        applyLayoutParams(view, node, parent)

        val onClickMethod = node.attributes["onClick"]
        view.tag = ViewMeta(id = simplifyIdRef(node.attributes["id"]), onClickMethod = onClickMethod)
        if (onClickMethod != null) {
            view.setOnClickListener { onViewClicked(onClickMethod) }
        }
        return view
    }

    private fun simplifyIdRef(raw: String?): String? =
        raw?.substringAfterLast('/')

    private fun placeholderView(tag: String, node: XmlNode): View {
        warnings.add("$tag at line ${node.lineNumber}: shown as a placeholder — use Real Build for an accurate preview")
        return TextView(context).apply {
            text = "[$tag — preview limited, use Real Build]"
            setBackgroundColor(Color.parseColor("#FFF3CD"))
            setTextColor(Color.parseColor("#664D03"))
            setPadding(24)
            gravity = Gravity.CENTER
        }
    }

    private fun <T : TextView> T.applyTextAttrs(node: XmlNode): T = apply {
        text = node.attributes["text"] ?: ""
        applyCommonTextSize(node)
        node.attributes["textColor"]?.let { c -> parseColorOrNull(c)?.let { setTextColor(it) } }
        applyGravity(node.attributes["gravity"])
    }

    private fun TextView.applyCommonTextSize(node: XmlNode) {
        node.attributes["textSize"]?.let { raw ->
            raw.removeSuffix("sp").toFloatOrNull()?.let { textSize = it }
        }
    }

    private fun View.applyGravity(raw: String?) {
        val g = when (raw) {
            "center" -> Gravity.CENTER
            "center_horizontal" -> Gravity.CENTER_HORIZONTAL
            "center_vertical" -> Gravity.CENTER_VERTICAL
            "left", "start" -> Gravity.START
            "right", "end" -> Gravity.END
            "top" -> Gravity.TOP
            "bottom" -> Gravity.BOTTOM
            else -> return
        }
        when (this) {
            is LinearLayout -> this.gravity = g
            is TextView -> this.gravity = g
        }
    }

    private fun applyLayoutParams(view: View, node: XmlNode, parent: ViewGroup?) {
        val widthAttr = node.attributes["layout_width"] ?: "wrap_content"
        val heightAttr = node.attributes["layout_height"] ?: "wrap_content"
        val width = resolveDimension(widthAttr)
        val height = resolveDimension(heightAttr)

        val params = when (parent) {
            is LinearLayout -> LinearLayout.LayoutParams(width, height).apply {
                node.attributes["layout_weight"]?.toFloatOrNull()?.let { weight = it }
            }
            is RelativeLayout -> RelativeLayout.LayoutParams(width, height)
            is FrameLayout -> FrameLayout.LayoutParams(width, height)
            else -> ViewGroup.LayoutParams(width, height)
        }
        view.layoutParams = params

        node.attributes["padding"]?.let { p -> dpToPxOrNull(p)?.let { view.setPadding(it) } }
    }

    private fun resolveDimension(raw: String): Int = when (raw) {
        "match_parent", "fill_parent" -> ViewGroup.LayoutParams.MATCH_PARENT
        "wrap_content" -> ViewGroup.LayoutParams.WRAP_CONTENT
        else -> dpToPxOrNull(raw) ?: ViewGroup.LayoutParams.WRAP_CONTENT
    }

    private fun dpToPxOrNull(raw: String): Int? {
        val value = raw.removeSuffix("dp").removeSuffix("dip").toFloatOrNull() ?: return null
        val density = context.resources.displayMetrics.density
        return (value * density).toInt()
    }

    private fun parseColorOrNull(raw: String?): Int? =
        if (raw == null) null else try { Color.parseColor(raw) } catch (e: IllegalArgumentException) { null }
}
