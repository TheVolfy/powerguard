package com.powerguard.app.ui

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.powerguard.app.R
import com.powerguard.app.ui.navigation.AppNavHost
import com.powerguard.app.ui.navigation.Screen
import com.powerguard.app.ui.theme.PowerGuardTheme
import com.powerguard.app.util.BirthdayPrefs
import com.powerguard.app.util.wrapWithSavedLocale

class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(newBase.wrapWithSavedLocale())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val activity = this
        setContent {
            PowerGuardTheme {
                var showBirthday by remember { mutableStateOf(!BirthdayPrefs.isShown(activity)) }
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                data class NavItem(val screen: Screen, val icon: androidx.compose.ui.graphics.vector.ImageVector, val labelRes: Int)

                val navItems = listOf(
                    NavItem(Screen.Dashboard, Icons.Filled.Home, R.string.nav_dashboard),
                    NavItem(Screen.Features, Icons.Filled.Tune, R.string.nav_features),
                    NavItem(Screen.Log, Icons.AutoMirrored.Filled.List, R.string.nav_log),
                    NavItem(Screen.Settings, Icons.Filled.Settings, R.string.nav_settings),
                )

                Box(modifier = androidx.compose.ui.Modifier.fillMaxSize()) {
                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            navItems.forEach { item ->
                                val selected = currentDestination?.hierarchy
                                    ?.any { it.route == item.screen.route } == true
                                NavigationBarItem(
                                    icon = { Icon(item.icon, contentDescription = stringResource(item.labelRes)) },
                                    label = { Text(stringResource(item.labelRes)) },
                                    selected = selected,
                                    onClick = {
                                        navController.navigate(item.screen.route) {
                                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                )
                            }
                        }
                    },
                ) { innerPadding ->
                    // fillMaxSize() is required so the Box fills the Scaffold content area and
                    // passes correct bounded constraints down to NavHost and each screen.
                    // Without it, the Box collapses to zero height and verticalScroll stops working.
                    Box(modifier = androidx.compose.ui.Modifier
                        .fillMaxSize()
                        .padding(innerPadding)) {
                        AppNavHost(navController)
                    }
                }

                if (showBirthday) {
                    BirthdayOverlay(onDismiss = {
                        BirthdayPrefs.markShown(activity)
                        showBirthday = false
                    })
                }
                } // end Box
            }
        }
    }
}
