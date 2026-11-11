package com.arabiyyah.app.data

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Data model representing a vocabulary word with singular form,
 * optional plural form, part of speech (pos), and Russian meaning.
 */
@Serializable
data class VocabWord(
    @SerializedName("word")
    @SerialName("word")
    val word: String,

    @SerializedName("plural")
    @SerialName("plural")
    val plural: String = "",

    @SerializedName("pos")
    @SerialName("pos")
    val pos: String = "",

    @SerializedName("meaning")
    @SerialName("meaning")
    val meaning: String = ""
)
