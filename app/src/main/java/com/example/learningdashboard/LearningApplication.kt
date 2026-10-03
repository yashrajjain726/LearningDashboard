package com.example.learningdashboard

import android.app.Application
import com.example.learningdashboard.data.*

class LearningApplication : Application() {
    val api by lazy { MockCourseApi(this) }
    val courses by lazy { CourseRepository(api, JsonCourseStore(this)) }
    val auth by lazy { MockAuthRepository() }
}
