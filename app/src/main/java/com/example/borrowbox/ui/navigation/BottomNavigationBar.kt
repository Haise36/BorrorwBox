package com.example.borrowbox.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.borrowbox.ui.theme.NavBackground
import com.example.borrowbox.ui.theme.PrimaryPurple

sealed class NavItem(
    val route: String, 
    val label: String, 
    val icon: ImageVector,
    val selectedIcon: ImageVector
) {
    data object Home : NavItem("home", "Home", Icons.Outlined.Home, Icons.Default.Home)
    data object History : NavItem("history", "History", Icons.Outlined.History, Icons.Default.History)
    data object Statistics : NavItem("statistics", "Stats", Icons.Outlined.BarChart, Icons.Default.BarChart)
    data object Admin : NavItem("admin_dashboard", "Admin", Icons.Outlined.AdminPanelSettings, Icons.Default.AdminPanelSettings)
    data object Settings : NavItem("settings", "Settings", Icons.Outlined.Settings, Icons.Default.Settings)
}

@Composable
fun BottomNavigationBar(
    currentRoute: String?, 
    isAdmin: Boolean = false, 
    onNavigate: (String) -> Unit
) {
    val items = if (isAdmin) {
        listOf(NavItem.Home, NavItem.History, NavItem.Statistics, NavItem.Admin, NavItem.Settings)
    } else {
        listOf(NavItem.Home, NavItem.History, NavItem.Statistics, NavItem.Settings)
    }
    
    NavigationBar(
        tonalElevation = 8.dp,
        containerColor = NavBackground,
        contentColor = Color.White
    ) {
        items.forEach { item ->
            val isSelected = currentRoute == item.route
            NavigationBarItem(
                selected = isSelected,
                onClick = { onNavigate(item.route) },
                icon = { 
                    Icon(
                        imageVector = if (isSelected) item.selectedIcon else item.icon, 
                        contentDescription = item.label 
                    ) 
                },
                label = { Text(item.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.White,
                    selectedTextColor = Color.White,
                    indicatorColor = PrimaryPurple,
                    unselectedIconColor = Color.Gray,
                    unselectedTextColor = Color.Gray
                )
            )
        }
    }
}
