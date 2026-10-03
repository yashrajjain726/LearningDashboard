package com.example.learningdashboard.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.learningdashboard.data.MockAuthRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LoginState(
    val loading: Boolean = false, val authenticated: Boolean = false,
    val emailError: String? = null, val passwordError: String? = null, val error: String? = null
)
class LoginViewModel(private val auth: MockAuthRepository) : ViewModel() {
    private val mutable = MutableStateFlow(LoginState())
    val state = mutable.asStateFlow()
    fun login(email: String, password: String) {
        if (mutable.value.loading) return
        val cleaned = email.trim()
        val emailError = if (!Regex("""^[^\s@]+@[^\s@]+\.[^\s@]+$""").matches(cleaned)) "Enter a valid email" else null
        val passwordError = if (password.length < 8) "Use at least 8 characters" else null
        mutable.value = LoginState(emailError = emailError, passwordError = passwordError)
        if (emailError != null || passwordError != null) return
        mutable.value = LoginState(loading = true)
        viewModelScope.launch {
            try {
                mutable.value = if (auth.login(cleaned, password)) LoginState(authenticated = true)
                else LoginState(error = "Incorrect email or password")
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { mutable.value = LoginState(error = "Unable to sign in. Please retry.") }
        }
    }
    fun logout() { mutable.value = LoginState() }
}
