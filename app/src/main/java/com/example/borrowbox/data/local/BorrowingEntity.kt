package com.example.borrowbox.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.borrowbox.data.model.Borrowing

@Entity(tableName = "borrowings")
data class BorrowingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long = 0L,
    val itemName: String,
    val borrowerName: String,
    val category: String,
    val contactNumber: String?,
    val classSection: String?,
    val returnLocation: String? = null,
    val dateBorrowed: Long,
    val expectedReturnDate: Long,
    val notes: String,
    val isReturned: Boolean,
    val returnedDate: Long?,
    val photoUri: String?,
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val conditionLent: String? = null,
    val conditionReturned: String? = null,
    val returnPhotoUri: String? = null
)

fun BorrowingEntity.toDomainModel() = Borrowing(
    id = id,
    userId = userId,
    itemName = itemName,
    borrowerName = borrowerName,
    category = category,
    contactNumber = contactNumber,
    classSection = classSection,
    returnLocation = returnLocation,
    dateBorrowed = dateBorrowed,
    expectedReturnDate = expectedReturnDate,
    notes = notes,
    isReturned = isReturned,
    returnedDate = returnedDate,
    photoUri = photoUri,
    isPinned = isPinned,
    isArchived = isArchived,
    conditionLent = conditionLent,
    conditionReturned = conditionReturned,
    returnPhotoUri = returnPhotoUri
)

fun Borrowing.toEntity() = BorrowingEntity(
    id = id,
    userId = userId,
    itemName = itemName,
    borrowerName = borrowerName,
    category = category,
    contactNumber = contactNumber,
    classSection = classSection,
    returnLocation = returnLocation,
    dateBorrowed = dateBorrowed,
    expectedReturnDate = expectedReturnDate,
    notes = notes,
    isReturned = isReturned,
    returnedDate = returnedDate,
    photoUri = photoUri,
    isPinned = isPinned,
    isArchived = isArchived,
    conditionLent = conditionLent,
    conditionReturned = conditionReturned,
    returnPhotoUri = returnPhotoUri
)
