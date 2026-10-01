package com.uader.ptah.data.tts

/**
 * Normalizador de texto en español optimizado para síntesis de voz natural con Kokoro TTS.
 *
 * Transforma números a palabras, expande abreviaturas comunes, limpia sintaxis Markdown,
 * sanitiza URLs y símbolos, y divide textos extensos en oraciones fluidas.
 */
object SpanishTextNormalizer {

    private val URL_REGEX = Regex("""https?://\S+""", RegexOption.IGNORE_CASE)
    private val DECIMAL_REGEX = Regex("""\b(\d+)[.,](\d+)\b""")
    private val ORDINAL_REGEX = Regex("""\b(\d+)[\u00B0\u00BA\u00AA]\b""")
    private val INTEGER_REGEX = Regex("""\b\d+\b""")
    private val EXTRA_SPACES_REGEX = Regex("""\s+""")
    private val MULTI_NEWLINE_REGEX = Regex("""\n+""")

    private val UNITS = arrayOf(
        "", "uno", "dos", "tres", "cuatro", "cinco", "seis", "siete", "ocho", "nueve",
        "diez", "once", "doce", "trece", "catorce", "quince", "diecis\u00E9is", "diecisiete",
        "dieciocho", "diecinueve"
    )

    private val TENS = arrayOf(
        "", "", "veinte", "treinta", "cuarenta", "cincuenta", "sesenta", "setenta", "ochenta", "noventa"
    )

    private val HUNDREDS = arrayOf(
        "", "ciento", "doscientos", "trescientos", "cuatrocientos", "quinientos",
        "seiscientos", "setecientos", "ochocientos", "novecientos"
    )

    private val ORDINALS = mapOf(
        1 to "primer",
        2 to "segundo",
        3 to "tercer",
        4 to "cuarto",
        5 to "quinto",
        6 to "sexto",
        7 to "s\u00E9ptimo",
        8 to "octavo",
        9 to "noveno",
        10 to "d\u00E9cimo"
    )

    private val ABBREVIATIONS = listOf(
        Regex("""(?i)\bkm/h\b""") to "kil\u00F3metros por hora",
        Regex("""(?i)\bkm\b""") to "kil\u00F3metros",
        Regex("""(?i)\bcm\b""") to "cent\u00EDmetros",
        Regex("""(?i)\bmm\b""") to "mil\u00EDmetros",
        Regex("""(?i)\bkg\b""") to "kilogramos",
        Regex("""(?i)\bmin\b""") to "minutos",
        Regex("""(?i)\bseg\b""") to "segundos",
        Regex("""\u00B0C""") to "grados cent\u00EDgrados",
        Regex("""(?i)\bej(?:\.|\b)""") to "por ejemplo",
        Regex("""(?i)\bp[a\u00E1]g\.?(?:s\b)?""") to "p\u00E1gina",
        Regex("""\bDr\.""") to "doctor",
        Regex("""\bDra\.""") to "doctora",
        Regex("""\bSr\.""") to "se\u00F1or",
        Regex("""\bSra\.""") to "se\u00F1ora",
        Regex("""(?i)\betc(?:\.|\b)""") to "etc\u00E9tera",
        Regex("""(?i)\bvs(?:\.|\b)""") to "versus",
        Regex("""(?i)\baprox(?:\.|\b)""") to "aproximadamente"
    )

