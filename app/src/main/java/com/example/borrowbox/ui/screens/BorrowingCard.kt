package com.example.borrowbox.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.borrowbox.data.model.*
import com.example.borrowbox.ui.theme.LocalStatusColors
import com.example.borrowbox.ui.components.BorrowingStatusBadge
import com.example.borrowbox.ui.components.BorrowingPhotoView

@Composable
fun BorrowingCard(
    borrowing: Borrowing, 
    onClick: () -> Unit,
    onMarkAsReturned: (() -> Unit)? = null
) {
    val status = borrowing.calculateStatus()
    val statusColors = LocalStatusColors.current
    val statusColor = when (status) {
        BorrowingStatus.ACTIVE -> statusColors.info
        BorrowingStatus.DUE_SOON -> statusColors.warning
        BorrowingStatus.DUE_TODAY -> statusColors.warning
        BorrowingStatus.OVERDUE -> statusColors.error
        BorrowingStatus.RETURNED -> statusColors.success
        BorrowingStatus.ARCHIVED -> MaterialTheme.colorScheme.outline
    }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        border = if (borrowing.isPinned) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)) else null
    ) {
        Column {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BorrowingPhotoView(
                    photoUri = borrowing.photoUri,
                    category = borrowing.category,
                    modifier = Modifier
                        .size(64.dp)
                        .padding(4.dp)
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = borrowing.itemName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (borrowing.isPinned) {
                            Icon(
                                imageVector = Icons.Default.PushPin,
                                contentDescription = "Pinned",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    
                    Text(
                        text = borrowing.borrowerName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BorrowingStatusBadge(status, statusColor)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = borrowing.getStatusLabel(),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (status == BorrowingStatus.OVERDUE) statusColors.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            if (!borrowing.isReturned && !borrowing.isArchived) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    if (onMarkAsReturned != null) {
                        TextButton(
                            onClick = onMarkAsReturned,
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Mark Returned")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableBorrowingCard(
    borrowing: Borrowing,
    onClick: () -> Unit,
    onMarkAsReturned: () -> Unit,
    onArchive: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statusColors = LocalStatusColors.current
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            when (dismissValue) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    onMarkAsReturned()
                    false
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    onArchive()
                    false
                }
                SwipeToDismissBoxValue.Settled -> false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val color = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> statusColors.success
                SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.primary
                else -> Color.Transparent
            }
            val icon = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> Icons.Default.Check
                SwipeToDismissBoxValue.EndToStart -> Icons.Default.Archive
                else -> null
            }
            val label = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> "Return"
                SwipeToDismissBoxValue.EndToStart -> "Archive"
                else -> ""
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color, shape = MaterialTheme.shapes.large)
                    .padding(horizontal = 20.dp),
                contentAlignment = if (direction == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
            ) {
                if (icon != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (direction == SwipeToDismissBoxValue.StartToEnd) {
                            Icon(imageVector = icon, contentDescription = null, tint = Color.White)
                            Spacer(Modifier.width(8.dp))
                            Text(label, color = Color.White, fontWeight = FontWeight.Bold)
                        } else {
                            Text(label, color = Color.White, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(8.dp))
                            Icon(imageVector = icon, contentDescription = null, tint = Color.White)
                        }
                    }
                }
            }
        }
    ) {
        BorrowingCard(
            borrowing = borrowing,
            onClick = onClick,
            onMarkAsReturned = onMarkAsReturned
        )
    }
}
