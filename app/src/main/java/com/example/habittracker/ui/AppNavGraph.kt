package com.example.habittracker.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.rememberDrawerState
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.habittracker.ui.icons.AppIcons
import com.example.habittracker.ui.screens.GoalsScreen
import com.example.habittracker.ui.screens.BirthdaysScreen
import com.example.habittracker.ui.screens.MainScreen
import com.example.habittracker.ui.screens.MotivationScreen
import com.example.habittracker.ui.screens.NotificationsScreen
import com.example.habittracker.ui.screens.TasksScreen
import com.example.habittracker.ui.screens.WhoAmIScreen
import kotlinx.coroutines.launch

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
    val Goals = BottomRoute("goals", "Goals", AppIcons.Flag)
    const val BirthdaysRoute = "birthdays"
    const val NotificationsRoute = "notifications"
    val BottomItems = listOf(Home, WhoAmI, Motivation, Tasks, Goals)
    val DrawerItems = listOf(
        Home,
        WhoAmI,
        Motivation,
        Tasks,
        Goals,
        BottomRoute(BirthdaysRoute, "Birthdays", AppIcons.Cake),
        BottomRoute(NotificationsRoute, "Notifications", Icons.Default.Notifications)
    )
}

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    fun openDrawer() {
        scope.launch { drawerState.open() }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawerContent(
                currentRoute = currentRoute,
                drawerState = drawerState,
                onNavigate = { route ->
                    when (route) {
                        Routes.BirthdaysRoute, Routes.NotificationsRoute -> {
                            navController.navigate(route) { launchSingleTop = true }
                        }

                        else -> {
                            if (currentRoute == Routes.BirthdaysRoute && route == Routes.Home.route) {
                                val popped = navController.popBackStack(Routes.Home.route, false)
                                if (!popped) {
                                    navController.navigate(Routes.Home.route) { launchSingleTop = true }
                                }
                            } else {
                                navController.navigate(route) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    }
                }
            )
        }
    ) {
        Scaffold(
            bottomBar = {
                NavigationBar {
                    Routes.BottomItems.forEach { item ->
                        NavigationBarItem(
                            selected = currentRoute == item.route,
                            onClick = {
                                if (currentRoute == Routes.BirthdaysRoute && item.route == Routes.Home.route) {
                                    val popped = navController.popBackStack(Routes.Home.route, false)
                                    if (!popped) {
                                        navController.navigate(Routes.Home.route) { launchSingleTop = true }
                                    }
                                } else {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.startDestinationId) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
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
                composable(Routes.Home.route) {
                    MainScreen(
                        onOpenMenu = ::openDrawer,
                        onOpenBirthdays = {
                            navController.navigate(Routes.BirthdaysRoute) { launchSingleTop = true }
                        },
                        onOpenNotifications = {
                            navController.navigate(Routes.NotificationsRoute) { launchSingleTop = true }
                        }
                    )
                }
                composable(Routes.WhoAmI.route) { WhoAmIScreen(onOpenMenu = ::openDrawer) }
                composable(Routes.Motivation.route) { MotivationScreen(onOpenMenu = ::openDrawer) }
                composable(Routes.Tasks.route) { TasksScreen(onOpenMenu = ::openDrawer) }
                composable(Routes.BirthdaysRoute) {
                    BirthdaysScreen(onOpenMenu = ::openDrawer)
                }
                composable(Routes.NotificationsRoute) {
                    NotificationsScreen(onOpenMenu = ::openDrawer)
                }
                composable(Routes.Goals.route) { GoalsScreen(onOpenMenu = ::openDrawer) }
            }
        }
    }
}

@Composable
private fun AppDrawerContent(
    currentRoute: String?,
    drawerState: DrawerState,
    onNavigate: (String) -> Unit
) {
    val scope = rememberCoroutineScope()

    ModalDrawerSheet {
        Text(
            text = "Navigate",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp)
        )
        Routes.DrawerItems.forEach { item ->
            NavigationDrawerItem(
                label = { Text(item.label) },
                selected = currentRoute == item.route,
                onClick = {
                    onNavigate(item.route)
                    scope.launch { drawerState.close() }
                },
                icon = { Icon(item.icon, contentDescription = item.label) },
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }
    }
}
