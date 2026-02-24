package com.example.habittracker.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.habittracker.ui.screens.GoalsScreen
import com.example.habittracker.ui.screens.MainScreen
import com.example.habittracker.ui.screens.MotivationScreen
import com.example.habittracker.ui.screens.TasksScreen
import com.example.habittracker.ui.screens.WhoAmIScreen

data class BottomRoute(
    val route: String,
    val label: String,
    val icon: ImageVector
)

object Routes {
    val Home = BottomRoute("home", "Home", Icons.Default.Home)
    val WhoAmI = BottomRoute("who_am_i", "Who am I?", Icons.Default.Person)
    val Motivation = BottomRoute("motivational", "Motivation", Icons.Default.Star)
    val Tasks = BottomRoute("tasks", "Tasks", Icons.AutoMirrored.Filled.List)
    val Goals = BottomRoute("goals", "Goals", Icons.Default.Flag)
    val BottomItems = listOf(Home, WhoAmI, Motivation, Tasks, Goals)
}

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
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
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.Home.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Routes.Home.route) { MainScreen() }
            composable(Routes.WhoAmI.route) { WhoAmIScreen() }
            composable(Routes.Motivation.route) { MotivationScreen() }
            composable(Routes.Tasks.route) { TasksScreen() }
            composable(Routes.Goals.route) { GoalsScreen() }
        }
    }
}
