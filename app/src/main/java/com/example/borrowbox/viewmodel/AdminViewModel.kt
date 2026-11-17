package com.example.borrowbox.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.borrowbox.data.local.BorrowBoxDatabase
import com.example.borrowbox.data.model.User
import com.example.borrowbox.data.model.UserRole
import com.example.borrowbox.data.repository.AuthRepository
import com.example.borrowbox.data.repository.BorrowingRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class AdminSystemStats(
    val totalUsers: Int = 0,
    val activeUsers: Int = 0,
    val totalBorrowings: Int = 0,
    val activeBorrowings: Int = 0,
    val overdueBorrowings: Int = 0,
    val returnedBorrowings: Int = 0
)

class AdminViewModel(app: Application) : AndroidViewModel(app) {
    private val db = BorrowBoxDatabase.getInstance(app)
    private val authRepository = AuthRepository(db.userDao(), app)
    private val borrowingRepository = BorrowingRepository(db.borrowingDao(), db.borrowerDao())

    val currentUser: StateFlow<User?> = authRepository.observeCurrentUser()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val allUsers: StateFlow<List<User>> = authRepository.observeAllUsers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    val systemStats: StateFlow<AdminSystemStats> = combine(
        authRepository.observeAllUsers(),
        borrowingRepository.countTotal(),
        borrowingRepository.countActive(),
        borrowingRepository.countOverdue(System.currentTimeMillis()),
        borrowingRepository.countReturned()
    ) { users, totalB, activeB, overdueB, returnedB ->
        AdminSystemStats(
            totalUsers = users.size,
            activeUsers = users.count { it.isActive },
            totalBorrowings = totalB,
            activeBorrowings = activeB,
            overdueBorrowings = overdueB,
            returnedBorrowings = returnedB
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AdminSystemStats())

    val allGlobalBorrowings = borrowingRepository.allBorrowings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun toggleUserActive(user: User) {
        viewModelScope.launch {
            val currentAdminId = currentUser.value?.id
            if (user.id == currentAdminId && user.isActive) {
                _uiEvent.emit(UiEvent.ShowSnackbar("You cannot deactivate your own active admin account."))
                return@launch
            }
            if (user.role == UserRole.ADMIN && user.isActive) {
                val activeAdmins = allUsers.value.count { it.role == UserRole.ADMIN && it.isActive }
                if (activeAdmins <= 1) {
                    _uiEvent.emit(UiEvent.ShowSnackbar("Cannot deactivate the last active administrator."))
                    return@launch
                }
            }
            authRepository.toggleUserActive(user.id, !user.isActive)
            _uiEvent.emit(UiEvent.ShowSnackbar("User status updated."))
        }
    }

    fun changeUserRole(user: User, newRole: UserRole) {
        viewModelScope.launch {
            val currentAdminId = currentUser.value?.id
            if (user.id == currentAdminId && newRole == UserRole.USER) {
                _uiEvent.emit(UiEvent.ShowSnackbar("You cannot demote yourself to a regular user."))
                return@launch
            }
            if (user.role == UserRole.ADMIN && newRole == UserRole.USER) {
                val activeAdmins = allUsers.value.count { it.role == UserRole.ADMIN && it.isActive }
                if (activeAdmins <= 1) {
                    _uiEvent.emit(UiEvent.ShowSnackbar("Cannot demote the last administrator in the system."))
                    return@launch
                }
            }
            authRepository.changeUserRole(user.id, newRole)
            _uiEvent.emit(UiEvent.ShowSnackbar("User role updated to ${newRole.name}."))
        }
    }

    fun deleteUser(user: User) {
        viewModelScope.launch {
            val currentAdminId = currentUser.value?.id
            if (user.id == currentAdminId) {
                _uiEvent.emit(UiEvent.ShowSnackbar("You cannot delete your own administrator account."))
                return@launch
            }
            if (user.role == UserRole.ADMIN) {
                val activeAdmins = allUsers.value.count { it.role == UserRole.ADMIN && it.isActive }
                if (activeAdmins <= 1) {
                    _uiEvent.emit(UiEvent.ShowSnackbar("Cannot delete the last administrator in the system."))
                    return@launch
                }
            }
            authRepository.deleteUser(user)
            _uiEvent.emit(UiEvent.ShowSnackbar("User account deleted."))
        }
    }
}
