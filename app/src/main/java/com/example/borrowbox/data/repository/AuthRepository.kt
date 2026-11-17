package com.example.borrowbox.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import com.example.borrowbox.data.local.UserDao
import com.example.borrowbox.data.local.UserEntity
import com.example.borrowbox.data.local.toDomainModel
import com.example.borrowbox.data.local.toEntity
import com.example.borrowbox.data.model.User
import com.example.borrowbox.data.model.UserRole
import com.example.borrowbox.utils.PasswordUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

@OptIn(ExperimentalCoroutinesApi::class)
class AuthRepository(
    private val userDao: UserDao,
    private val context: Context
) {
    private val currentUserIdKey = longPreferencesKey("current_user_id")
    private val isLoggedInKey = booleanPreferencesKey("is_logged_in")

    val currentUserId: Flow<Long?> = context.dataStore.data.map { prefs ->
        if (prefs[isLoggedInKey] == true) prefs[currentUserIdKey] else null
    }

    val isLoggedIn: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[isLoggedInKey] ?: false
    }

    val hasNoAdmin: Flow<Boolean> = userDao.observeAllUsers().map { users ->
        users.none { it.role == UserRole.ADMIN.name && it.isActive }
    }

    fun observeCurrentUser(): Flow<User?> = currentUserId.flatMapLatest { id ->
        if (id != null) {
            try {
                userDao.observeUserById(id).map { it?.toDomainModel() }
            } catch (e: Exception) {
                flowOf(null)
            }
        } else {
            flowOf(null)
        }
    }

    suspend fun getCurrentUser(): User? {
        return try {
            val id = currentUserId.first() ?: return null
            userDao.getUserById(id)?.toDomainModel()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun login(usernameOrEmail: String, password: String): Result<User> {
        return try {
            val trimmed = usernameOrEmail.trim()
            val userEntity = userDao.getUserByUsernameOrEmail(trimmed)
                ?: return Result.failure(Exception("Invalid username or password"))

            if (!userEntity.isActive) {
                return Result.failure(Exception("Account is deactivated. Please contact an administrator."))
            }

            val verification = PasswordUtils.verifyPassword(password, userEntity.passwordHash, userEntity.salt)
            if (!verification.isValid) {
                return Result.failure(Exception("Invalid username or password"))
            }

            // Upgrade legacy SHA-256 password hash to PBKDF2 if needed
            if (verification.needsUpgrade) {
                val newHash = PasswordUtils.hashPassword(password, userEntity.salt)
                userDao.updateUser(userEntity.copy(passwordHash = newHash))
            }

            context.dataStore.edit { prefs ->
                prefs[currentUserIdKey] = userEntity.id
                prefs[isLoggedInKey] = true
            }

            Result.success(userEntity.toDomainModel())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun register(
        fullName: String,
        username: String,
        email: String,
        password: String,
        role: UserRole = UserRole.USER
    ): Result<User> {
        return try {
            val existing = userDao.getUserByUsername(username.trim())
            if (existing != null) {
                return Result.failure(Exception("Username already exists"))
            }

            val salt = PasswordUtils.generateSalt()
            val passwordHash = PasswordUtils.hashPassword(password, salt)

            val newUserEntity = UserEntity(
                username = username.trim(),
                passwordHash = passwordHash,
                salt = salt,
                role = role.name,
                fullName = fullName.trim(),
                email = email.trim(),
                isActive = true,
                createdAt = System.currentTimeMillis()
            )

            val newId = userDao.insertUser(newUserEntity)
            val createdUser = newUserEntity.copy(id = newId).toDomainModel()

            context.dataStore.edit { prefs ->
                prefs[currentUserIdKey] = newId
                prefs[isLoggedInKey] = true
            }

            Result.success(createdUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logout() {
        try {
            context.dataStore.edit { prefs ->
                prefs.remove(currentUserIdKey)
                prefs[isLoggedInKey] = false
            }
        } catch (e: Exception) {
            // Safe catch
        }
    }

    fun observeAllUsers(): Flow<List<User>> = userDao.observeAllUsers().map { list ->
        list.map { it.toDomainModel() }
    }

    suspend fun toggleUserActive(userId: Long, isActive: Boolean) {
        try {
            userDao.updateUserActiveState(userId, isActive)
        } catch (e: Exception) {}
    }

    suspend fun changeUserRole(userId: Long, newRole: UserRole) {
        try {
            userDao.updateUserRole(userId, newRole.name)
        } catch (e: Exception) {}
    }

    suspend fun deleteUser(user: User) {
        try {
            userDao.deleteUser(user.toEntity())
        } catch (e: Exception) {}
    }
}
