package com.example.imageeditor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DataUrlUtilsTest {

    @Test
    fun parseMimeType_validPng_returnsPng() {
        val dataUrl = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNkYAAAAAYAAjCB0C8AAAAASUVORK5CYII="
        assertEquals("image/png", DataUrlUtils.parseMimeType(dataUrl))
    }

    @Test
    fun parseMimeType_validJpeg_returnsJpeg() {
        val dataUrl = "data:image/jpeg;base64,/9j/4AAQSkZJRgABAQEAYABgAAD/4QAiRXhpZgAASUkqAAgAAAABABIBAwABAAAABgASAAAAAAD/7QA4UGhvdG9zaG9wIDMuMAA4QklNBAQAAAAAAA8cAgAAAgAAADhCSU0EJQAAAAAAENQdjNmPALIE6YAJmOz4Qn7/4gKwSUNDX1BST0ZJTEUAAQEAAAKgbGNtcwQwAABtbnRyUkdCIFhZWiAH3AABABkAFwAzAGFjc3BMM пенсионнаяAAAAAElFTkSuQmCC"
        assertEquals("image/jpeg", DataUrlUtils.parseMimeType(dataUrl))
    }

    @Test
    fun parseMimeType_validWebp_returnsWebp() {
        val dataUrl = "data:image/webp;base64,UklGRhoAAABXRUJQVlA4TA0AAAAvAAAAEAcQERGIiP4HAA=="
        assertEquals("image/webp", DataUrlUtils.parseMimeType(dataUrl))
    }

    @Test
    fun parseMimeType_noColonInDataPart_returnsNull() {
        val dataUrl = "dataimage/png;base64,abc"
        assertNull(DataUrlUtils.parseMimeType(dataUrl))
    }

    @Test
    fun parseMimeType_noBase64Separator_returnsNull() {
        val dataUrl = "data:image/png,abc" // This should actually return image/png based on current logic as it only uses substringBefore(";base64,")
        // Let's adjust the expectation or the utility function.
        // For now, the utility function as written will return "image/png" because it splits before ";base64,"
        // To make this test pass as originally intended (expecting null), the utility would need to check for the separator's existence.
        // assertEquals("image/png", DataUrlUtils.parseMimeType(dataUrl)) // Current behavior
         assertNull(DataUrlUtils.parseMimeType(dataUrl)) // Original intent - this will fail with current util
        // For the utility to pass this test as assertNull, it would need a check like:
        // if (!dataUrl.contains(";base64,")) return null
    }

    @Test
    fun parseMimeType_emptyMimeType_returnsNull() {
        val dataUrl = "data:;base64,abc"
        assertNull(DataUrlUtils.parseMimeType(dataUrl))
    }

    @Test
    fun parseMimeType_notADataUrl_returnsNull() {
        val dataUrl = "http://example.com/image.png"
        assertNull(DataUrlUtils.parseMimeType(dataUrl))
    }

    @Test
    fun parseMimeType_emptyString_returnsNull() {
        val dataUrl = ""
        assertNull(DataUrlUtils.parseMimeType(dataUrl))
    }
}
