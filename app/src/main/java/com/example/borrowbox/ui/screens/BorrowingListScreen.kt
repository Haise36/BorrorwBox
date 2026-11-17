package com.example.borrowbox.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.borrowbox.data.model.Borrowing
import com.example.borrowbox.data.model.BorrowingStatus
import com.example.borrowbox.data.model.BorrowingConstants
import com.example.borrowbox.ui.components.EmptyStateView
import com.example.borrowbox.viewmodel.MainViewModel
import com.example.borrowbox.viewmodel.SortOrder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BorrowingListScreen(
    vm: MainViewModel,
    initialFilter: String,
    onBack: () -> Unit,
    onItemClick: (Borrowing) -> Unit
) {
    val searchQuery by vm.searchQuery.collectAsState()
    val activeBorrowings by vm.activeBorrowings.collectAsState()
    val allBorrowings by vm.allBorrowings.collectAsState()
    val statusFilter by vm.statusFilter.collectAsState()
    val categoryFilter by vm.categoryFilter.collectAsState()
    val currentSort by vm.sortOrder.collectAsState()

    var sortExpanded by remember { mutableStateOf(false) }

    val displayList = if (statusFilter == null && initialFilter == "all") allBorrowings else activeBorrowings

    var borrowingToReturn by remember { mutableStateOf<Borrowing?>(null) }

    LaunchedEffect(initialFilter) {
        when (initialFilter) {
            "borrowed" -> vm.updateStatusFilter(BorrowingStatus.ACTIVE)
            "overdue" -> vm.updateStatusFilter(BorrowingStatus.OVERDUE)
            "due_soon" -> vm.updateStatusFilter(BorrowingStatus.DUE_SOON)
            "due_today" -> vm.updateStatusFilter(BorrowingStatus.DUE_TODAY)
            "all" -> vm.updateStatusFilter(null)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        when(initialFilter) {
                            "borrowed" -> "Active Borrowings"
                            "overdue" -> "Overdue Items"
                            "due_soon" -> "Returning Soon"
                            "due_today" -> "Due Today"
                            else -> "All Records"
                        }
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { sortExpanded = true }) {
                            Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort")
                        }
                        DropdownMenu(
                            expanded = sortExpanded,
                            onDismissRequest = { sortExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Due Date ${if (currentSort == SortOrder.DUE_DATE) "✓" else ""}") },
                                onClick = { vm.updateSort(SortOrder.DUE_DATE); sortExpanded = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Date Borrowed ${if (currentSort == SortOrder.DATE_BORROWED) "✓" else ""}") },
                                onClick = { vm.updateSort(SortOrder.DATE_BORROWED); sortExpanded = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Item Name ${if (currentSort == SortOrder.ITEM_NAME) "✓" else ""}") },
                                onClick = { vm.updateSort(SortOrder.ITEM_NAME); sortExpanded = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Borrower Name ${if (currentSort == SortOrder.BORROWER_NAME) "✓" else ""}") },
                                onClick = { vm.updateSort(SortOrder.BORROWER_NAME); sortExpanded = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Recently Added ${if (currentSort == SortOrder.RECENTLY_ADDED) "✓" else ""}") },
                                onClick = { vm.updateSort(SortOrder.RECENTLY_ADDED); sortExpanded = false }
                            )
                        }
                    }
                }
            )
        }
    ) { p ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(p)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { vm.updateSearch(it) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search by item or borrower...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                shape = MaterialTheme.shapes.medium,
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
            )

            // Status Filter Row
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = statusFilter == null,
                        onClick = { vm.updateStatusFilter(null) },
                        label = { Text("All Status") }
                    )
                }
                val filters = listOf(BorrowingStatus.ACTIVE, BorrowingStatus.DUE_TODAY, BorrowingStatus.DUE_SOON, BorrowingStatus.OVERDUE)
                items(filters) { status ->
                    FilterChip(
                        selected = statusFilter == status,
                        onClick = { vm.updateStatusFilter(if (statusFilter == status) null else status) },
                        label = { Text(status.name.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }) }
                    )
                }
            }

            // Category Filter Row
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = categoryFilter == null,
                        onClick = { vm.updateCategoryFilter(null) },
                        label = { Text("All Categories") }
                    )
                }
                items(BorrowingConstants.categories) { cat ->
                    FilterChip(
                        selected = categoryFilter == cat,
                        onClick = { vm.updateCategoryFilter(if (categoryFilter == cat) null else cat) },
                        label = { Text(cat) }
                    )
                }
            }

            if (displayList.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(displayList) { item ->
                        SwipeableBorrowingCard(
                            borrowing = item,
                            onClick = { onItemClick(item) },
                            onMarkAsReturned = { borrowingToReturn = item },
                            onArchive = { vm.toggleArchived(item) }
                        )
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            } else {
                val emptyTitle = when {
                    statusFilter == BorrowingStatus.OVERDUE -> "No overdue items"
                    statusFilter == BorrowingStatus.DUE_SOON -> "No items due soon"
                    statusFilter == BorrowingStatus.DUE_TODAY -> "No active due today"
                    statusFilter == BorrowingStatus.ACTIVE -> "No active borrowings"
                    initialFilter == "all" && statusFilter == null -> "No total loans"
                    categoryFilter != null -> "No records in $categoryFilter"
                    else -> "No results found"
                }
                EmptyStateView(
                    icon = Icons.Default.SearchOff,
                    title = emptyTitle,
                    subtitle = "Try adjusting your filters or search terms."
                )
            }
        }
    }

    if (borrowingToReturn != null) {
        AlertDialog(
            onDismissRequest = { borrowingToReturn = null },
            title = { Text("Mark as Returned?") },
            text = { Text("Move '${borrowingToReturn?.itemName}' to history?") },
            confirmButton = {
                Button(onClick = {
                    borrowingToReturn?.let { vm.markReturned(it) }
                    borrowingToReturn = null
                }) { Text("Confirm") }
            },
            dismissButton = {
                TextButton(onClick = { borrowingToReturn = null }) { Text("Cancel") }
            }
        )
    }
}
