package com.tanzir.diabo.preview

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast

/**
 * Simulates a SAFE, tiny subset of Java `onClick` handler bodies against the rendered
 * preview View tree — NOT a JVM, NOT arbitrary code execution. Anything outside the
 * supported patterns below is reported as "not simulated" rather than silently ignored
 * or (worse) attempting to actually execute arbitrary user Java in-process.
 *
 * Supported per PRD §8.1:
 *   - Toast.makeText(this, "...", Toast.LENGTH_SHORT).show();
 *   - <viewId>.setText("...");
 *   - <viewId>.setVisibility(View.VISIBLE / View.GONE / View.INVISIBLE);
 *
 * Matching is done with simple line-based regex against the method body text —
 * there is no reflection, no classloading, no dynamic code execution of any kind.
 */
class JavaClickSimulator(private val rootView: View) {

    data class SimulationOutcome(val handledLines: Int, val skippedLines: List<String>)

    private val toastRegex = Regex("""Toast\.makeText\([^,]+,\s*"([^"]*)"\s*,\s*Toast\.\w+\)\.show\(\);?""")
    private val setTextRegex = Regex("""(\w+)\.setText\(\s*"([^"]*)"\s*\);?""")
    private val setVisibilityRegex = Regex("""(\w+)\.setVisibility\(\s*View\.(VISIBLE|GONE|INVISIBLE)\s*\);?""")

    /** Runs every statement found in [methodBody] against the preview tree; never throws. */
    fun simulate(methodBody: String): SimulationOutcome {
        var handled = 0
        val skipped = mutableListOf<String>()

        methodBody.lines().map { it.trim() }.filter { it.isNotBlank() }.forEach { line ->
            when {
                toastRegex.matches(line) -> {
                    val msg = toastRegex.find(line)!!.groupValues[1]
                    Toast.makeText(rootView.context, msg, Toast.LENGTH_SHORT).show()
                    handled++
                }
                setTextRegex.matches(line) -> {
                    val (id, text) = setTextRegex.find(line)!!.destructured
                    (findViewByTag(id) as? TextView)?.let { it.text = text; handled++ }
                        ?: skipped.add(line)
                }
                setVisibilityRegex.matches(line) -> {
                    val (id, vis) = setVisibilityRegex.find(line)!!.destructured
                    findViewByTag(id)?.let {
                        it.visibility = when (vis) {
                            "GONE" -> View.GONE
                            "INVISIBLE" -> View.INVISIBLE
                            else -> View.VISIBLE
                        }
                        handled++
                    } ?: skipped.add(line)
                }
                else -> skipped.add(line)
            }
        }
        return SimulationOutcome(handled, skipped)
    }

    /** Views are tagged with a ViewMeta(id, onClickMethod) at render time (see XmlViewRenderer). */
    private fun findViewByTag(id: String): View? {
        fun search(v: View): View? {
            if ((v.tag as? XmlViewRenderer.ViewMeta)?.id == id) return v
            if (v is ViewGroup) {
                for (i in 0 until v.childCount) {
                    search(v.getChildAt(i))?.let { return it }
                }
            }
            return null
        }
        return search(rootView)
    }
}
