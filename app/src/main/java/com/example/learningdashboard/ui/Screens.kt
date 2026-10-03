package com.example.learningdashboard.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.learningdashboard.BuildConfig
import com.example.learningdashboard.data.DemoMode
import com.example.learningdashboard.domain.Course

@Composable
fun LearningTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = lightColorScheme(
        primary = Color(0xFF315DA8), secondary = Color(0xFF46634C),
        background = Color(0xFFF7F8FC), surface = Color.White
    ), content = content)
}

@Composable
fun LoginScreen(state: LoginState, onLogin: (String, String) -> Unit) {
    var email by rememberSaveable { mutableStateOf("") }
    // Password intentionally stays in memory only, outside saved instance state.
    var password by remember { mutableStateOf("") }
    Scaffold { padding ->
        Column(Modifier.fillMaxSize().padding(padding).imePadding().padding(24.dp),
            verticalArrangement = Arrangement.Center) {
            Text("LEARNING DASHBOARD", style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(12.dp))
            Text("Welcome back", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Text("Sign in to continue learning.", modifier = Modifier.padding(top = 8.dp, bottom = 28.dp))
            OutlinedTextField(email, { email = it }, label = { Text("Email") },
                singleLine = true, enabled = !state.loading, isError = state.emailError != null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                supportingText = { state.emailError?.let { Text(it) } }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(password, { password = it }, label = { Text("Password") },
                singleLine = true, enabled = !state.loading, isError = state.passwordError != null,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                supportingText = { state.passwordError?.let { Text(it) } }, modifier = Modifier.fillMaxWidth())
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(vertical = 8.dp)) }
            Button(onClick = { onLogin(email, password) }, enabled = !state.loading,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp).height(52.dp)) {
                if (state.loading) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(12.dp))
                    Text("Signing in…")
                } else Text("Login")
            }
            Text("Demo: learner@example.com / Learn123!", style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 20.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseScreens(state: CoursesState, selectedId: Int, onSelect: (Int) -> Unit,
    onBack: () -> Unit, onRefresh: () -> Unit, onComplete: (Int, Int) -> Unit,
    onLogout: () -> Unit, onDemoMode: (DemoMode) -> Unit) {
    val detail = selectedId != -1
    val course = state.courses?.find { it.id == selectedId }
    var debugMenu by remember { mutableStateOf(false) }
    BackHandler(enabled = detail, onBack = onBack)
    Scaffold(topBar = {
        TopAppBar(title = { Text(if (detail) "Course details" else "My learning") },
            navigationIcon = { if (detail) TextButton(onClick = onBack) { Text("Back") } },
            actions = {
                if (!detail) {
                    TextButton(onClick = onLogout) { Text("Sign out") }
                    if (BuildConfig.DEBUG) {
                        Box {
                            TextButton(onClick = { debugMenu = true }, enabled = !state.loading) { Text("Demo") }
                            DropdownMenu(expanded = debugMenu, onDismissRequest = { debugMenu = false }) {
                                DemoMode.entries.forEach { mode ->
                                    DropdownMenuItem(text = { Text(when (mode) {
                                        DemoMode.NORMAL -> "Normal API"
                                        DemoMode.FAILURE -> "Fail API"
                                        DemoMode.EMPTY -> "Empty API"
                                    }) }, onClick = { debugMenu = false; onDemoMode(mode) })
                                }
                            }
                        }
                    }
                }
            })
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (state.loading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            if (state.courses != null && (state.fromCache || state.error != null)) {
                Surface(color = MaterialTheme.colorScheme.secondaryContainer) {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) {
                        Text(if (state.error != null) "Saved courses • ${state.error}" else "Showing saved courses • refreshing…",
                            style = MaterialTheme.typography.bodySmall)
                        if (state.error != null) TextButton(onClick = onRefresh, enabled = !state.loading) { Text("Retry") }
                    }
                }
            }
            state.saveError?.let {
                Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(20.dp))
            }
            when {
                state.courses == null && state.loading -> MessageState("Loading your courses…")
                state.courses == null -> MessageState(state.error ?: "Courses could not be loaded.", "Retry", onRefresh)
                detail && course == null -> MessageState("This course is no longer available.", "Back to courses", onBack)
                detail && course != null -> DetailsScreen(course, state.saving, onComplete)
                state.courses.isEmpty() -> MessageState("No courses yet", "Refresh", onRefresh)
                else -> DashboardScreen(state.courses, state.loading, onSelect, onRefresh)
            }
        }
    }
}

@Composable
private fun MessageState(message: String, action: String? = null, onAction: () -> Unit = {}) {
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center) {
        Text(message, style = MaterialTheme.typography.titleMedium)
        action?.let { Button(onClick = onAction, modifier = Modifier.padding(top = 16.dp)) { Text(it) } }
    }
}

@Composable
private fun DashboardScreen(courses: List<Course>, loading: Boolean, onSelect: (Int) -> Unit, onRefresh: () -> Unit) {
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Keep moving forward", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("${courses.size} courses in your library", style = MaterialTheme.typography.bodyMedium)
                }
                TextButton(onClick = onRefresh, enabled = !loading) { Text("Refresh") }
            }
        }
        items(courses, key = { it.id }) { course ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(course.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text("Instructor: ${course.instructor}", style = MaterialTheme.typography.bodyMedium)
                    Progress(course)
                    Text("${course.lessons.size} lessons • ${course.completedLessons} completed",
                        style = MaterialTheme.typography.bodySmall)
                    Button(onClick = { onSelect(course.id) }, modifier = Modifier.fillMaxWidth()) { Text("Continue") }
                }
            }
        }
    }
}

@Composable
private fun Progress(course: Course) {
    Text("${course.progress}% complete", style = MaterialTheme.typography.labelLarge)
    LinearProgressIndicator(progress = { course.progress / 100f }, modifier = Modifier.fillMaxWidth())
}

@Composable
private fun DetailsScreen(course: Course, saving: Boolean, onComplete: (Int, Int) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(course.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(course.instructor)
                Progress(course)
                Text("${course.completedLessons} of ${course.lessons.size} lessons completed")
                Text("Lessons", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 12.dp))
            }
        }
        if (course.lessons.isEmpty()) item { Text("No lessons available yet.") }
        items(course.lessons, key = { it.id }) { lesson ->
            OutlinedCard(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("${lesson.id}. ${lesson.title}", style = MaterialTheme.typography.titleMedium)
                        Text(if (lesson.completed) "✓ Completed" else "○ Pending",
                            color = if (lesson.completed) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (!lesson.completed) TextButton(onClick = { onComplete(course.id, lesson.id) }, enabled = !saving) { Text("Complete") }
                }
            }
        }
    }
}
