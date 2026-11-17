package com.example.borrowbox.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BorrowingDao {
    @Query("SELECT * FROM borrowings WHERE isReturned = 0 AND isArchived = 0 ORDER BY isPinned DESC, expectedReturnDate ASC")
    fun observeActiveBorrowings(): Flow<List<BorrowingEntity>>

    @Query("SELECT * FROM borrowings WHERE isReturned = 0 AND isArchived = 0 AND (userId = :userId OR userId = 0) ORDER BY isPinned DESC, expectedReturnDate ASC")
    fun observeActiveBorrowingsForUser(userId: Long): Flow<List<BorrowingEntity>>

    @Query("SELECT * FROM borrowings WHERE (isReturned = 1 OR isArchived = 1) ORDER BY returnedDate DESC, id DESC")
    fun observeBorrowingHistory(): Flow<List<BorrowingEntity>>

    @Query("SELECT * FROM borrowings WHERE (isReturned = 1 OR isArchived = 1) AND (userId = :userId OR userId = 0) ORDER BY returnedDate DESC, id DESC")
    fun observeBorrowingHistoryForUser(userId: Long): Flow<List<BorrowingEntity>>

    @Query("SELECT * FROM borrowings WHERE isArchived = 1 ORDER BY id DESC")
    fun observeArchivedBorrowings(): Flow<List<BorrowingEntity>>

    @Query("SELECT * FROM borrowings ORDER BY isPinned DESC, id DESC")
    fun observeAllBorrowings(): Flow<List<BorrowingEntity>>

    @Query("SELECT * FROM borrowings WHERE (userId = :userId OR userId = 0) ORDER BY isPinned DESC, id DESC")
    fun observeAllBorrowingsForUser(userId: Long): Flow<List<BorrowingEntity>>

    @Query("SELECT * FROM borrowings WHERE id = :id LIMIT 1")
    fun observeBorrowingById(id: Long): Flow<BorrowingEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBorrowing(borrowing: BorrowingEntity): Long

    @Update suspend fun updateBorrowing(borrowing: BorrowingEntity)

    @Delete suspend fun deleteBorrowing(borrowing: BorrowingEntity)

    @Query("SELECT COUNT(*) FROM borrowings WHERE isReturned = 0 AND isArchived = 0")
    fun countActiveBorrowings(): Flow<Int>

    @Query("SELECT COUNT(*) FROM borrowings WHERE isReturned = 0 AND isArchived = 0 AND (userId = :userId OR userId = 0)")
    fun countActiveBorrowingsForUser(userId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM borrowings WHERE isReturned = 1 AND isArchived = 0")
    fun countReturnedBorrowings(): Flow<Int>

    @Query("SELECT COUNT(*) FROM borrowings WHERE isReturned = 1 AND isArchived = 0 AND (userId = :userId OR userId = 0)")
    fun countReturnedBorrowingsForUser(userId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM borrowings WHERE isReturned = 0 AND isArchived = 0 AND expectedReturnDate < :now")
    fun countOverdueBorrowings(now: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM borrowings WHERE isReturned = 0 AND isArchived = 0 AND expectedReturnDate < :now AND (userId = :userId OR userId = 0)")
    fun countOverdueBorrowingsForUser(now: Long, userId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM borrowings WHERE isReturned = 0 AND isArchived = 0 AND expectedReturnDate >= :start AND expectedReturnDate <= :end")
    fun countBorrowingsDueInRange(start: Long, end: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM borrowings WHERE isReturned = 0 AND isArchived = 0 AND expectedReturnDate >= :start AND expectedReturnDate <= :end AND (userId = :userId OR userId = 0)")
    fun countBorrowingsDueInRangeForUser(start: Long, end: Long, userId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM borrowings")
    fun countTotalBorrowings(): Flow<Int>

    @Query("SELECT COUNT(*) FROM borrowings WHERE (userId = :userId OR userId = 0)")
    fun countTotalBorrowingsForUser(userId: Long): Flow<Int>

    @Query("SELECT * FROM borrowings WHERE borrowerName = :name ORDER BY dateBorrowed DESC")
    fun observeBorrowingsByBorrower(name: String): Flow<List<BorrowingEntity>>

    @Query("SELECT * FROM borrowings WHERE borrowerName = :name AND (userId = :userId OR userId = 0) ORDER BY dateBorrowed DESC")
    fun observeBorrowingsByBorrowerForUser(name: String, userId: Long): Flow<List<BorrowingEntity>>

    @Query("SELECT * FROM borrowings WHERE isReturned = 0 AND isArchived = 0 ORDER BY expectedReturnDate ASC LIMIT :limit")
    fun observeUrgentBorrowings(limit: Int): Flow<List<BorrowingEntity>>

    @Query("SELECT * FROM borrowings WHERE isReturned = 0 AND isArchived = 0 AND (userId = :userId OR userId = 0) ORDER BY expectedReturnDate ASC LIMIT :limit")
    fun observeUrgentBorrowingsForUser(limit: Int, userId: Long): Flow<List<BorrowingEntity>>
    
    @Query("SELECT * FROM borrowings")
    suspend fun getAllBorrowingsSync(): List<BorrowingEntity>

    @Query("SELECT * FROM borrowings WHERE (userId = :userId OR userId = 0)")
    suspend fun getAllBorrowingsSyncForUser(userId: Long): List<BorrowingEntity>

    @Query("SELECT category, COUNT(*) as count FROM borrowings GROUP BY category ORDER BY count DESC LIMIT 1")
    fun getMostPopularCategory(): Flow<CategoryAnalytics?>

    @Query("SELECT category, COUNT(*) as count FROM borrowings WHERE (userId = :userId OR userId = 0) GROUP BY category ORDER BY count DESC LIMIT 1")
    fun getMostPopularCategoryForUser(userId: Long): Flow<CategoryAnalytics?>

    @Query("SELECT itemName, COUNT(*) as count FROM borrowings GROUP BY itemName ORDER BY count DESC LIMIT 1")
    fun getMostBorrowedItem(): Flow<ItemAnalytics?>

    @Query("SELECT itemName, COUNT(*) as count FROM borrowings WHERE (userId = :userId OR userId = 0) GROUP BY itemName ORDER BY count DESC LIMIT 1")
    fun getMostBorrowedItemForUser(userId: Long): Flow<ItemAnalytics?>
}

data class CategoryAnalytics(val category: String, val count: Int)
data class ItemAnalytics(val itemName: String, val count: Int)
