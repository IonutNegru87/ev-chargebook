package io.github.inegru.chargebook.web

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import io.github.inegru.chargebook.web.api.ChargebookApi
import io.github.inegru.chargebook.web.dashboard.DashboardScreen
import io.github.inegru.chargebook.web.dashboard.DashboardViewModel

@Composable
fun App() {
    val api = remember { ChargebookApi() }
    val viewModel = remember { DashboardViewModel(api) }
    MaterialTheme {
        DashboardScreen(viewModel = viewModel, signInUrl = api.signInUrl)
    }
}
