package com.example.borrowbox.utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import java.text.SimpleDateFormat
import java.util.*

/**
 * Formats a timestamp into a user-friendly date string.
 */
fun formatTimestamp(time: Long): String {
    return SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(time))
}

/**
 * Returns the appropriate icon for a given category name.
 */
fun getIconForCategory(category: String): ImageVector = when (category) {
    "Books" -> Icons.AutoMirrored.Filled.MenuBook
    "Electronics" -> Icons.Default.Devices
    "School Supplies" -> Icons.Default.School
    "Tools" -> Icons.Default.Build
    "Personal Items" -> Icons.Default.Person
    "Accessories" -> Icons.Default.Headphones
    else -> Icons.AutoMirrored.Filled.List
}
