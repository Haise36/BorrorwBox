package com.example.borrowbox.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.borrowbox.data.model.UserRole
import com.example.borrowbox.ui.navigation.BottomNavigationBar
import com.example.borrowbox.ui.screens.*
import com.example.borrowbox.viewmodel.AdminViewModel
import com.example.borrowbox.viewmodel.AuthViewModel
import com.example.borrowbox.viewmodel.MainViewModel
import com.example.borrowbox.viewmodel.UiEvent

@Composable
fun BorrowBoxApp(
    vm: MainViewModel,
    authVm: AuthViewModel = viewModel(),
    adminVm: AdminViewModel = viewModel()
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    val currentUser by vm.currentUser.collectAsState()
    val hasNoAdmin by authVm.hasNoAdmin.collectAsState()
    val isAdmin = currentUser?.role == UserRole.ADMIN

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        vm.uiEvent.collect { event ->
            when (event) {
                is UiEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
                is UiEvent.NavigateBack -> navController.popBackStack()
            }
        }
    }

    // Safe session-based navigation check
    LaunchedEffect(currentUser, hasNoAdmin, currentRoute) {
        val route = currentRoute ?: return@LaunchedEffect
        val user = currentUser

        if (hasNoAdmin && route != "initial_admin") {
            navController.navigate("initial_admin") {
                popUpTo(0) { inclusive = true }
            }
        } else if (!hasNoAdmin && route == "initial_admin") {
            navController.navigate(if (user != null) "home" else "login") {
                popUpTo("initial_admin") { inclusive = true }
            }
        } else if (user != null && (route == "login" || route == "register")) {
            navController.navigate("home") {
                popUpTo("login") { inclusive = true }
            }
        } else if (user == null && !hasNoAdmin && route != "login" && route != "register") {
            navController.navigate("login") {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    val bottomBarRoutes = if (isAdmin) {
        listOf("home", "history", "statistics", "admin_dashboard", "settings")
    } else {
        listOf("home", "history", "statistics", "settings")
    }
    val shouldShowBottomBar = currentUser != null && currentRoute in bottomBarRoutes

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (shouldShowBottomBar) {
                BottomNavigationBar(
                    currentRoute = currentRoute,
                    isAdmin = isAdmin
                ) { route ->
                    navController.navigate(route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = "login",
            modifier = Modifier.padding(paddingValues)
        ) {
            composable("initial_admin") {
                InitialAdminScreen(
                    authVm = authVm,
                    onAdminCreated = {
                        navController.navigate("home") {
                            popUpTo("initial_admin") { inclusive = true }
                        }
                    }
                )
            }
            composable("login") {
                LoginScreen(
                    authVm = authVm,
                    onNavigateToRegister = { navController.navigate("register") },
                    onLoginSuccess = {
                        navController.navigate("home") {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                )
            }
            composable("register") {
                RegisterScreen(
                    authVm = authVm,
                    onNavigateToLogin = { navController.popBackStack() },
                    onRegisterSuccess = {
                        navController.navigate("home") {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                )
            }
            composable("home") {
                HomeScreen(
                    vm = vm,
                    onAddBorrowing = { navController.navigate("add_borrowing/0") },
                    onBorrowingClick = { item -> navController.navigate("borrowing_details/${item.id}") },
                    onBorrowersClick = { navController.navigate("borrowers") },
                    onStatClick = { filterType ->
                        if (filterType == "returned") {
                            navController.navigate("history")
                        } else {
                            navController.navigate("borrowing_list/$filterType")
                        }
                    }
                )
            }
            composable("admin_dashboard") {
                if (isAdmin) {
                    AdminDashboardScreen(
                        adminVm = adminVm,
                        mainVm = vm,
                        onBack = { navController.popBackStack() },
                        onBorrowingClick = { item -> navController.navigate("borrowing_details/${item.id}") }
                    )
                } else {
                    LaunchedEffect(Unit) {
                        navController.navigate("home") { popUpTo("home") { inclusive = true } }
                    }
                }
            }
            composable(
                route = "borrowing_list/{filter}",
                arguments = listOf(navArgument("filter") { type = NavType.StringType })
            ) { backStackEntry ->
                val filter = backStackEntry.arguments?.getString("filter") ?: "all"
                BorrowingListScreen(
                    vm = vm,
                    initialFilter = filter,
                    onBack = { 
                        navController.navigate("home") {
                            popUpTo(navController.graph.findStartDestination().id) { inclusive = false }
                            launchSingleTop = true
                        }
                    },
                    onItemClick = { item -> navController.navigate("borrowing_details/${item.id}") }
                )
            }
            composable("borrowers") {
                BorrowersScreen(
                    vm = vm,
                    onBorrowerClick = { name -> navController.navigate("borrower_details/$name") },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                route = "borrower_details/{borrowerName}",
                arguments = listOf(navArgument("borrowerName") { type = NavType.StringType })
            ) { backStackEntry ->
                val name = backStackEntry.arguments?.getString("borrowerName") ?: ""
                BorrowerDetailsScreen(
                    vm = vm,
                    borrowerName = name,
                    onBorrowingClick = { item -> navController.navigate("borrowing_details/${item.id}") },
                    onBack = { navController.popBackStack() }
                )
            }
            composable("history") {
                HistoryScreen(
                    vm = vm,
                    onBorrowingClick = { item -> navController.navigate("borrowing_details/${item.id}") },
                    onBorrowAgain = { item ->
                        navController.currentBackStackEntry?.savedStateHandle?.set("prefill_item_name", item.itemName)
                        navController.currentBackStackEntry?.savedStateHandle?.set("prefill_borrower", item.borrowerName)
                        navController.currentBackStackEntry?.savedStateHandle?.set("prefill_category", item.category)
                        navController.currentBackStackEntry?.savedStateHandle?.set("prefill_contact", item.contactNumber ?: "")
                        navController.navigate("add_borrowing/0")
                    },
                    onBack = {
                        navController.navigate("home") {
                            popUpTo(navController.graph.findStartDestination().id) { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable("statistics") {
                StatisticsScreen(vm = vm)
            }
            composable("settings") {
                SettingsScreen(
                    vm = vm,
                    onNavigateToAdmin = { navController.navigate("admin_dashboard") }
                )
            }
            composable(
                route = "add_borrowing/{borrowingId}",
                arguments = listOf(navArgument("borrowingId") { type = NavType.LongType })
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getLong("borrowingId") ?: 0L
                AddBorrowingScreen(
                    vm = vm,
                    borrowingId = id,
                    onBack = { navController.popBackStack() },
                    onTakePhoto = { navController.navigate("camera") },
                    navController = navController
                )
            }
            composable(
                route = "borrowing_details/{borrowingId}",
                arguments = listOf(navArgument("borrowingId") { type = NavType.LongType })
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getLong("borrowingId") ?: 0L
                BorrowingDetailsScreen(
                    vm = vm,
                    borrowingId = id,
                    onEdit = { borrowingId -> navController.navigate("add_borrowing/$borrowingId") },
                    onBack = { navController.popBackStack() }
                )
            }
            composable("camera") {
                CameraScreen(
                    onPhotoCaptured = { uri ->
                        navController.previousBackStackEntry?.savedStateHandle?.set("photoUri", uri.toString())
                        navController.popBackStack()
                    },
                    onCancel = { navController.popBackStack() }
                )
            }
        }
    }
}
