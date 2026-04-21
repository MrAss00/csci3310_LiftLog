package edu.cuhk.csci3310.liftlog.data.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import edu.cuhk.csci3310.liftlog.data.remote.model.BodyPart
import edu.cuhk.csci3310.liftlog.data.remote.model.Exercise

class ExerciseRepository(private val context: Context) {

    private val gson = Gson()

    // lazily load and cache all exercises from assets on first access
    private val allExercises: List<Exercise> by lazy {
        val json = context.assets.open("exercises.json").bufferedReader().use { it.readText() }
        val type = object : TypeToken<List<Exercise>>() {}.type
        gson.fromJson(json, type)
    }

    private val allBodyParts: List<String> by lazy {
        val json = context.assets.open("bodyparts.json").bufferedReader().use { it.readText() }
        val type = object : TypeToken<List<BodyPart>>() {}.type
        val parts: List<BodyPart> = gson.fromJson(json, type)
        parts.map { it.name }
    }

    fun searchExercises(
        query: String,
        offset: Int = 0,
        limit: Int = 20,
    ): Result<List<Exercise>> {
        return try {
            val filtered = if (query.isBlank()) {
                allExercises
            } else {
                allExercises.filter { it.name.contains(query, ignoreCase = true) }
            }
            Result.success(filtered.drop(offset).take(limit))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun listExercisesByBodyPart(
        bodyPart: String,
        offset: Int = 0,
        limit: Int = 20,
    ): Result<List<Exercise>> {
        return try {
            val filtered = allExercises.filter { exercise ->
                exercise.bodyParts.any { it.equals(bodyPart, ignoreCase = true) }
            }
            Result.success(filtered.drop(offset).take(limit))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getBodyParts(): Result<List<String>> {
        return try {
            Result.success(allBodyParts)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
