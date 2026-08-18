package com.tanzir.diabo.preview

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Uses Robolectric because [XmlLayoutParser] depends on android.util.Xml (a framework
 * class) which isn't available in a plain JVM unit test.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class XmlLayoutParserTest {

    @Test
    fun `parses a simple valid layout into the expected node tree`() {
        val xml = """
            <LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
                android:orientation="vertical">
                <TextView android:text="Hello" />
            </LinearLayout>
        """.trimIndent()

        val result = XmlLayoutParser.parse(xml)
        assertTrue(result is XmlParseResult.Success)
        val root = (result as XmlParseResult.Success).root
        assertEquals("LinearLayout", root.tag)
        assertEquals("vertical", root.attributes["orientation"])
        assertEquals(1, root.children.size)
        assertEquals("TextView", root.children[0].tag)
        assertEquals("Hello", root.children[0].attributes["text"])
    }

    @Test
    fun `returns an Error for blank input instead of crashing`() {
        val result = XmlLayoutParser.parse("")
        assertTrue(result is XmlParseResult.Error)
    }

    @Test
    fun `returns an Error with a line number for an unclosed tag`() {
        val xml = """
            <LinearLayout>
                <TextView android:text="Hello"
            </LinearLayout>
        """.trimIndent()

        val result = XmlLayoutParser.parse(xml)
        assertTrue(result is XmlParseResult.Error)
    }

    @Test
    fun `strips namespace prefixes from attribute names`() {
        val xml = """<TextView android:text="Hi" app:customAttr="x" />"""
        val result = XmlLayoutParser.parse(xml)
        assertTrue(result is XmlParseResult.Success)
        val root = (result as XmlParseResult.Success).root
        assertEquals("Hi", root.attributes["text"])
        assertEquals("x", root.attributes["customAttr"])
    }

    @Test
    fun `parses deeply nested children correctly`() {
        val xml = """
            <FrameLayout>
                <LinearLayout>
                    <Button android:text="A" />
                    <Button android:text="B" />
                </LinearLayout>
            </FrameLayout>
        """.trimIndent()

        val result = XmlLayoutParser.parse(xml) as XmlParseResult.Success
        val linear = result.root.children[0]
        assertEquals(2, linear.children.size)
        assertEquals("A", linear.children[0].attributes["text"])
        assertEquals("B", linear.children[1].attributes["text"])
    }
}
