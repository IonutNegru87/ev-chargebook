package io.github.inegru.chargebook.web

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import io.github.inegru.chargebook.web.analytics.AnalyticsScreen
import io.github.inegru.chargebook.web.analytics.AnalyticsViewModel
import io.github.inegru.chargebook.web.api.ChargebookApi
import io.github.inegru.chargebook.web.dashboard.DashboardScreen
import io.github.inegru.chargebook.web.dashboard.DashboardViewModel
import io.github.inegru.chargebook.web.sessions.SessionsScreen
import io.github.inegru.chargebook.web.sessions.SessionsViewModel

private enum class Tab(val label: String) {
    Dashboard("Dashboard"),
    Sessions("Sessions"),
    Analytics("Analytics"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    val api = remember { ChargebookApi() }
    val dashboardVm = remember { DashboardViewModel(api) }
    val sessionsVm = remember { SessionsViewModel(api) }
    val analyticsVm = remember { AnalyticsViewModel(api) }

    var current by remember { mutableStateOf(Tab.Dashboard) }

    MaterialTheme {
        Scaffold(
            topBar = {
                TopAppBar(title = { Text("ev-chargebook") })
            },
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                androidx.compose.foundation.layout.Column {
                    TabRow(selectedTabIndex = current.ordinal) {
                        Tab.entries.forEach { tab ->
                            Tab(
                                selected = current == tab,
                                onClick = {
                                    current = tab
                                    when (tab) {
                                        Tab.Sessions -> sessionsVm.refresh()
                                        Tab.Analytics -> analyticsVm.refresh()
                                        Tab.Dashboard -> Unit
                                    }
                                },
                                text = { Text(tab.label) },
                            )
                        }
                    }
                    when (current) {
                        Tab.Dashboard -> DashboardScreen(viewModel = dashboardVm, signInUrl = api.signInUrl)
                        Tab.Sessions -> SessionsScreen(viewModel = sessionsVm)
                        Tab.Analytics -> AnalyticsScreen(viewModel = analyticsVm)
                    }
                }
            }
        }
    }
}
