package com.arabiyyah.app.data

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Data model representing an Arabic learning unit.
 * Contains paragraphs for reading, verbs with conjugations,
 * collocations (idioms/expressions), and vocabulary items.
 */
@Serializable
data class UnitData(
    @SerializedName("unit_num")
    @SerialName("unit_num")
    val unitNum: Int,

    @SerializedName("lesson_num")
    @SerialName("lesson_num")
    val lessonNum: Int,

    @SerializedName("title_ar")
    @SerialName("title_ar")
    val titleAr: String,

    @SerializedName("title_ru")
    @SerialName("title_ru")
    val titleRu: String,

    @SerializedName("desc_ru")
    @SerialName("desc_ru")
    val descRu: String,

    @SerializedName("icon")
    @SerialName("icon")
    val icon: String = "",

    @SerializedName("paragraphs")
    @SerialName("paragraphs")
    val paragraphs: List<Paragraph> = emptyList(),

    @SerializedName("verbs")
    @SerialName("verbs")
    val verbs: List<Verb> = emptyList(),

    @SerializedName("collocations")
    @SerialName("collocations")
    val collocations: List<Collocation> = emptyList(),

    @SerializedName("vocabulary")
    @SerialName("vocabulary")
    val vocabulary: List<VocabWord> = emptyList()
)
