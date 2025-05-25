package com.github.smugapp.ui.screens

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.github.smugapp.data.DrinkRepo
import com.github.smugapp.network.ble.BluetoothLEConnectionHandler
import com.github.smugapp.network.ble.BluetoothLEDiscoveryHandler
import com.github.smugapp.ui.theme.SmugAppTheme


data class NavItem(
    val label: String, val icon: ImageVector, val route: Any
)

class MainContent(
    mainActivity: ComponentActivity,
    private val bluetoothLEDiscoveryHandler: BluetoothLEDiscoveryHandler,
    private val bluetoothLEConnectionHandler: BluetoothLEConnectionHandler,
    private val repo: DrinkRepo
) {
    init {
        mainActivity.enableEdgeToEdge()
        mainActivity.setContent {
            SmugAppTheme {
                val navController = rememberNavController()

                val navItems = arrayOf(
                    NavItem("Home", Icons.Filled.Home, HomeScreenRoute),
                    NavItem("Scanner", Icons.Filled.Search, BarCodeScannerRoute)
                )

                Scaffold(
                    bottomBar = { CustomBottomBar(navItems, navController) }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = HomeScreenRoute,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable<HomeScreenRoute> {
                            HomeScreenContent(
                                bluetoothLEDiscoveryHandler, bluetoothLEConnectionHandler
                            )
                        }
                        composable<BarCodeScannerRoute> {
                            BarCodeScannerContent(repo)
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun CustomBottomBar(navItems: Array<NavItem>, navController: NavHostController) {
        NavigationBar {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination

            navItems.forEach { item ->
                NavigationBarItem(
                    icon = {
                        Icon(
                            item.icon,
                            contentDescription = item.label
                        )
                    },
                    label = { Text(item.label) },
                    selected = currentDestination?.hierarchy?.any { it.route == item.route::class.qualifiedName } == true ||
                            currentDestination?.route == item.route::class.qualifiedName,
                    onClick = {
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    })
            }
        }
    }
}