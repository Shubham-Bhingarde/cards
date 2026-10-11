package com.shubham.cards

import com.shubham.cards.nfc.toHex
import org.junit.Assert.assertEquals
import org.junit.Test

class HexTest {

    @Test
    fun emptyArray_rendersNone() {
        assertEquals("(none)", ByteArray(0).toHex())
    }

    @Test
    fun bytes_renderUppercaseSpaceSeparated() {
        val bytes = byteArrayOf(0x04, 0xA3.toByte(), 0x00, 0xFF.toByte())
        assertEquals("04 A3 00 FF", bytes.toHex())
    }
}
