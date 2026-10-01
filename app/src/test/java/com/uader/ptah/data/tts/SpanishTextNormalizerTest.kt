package com.uader.ptah.data.tts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpanishTextNormalizerTest {

    @Test
    fun numberToSpanish_basicNumbers() {
        assertEquals("cero", SpanishTextNormalizer.numberToSpanish(0))
        assertEquals("cinco", SpanishTextNormalizer.numberToSpanish(5))
        assertEquals("quince", SpanishTextNormalizer.numberToSpanish(15))
        assertEquals("veintiuno", SpanishTextNormalizer.numberToSpanish(21))
        assertEquals("veinticuatro", SpanishTextNormalizer.numberToSpanish(24))
        assertEquals("treinta", SpanishTextNormalizer.numberToSpanish(30))
        assertEquals("cuarenta y dos", SpanishTextNormalizer.numberToSpanish(42))
        assertEquals("cien", SpanishTextNormalizer.numberToSpanish(100))
        assertEquals("ciento cinco", SpanishTextNormalizer.numberToSpanish(105))
        assertEquals("quinientos", SpanishTextNormalizer.numberToSpanish(500))
        assertEquals("mil", SpanishTextNormalizer.numberToSpanish(1000))
        assertEquals("dos mil veinticuatro", SpanishTextNormalizer.numberToSpanish(2024))
        assertEquals("un mill\u00F3n", SpanishTextNormalizer.numberToSpanish(1000000))
    }

    @Test
    fun normalize_handlesDecimalsAndPercentages() {
        val input = "El rendimiento aument\u00F3 un 15% y la versi\u00F3n es 3.5."
        val normalized = SpanishTextNormalizer.normalize(input)

        assertTrue(normalized.contains("quince por ciento"))
        assertTrue(normalized.contains("tres coma cinco"))
    }

    @Test
    fun normalize_removesMarkdownAndUrls() {
        val input = "Consulta la **documentaci\u00F3n** en https://uader.edu.ar o revisa `#inicio`."
        val normalized = SpanishTextNormalizer.normalize(input)

        assertFalse(normalized.contains("**"))
        assertFalse(normalized.contains("https://"))
        assertTrue(normalized.contains("documentaci\u00F3n"))
        assertTrue(normalized.contains("enlace"))
    }

    @Test
    fun normalize_expandsSpanishAbbreviations() {
        val input = "El Dr. P\u00E9rez viaj\u00F3 a 120 km/h aprox. seg\u00FAn la p\u00E1g. 4."
        val normalized = SpanishTextNormalizer.normalize(input)

        assertTrue(normalized.contains("doctor"))
        assertTrue(normalized.contains("kil\u00F3metros por hora"))
        assertTrue(normalized.contains("aproximadamente"))
        assertTrue(normalized.contains("p\u00E1gina"))
    }

    @Test
    fun splitIntoSentences_splitsByPunctuationAndLimitsChunkLength() {
        val text = "Esta es la primera oraci\u00F3n. \u00BFEsta es la segunda pregunta? \u00A1Y la tercera! Todo funciona correctamente."
        val sentences = SpanishTextNormalizer.splitIntoSentences(text, maxChunkLength = 100)

        assertTrue(sentences.size >= 4)
        assertTrue(sentences.all { it.length <= 100 })
    }
}
