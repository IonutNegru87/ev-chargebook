package io.github.inegru.chargebook.web.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.inegru.chargebook.web.platform.navigateSameWindow
import io.github.inegru.chargebook.shared.model.ChargingConnectionStatus
import io.github.inegru.chargebook.shared.model.ChargingSnapshot
import io.github.inegru.chargebook.shared.model.ChargingSystemStatus

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    signInUrl: String,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Header(liveStreamActive = state.liveStreamActive)

            when {
                state.isLoading -> LoadingCard()
                state.needsAuth -> SignInCard(signInUrl = signInUrl)
                state.snapshot != null -> SnapshotCard(state.snapshot!!)
                state.errorMessage != null -> ErrorCard(state.errorMessage!!)
                else -> EmptyCard(signInUrl = signInUrl)
            }
        }
    }
}

@Composable
private fun Header(liveStreamActive: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "ev-chargebook",
            style = MaterialTheme.typography.headlineMedium,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(if (liveStreamActive) Color(0xFF34A853) else Color(0xFF9AA0A6)),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = if (liveStreamActive) "live" else "no live stream",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun LoadingCard() {
    Card(elevation = CardDefaults.elevatedCardElevation()) {
        Row(modifier = Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            Spacer(modifier = Modifier.width(12.dp))
            Text("Loading snapshot…")
        }
    }
}

@Composable
private fun SignInCard(signInUrl: String) {
    Card(elevation = CardDefaults.elevatedCardElevation()) {
        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Sign in required", style = MaterialTheme.typography.titleMedium)
            Text(
                "Authorize against your Volvo account so the backend can read your vehicle's charging state.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(onClick = { navigateSameWindow(signInUrl) }) {
                Text("Sign in with Volvo")
            }
        }
    }
}

@Composable
private fun ErrorCard(message: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
    ) {
        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "Couldn't reach the backend",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
    }
}

@Composable
private fun EmptyCard(signInUrl: String) {
    Card(elevation = CardDefaults.elevatedCardElevation()) {
        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("No snapshots yet", style = MaterialTheme.typography.titleMedium)
            Text(
                "The polling loop hasn't persisted anything yet. If you haven't signed in since the backend last restarted, do that first.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(onClick = { navigateSameWindow(signInUrl) }) {
                Text("Sign in with Volvo")
            }
        }
    }
}

@Composable
private fun SnapshotCard(snapshot: ChargingSnapshot) {
    Card(elevation = CardDefaults.elevatedCardElevation()) {
        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                snapshot.vehicleVin,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = snapshot.socPct?.toString() ?: "—",
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = " %",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 12.dp, start = 4.dp),
                )
            }

            LinearProgressIndicator(
                progress = { ((snapshot.socPct ?: 0) / 100f).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Stat("status", snapshot.chargingStatus.label())
                Stat("connection", snapshot.connectionStatus.label())
                Stat("range", snapshot.rangeKm?.let { "$it km" } ?: "—")
                Stat("power", snapshot.powerKw?.let { "${kotlin.math.round(it * 10) / 10} kW" } ?: "—")
                Stat(
                    label = snapshot.targetSocPct?.let { "time to $it%" } ?: "time to full",
                    value = snapshot.estimatedMinutes?.let { formatDuration(it) } ?: "—",
                )
            }

            Text(
                "recorded at ${snapshot.recordedAt}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}

private fun ChargingSystemStatus.label(): String = name.lowercase().replaceFirstChar { it.uppercase() }

/** Formats a minute count as `Xh Ym`, `Ym`, or `Xh` — whichever applies. */
private fun formatDuration(minutes: Int): String {
    if (minutes <= 0) return "0m"
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h == 0 -> "${m}m"
        m == 0 -> "${h}h"
        else -> "${h}h ${m}m"
    }
}
private fun ChargingConnectionStatus.label(): String = when (this) {
    ChargingConnectionStatus.CONNECTED_AC -> "Connected (AC)"
    ChargingConnectionStatus.CONNECTED_DC -> "Connected (DC)"
    ChargingConnectionStatus.DISCONNECTED -> "Disconnected"
    ChargingConnectionStatus.FAULT -> "Fault"
    ChargingConnectionStatus.UNSPECIFIED -> "—"
    ChargingConnectionStatus.UNKNOWN -> "Unknown"
}