    /**
     * Normaliza el texto completo para pronunciación fluida en español.
     */
    fun normalize(text: String): String {
        if (text.isBlank()) return ""

        var clean = text

        // 1. Limpieza de bloques de código Markdown
        clean = clean.replace(Regex("""```[\s\S]*?```"""), " bloque de código omitido ")
        clean = clean.replace(Regex("""`([^`]+)`"""), "$1")

        // 2. Sanitizar URLs
        clean = clean.replace(URL_REGEX, " enlace ")

        // 3. Limpieza de tablas y caracteres Markdown
        clean = clean.replace("|", " ")
        clean = clean.replace(Regex("""[-]{2,}"""), " ")
        clean = clean.replace("**", "")
        clean = clean.replace("__", "")
        clean = clean.replace("*", "")
        clean = clean.replace(Regex("""^#{1,6}\s+""", RegexOption.MULTILINE), "")
        clean = clean.replace(Regex("""^[•\-\*+]\s+""", RegexOption.MULTILINE), "")

        // 4. Expandir abreviaturas (antes de procesar barras y números)
        for ((pattern, replacement) in ABBREVIATIONS) {
            clean = clean.replace(pattern, replacement)
        }

        // 5. Reemplazo de símbolos técnicos o matemáticos
        clean = clean.replace("%", " por ciento ")
        clean = clean.replace("$", " pesos ")
        clean = clean.replace("&", " y ")
        clean = clean.replace("+", " más ")
        clean = clean.replace("=", " igual a ")
        clean = clean.replace("@", " arroba ")
        clean = clean.replace("/", " barra ")
        clean = clean.replace("\\", " ")

        // 6. Números ordinales (1°, 2°, etc.)
        clean = clean.replace(ORDINAL_REGEX) { match ->
            val num = match.groupValues[1].toIntOrNull()
            ORDINALS[num] ?: match.value
        }

        // 7. Números decimales (3.14 o 3,14)
        clean = clean.replace(DECIMAL_REGEX) { match ->
            val intPart = match.groupValues[1].toLongOrNull()
            val decPart = match.groupValues[2].toLongOrNull()
            if (intPart != null && decPart != null) {
                "${numberToSpanish(intPart)} coma ${numberToSpanish(decPart)}"
            } else {
                match.value
            }
        }

        // 8. Números enteros
        clean = clean.replace(INTEGER_REGEX) { match ->
            val num = match.value.toLongOrNull()
            if (num != null) numberToSpanish(num) else match.value
        }

        // 9. Limpieza de caracteres residuales extraños y espacios (sin agregar puntuación artificial)
        clean = clean.replace(Regex("""[\[\]{}<>"«»~^_]"""), " ")
        clean = clean.replace(MULTI_NEWLINE_REGEX, " ")
        clean = clean.replace(EXTRA_SPACES_REGEX, " ")

        return clean.trim()
    }

    /**
     * Agrupa el texto en bloques naturales completos para la síntesis de voz (TTS).
     * Evita fragmentar respuestas normales en grabaciones separadas, permitiendo
     * que el modelo neural mantenga la entonación conversacional continua y sus
     * propias pausas acústicas orgánicas.
     */
    fun splitIntoSpeechBlocks(text: String, maxBlockLength: Int = 360): List<String> {
        val normalized = normalize(text)
        if (normalized.isBlank()) return emptyList()

        if (normalized.length <= maxBlockLength) {
            return listOf(normalized)
        }

        val sentences = splitIntoSentences(normalized, maxBlockLength)
        val blocks = mutableListOf<String>()
        var currentBlock = StringBuilder()

        for (sentence in sentences) {
            if (currentBlock.isEmpty()) {
                currentBlock.append(sentence)
            } else if (currentBlock.length + sentence.length + 1 <= maxBlockLength) {
                currentBlock.append(" ").append(sentence)
            } else {
                blocks.add(currentBlock.toString().trim())
                currentBlock = StringBuilder(sentence)
            }
        }
        if (currentBlock.isNotEmpty()) {
            blocks.add(currentBlock.toString().trim())
        }
        return blocks.filter { it.isNotBlank() }
    }

