package com.example.borrowbox.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.borrowbox.data.model.User
import com.example.borrowbox.data.model.UserRole

@Entity(
    tableName = "users",
    indices = [Index(value = ["username"], unique = true)]
)
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val username: String,
    val passwordHash: String,
    val salt: String,
    val role: String,
    val fullName: String,
    val email: String,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

fun UserEntity.toDomainModel() = User(
    id = id,
    username = username,
    passwordHash = passwordHash,
    salt = salt,
    role = try { UserRole.valueOf(role) } catch (e: Exception) { UserRole.USER },
    fullName = fullName,
    email = email,
    isActive = isActive,
    createdAt = createdAt
)

fun User.toEntity() = UserEntity(
    id = id,
    username = username,
    passwordHash = passwordHash,
    salt = salt,
    role = role.name,
    fullName = fullName,
    email = email,
    isActive = isActive,
    createdAt = createdAt
)
