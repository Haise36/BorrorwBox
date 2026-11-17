package com.example.borrowbox.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users ORDER BY id ASC")
    fun observeAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    fun observeUserById(id: Long): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: Long): UserEntity?

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): UserEntity?

    @Query("SELECT * FROM users WHERE username = :usernameOrEmail OR email = :usernameOrEmail LIMIT 1")
    suspend fun getUserByUsernameOrEmail(usernameOrEmail: String): UserEntity?

    @Query("SELECT COUNT(*) FROM users WHERE role = 'ADMIN'")
    suspend fun countAdmins(): Int

    @Query("SELECT COUNT(*) FROM users")
    fun countTotalUsers(): Flow<Int>

    @Query("SELECT COUNT(*) FROM users WHERE isActive = 1")
    fun countActiveUsers(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity): Long

    @Update
    suspend fun updateUser(user: UserEntity)

    @Delete
    suspend fun deleteUser(user: UserEntity)

    @Query("UPDATE users SET isActive = :isActive WHERE id = :userId")
    suspend fun updateUserActiveState(userId: Long, isActive: Boolean)

    @Query("UPDATE users SET role = :role WHERE id = :userId")
    suspend fun updateUserRole(userId: Long, role: String)
}
