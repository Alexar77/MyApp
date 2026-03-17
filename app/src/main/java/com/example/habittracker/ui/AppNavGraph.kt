package com.example.habittracker.ui

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
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
import com.example.habittracker.ui.screens.MoneyScreen
import com.example.habittracker.ui.screens.MotivationScreen
import com.example.habittracker.ui.screens.NotificationsScreen
import com.example.habittracker.ui.screens.TasksScreen
import com.example.habittracker.ui.screens.WeightScreen
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
    const val MoneyRoute = "money"
    const val WeightRoute = "weight"
    val BottomItems = listOf(Home, WhoAmI, Motivation, Tasks, Goals)
    val DrawerItems = listOf(
        Home,
        WhoAmI,
        Motivation,
        Tasks,
        Goals,
        BottomRoute(MoneyRoute, "Money", AppIcons.Euro),
        BottomRoute(WeightRoute, "Weight", AppIcons.RadioButtonUnchecked),
        BottomRoute(BirthdaysRoute, "Birthdays", AppIcons.Cake),
        BottomRoute(NotificationsRoute, "Notifications", Icons.Default.Notifications)
    )
}

@Composable
fun AppNavGraph() {
    val context = LocalContext.current
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val drawerItems = remember {
        mutableStateListOf<BottomRoute>().apply {
            addAll(loadDrawerItems(context))
        }
    }

    fun openDrawer() {
        scope.launch { drawerState.open() }
    }

    fun navigateToTopLevel(route: String) {
        if (currentRoute == route) return
        if (route == Routes.Home.route) {
            val popped = navController.popBackStack(Routes.Home.route, false)
            if (!popped || navController.currentDestination?.route != Routes.Home.route) {
                navController.navigate(Routes.Home.route) {
                    popUpTo(navController.graph.startDestinationId) { inclusive = false }
                    launchSingleTop = true
                    restoreState = true
                }
            }
            return
        }
        navController.navigate(route) {
            popUpTo(navController.graph.startDestinationId) {
                saveState = true
                inclusive = false
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawerContent(
                currentRoute = currentRoute,
                drawerState = drawerState,
                items = drawerItems,
                onReorder = { reorderedItems ->
                    drawerItems.clear()
                    drawerItems.addAll(reorderedItems)
                    saveDrawerItems(context, reorderedItems)
                },
                onNavigate = ::navigateToTopLevel
            )
        }
    ) {
        Scaffold(
            bottomBar = {
                NavigationBar {
                    Routes.BottomItems.forEach { item ->
                        NavigationBarItem(
                            selected = currentRoute == item.route,
                            onClick = { navigateToTopLevel(item.route) },
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
                        onOpenMenu = ::openDrawer
                    )
                }
                composable(Routes.WhoAmI.route) { WhoAmIScreen(onOpenMenu = ::openDrawer) }
                composable(Routes.Motivation.route) { MotivationScreen(onOpenMenu = ::openDrawer) }
                composable(Routes.Tasks.route) { TasksScreen(onOpenMenu = ::openDrawer) }
                composable(Routes.MoneyRoute) { MoneyScreen(onOpenMenu = ::openDrawer) }
                composable(Routes.WeightRoute) { WeightScreen(onOpenMenu = ::openDrawer) }
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
    items: List<BottomRoute>,
    onReorder: (List<BottomRoute>) -> Unit,
    onNavigate: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var draggingRoute by remember { mutableStateOf<String?>(null) }

    ModalDrawerSheet {
        Text(
            text = "Navigate",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp)
        )
        items.forEach { item ->
            key(item.route) {
                var dragOffsetY by remember(item.route) { mutableFloatStateOf(0f) }
                var itemHeightPx by remember(item.route) { mutableFloatStateOf(0f) }
                val isDragging = draggingRoute == item.route
                NavigationDrawerItem(
                    label = { Text(item.label) },
                    selected = currentRoute == item.route,
                    onClick = {
                        onNavigate(item.route)
                        scope.launch { drawerState.close() }
                    },
                    icon = { Icon(item.icon, contentDescription = item.label) },
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                        .graphicsLayer {
                            translationY = dragOffsetY
                            alpha = if (isDragging) 0.85f else 1f
                        }
                        .zIndex(if (isDragging) 1f else 0f)
                        .onSizeChanged { size ->
                            itemHeightPx = size.height.toFloat().coerceAtLeast(1f)
                        }
                        .pointerInput(item.route, items) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    dragOffsetY = 0f
                                    draggingRoute = item.route
                                },
                                onDragEnd = {
                                    dragOffsetY = 0f
                                    draggingRoute = null
                                },
                                onDragCancel = {
                                    dragOffsetY = 0f
                                    draggingRoute = null
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    dragOffsetY += dragAmount.y
                                    val reorderStepPx = itemHeightPx
                                    while (dragOffsetY > reorderStepPx) {
                                        val currentIndex = items.indexOfFirst { it.route == item.route }
                                        if (currentIndex in 0 until items.lastIndex) {
                                            val reordered = items.toMutableList().apply {
                                                add(currentIndex + 1, removeAt(currentIndex))
                                            }
                                            onReorder(reordered)
                                        }
                                        dragOffsetY -= reorderStepPx
                                    }
                                    while (dragOffsetY < -reorderStepPx) {
                                        val currentIndex = items.indexOfFirst { it.route == item.route }
                                        if (currentIndex > 0) {
                                            val reordered = items.toMutableList().apply {
                                                add(currentIndex - 1, removeAt(currentIndex))
                                            }
                                            onReorder(reordered)
                                        }
                                        dragOffsetY += reorderStepPx
                                    }
                                }
                            )
                        }
                )
            }
        }
    }
}

private const val DRAWER_ORDER_PREF = "drawer_order"

private fun loadDrawerItems(context: android.content.Context): List<BottomRoute> {
    val prefs = context.getSharedPreferences("myapp_prefs", android.content.Context.MODE_PRIVATE)
    val savedRoutes = prefs.getString(DRAWER_ORDER_PREF, null)
        ?.split("|")
        ?.map { it.trim() }
        ?.filter { it.isNotEmpty() }
        .orEmpty()
    if (savedRoutes.isEmpty()) return Routes.DrawerItems

    val itemsByRoute = Routes.DrawerItems.associateBy { it.route }
    val ordered = savedRoutes.mapNotNull { itemsByRoute[it] }.toMutableList()
    Routes.DrawerItems.forEach { item ->
        if (ordered.none { it.route == item.route }) {
            ordered.add(item)
        }
    }
    return ordered
}

private fun saveDrawerItems(
    context: android.content.Context,
    items: List<BottomRoute>
) {
    val prefs = context.getSharedPreferences("myapp_prefs", android.content.Context.MODE_PRIVATE)
    prefs.edit()
        .putString(DRAWER_ORDER_PREF, items.joinToString("|") { it.route })
        .apply()
}
