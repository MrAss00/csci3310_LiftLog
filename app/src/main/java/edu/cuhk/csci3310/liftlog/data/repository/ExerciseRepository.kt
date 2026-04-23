package edu.cuhk.csci3310.liftlog.data.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import edu.cuhk.csci3310.liftlog.data.remote.RetrofitInstance
import edu.cuhk.csci3310.liftlog.data.remote.model.BodyPart
import edu.cuhk.csci3310.liftlog.data.remote.model.Exercise

class ExerciseRepository(private val context: Context) {

    private val gson = Gson()

    // ── local (asset-backed) ─────────────────────────────────────────────────

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

    private fun searchLocal(query: String, offset: Int, limit: Int): Result<List<Exercise>> =
        runCatching {
            val filtered = if (query.isBlank()) allExercises
            else allExercises.filter { it.name.contains(query, ignoreCase = true) }
            filtered.drop(offset).take(limit)
        }

    private fun listByBodyPartLocal(
        bodyPart: String,
        offset: Int,
        limit: Int,
    ): Result<List<Exercise>> =
        runCatching {
            allExercises
                .filter { ex -> ex.bodyParts.any { it.equals(bodyPart, ignoreCase = true) } }
                .drop(offset).take(limit)
        }

    private fun getBodyPartsLocal(): Result<List<String>> = runCatching { allBodyParts }

    // ── remote (Retrofit) ────────────────────────────────────────────────────

    private val api get() = RetrofitInstance.api

    private suspend fun searchRemote(
        query: String,
        offset: Int,
        limit: Int,
    ): Result<List<Exercise>> =
        runCatching { api.searchExercises(search = query, offset = offset, limit = limit).data }

    private suspend fun listByBodyPartRemote(
        bodyPart: String,
        offset: Int,
        limit: Int,
    ): Result<List<Exercise>> =
        runCatching {
            api.listExercisesByBodyPart(
                bodyPart = bodyPart,
                offset = offset,
                limit = limit,
            ).data
        }

    private suspend fun getBodyPartsRemote(): Result<List<String>> =
        runCatching { api.getBodyParts().data.map { it.name } }

    // ── public API ───────────────────────────────────────────────────────────

    suspend fun searchExercises(
        query: String,
        offset: Int = 0,
        limit: Int = 20,
        useRemote: Boolean = false,
    ): Result<List<Exercise>> =
        if (useRemote) searchRemote(query, offset, limit)
        else searchLocal(query, offset, limit)

    suspend fun listExercisesByBodyPart(
        bodyPart: String,
        offset: Int = 0,
        limit: Int = 20,
        useRemote: Boolean = false,
    ): Result<List<Exercise>> =
        if (useRemote) listByBodyPartRemote(bodyPart, offset, limit)
        else listByBodyPartLocal(bodyPart, offset, limit)

    suspend fun getBodyParts(useRemote: Boolean = false): Result<List<String>> =
        if (useRemote) getBodyPartsRemote()
        else getBodyPartsLocal()
}
