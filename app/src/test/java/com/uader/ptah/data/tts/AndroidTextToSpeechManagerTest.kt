package com.uader.ptah.data.tts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidTextToSpeechManagerTest {
    @Test
    fun shortResponseRemainsInOneChunk() {
        assertEquals(
            listOf("Respuesta breve"),
            AndroidTextToSpeechManager.splitForEngine("  Respuesta breve  ", maxLength = 40)
        )
    }

    @Test
    fun longResponseIsSplitWithinEngineLimitWithoutLosingText() {
        val source = "0123456789".repeat(11)
        val chunks = AndroidTextToSpeechManager.splitForEngine(source, maxLength = 25)

        assertTrue(chunks.size > 1)
        assertTrue(chunks.all { it.length <= 25 })
        assertEquals(source, chunks.joinToString(separator = ""))
    }
}
