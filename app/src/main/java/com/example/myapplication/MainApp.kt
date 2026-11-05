package com.example.myapplication

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.navigation3.ui.NavDisplay
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import com.example.myapplication.ui.CallLogScreen
import com.example.myapplication.ui.HomeScreen
import com.example.myapplication.ui.RecipeHubScreen
import com.example.myapplication.ui.ReportDialog
import androidx.compose.foundation.layout.*
import androidx.compose.ui.unit.dp

@Composable
fun MainApp() {
    val backStack = rememberNavBackStack(Destination.Home as NavKey)
    val currentKey = backStack.lastOrNull() ?: Destination.Home
    var showReportDialog by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    
    val entries by com.example.myapplication.data.CallLogRepository.entries.collectAsState()

    androidx.compose.material3.Surface(
        modifier = androidx.compose.ui.Modifier.fillMaxSize().safeDrawingPadding(),
        color = MaterialTheme.colorScheme.background
    ) {
        if (currentKey == Destination.Home) {
            NavDisplay(backStack = backStack) { key ->
            NavEntry(key) {
                HomeScreen(onStart = {
                    backStack.clear()
                    backStack.add(Destination.CallLog)
                })
            }
        }
    } else {
        NavigationSuiteScaffold(
            containerColor = MaterialTheme.colorScheme.background,
            navigationSuiteColors = NavigationSuiteDefaults.colors(
                navigationBarContainerColor = MaterialTheme.colorScheme.surface,
                navigationBarContentColor = MaterialTheme.colorScheme.primary,
                navigationRailContainerColor = MaterialTheme.colorScheme.surface,
                navigationRailContentColor = MaterialTheme.colorScheme.primary
            ),
            navigationSuiteItems = {
                item(
                    selected = currentKey == Destination.CallLog,
                    onClick = {
                        backStack.clear()
                        backStack.add(Destination.CallLog)
                    },
                    icon = { Icon(Icons.Default.Call, contentDescription = "Call Log") },
                    label = { Text("Lista") }
                )
                item(
                    selected = currentKey == Destination.RecipeHub,
                    onClick = {
                        backStack.clear()
                        backStack.add(Destination.RecipeHub)
                    },
                    icon = { Icon(painterResource(id = R.drawable.ic_recipe_hub), contentDescription = "Recipe Hub", modifier = androidx.compose.ui.Modifier.size(24.dp)) },
                    label = { Text("Filtros") }
                )
                item(
                    selected = false,
                    onClick = { showReportDialog = true },
                    icon = { Icon(Icons.Default.Description, contentDescription = "Informe") },
                    label = { Text("Informe") }
                )
                item(
                    selected = false,
                    onClick = {
                        backStack.clear()
                        backStack.add(Destination.Home)
                    },
                    icon = { Icon(Icons.Default.PowerSettingsNew, contentDescription = "Deactivate") },
                    label = { Text("Desactivar") }
                )



            }
        ) {
            NavDisplay(
                backStack = backStack
            ) { key ->
                NavEntry(key) {
                    when (key) {
                        Destination.CallLog -> CallLogScreen()
                        Destination.RecipeHub -> RecipeHubScreen()
                        else -> Text("Unknown")
                    }
                }
            }
        }
    }

    if (showReportDialog) {
        ReportDialog(
            entries = entries,
            onDismiss = { showReportDialog = false }
        )
    }
}
}
