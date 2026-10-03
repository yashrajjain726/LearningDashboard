package com.example.learningdashboard.data

import kotlinx.coroutines.delay

class MockAuthRepository {
    suspend fun login(email: String, password: String): Boolean {
        delay(700)
        return email.equals("learner@example.com", ignoreCase = true) && password == "Learn123!"
    }
}
