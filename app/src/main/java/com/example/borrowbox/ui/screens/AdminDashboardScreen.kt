package com.example.borrowbox.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.borrowbox.data.model.User
import com.example.borrowbox.data.model.UserRole
import com.example.borrowbox.data.model.Borrowing
import com.example.borrowbox.ui.theme.LocalStatusColors
import com.example.borrowbox.viewmodel.AdminViewModel
import com.example.borrowbox.viewmodel.MainViewModel
import com.example.borrowbox.viewmodel.UiEvent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    adminVm: AdminViewModel,
    mainVm: MainViewModel,
    onBack: () -> Unit,
    onBorrowingClick: (Borrowing) -> Unit
) {
    val stats by adminVm.systemStats.collectAsState()
    val users by adminVm.allUsers.collectAsState()
    val globalBorrowings by adminVm.allGlobalBorrowings.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var userToDelete by remember { mutableStateOf<User?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        adminVm.uiEvent.collect { event ->
            when (event) {
                is UiEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
                else -> {}
            }
        }
    }

    val filteredUsers = remember(users, searchQuery) {
        if (searchQuery.isBlank()) users
        else users.filter { 
            it.fullName.contains(searchQuery, ignoreCase = true) || 
            it.username.contains(searchQuery, ignoreCase = true) ||
            it.email.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.AdminPanelSettings,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Admin Dashboard", fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab Row
            PrimaryTabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Overview") },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Users (${users.size})") },
                    icon = { Icon(Icons.Default.People, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Items (${globalBorrowings.size})") },
                    icon = { Icon(Icons.Default.Inventory, contentDescription = null) }
                )
            }

            when (selectedTab) {
                0 -> OverviewTab(stats = stats)
                1 -> UsersTab(
                    users = filteredUsers,
                    searchQuery = searchQuery,
                    onSearchChange = { searchQuery = it },
                    onToggleActive = { adminVm.toggleUserActive(it) },
                    onChangeRole = { user, newRole -> adminVm.changeUserRole(user, newRole) },
                    onDeleteUser = { userToDelete = it }
                )
                2 -> GlobalItemsTab(
                    borrowings = globalBorrowings,
                    onBorrowingClick = onBorrowingClick
                )
            }
        }
    }

    // Confirm Delete User Dialog
    if (userToDelete != null) {
        AlertDialog(
            onDismissRequest = { userToDelete = null },
            title = { Text("Delete User Account") },
            text = { Text("Are you sure you want to delete '${userToDelete?.fullName}' (@${userToDelete?.username})? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        userToDelete?.let { adminVm.deleteUser(it) }
                        userToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { userToDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun OverviewTab(stats: com.example.borrowbox.viewmodel.AdminSystemStats) {
    val statusColors = LocalStatusColors.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "System Metrics",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AdminStatCard(
                    title = "Total Users",
                    value = stats.totalUsers.toString(),
                    subtitle = "${stats.activeUsers} Active",
                    icon = Icons.Default.People,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                AdminStatCard(
                    title = "Total Loans",
                    value = stats.totalBorrowings.toString(),
                    subtitle = "${stats.activeBorrowings} Active",
                    icon = Icons.Default.AllInbox,
                    color = statusColors.info,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AdminStatCard(
                    title = "Overdue Items",
                    value = stats.overdueBorrowings.toString(),
                    subtitle = "Needs Follow-up",
                    icon = Icons.Default.Warning,
                    color = statusColors.error,
                    modifier = Modifier.weight(1f)
                )
                AdminStatCard(
                    title = "Returned",
                    value = stats.returnedBorrowings.toString(),
                    subtitle = "Completed",
                    icon = Icons.Default.CheckCircle,
                    color = statusColors.success,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                shape = MaterialTheme.shapes.large
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Administrator Privileges",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("• You have full authority over user accounts and data.")
                    Text("• Manage roles, toggle user activity, and remove unauthorized accounts.")
                    Text("• Monitor all active, overdue, and returned borrowed items globally across all users.")
                }
            }
        }
    }
}

@Composable
private fun UsersTab(
    users: List<User>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onToggleActive: (User) -> Unit,
    onChangeRole: (User, UserRole) -> Unit,
    onDeleteUser: (User) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search users by name, username, email...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            shape = MaterialTheme.shapes.medium,
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (users.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No users found", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(users) { user ->
                    UserCard(
                        user = user,
                        onToggleActive = { onToggleActive(user) },
                        onChangeRole = { newRole -> onChangeRole(user, newRole) },
                        onDelete = { onDeleteUser(user) }
                    )
                }
            }
        }
    }
}

@Composable
private fun UserCard(
    user: User,
    onToggleActive: () -> Unit,
    onChangeRole: (UserRole) -> Unit,
    onDelete: () -> Unit
) {
    val statusColors = LocalStatusColors.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = user.fullName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "@${user.username} • ${user.email.ifBlank { "No email" }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    color = if (user.role == UserRole.ADMIN) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                    shape = MaterialTheme.shapes.extraSmall
                ) {
                    Text(
                        text = user.role.name,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (user.role == UserRole.ADMIN) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (user.isActive) "Account Active" else "Deactivated",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (user.isActive) statusColors.success else statusColors.error,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = user.isActive,
                        onCheckedChange = { onToggleActive() },
                        modifier = Modifier.scale(0.8f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = {
                            val newRole = if (user.role == UserRole.ADMIN) UserRole.USER else UserRole.ADMIN
                            onChangeRole(newRole)
                        }
                    ) {
                        Icon(
                            imageVector = if (user.role == UserRole.ADMIN) Icons.Default.PersonOff else Icons.Default.Shield,
                            contentDescription = "Toggle Admin Role",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete User",
                            tint = statusColors.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GlobalItemsTab(
    borrowings: List<Borrowing>,
    onBorrowingClick: (Borrowing) -> Unit
) {
    if (borrowings.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No borrowings recorded in system", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(borrowings) { borrowing ->
                BorrowingCard(
                    borrowing = borrowing,
                    onClick = { onBorrowingClick(borrowing) }
                )
            }
        }
    }
}

@Composable
private fun AdminStatCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = MaterialTheme.shapes.large
    ) {
        CountryCardContent(title, value, subtitle, icon, color)
    }
}

@Composable
private fun CountryCardContent(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    color: Color
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Surface(
            color = color.copy(alpha = 0.15f),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(text = title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
