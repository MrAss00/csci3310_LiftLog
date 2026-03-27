package edu.cuhk.csci3310.liftlog.data.remote.model

import com.google.gson.annotations.SerializedName

data class ExerciseResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("metadata") val metadata: ExerciseMetadata?,
    @SerializedName("data") val data: List<ExerciseDto>
)

data class ExerciseMetadata(
    @SerializedName("totalPages") val totalPages: Int,
    @SerializedName("totalExercises") val totalExercises: Int,
    @SerializedName("currentPage") val currentPage: Int
)

data class ExerciseDto(
    @SerializedName("exerciseId") val exerciseId: String,
    @SerializedName("name") val name: String,
    @SerializedName("gifUrl") val gifUrl: String,
    @SerializedName("targetMuscles") val targetMuscles: List<String>,
    @SerializedName("bodyParts") val bodyParts: List<String>,
    @SerializedName("equipments") val equipments: List<String>,
    @SerializedName("secondaryMuscles") val secondaryMuscles: List<String>,
    @SerializedName("instructions") val instructions: List<String>
)

data class BodyPartsResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: List<BodyPartDto>
)

data class BodyPartDto(
    @SerializedName("name") val name: String
)
