package com.example.learningdashboard.domain

import kotlin.math.roundToInt

data class Lesson(val id: Int, val title: String, val completed: Boolean)
data class Course(val id: Int, val title: String, val instructor: String, val lessons: List<Lesson>) {
    val completedLessons: Int get() = lessons.count { it.completed }
    val progress: Int get() = if (lessons.isEmpty()) 0
        else (completedLessons * 100.0 / lessons.size).roundToInt()
}

interface CourseSource { suspend fun fetch(): List<Course> }
interface CourseStore {
    suspend fun read(): List<Course>?
    suspend fun write(courses: List<Course>)
}
