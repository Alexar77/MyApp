package com.example.habittracker.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.habittracker.ui.icons.AppIcons
import com.example.habittracker.ui.screens.BirthdaysScreen
import com.example.habittracker.ui.screens.GoalsScreen
import com.example.habittracker.ui.screens.MainScreen
import com.example.habittracker.ui.screens.MotivationScreen
import com.example.habittracker.ui.screens.NotificationsScreen
import com.example.habittracker.ui.screens.StatsScreen
import com.example.habittracker.ui.screens.TasksScreen
import com.example.habittracker.ui.screens.WhoAmIScreen

data class BottomRoute(
    val route: String,
    val label: String,
    val icon: ImageVector
)

object Routes {
    val Today = BottomRoute("today", "Today", Icons.Default.Home)
    val Tasks = BottomRoute("tasks", "Tasks", Icons.AutoMirrored.Filled.List)
    val Goals = BottomRoute("goals", "Goals", AppIcons.Flag)
    val Journal = BottomRoute("journal", "Journal", Icons.Default.Person)

    const val Motivation = "motivation"
    const val Birthdays = "birthdays"
    const val Notifications = "notifications"
    const val Stats = "stats"
    const val StatsRoute = "stats/{habitId}"

    val BottomItems = listOf(Today, Tasks, Goals, Journal)
}

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute in Routes.BottomItems.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    Routes.BottomItems.forEach { item ->
                        NavigationBarItem(
                            selected = currentRoute == item.route,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.Today.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Routes.Today.route) {
                MainScreen(
                    onOpenBirthdays = { navController.navigate(Routes.Birthdays) },
                    onOpenNotifications = { navController.navigate(Routes.Notifications) },
                    onOpenMotivation = { navController.navigate(Routes.Motivation) },
                    onOpenTasks = {
                        navController.navigate(Routes.Tasks.route) {
                            launchSingleTop = true
                        }
                    },
                    onOpenStats = { habitId ->
                        navController.navigate("${Routes.Stats}/$habitId")
                    }
                )
            }
            composable(Routes.Tasks.route) { TasksScreen() }
            composable(Routes.Goals.route) { GoalsScreen() }
            composable(Routes.Journal.route) { WhoAmIScreen() }
            composable(Routes.Motivation) {
                MotivationScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.Birthdays) {
                BirthdaysScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.Notifications) {
                NotificationsScreen(onBack = { navController.popBackStack() })
            }
            composable(
                route = Routes.StatsRoute,
                arguments = listOf(navArgument("habitId") { type = NavType.LongType })
            ) {
                StatsScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
