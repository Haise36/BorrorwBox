package com.example.borrowbox.data.model

enum class UserRole {
    USER,
    ADMIN
}

data class User(
    val id: Long = 0,
    val username: String,
    val passwordHash: String,
    val salt: String,
    val role: UserRole = UserRole.USER,
    val fullName: String,
    val email: String,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
