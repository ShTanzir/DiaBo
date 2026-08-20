package com.tanzir.diabo.preview

import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class XmlViewRendererTest {

    private lateinit var renderer: XmlViewRenderer

    // android.util.Xml.newPullParser() always parses namespace-aware (cannot be
    // turned off), so every "android:"-prefixed attribute needs this declared
    // somewhere on an ancestor — exactly like every real Android layout XML does.
    private val xmlns = """xmlns:android="http://schemas.android.com/apk/res/android""""

    @Before
    fun setUp() {
        renderer = XmlViewRenderer(ApplicationProvider.getApplicationContext())
    }

    @Test
    fun `renders a LinearLayout with a TextView child as real Android views`() {
        val xml = """
            <LinearLayout $xmlns android:orientation="vertical">
                <TextView android:text="Hello, DiaBo!" />
            </LinearLayout>
        """.trimIndent()

        val node = (XmlLayoutParser.parse(xml) as XmlParseResult.Success).root
        val result = renderer.render(node)

        assertTrue(result is ViewRenderResult.Success)
        val view = (result as ViewRenderResult.Success).view
        assertTrue(view is LinearLayout)
        assertEquals(LinearLayout.VERTICAL, (view as LinearLayout).orientation)
        assertEquals(1, view.childCount)
        assertTrue(view.getChildAt(0) is TextView)
        assertEquals("Hello, DiaBo!", (view.getChildAt(0) as TextView).text.toString())
    }

    @Test
    fun `unsupported tag returns a friendly Error instead of throwing`() {
        val xml = """<SomeRandomCustomView $xmlns android:text="x" />"""
        val node = (XmlLayoutParser.parse(xml) as XmlParseResult.Success).root
        val result = renderer.render(node)
        assertTrue(result is ViewRenderResult.Error)
    }

    @Test
    fun `ConstraintLayout renders as a labeled placeholder with a warning, not a crash`() {
        val xml = """<androidx.constraintlayout.widget.ConstraintLayout />"""
        val node = (XmlLayoutParser.parse(xml) as XmlParseResult.Success).root
        val result = renderer.render(node) as ViewRenderResult.Success
        assertTrue(result.warnings.isNotEmpty())
    }

    @Test
    fun `Button click fires the onViewClicked callback with the onClick method name`() {
        var clickedMethod: String? = null
        val clickRenderer = XmlViewRenderer(ApplicationProvider.getApplicationContext()) { method ->
            clickedMethod = method
        }
        val xml = """<Button $xmlns android:text="Go" android:onClick="onGoClick" />"""
        val node = (XmlLayoutParser.parse(xml) as XmlParseResult.Success).root
        val result = clickRenderer.render(node) as ViewRenderResult.Success

        (result.view as Button).performClick()
        assertEquals("onGoClick", clickedMethod)
    }

    @Test
    fun `id attribute is simplified from an @+id-style reference`() {
        val xml = """<TextView $xmlns android:id="@+id/myLabel" />"""
        val node = (XmlLayoutParser.parse(xml) as XmlParseResult.Success).root
        val result = renderer.render(node) as ViewRenderResult.Success
        val meta = result.view.tag as XmlViewRenderer.ViewMeta
        assertEquals("myLabel", meta.id)
    }
}
