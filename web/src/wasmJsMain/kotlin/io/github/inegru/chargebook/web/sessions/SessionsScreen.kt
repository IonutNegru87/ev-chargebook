package io.github.inegru.chargebook.web.sessions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.inegru.chargebook.shared.model.ChargingSession
import io.github.inegru.chargebook.shared.model.ChargingSnapshot
import io.github.inegru.chargebook.shared.model.ConnectionType
import io.github.inegru.chargebook.shared.model.SessionWithSnapshots
import io.github.inegru.chargebook.web.platform.formatDateTime
import io.github.inegru.chargebook.web.platform.formatDuration
import io.github.inegru.chargebook.web.platform.minutesBetween
import io.github.inegru.chargebook.web.platform.round1
import io.github.inegru.chargebook.web.platform.round2

@Composable
fun SessionsScreen(viewModel: SessionsViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val showDetail = state.detail != null || state.detailLoading || state.detailError != null
    if (showDetail) {
        SessionDetailView(
            detail = state.detail,
            isLoading = state.detailLoading,
            error = state.detailError,
            onBack = viewModel::closeDetail,
        )
        return
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Charging sessions", style = MaterialTheme.typography.headlineSmall)

        when {
            state.isLoading -> Text("Loading…")
            state.needsAuth -> Text("Sign in on the Dashboard tab first.")
            state.errorMessage != null -> Text(
                "Error: ${state.errorMessage}",
                color = MaterialTheme.colorScheme.error,
            )
            state.sessions.isEmpty() -> Text(
                "No sessions recorded yet. They appear once the car has charged while the poller was running.",
                style = MaterialTheme.typography.bodyMedium,
            )
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.sessions) { session ->
                    SessionRow(session, onClick = { viewModel.openDetail(session.id) })
                }
            }
        }
    }
}

@Composable
private fun SessionRow(session: ChargingSession, onClick: () -> Unit) {
    Card(
        elevation = CardDefaults.elevatedCardElevation(),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(session.startedAt.formatDateTime(), fontWeight = FontWeight.SemiBold)
                Text(session.durationLabel(), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Stat("energy", session.energyKwh?.let { "${it.round2()} kWh" } ?: "—")
                Stat("SoC", socDelta(session))
                Stat("type", session.connectionType.label())
                session.solarKwh?.let { Stat("solar", "${it.round2()} kWh") }
                Stat("cost", session.costEur?.let { "€${it.round2()}" } ?: "—")
            }
        }
    }
}

@Composable
private fun SessionDetailView(
    detail: SessionWithSnapshots?,
    isLoading: Boolean,
    error: String?,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        TextButton(onClick = onBack) { Text("← Back to sessions") }

        when {
            isLoading -> Text("Loading session…")
            error != null -> Text("Error: $error", color = MaterialTheme.colorScheme.error)
            detail != null -> {
                val s = detail.session
                Text(s.startedAt.formatDateTime(), style = MaterialTheme.typography.headlineSmall)
                Card(elevation = CardDefaults.elevatedCardElevation(), modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                    ) {
                        Stat("duration", s.durationLabel())
                        Stat("energy", s.energyKwh?.let { "${it.round2()} kWh" } ?: "—")
                        Stat("SoC", socDelta(s))
                        Stat("avg power", s.avgPowerKw?.let { "${it.round1()} kW" } ?: "—")
                        Stat("peak", s.peakPowerKw?.let { "${it.round1()} kW" } ?: "—")
                        Stat("cost", s.costEur?.let { "€${it.round2()}" } ?: "—")
                    }
                }

                Text(
                    "Snapshots (${detail.snapshots.size})",
                    style = MaterialTheme.typography.titleMedium,
                )
                LazyColumn(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                    items(detail.snapshots) { snap -> SnapshotRow(snap) }
                }
            }
        }
    }
}

@Composable
private fun SnapshotRow(snap: ChargingSnapshot) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text(
                snap.recordedAt.formatDateTime(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text("${snap.socPct ?: "—"}%", style = MaterialTheme.typography.bodyMedium)
            Text(
                snap.powerKw?.let { "${it.round1()} kW" } ?: "—",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                snap.chargingStatus.name.lowercase().replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        HorizontalDivider()
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

private fun ChargingSession.durationLabel(): String {
    val end = endedAt ?: return "ongoing"
    return formatDuration(minutesBetween(startedAt, end))
}

private fun socDelta(session: ChargingSession): String {
    val start = session.startSocPct
    val end = session.endSocPct
    return if (start != null && end != null) "$start% → $end%" else "—"
}

private fun ConnectionType.label(): String = when (this) {
    ConnectionType.AC -> "AC"
    ConnectionType.DC -> "DC"
    ConnectionType.UNKNOWN -> "—"
}
