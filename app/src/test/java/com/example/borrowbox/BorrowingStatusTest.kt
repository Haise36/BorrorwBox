package com.example.borrowbox

import com.example.borrowbox.data.model.Borrowing
import com.example.borrowbox.data.model.BorrowingStatus
import com.example.borrowbox.data.model.calculateStatus
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class BorrowingStatusTest {

    private fun getTodayMidnight(): Calendar {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    @Test
    fun testDueToday() {
        val nowCal = getTodayMidnight()
        val now = nowCal.timeInMillis
        
        val borrowing = Borrowing(
            itemName = "Book",
            borrowerName = "John",
            dateBorrowed = now - 86_400_000L,
            expectedReturnDate = now
        )
        
        assertEquals(BorrowingStatus.DUE_TODAY, borrowing.calculateStatus(now))
    }

    @Test
    fun testDueInOneDay() {
        val nowCal = getTodayMidnight()
        val now = nowCal.timeInMillis
        val expectedReturn = nowCal.apply { add(Calendar.DAY_OF_YEAR, 1) }.timeInMillis

        val borrowing = Borrowing(
            itemName = "Laptop",
            borrowerName = "Alice",
            dateBorrowed = now,
            expectedReturnDate = expectedReturn
        )

        assertEquals(BorrowingStatus.DUE_SOON, borrowing.calculateStatus(now))
    }

    @Test
    fun testDueInTwoDays() {
        val nowCal = getTodayMidnight()
        val now = nowCal.timeInMillis
        val expectedReturn = nowCal.apply { add(Calendar.DAY_OF_YEAR, 2) }.timeInMillis

        val borrowing = Borrowing(
            itemName = "Camera",
            borrowerName = "Bob",
            dateBorrowed = now,
            expectedReturnDate = expectedReturn
        )

        assertEquals(BorrowingStatus.DUE_SOON, borrowing.calculateStatus(now))
    }

    @Test
    fun testOverdueByOneDay() {
        val nowCal = getTodayMidnight()
        val now = nowCal.timeInMillis
        val expectedReturn = nowCal.apply { add(Calendar.DAY_OF_YEAR, -1) }.timeInMillis

        val borrowing = Borrowing(
            itemName = "Calculator",
            borrowerName = "Charlie",
            dateBorrowed = now - 2 * 86_400_000L,
            expectedReturnDate = expectedReturn
        )

        assertEquals(BorrowingStatus.OVERDUE, borrowing.calculateStatus(now))
    }

    @Test
    fun testReturned() {
        val now = System.currentTimeMillis()
        val borrowing = Borrowing(
            itemName = "Headphones",
            borrowerName = "Dave",
            dateBorrowed = now - 86_400_000L,
            expectedReturnDate = now + 86_400_000L,
            isReturned = true,
            returnedDate = now
        )

        assertEquals(BorrowingStatus.RETURNED, borrowing.calculateStatus(now))
    }

    @Test
    fun testArchived() {
        val now = System.currentTimeMillis()
        val borrowing = Borrowing(
            itemName = "Tool",
            borrowerName = "Eve",
            dateBorrowed = now - 86_400_000L,
            expectedReturnDate = now + 86_400_000L,
            isArchived = true
        )

        assertEquals(BorrowingStatus.ARCHIVED, borrowing.calculateStatus(now))
    }
}
