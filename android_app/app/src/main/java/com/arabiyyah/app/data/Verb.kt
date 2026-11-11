package com.arabiyyah.app.data

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Data model representing an Arabic verb with past (ماضٍ), present (مضارع),
 * masdar (مصدر), Russian translation, and verb form pattern (وزن / باب).
 */
@Serializable
data class Verb(
    @SerializedName("past")
    @SerialName("past")
    val past: String,

    @SerializedName("present")
    @SerialName("present")
    val present: String,

    @SerializedName("masdar")
    @SerialName("masdar")
    val masdar: String,

    @SerializedName("meaning")
    @SerialName("meaning")
    val meaning: String,

    @SerializedName("form")
    @SerialName("form")
    val form: String
)
