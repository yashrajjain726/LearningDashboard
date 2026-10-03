package com.example.learningdashboard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.learningdashboard.ui.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
        )
        val app = application as LearningApplication
        setContent { LearningTheme { LearningApp(app) } }
    }
}

private class Factory<T : ViewModel>(val create: () -> T) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <V : ViewModel> create(modelClass: Class<V>): V = create() as V
}

@Composable
private fun LearningApp(app: LearningApplication) {
    val login: LoginViewModel = viewModel(factory = Factory { LoginViewModel(app.auth) })
    val auth by login.state.collectAsStateWithLifecycle()
    var courseId by rememberSaveable { mutableIntStateOf(-1) }
    if (!auth.authenticated) {
        LoginScreen(auth, login::login)
    } else {
        val courses: CoursesViewModel = viewModel(factory = Factory { CoursesViewModel(app.courses) })
        val state by courses.state.collectAsStateWithLifecycle()
        CourseScreens(
            state = state, selectedId = courseId, onSelect = { courseId = it },
            onBack = { courseId = -1 }, onRefresh = courses::refresh,
            onComplete = courses::complete, onLogout = { courseId = -1; login.logout() },
            onDemoMode = { app.api.mode = it; courses.refresh() }
        )
    }
}
