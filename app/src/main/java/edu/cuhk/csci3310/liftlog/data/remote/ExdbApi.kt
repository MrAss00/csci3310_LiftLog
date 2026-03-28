package edu.cuhk.csci3310.liftlog.data.remote

import edu.cuhk.csci3310.liftlog.data.remote.model.BodyPartsResponse
import edu.cuhk.csci3310.liftlog.data.remote.model.ExerciseResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface ExdbApi {

    @GET("exercises")
    suspend fun searchExercises(
        @Query("search") search: String = "",
        @Query("offset") offset: Int = 0,
        @Query("limit") limit: Int = 20
    ): ExerciseResponse

    @GET("exercises/bodyPart/{bodyPart}")
    suspend fun getExercisesByBodyPart(
        @Path("bodyPart") bodyPart: String,
        @Query("offset") offset: Int = 0,
        @Query("limit") limit: Int = 20
    ): ExerciseResponse

    @GET("bodyparts")
    suspend fun getBodyParts(): BodyPartsResponse
}
