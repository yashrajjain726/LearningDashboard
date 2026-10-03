package com.example.learningdashboard.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.learningdashboard.data.CourseRepository
import com.example.learningdashboard.domain.Course
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class CoursesState(
    val courses: List<Course>? = null, val loading: Boolean = true,
    val fromCache: Boolean = false, val error: String? = null,
    val saving: Boolean = false, val saveError: String? = null
)
class CoursesViewModel(private val repository: CourseRepository) : ViewModel() {
    private val status = MutableStateFlow(CoursesState())
    val state = combine(repository.snapshot, status) { snapshot, status ->
        status.copy(courses = snapshot.courses, fromCache = snapshot.fromCache)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CoursesState())
    init { refresh() }
    private var refreshing = false
    fun refresh() {
        if (refreshing) return
        refreshing = true
        status.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try {
                // A corrupt cache may be repaired by a successful API refresh.
                try { repository.loadCached() }
                catch (e: CancellationException) { throw e }
                catch (e: Exception) { status.update { it.copy(error = "Saved data could not be read.") } }
                repository.refresh()
                status.update { it.copy(error = null) }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                status.update { it.copy(error = e.message ?: "Courses could not be loaded.") }
            } finally {
                refreshing = false
                status.update { it.copy(loading = false) }
            }
        }
    }
    fun complete(courseId: Int, lessonId: Int) {
        if (status.value.saving) return
        status.update { it.copy(saving = true, saveError = null) }
        viewModelScope.launch {
            try { repository.completeLesson(courseId, lessonId) }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { status.update { it.copy(saveError = "Could not save your progress. Please retry.") } }
            finally { status.update { it.copy(saving = false) } }
        }
    }
}
