package com.arabiyyah.app.data

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Data model representing a reading paragraph within a unit.
 */
@Serializable
data class Paragraph(
    @SerializedName("id")
    @SerialName("id")
    val id: Int,

    @SerializedName("title_ar")
    @SerialName("title_ar")
    val titleAr: String,

    @SerializedName("title_ru")
    @SerialName("title_ru")
    val titleRu: String,

    @SerializedName("text")
    @SerialName("text")
    val text: String
)
