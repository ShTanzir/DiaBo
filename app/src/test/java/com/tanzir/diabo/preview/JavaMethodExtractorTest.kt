package com.tanzir.diabo.preview

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class JavaMethodExtractorTest {

    @Test
    fun `extracts a simple single-line method body`() {
        val source = """
            public class MainActivity {
                public void onLoginClick(View v) {
                    Toast.makeText(this, "Hi", Toast.LENGTH_SHORT).show();
                }
            }
        """.trimIndent()

        val body = JavaMethodExtractor.extractMethodBody(source, "onLoginClick")
        assertEquals(true, body?.contains("Toast.makeText"))
    }

    @Test
    fun `handles nested braces inside the method body correctly`() {
        val source = """
            public void onClick(View v) {
                if (v != null) {
                    myTextView.setText("Clicked");
                }
            }
        """.trimIndent()

        val body = JavaMethodExtractor.extractMethodBody(source, "onClick")
        assertEquals(true, body?.contains("myTextView.setText"))
        // Body must NOT include the closing brace of the outer method itself
        assertEquals(false, body?.trim()?.endsWith("}\n}"))
    }

    @Test
    fun `returns null when method name is not found`() {
        val source = "public void somethingElse() { }"
        val body = JavaMethodExtractor.extractMethodBody(source, "onLoginClick")
        assertNull(body)
    }

    @Test
    fun `returns null on unbalanced braces instead of guessing`() {
        val source = "public void onClick(View v) { Toast.makeText(this, \"x\", 0).show();"
        val body = JavaMethodExtractor.extractMethodBody(source, "onClick")
        assertNull(body)
    }

    @Test
    fun `does not match a substring of another method name`() {
        val source = """
            public void onClickAlt(View v) { doSomething(); }
            public void onClick(View v) { doOther(); }
        """.trimIndent()

        val body = JavaMethodExtractor.extractMethodBody(source, "onClick")
        // Regex uses \b word boundary, so "onClickAlt" must not match a lookup for "onClick"
        assertEquals(true, body?.contains("doOther"))
        assertEquals(false, body?.contains("doSomething") ?: false)
    }
}
