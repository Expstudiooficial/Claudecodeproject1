package com.expstudio.localai.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.expstudio.localai.agent.AgentMode
import com.expstudio.localai.data.model.InferenceParams
import com.expstudio.localai.smart.DeviceProfile
import com.expstudio.localai.smart.SmartRecommendation
import com.expstudio.localai.ui.components.FloatParam
import com.expstudio.localai.ui.components.IntParam
import com.expstudio.localai.ui.vm.AppViewModelFactory
import com.expstudio.localai.ui.vm.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    vm: SettingsViewModel = viewModel(factory = AppViewModelFactory),
) {
    val device by vm.device.collectAsStateSafe()
    val recommendation by vm.recommendation.collectAsStateSafe()
    val cacheBytes by vm.cacheBytes.collectAsStateSafe()
    val settings by vm.settings.collectAsStateSafe()

    Scaffold(topBar = { TopAppBar(title = { Text("Settings") }) }) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // ---- The headline Smart Setup button ----
            Card {
                Column(Modifier.padding(16.dp)) {
                    Text("Smart Setup", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        "One tap configures the best model and settings for THIS device — " +
                            "based on the RAM that's actually free right now, leaving headroom " +
                            "for the system and your other apps.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                    FilledTonalButton(
                        onClick = { vm.runSmartSetup() },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null)
                        Text("  Auto-configure for my device")
                    }
                }
            }

            // ---- Manual default generation settings ----
            ManualSettingsCard(
                defaults = settings.defaults,
                maxCores = device?.cpuCores ?: 8,
                onChange = { vm.updateDefaults(it) },
            )

            // ---- Agent configuration ----
            AgentSettingsCard(
                enabled = settings.agentEnabled,
                mode = settings.agentMode,
                onEnabledChange = { vm.setAgentEnabled(it) },
                onModeChange = { vm.setAgentMode(it) },
            )

            // ---- System info dashboard ----
            SystemInfoCard(device, cacheBytes, vm.backendLabel) { vm.refreshSystemInfo() }
        }
    }

    recommendation?.let { rec ->
        SmartResultDialog(rec, onDismiss = { vm.dismissRecommendation() })
    }
}

@Composable
private fun ManualSettingsCard(
    defaults: InferenceParams,
    maxCores: Int,
    onChange: (InferenceParams) -> Unit,
) {
    Card {
        Column(Modifier.padding(16.dp)) {
            Text("Default chat settings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "Applied to every new chat. Tune these by hand, or let Smart Setup pick them. " +
                    "You can still override per-chat from inside a conversation.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
            )
            FloatParam("Temperature", defaults.temperature, 0f..2f) {
                onChange(defaults.copy(temperature = it))
            }
            FloatParam("Top-P", defaults.topP, 0f..1f) {
                onChange(defaults.copy(topP = it))
            }
            FloatParam("Repeat penalty", defaults.repeatPenalty, 1f..2f) {
                onChange(defaults.copy(repeatPenalty = it))
            }
            IntParam("Context window", defaults.contextWindow, 512..32768, steps = 0) {
                // snap to 512-token increments for sane values
                onChange(defaults.copy(contextWindow = (it / 512).coerceAtLeast(1) * 512))
            }
            IntParam("Max tokens / reply", defaults.maxTokens, 64..4096) {
                onChange(defaults.copy(maxTokens = it))
            }
            IntParam("CPU threads", defaults.threads.coerceIn(1, maxCores), 1..maxCores) {
                onChange(defaults.copy(threads = it))
            }
            IntParam("Batch size", defaults.batchSize, 32..1024) {
                onChange(defaults.copy(batchSize = it))
            }
        }
    }
}

@Composable
private fun AgentSettingsCard(
    enabled: Boolean,
    mode: AgentMode,
    onEnabledChange: (Boolean) -> Unit,
    onModeChange: (AgentMode) -> Unit,
) {
    Card {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Agent mode", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "Let the AI act on your device — read/write files, list & open apps, " +
                            "run commands. Grant permissions when prompted.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = enabled, onCheckedChange = onEnabledChange)
            }
            if (enabled) {
                Spacer(Modifier.height(8.dp))
                AgentMode.entries.forEach { m ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .selectable(selected = m == mode, onClick = { onModeChange(m) })
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        RadioButton(selected = m == mode, onClick = { onModeChange(m) })
                        Column(Modifier.padding(start = 4.dp)) {
                            Text(m.label, fontWeight = FontWeight.Medium)
                            Text(
                                m.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SystemInfoCard(
    device: DeviceProfile?,
    cacheBytes: Long,
    backend: String,
    onRefresh: () -> Unit,
) {
    Card {
        Column(Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "System info",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onRefresh) { Text("Refresh") }
            }
            if (device == null) {
                Text("Reading device info…")
            } else {
                InfoRow("Device", device.deviceModel)
                InfoRow("CPU cores", device.cpuCores.toString())
                InfoRow("Architecture", device.primaryAbi)
                InfoRow("Total RAM", "${device.totalRamMb} MB")
                InfoRow("Available RAM", "${device.availableRamMb} MB")
                InfoRow("In use (OS + apps)", "${device.usedRamMb} MB")
                InfoRow("Low-RAM device", if (device.isLowRamDevice) "Yes" else "No")
            }
            InfoRow("Inference backend", backend)
            InfoRow("Model cache", "%.2f GB".format(cacheBytes / 1_073_741_824.0))
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun SmartResultDialog(rec: SmartRecommendation, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Got it") } },
        title = { Text("Smart Setup results") },
        text = {
            Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                Text(rec.model.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    if (rec.isInstalled) "Ready to use (installed)"
                    else "Recommended — download it from the Models tab",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
                Spacer(Modifier.height(8.dp))
                InfoRow("Context window", "${rec.params.contextWindow} tokens")
                InfoRow("Threads", rec.params.threads.toString())
                InfoRow("Batch size", rec.params.batchSize.toString())
                InfoRow("Max tokens", rec.params.maxTokens.toString())
                InfoRow("Est. speed", "%.1f tok/s".format(rec.estimatedTokensPerSecond))
                Spacer(Modifier.height(8.dp))
                Text("Why these settings:", fontWeight = FontWeight.Bold)
                rec.reasoning.forEach { line ->
                    Text("• $line", style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp))
                }
            }
        },
    )
}
