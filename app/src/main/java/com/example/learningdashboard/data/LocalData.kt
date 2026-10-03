package com.example.learningdashboard.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.AtomicFile
import com.example.learningdashboard.domain.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

private data class CacheEnvelope(val version: Int = 1, val courses: List<Course>)

class JsonCourseStore(context: Context) : CourseStore {
    private val file = AtomicFile(File(context.filesDir, "courses-v1.json"))
    private val gson = Gson()
    override suspend fun read(): List<Course>? = withContext(Dispatchers.IO) {
        if (!file.baseFile.exists()) return@withContext null
        val envelope = file.openRead().bufferedReader().use {
            gson.fromJson(it, CacheEnvelope::class.java)
        }
        if (envelope == null || envelope.version != 1) throw IOException("Unsupported course cache")
        validate(envelope.courses)
    }
    override suspend fun write(courses: List<Course>) = withContext(Dispatchers.IO) {
        validate(courses)
        val output = file.startWrite()
        try {
            output.write(gson.toJson(CacheEnvelope(courses = courses)).toByteArray(Charsets.UTF_8))
            file.finishWrite(output)
        } catch (e: Exception) {
            file.failWrite(output)
            throw e
        }
    }
}

enum class DemoMode { NORMAL, FAILURE, EMPTY }

/** Delayed mock API: asset payload + real connectivity gate, no external backend. */
class MockCourseApi(private val context: Context) : CourseSource {
    var mode = DemoMode.NORMAL
    override suspend fun fetch(): List<Course> {
        delay(800)
        val manager = context.getSystemService(ConnectivityManager::class.java)
        val capabilities = manager.getNetworkCapabilities(manager.activeNetwork)
        if (capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) != true)
            throw IOException("You are offline. Showing saved courses when available.")
        return when (mode) {
            DemoMode.FAILURE -> throw IOException("The mock API is unavailable. Please retry.")
            DemoMode.EMPTY -> emptyList()
            DemoMode.NORMAL -> withContext(Dispatchers.IO) {
                val type = object : TypeToken<List<Course>>() {}.type
                context.assets.open("courses.json").bufferedReader().use {
                    validate(Gson().fromJson(it, type))
                }
            }
        }
    }
}

private fun validate(courses: List<Course>): List<Course> {
    require(courses.map { it.id }.distinct().size == courses.size) { "Duplicate course IDs" }
    courses.forEach { c ->
        require(c.title.isNotBlank() && c.instructor.isNotBlank()) { "Invalid course" }
        require(c.lessons.map { it.id }.distinct().size == c.lessons.size) { "Duplicate lesson IDs" }
        require(c.lessons.all { it.title.isNotBlank() }) { "Invalid lesson" }
    }
    return courses
}
