package edu.cuhk.csci3310.liftlog.data.repository

import edu.cuhk.csci3310.liftlog.data.remote.ExdbApi
import edu.cuhk.csci3310.liftlog.data.remote.model.Exercise

class ExerciseRepository(private val api: ExdbApi) {

    suspend fun searchExercises(
        query: String,
        offset: Int = 0,
        limit: Int = 20
    ): Result<List<Exercise>> {
        return try {
            val response = api.searchExercises(
                search = query,
                offset = offset,
                limit = limit,
            )
            if (response.success) Result.success(response.data)
            else Result.failure(Exception("unsuccessful response"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getExercisesByBodyPart(
        bodyPart: String,
        offset: Int = 0,
        limit: Int = 20
    ): Result<List<Exercise>> {
        return try {
            val response = api.getExercisesByBodyPart(
                bodyPart = bodyPart,
                offset = offset,
                limit = limit,
            )
            if (response.success) Result.success(response.data)
            else Result.failure(Exception("unsuccessful response"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getBodyParts(): Result<List<String>> {
        return try {
            val response = api.getBodyParts()
            if (response.success) Result.success(response.data.map { it.name })
            else Result.failure(Exception("unsuccessful response"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
