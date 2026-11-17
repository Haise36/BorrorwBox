package com.example.borrowbox.data.repository

import com.example.borrowbox.data.local.*
import com.example.borrowbox.data.model.Borrowing
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BorrowingRepository(private val borrowingDao: BorrowingDao, private val borrowerDao: BorrowerDao) {
    val activeBorrowings: Flow<List<Borrowing>> = borrowingDao.observeActiveBorrowings().map { it.map { e -> e.toDomainModel() } }
    val borrowingHistory: Flow<List<Borrowing>> = borrowingDao.observeBorrowingHistory().map { it.map { e -> e.toDomainModel() } }
    val allBorrowings: Flow<List<Borrowing>> = borrowingDao.observeAllBorrowings().map { it.map { e -> e.toDomainModel() } }

    fun getActiveBorrowingsForUser(userId: Long?): Flow<List<Borrowing>> {
        return if (userId == null || userId == 0L) activeBorrowings
        else borrowingDao.observeActiveBorrowingsForUser(userId).map { it.map { e -> e.toDomainModel() } }
    }

    fun getBorrowingHistoryForUser(userId: Long?): Flow<List<Borrowing>> {
        return if (userId == null || userId == 0L) borrowingHistory
        else borrowingDao.observeBorrowingHistoryForUser(userId).map { it.map { e -> e.toDomainModel() } }
    }

    fun getAllBorrowingsForUser(userId: Long?): Flow<List<Borrowing>> {
        return if (userId == null || userId == 0L) allBorrowings
        else borrowingDao.observeAllBorrowingsForUser(userId).map { it.map { e -> e.toDomainModel() } }
    }

    fun getBorrowingById(id: Long): Flow<Borrowing?> = borrowingDao.observeBorrowingById(id).map { it?.toDomainModel() }

    suspend fun saveBorrowing(borrowing: Borrowing) {
        if (borrowing.id == 0L) {
            borrowingDao.insertBorrowing(borrowing.toEntity())
        } else {
            borrowingDao.updateBorrowing(borrowing.toEntity())
        }
        
        // Ensure borrower exists in the system
        if (borrowerDao.getBorrowerByName(borrowing.borrowerName) == null) {
            borrowerDao.insert(BorrowerEntity(
                name = borrowing.borrowerName,
                contactNumber = borrowing.contactNumber,
                classSection = borrowing.classSection
            ))
        }
    }

    suspend fun deleteBorrowing(borrowing: Borrowing) = borrowingDao.deleteBorrowing(borrowing.toEntity())

    suspend fun deleteBorrower(borrower: BorrowerEntity) = borrowerDao.delete(borrower)

    suspend fun markAsReturned(borrowing: Borrowing) = borrowingDao.updateBorrowing(
        borrowing.copy(isReturned = true, returnedDate = System.currentTimeMillis()).toEntity()
    )
    
    suspend fun toggleArchived(borrowing: Borrowing) = borrowingDao.updateBorrowing(
        borrowing.copy(isArchived = !borrowing.isArchived).toEntity()
    )

    fun countActive(userId: Long? = null): Flow<Int> = 
        if (userId == null || userId == 0L) borrowingDao.countActiveBorrowings() 
        else borrowingDao.countActiveBorrowingsForUser(userId)

    fun countReturned(userId: Long? = null): Flow<Int> = 
        if (userId == null || userId == 0L) borrowingDao.countReturnedBorrowings() 
        else borrowingDao.countReturnedBorrowingsForUser(userId)

    fun countOverdue(now: Long, userId: Long? = null): Flow<Int> = 
        if (userId == null || userId == 0L) borrowingDao.countOverdueBorrowings(now) 
        else borrowingDao.countOverdueBorrowingsForUser(now, userId)

    fun countDueInRange(start: Long, end: Long, userId: Long? = null): Flow<Int> = 
        if (userId == null || userId == 0L) borrowingDao.countBorrowingsDueInRange(start, end) 
        else borrowingDao.countBorrowingsDueInRangeForUser(start, end, userId)

    fun countTotal(userId: Long? = null): Flow<Int> = 
        if (userId == null || userId == 0L) borrowingDao.countTotalBorrowings() 
        else borrowingDao.countTotalBorrowingsForUser(userId)

    fun observeUrgent(limit: Int, userId: Long? = null): Flow<List<Borrowing>> = 
        if (userId == null || userId == 0L) borrowingDao.observeUrgentBorrowings(limit).map { it.map { e -> e.toDomainModel() } }
        else borrowingDao.observeUrgentBorrowingsForUser(limit, userId).map { it.map { e -> e.toDomainModel() } }
        
    fun observeByBorrower(name: String, userId: Long? = null): Flow<List<Borrowing>> =
        if (userId == null || userId == 0L) borrowingDao.observeBorrowingsByBorrower(name).map { it.map { e -> e.toDomainModel() } }
        else borrowingDao.observeBorrowingsByBorrowerForUser(name, userId).map { it.map { e -> e.toDomainModel() } }
        
    fun getAllBorrowers(): Flow<List<BorrowerEntity>> = borrowerDao.getAllBorrowers()
    
    suspend fun getAllBorrowingsSync(userId: Long? = null): List<Borrowing> = 
        if (userId == null || userId == 0L) borrowingDao.getAllBorrowingsSync().map { it.toDomainModel() }
        else borrowingDao.getAllBorrowingsSyncForUser(userId).map { it.toDomainModel() }
    
    fun getMostPopularCategory(userId: Long? = null) = 
        if (userId == null || userId == 0L) borrowingDao.getMostPopularCategory()
        else borrowingDao.getMostPopularCategoryForUser(userId)

    fun getMostBorrowedItem(userId: Long? = null) = 
        if (userId == null || userId == 0L) borrowingDao.getMostBorrowedItem()
        else borrowingDao.getMostBorrowedItemForUser(userId)
}
