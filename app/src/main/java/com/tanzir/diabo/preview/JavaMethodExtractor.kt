package com.tanzir.diabo.preview

/**
 * Finds `public void <methodName>(View v) { ... }`-style methods in raw Java source text
 * and extracts their body via simple brace counting. This is text scanning only — no
 * compilation, no reflection, no classloading — matching the "safe subset" simulation
 * boundary described in the PRD.
 */
object JavaMethodExtractor {

    fun extractMethodBody(javaSource: String, methodName: String): String? {
        val signatureRegex = Regex("""\b$methodName\s*\([^)]*\)\s*\{""")
        val match = signatureRegex.find(javaSource) ?: return null

        var depth = 0
        var bodyStart = -1
        var i = match.range.last // position of the opening '{'

        while (i < javaSource.length) {
            when (javaSource[i]) {
                '{' -> {
                    if (depth == 0) bodyStart = i + 1
                    depth++
                }
                '}' -> {
                    depth--
                    if (depth == 0 && bodyStart != -1) {
                        return javaSource.substring(bodyStart, i)
                    }
                }
            }
            i++
        }
        return null // unbalanced braces — caller should treat as "not simulated" rather than guess
    }
}
