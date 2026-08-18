package com.tanzir.diabo.preview

/**
 * Intermediate parse tree for a layout XML file, built BEFORE any Android View is touched.
 * Keeping parse and render as separate steps means a malformed-XML error can point at an
 * exact line number, and the renderer never has to guess whether a failure was a parse
 * problem or a View-construction problem.
 */
data class XmlNode(
    val tag: String,
    val attributes: Map<String, String>,
    val children: List<XmlNode>,
    val lineNumber: Int
)

sealed class XmlParseResult {
    data class Success(val root: XmlNode) : XmlParseResult()
    data class Error(val message: String, val lineNumber: Int? = null) : XmlParseResult()
}
