package com.arabiyyah.app.data

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Data model representing an Arabic collocation, idiom, or fixed expression.
 */
@Serializable
data class Collocation(
    @SerializedName("phrase")
    @SerialName("phrase")
    val phrase: String,

    @SerializedName("meaning")
    @SerialName("meaning")
    val meaning: String,

    @SerializedName("note")
    @SerialName("note")
    val note: String = ""
)
