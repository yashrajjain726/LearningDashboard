package com.example.learningdashboard.data

import com.example.learningdashboard.domain.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class CourseSnapshot(val courses: List<Course>? = null, val fromCache: Boolean = false)

/** Serializes refreshes and edits; disk must commit before UI reports success. */
class CourseRepository(private val source: CourseSource, private val store: CourseStore) {
    private val mutex = Mutex()
    private val mutable = MutableStateFlow(CourseSnapshot())
    val snapshot = mutable.asStateFlow()

    suspend fun loadCached() = mutex.withLock {
        if (mutable.value.courses == null) {
            store.read()?.let { mutable.value = CourseSnapshot(it, fromCache = true) }
        }
    }

    suspend fun refresh() = mutex.withLock {
        val remote = source.fetch()
        val previous = mutable.value.courses ?: try {
            store.read().orEmpty()
        } catch (e: CancellationException) { throw e }
        catch (_: Exception) { emptyList() } // A valid API payload repairs an unreadable cache.
        val completed = previous.associate { course ->
            course.id to course.lessons.filter { it.completed }.map { it.id }.toSet()
        }
        // A mock server never acknowledges local edits. Preserve them by stable IDs.
        val merged = remote.map { course ->
            course.copy(lessons = course.lessons.map { lesson ->
                lesson.copy(completed = lesson.completed || lesson.id in completed[course.id].orEmpty())
            })
        }
        store.write(merged)
        mutable.value = CourseSnapshot(merged)
    }

    suspend fun completeLesson(courseId: Int, lessonId: Int) = mutex.withLock {
        val courses = requireNotNull(mutable.value.courses) { "Courses have not loaded" }
        val course = requireNotNull(courses.find { it.id == courseId }) { "Course not found" }
        val lesson = requireNotNull(course.lessons.find { it.id == lessonId }) { "Lesson not found" }
        if (lesson.completed) return@withLock
        val updated = courses.map {
            if (it.id != courseId) it else it.copy(lessons = it.lessons.map { l ->
                if (l.id == lessonId) l.copy(completed = true) else l
            })
        }
        store.write(updated)
        mutable.value = mutable.value.copy(courses = updated)
    }
}
