package com.example.borrowbox.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BorrowerDao {
    @Query("SELECT * FROM borrowers ORDER BY name ASC")
    fun getAllBorrowers(): Flow<List<BorrowerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(borrower: BorrowerEntity): Long

    @Update
    suspend fun update(borrower: BorrowerEntity)

    @Delete
    suspend fun delete(borrower: BorrowerEntity)

    @Query("SELECT * FROM borrowers WHERE name = :name LIMIT 1")
    suspend fun getBorrowerByName(name: String): BorrowerEntity?
}
