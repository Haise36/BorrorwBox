package com.example.borrowbox.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "borrowers")
data class BorrowerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val contactNumber: String?,
    val classSection: String?
)