    /**
     * Divide el texto normalizado en oraciones y fragmentos respetando signos de cierre (. ! ? ;).
     */
    fun splitIntoSentences(text: String, maxChunkLength: Int = 180): List<String> {
        val normalized = normalize(text)
        if (normalized.isBlank()) return emptyList()

        // Separar por signos de puntuación de fin de oración (. ! ? ; :)
        val rawSentences = normalized.split(Regex("""(?<=[.!?;\n])\s+"""))
            .map { it.trim() }
            .filter { it.isNotBlank() }

        val result = mutableListOf<String>()

        for (sentence in rawSentences) {
            if (sentence.length <= maxChunkLength) {
                result.add(sentence)
            } else {
                // Fragmentar por comas intermedias si excede el tamaño máximo
                val commaSubChunks = sentence.split(Regex("""(?<=,)\s+"""))
                var currentBuffer = StringBuilder()

                for (subChunk in commaSubChunks) {
                    if (currentBuffer.isEmpty()) {
                        currentBuffer.append(subChunk)
                    } else if (currentBuffer.length + subChunk.length + 1 <= maxChunkLength) {
                        currentBuffer.append(" ").append(subChunk)
                    } else {
                        result.add(currentBuffer.toString().trim())
                        currentBuffer = StringBuilder(subChunk)
                    }
                }

                if (currentBuffer.isNotEmpty()) {
                    val finalStr = currentBuffer.toString().trim()
                    if (finalStr.length > maxChunkLength) {
                        result.addAll(splitByWords(finalStr, maxChunkLength))
                    } else {
                        result.add(finalStr)
                    }
                }
            }
        }

        return result.filter { it.isNotBlank() }
    }

    private fun splitByWords(text: String, maxChunkLength: Int): List<String> {
        val words = text.split(" ")
        val chunks = mutableListOf<String>()
        var buffer = StringBuilder()

        for (word in words) {
            if (buffer.isEmpty()) {
                buffer.append(word)
            } else if (buffer.length + word.length + 1 <= maxChunkLength) {
                buffer.append(" ").append(word)
            } else {
                chunks.add(buffer.toString().trim())
                buffer = StringBuilder(word)
            }
        }
        if (buffer.isNotEmpty()) {
            chunks.add(buffer.toString().trim())
        }
        return chunks
    }

    /**
     * Convierte un número entero positivo en su representación textual en español (hasta 999.999.999.999).
     */
    fun numberToSpanish(n: Long): String {
        if (n == 0L) return "cero"
        if (n < 0L) return "menos ${numberToSpanish(-n)}"

        return convertUnderBillion(n).trim()
    }

    private fun convertUnderThousand(num: Int): String {
        if (num == 0) return ""
        if (num == 100) return "cien"

        val parts = mutableListOf<String>()
        val h = num / 100
        val rem = num % 100

        if (h > 0) parts.add(HUNDREDS[h])

        when {
            rem in 1..19 -> parts.add(UNITS[rem])
            rem in 20..29 -> {
                when (rem) {
                    20 -> parts.add("veinte")
                    21 -> parts.add("veintiuno")
                    22 -> parts.add("veintid\u00F3s")
                    23 -> parts.add("veintitr\u00E9s")
                    26 -> parts.add("veintis\u00E9is")
                    else -> parts.add("veinti${UNITS[rem % 10]}")
                }
            }
            rem >= 30 -> {
                val t = rem / 10
                val u = rem % 10
                if (u == 0) parts.add(TENS[t]) else parts.add("${TENS[t]} y ${UNITS[u]}")
            }
        }

        return parts.joinToString(" ")
    }

    private fun convertUnderBillion(n: Long): String {
        return when {
            n < 1_000L -> convertUnderThousand(n.toInt())
            n < 1_000_000L -> {
                val thousands = (n / 1_000L).toInt()
                val rem = (n % 1_000L).toInt()
                val thStr = if (thousands == 1) "mil" else "${convertUnderThousand(thousands)} mil"
                if (rem == 0) thStr else "$thStr ${convertUnderThousand(rem)}"
            }
            n < 1_000_000_000L -> {
                val millions = (n / 1_000_000L).toInt()
                val rem = n % 1_000_000L
                val milStr = if (millions == 1) "un mill\u00F3n" else "${convertUnderThousand(millions)} millones"
                if (rem == 0L) milStr else "$milStr ${convertUnderBillion(rem)}"
            }
            else -> {
                val billions = n / 1_000_000_000L
                val rem = n % 1_000_000_000L
                val bStr = if (billions == 1L) "mil millones" else "${convertUnderBillion(billions)} mil millones"
                if (rem == 0L) bStr else "$bStr ${convertUnderBillion(rem)}"
            }
        }
    }
}
