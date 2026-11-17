package com.example.borrowbox.viewmodel

import android.app.Application
import android.util.Patterns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.borrowbox.data.local.BorrowBoxDatabase
import com.example.borrowbox.data.model.User
import com.example.borrowbox.data.model.UserRole
import com.example.borrowbox.data.repository.AuthRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false
)

class AuthViewModel(app: Application) : AndroidViewModel(app) {
    private val db = BorrowBoxDatabase.getInstance(app)
    private val authRepository = AuthRepository(db.userDao(), app)

    val currentUser: StateFlow<User?> = authRepository.observeCurrentUser()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val isLoggedIn: StateFlow<Boolean> = authRepository.isLoggedIn
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val hasNoAdmin: StateFlow<Boolean> = authRepository.hasNoAdmin
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun login(usernameOrEmail: String, password: String) {
        if (usernameOrEmail.isBlank()) {
            _uiState.value = AuthUiState(errorMessage = "Please enter your username or email.")
            return
        }
        if (password.isBlank()) {
            _uiState.value = AuthUiState(errorMessage = "Please enter your password.")
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            val result = authRepository.login(usernameOrEmail, password)
            if (result.isSuccess) {
                _uiState.value = AuthUiState(isSuccess = true)
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Login failed. Please check credentials."
                _uiState.value = AuthUiState(errorMessage = errorMsg)
            }
        }
    }

    fun register(
        fullName: String,
        username: String,
        email: String,
        password: String,
        confirmPassword: String,
        role: UserRole = UserRole.USER
    ) {
        if (fullName.isBlank()) {
            _uiState.value = AuthUiState(errorMessage = "Please enter your full name.")
            return
        }
        if (username.length < 3) {
            _uiState.value = AuthUiState(errorMessage = "Username must be at least 3 characters.")
            return
        }
        if (email.isNotBlank() && !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _uiState.value = AuthUiState(errorMessage = "Please enter a valid email address.")
            return
        }
        if (password.length < 6) {
            _uiState.value = AuthUiState(errorMessage = "Password must be at least 6 characters.")
            return
        }
        if (password != confirmPassword) {
            _uiState.value = AuthUiState(errorMessage = "Passwords do not match.")
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            val result = authRepository.register(
                fullName = fullName,
                username = username,
                email = email,
                password = password,
                role = role
            )
            if (result.isSuccess) {
                _uiState.value = AuthUiState(isSuccess = true)
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Registration failed."
                _uiState.value = AuthUiState(errorMessage = errorMsg)
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _uiState.value = AuthUiState()
        }
    }

    fun clearState() {
        _uiState.value = AuthUiState()
    }
}
