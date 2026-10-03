package com.example.learningdashboard.data

import com.example.learningdashboard.domain.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class CourseRepositoryTest {
    private val initial = Course(1, "Kotlin", "Instructor", listOf(
        Lesson(1, "Intro", true), Lesson(2, "Functions", false), Lesson(3, "OOP", false)))
    private class MemoryStore(var value: List<Course>? = null) : CourseStore {
        var failWrites = false
        override suspend fun read() = value
        override suspend fun write(courses: List<Course>) {
            if (failWrites) throw IOException("Disk full")
            value = courses
        }
    }
    private class Source(var value: List<Course>) : CourseSource {
        var offline = false
        override suspend fun fetch(): List<Course> {
            if (offline) throw IOException("Offline")
            return value
        }
    }

    @Test fun `offline completion survives restart and stale server refresh`() = runTest {
        val store = MemoryStore()
        val api = Source(listOf(initial))
        val first = CourseRepository(api, store)
        first.refresh()
        api.offline = true
        first.completeLesson(1, 2)
        first.completeLesson(1, 2) // idempotent: repeating cannot inflate progress
        assertEquals(67, first.snapshot.value.courses!!.single().progress)
        val restarted = CourseRepository(api, store)
        restarted.loadCached()
        try { restarted.refresh(); fail("Expected offline error") } catch (_: IOException) {}
        assertTrue(restarted.snapshot.value.fromCache)
        assertTrue(restarted.snapshot.value.courses!!.single().lessons[1].completed)
        api.offline = false
        restarted.refresh() // mock server still returns the old completion flags
        assertEquals(67, restarted.snapshot.value.courses!!.single().progress)
        assertTrue(store.value!!.single().lessons[1].completed)
    }

    @Test fun `failed disk write does not falsely report completion`() = runTest {
        val store = MemoryStore()
        val repository = CourseRepository(Source(listOf(initial)), store)
        repository.refresh()
        store.failWrites = true
        try { repository.completeLesson(1, 2); fail("Expected write failure") } catch (_: IOException) {}
        assertFalse(repository.snapshot.value.courses!!.single().lessons[1].completed)
        assertEquals(33, repository.snapshot.value.courses!!.single().progress)
    }

    @Test fun `empty API response is cached as empty success`() = runTest {
        val store = MemoryStore()
        val repository = CourseRepository(Source(emptyList()), store)
        repository.refresh()
        assertEquals(emptyList<Course>(), repository.snapshot.value.courses)
        assertEquals(emptyList<Course>(), store.value)
    }

    @Test fun `valid API response repairs unreadable cache`() = runTest {
        var written: List<Course>? = null
        val brokenStore = object : CourseStore {
            override suspend fun read(): List<Course>? = throw IOException("Corrupt cache")
            override suspend fun write(courses: List<Course>) { written = courses }
        }
        val repository = CourseRepository(Source(listOf(initial)), brokenStore)
        repository.refresh()
        assertEquals(listOf(initial), written)
        assertEquals(listOf(initial), repository.snapshot.value.courses)
    }

    @Test fun `course with no lessons has zero progress`() {
        assertEquals(0, initial.copy(lessons = emptyList()).progress)
    }
}
