package com.example.borrowbox.data.model

import java.util.Calendar

data class Borrowing(
    val id: Long = 0,
    val userId: Long = 0L,
    val itemName: String,
    val borrowerName: String,
    val category: String = "Others",
    val contactNumber: String? = null,
    val classSection: String? = null,
    val returnLocation: String? = null,
    val dateBorrowed: Long,
    val expectedReturnDate: Long,
    val notes: String = "",
    val isReturned: Boolean = false,
    val returnedDate: Long? = null,
    val photoUri: String? = null,
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val conditionLent: String? = null,
    val conditionReturned: String? = null,
    val returnPhotoUri: String? = null
)

enum class BorrowingStatus { ACTIVE, DUE_SOON, DUE_TODAY, OVERDUE, RETURNED, ARCHIVED }

object BorrowingConstants {
    val categories = listOf(
        "Books", "Electronics", "School Supplies", "Tools",
        "Personal Items", "Accessories", "Others"
    )

    val locations = listOf(
        "Classroom", "Library", "Student Lounge", "Campus Office", "Other"
    )

    val conditions = listOf(
        "Like New", "Good", "Fair", "Worn", "Damaged"
    )
}

fun normalizeToMidnight(timestamp: Long): Long {
    return Calendar.getInstance().apply {
        timeInMillis = timestamp
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

fun calculateCalendarDaysBetween(startMs: Long, endMs: Long): Int {
    val startMidnight = normalizeToMidnight(startMs)
    val endMidnight = normalizeToMidnight(endMs)
    val diff = endMidnight - startMidnight
    return (diff / 86_400_000L).toInt()
}

fun Borrowing.calculateStatus(now: Long = System.currentTimeMillis()): BorrowingStatus {
    if (isArchived) return BorrowingStatus.ARCHIVED
    if (isReturned) return BorrowingStatus.RETURNED
    
    val daysUntilDue = calculateCalendarDaysBetween(now, expectedReturnDate)
    
    return when {
        daysUntilDue < 0 -> BorrowingStatus.OVERDUE
        daysUntilDue == 0 -> BorrowingStatus.DUE_TODAY
        daysUntilDue in 1..2 -> BorrowingStatus.DUE_SOON
        else -> BorrowingStatus.ACTIVE
    }
}

fun Borrowing.getStatusLabel(now: Long = System.currentTimeMillis()): String {
    val status = calculateStatus(now)
    val daysDiff = calculateCalendarDaysBetween(expectedReturnDate, now)
    return when (status) {
        BorrowingStatus.OVERDUE -> {
            if (daysDiff <= 0) "Overdue" else "$daysDiff day${if (daysDiff > 1) "s" else ""} overdue"
        }
        BorrowingStatus.DUE_SOON -> "Due soon"
        BorrowingStatus.DUE_TODAY -> "Due today"
        BorrowingStatus.RETURNED -> "Returned"
        BorrowingStatus.ARCHIVED -> "Archived"
        BorrowingStatus.ACTIVE -> "Borrowed"
    }
}
