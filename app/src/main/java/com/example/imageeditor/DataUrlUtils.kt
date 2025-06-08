package com.example.imageeditor

object DataUrlUtils {
    fun parseMimeType(dataUrl: String): String? {
        if (!dataUrl.startsWith("data:") || !dataUrl.contains(";base64,")) {
            return null
        }
        val mimePartString = dataUrl.substringBefore(";base64,")
        val MimeParts = mimePartString.split(":")
        return if (MimeParts.size == 2 && MimeParts[0] == "data") {
            MimeParts[1].ifBlank { null } // Return null if mime type is blank
        } else {
            null
        }
    }
}
