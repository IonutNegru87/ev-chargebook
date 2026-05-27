package io.github.inegru.chargebook.web.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.inegru.chargebook.shared.analytics.MonthlyTotals
import io.github.inegru.chargebook.web.platform.monthName
import io.github.inegru.chargebook.web.platform.round2

@Composable
fun AnalyticsScreen(viewModel: AnalyticsViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Monthly analytics", style = MaterialTheme.typography.headlineSmall)

        when {
            state.isLoading -> Text("Loading…")
            state.needsAuth -> Text("Sign in on the Dashboard tab first.")
            state.errorMessage != null -> Text(
                "Error: ${state.errorMessage}",
                color = MaterialTheme.colorScheme.error,
            )
            state.months.isEmpty() -> Text(
                "No completed sessions yet, so there's nothing to total up.",
                style = MaterialTheme.typography.bodyMedium,
            )
            else -> {
                val maxEnergy = state.months.maxOf { it.energyKwh }.coerceAtLeast(0.001)
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.months) { m -> MonthRow(m, maxEnergy) }
                }
            }
        }
    }
}

@Composable
private fun MonthRow(m: MonthlyTotals, maxEnergy: Double) {
    Card(elevation = CardDefaults.elevatedCardElevation()) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("${monthName(m.month)} ${m.year}", fontWeight = FontWeight.SemiBold)
                Text("${m.sessions} session${if (m.sessions == 1) "" else "s"}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            EnergyBar(fraction = (m.energyKwh / maxEnergy).toFloat().coerceIn(0f, 1f))

            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Stat("energy", "${m.energyKwh.round2()} kWh")
                if (m.solarKwh > 0) Stat("solar", "${m.solarKwh.round2()} kWh")
                Stat("billable", "${m.billableKwh.round2()} kWh")
                Stat("cost", "€${m.costEur.round2()}")
            }
        }
    }
}

@Composable
private fun EnergyBar(fraction: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.primary),
        )
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}
