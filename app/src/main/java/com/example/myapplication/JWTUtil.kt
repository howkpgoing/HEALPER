package com.example.myapplication

import android.util.Base64
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser

object JWTUtil {
    // Gets the JSON from a token segment.
    fun getJsonFromSegment(segment: String): String {
        val json = JsonParser().parse(decodeJWTPart(segment))
        val gson = GsonBuilder().setPrettyPrinting().create()
        return gson.toJson(json)
    }

    // Decodes the JWT segment to JSON object then return as string
    private fun decodeJWTPart(segment: String) : String {
        val cleanedBase64 = segment.replace("-", "+").replace("_", "/")
        val decodedBytes: ByteArray = Base64.decode(cleanedBase64, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        return String(decodedBytes)
    }
}