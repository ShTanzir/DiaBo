package com.tanzir.diabo.preview

import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class JavaClickSimulatorTest {

    private val context: android.content.Context get() = ApplicationProvider.getApplicationContext()

    @Test
    fun `setText on a tagged TextView updates its text`() {
        val label = TextView(context).apply {
            tag = XmlViewRenderer.ViewMeta(id = "myLabel", onClickMethod = null)
        }
        val root = LinearLayout(context).apply { addView(label) }

        val outcome = JavaClickSimulator(root).simulate("""myLabel.setText("Updated!");""")

        assertEquals(1, outcome.handledLines)
        assertEquals("Updated!", label.text.toString())
    }

    @Test
    fun `setVisibility GONE hides the tagged view`() {
        val box = View(context).apply {
            tag = XmlViewRenderer.ViewMeta(id = "myBox", onClickMethod = null)
        }
        val root = LinearLayout(context).apply { addView(box) }

        JavaClickSimulator(root).simulate("myBox.setVisibility(View.GONE);")
        assertEquals(View.GONE, box.visibility)
    }

    @Test
    fun `unrecognized statements are reported as skipped, not silently dropped`() {
        val root = LinearLayout(context)
        val outcome = JavaClickSimulator(root).simulate("int x = complexCalculation(5, 10);")

        assertEquals(0, outcome.handledLines)
        assertEquals(1, outcome.skippedLines.size)
    }

    @Test
    fun `setText on an id that does not exist in the tree is reported as skipped`() {
        val root = LinearLayout(context)
        val outcome = JavaClickSimulator(root).simulate("""missingView.setText("x");""")

        assertEquals(0, outcome.handledLines)
        assertEquals(1, outcome.skippedLines.size)
    }

    @Test
    fun `multiple statements in one method body are each evaluated independently`() {
        val label = TextView(context).apply { tag = XmlViewRenderer.ViewMeta("label", null) }
        val root = LinearLayout(context).apply { addView(label) }

        val body = """
            label.setText("First");
            label.setText("Second");
        """.trimIndent()

        val outcome = JavaClickSimulator(root).simulate(body)
        assertEquals(2, outcome.handledLines)
        assertEquals("Second", label.text.toString()) // last statement wins, matches real execution order
    }
}
