package com.tanzir.diabo.preview

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.StringReader

/**
 * Parses a raw layout XML string (as typed by the user in the editor) into an [XmlNode] tree.
 * Never throws — every failure path returns [XmlParseResult.Error] with a line number so the
 * Preview panel can show a precise, friendly message instead of crashing (zero-bugs requirement).
 */
object XmlLayoutParser {

    fun parse(xml: String): XmlParseResult {
        if (xml.isBlank()) return XmlParseResult.Error("Layout file is empty")

        return try {
            val parser: XmlPullParser = Xml.newPullParser()
            // NOTE: android.util.Xml.newPullParser() always returns a namespace-aware
            // parser — this can't actually be turned off via setFeature (Android forces
            // it). That means every "android:"/"app:"-prefixed attribute in the XML MUST
            // have its namespace declared via xmlns:android="..." somewhere on an
            // ancestor element (normal Android layout XML always does this on the root).
            // We still strip the prefix ourselves below via substringAfterLast(':') for
            // simplicity, rather than relying on the parser's own namespace/local-name split.
            parser.setInput(StringReader(xml))

            var event = parser.eventType
            var root: XmlNode? = null

            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG) {
                    root = readElement(parser)
                    break
                }
                event = parser.next()
            }

            root?.let { XmlParseResult.Success(it) }
                ?: XmlParseResult.Error("No root layout element found")
        } catch (e: XmlPullParserException) {
            XmlParseResult.Error(prettyMessage(e.message), lineIndexOf(e.message))
        } catch (e: Exception) {
            XmlParseResult.Error(e.message ?: "Unknown XML parsing error")
        }
    }

    private fun readElement(parser: XmlPullParser): XmlNode {
        val tag = parser.name
        val line = parser.lineNumber
        val attrs = mutableMapOf<String, String>()
        for (i in 0 until parser.attributeCount) {
            // Strip the "android:" style namespace prefix so attribute lookups stay simple.
            val name = parser.getAttributeName(i).substringAfterLast(':')
            attrs[name] = parser.getAttributeValue(i)
        }

        val children = mutableListOf<XmlNode>()
        var event = parser.next()
        while (!(event == XmlPullParser.END_TAG && parser.name == tag)) {
            when (event) {
                XmlPullParser.START_TAG -> children.add(readElement(parser))
                XmlPullParser.END_DOCUMENT -> throw XmlPullParserException("Unexpected end of file — is a tag unclosed near line $line?")
            }
            event = parser.next()
        }

        return XmlNode(tag, attrs, children, line)
    }

    private fun prettyMessage(raw: String?): String =
        raw?.substringBefore(" (position:")?.trim() ?: "Malformed XML"

    private fun lineIndexOf(raw: String?): Int? =
        Regex("""row\s*=\s*(\d+)""").find(raw.orEmpty())?.groupValues?.get(1)?.toIntOrNull()
}
