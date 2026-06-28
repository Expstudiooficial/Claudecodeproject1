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
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.expstudio.localai.agent.AgentMode
import com.expstudio.localai.agent.AgentTool
import com.expstudio.localai.data.model.InferenceParams
import com.expstudio.localai.data.settings.AgentToolPermissions
import com.expstudio.localai.data.settings.AppTheme
import com.expstudio.localai.smart.DeviceProfile
import com.expstudio.localai.smart.SmartRecommendation
import com.expstudio.localai.ui.components.FloatParam
import com.expstudio.localai.ui.components.IntParam
import com.expstudio.localai.ui.components.SwitchParam
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
    val maxCores = device?.cpuCores ?: 8
    val defaults = settings.defaults

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
                        "One tap analyses the RAM that's actually free right now and tunes every " +
                            "setting below for THIS device — leaving headroom for the system and " +
                            "your other apps. Run it, then tap “Apply to my defaults”.",
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

            Text(
                "Manual controls",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "Full hands-on control of every generation knob. These apply to every new chat; " +
                    "you can still override per-chat from inside a conversation.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            SamplingCard(defaults, vm::updateDefaults)
            RepetitionCard(defaults, vm::updateDefaults)
            AdvancedSamplingCard(defaults, vm::updateDefaults)
            RuntimeCard(defaults, maxCores, vm::updateDefaults)
            BehaviorCard(defaults, vm::updateDefaults)

            OutlinedButton(
                onClick = { vm.resetDefaults() },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Reset all parameters to defaults") }

            // ---- Agent configuration ----
            AgentSettingsCard(
                enabled = settings.agentEnabled,
                mode = settings.agentMode,
                permissions = settings.agentPermissions,
                onEnabledChange = { vm.setAgentEnabled(it) },
                onModeChange = { vm.setAgentMode(it) },
                onPermissionChange = { tool, allowed -> vm.setAgentPermission(tool, allowed) },
            )

            // ---- Safety ----
            SectionCard("Safety", "Guardrails so a model can't overload your device.") {
                SwitchParam(
                    "Memory safeguards",
                    settings.memorySafeguards,
                    help = "Block loading a model when free RAM is below its minimum. " +
                        "Turn off to force-load anyway (may crash on low memory).",
                ) { vm.setMemorySafeguards(it) }
                Text(
                    "Only one model is ever held in memory at a time — loading a new one " +
                        "automatically unloads the previous, so there's no overload from " +
                        "stacking models.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            // ---- Appearance ----
            AppearanceCard(
                theme = settings.theme,
                dynamicColor = settings.dynamicColor,
                fontScale = settings.fontScale,
                showTimestamps = settings.showTimestamps,
                onTheme = { vm.setTheme(it) },
                onDynamic = { vm.setDynamicColor(it) },
                onFontScale = { vm.setFontScale(it) },
                onTimestamps = { vm.setShowTimestamps(it) },
            )

            // ---- System info dashboard ----
            SystemInfoCard(device, cacheBytes, vm.backendLabel) { vm.refreshSystemInfo() }
        }
    }

    recommendation?.let { rec ->
        SmartResultDialog(
            rec,
            onApply = { vm.applyRecommendationToDefaults(); vm.dismissRecommendation() },
            onDismiss = { vm.dismissRecommendation() },
        )
    }
}

/** A titled, collapsible settings card. Expanded by default for the first ones. */
@Composable
private fun SectionCard(
    title: String,
    subtitle: String? = null,
    startExpanded: Boolean = true,
    content: @Composable () -> Unit,
) {
    var expanded by remember { mutableStateOf(startExpanded) }
    Card {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth().selectable(selected = expanded) { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (subtitle != null) {
                        Text(
                            subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Icon(
                    if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                )
            }
            if (expanded) {
                Spacer(Modifier.height(8.dp))
                content()
            }
        }
    }
}

@Composable
private fun SamplingCard(p: InferenceParams, onChange: (InferenceParams) -> Unit) {
    SectionCard("Sampling", "How adventurous the model is when picking words.") {
        FloatParam("Temperature", p.temperature, 0f..2f,
            help = "Higher = more creative/random, lower = more focused.") {
            onChange(p.copy(temperature = it))
        }
        FloatParam("Top-P (nucleus)", p.topP, 0f..1f,
            help = "Keep the smallest set of tokens whose probability sums to this.") {
            onChange(p.copy(topP = it))
        }
        IntParam("Top-K", p.topK, 0..200,
            help = "Only sample from the K most likely tokens (0 = disabled).") {
            onChange(p.copy(topK = it))
        }
        FloatParam("Min-P", p.minP, 0f..1f,
            help = "Drop tokens below this fraction of the top token's probability.") {
            onChange(p.copy(minP = it))
        }
        FloatParam("Typical-P", p.typicalP, 0f..1f,
            help = "Locally-typical sampling (1.0 = off).") {
            onChange(p.copy(typicalP = it))
        }
    }
}

@Composable
private fun RepetitionCard(p: InferenceParams, onChange: (InferenceParams) -> Unit) {
    SectionCard("Repetition control", "Stop the model from looping or repeating itself.") {
        FloatParam("Repeat penalty", p.repeatPenalty, 1f..2f,
            help = "Penalise tokens already used (1.0 = off).") {
            onChange(p.copy(repeatPenalty = it))
        }
        FloatParam("Frequency penalty", p.frequencyPenalty, 0f..2f,
            help = "Scales with how often a token has appeared.") {
            onChange(p.copy(frequencyPenalty = it))
        }
        FloatParam("Presence penalty", p.presencePenalty, 0f..2f,
            help = "Flat penalty once a token has appeared at all.") {
            onChange(p.copy(presencePenalty = it))
        }
        IntParam("Repeat last N", p.repeatLastN, 0..2048,
            help = "How many recent tokens the penalties consider.") {
            onChange(p.copy(repeatLastN = it))
        }
    }
}

@Composable
private fun AdvancedSamplingCard(p: InferenceParams, onChange: (InferenceParams) -> Unit) {
    SectionCard(
        "Mirostat & advanced",
        "Adaptive perplexity and other expert knobs.",
        startExpanded = false,
    ) {
        IntParam("Mirostat mode", p.mirostat, 0..2,
            help = "0 = off, 1 = Mirostat v1, 2 = Mirostat v2.") {
            onChange(p.copy(mirostat = it))
        }
        FloatParam("Mirostat tau", p.mirostatTau, 0f..10f,
            help = "Target entropy (creativity setpoint).") {
            onChange(p.copy(mirostatTau = it))
        }
        FloatParam("Mirostat eta", p.mirostatEta, 0f..1f,
            help = "Learning rate for the Mirostat controller.") {
            onChange(p.copy(mirostatEta = it))
        }
        FloatParam("Tail-free Z", p.tfsZ, 0f..1f,
            help = "Tail-free sampling (1.0 = off).") {
            onChange(p.copy(tfsZ = it))
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = if (p.seed < 0) "" else p.seed.toString(),
            onValueChange = { txt ->
                val v = txt.filter { it.isDigit() }.toIntOrNull() ?: -1
                onChange(p.copy(seed = v))
            },
            label = { Text("Seed (blank = random)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun RuntimeCard(p: InferenceParams, maxCores: Int, onChange: (InferenceParams) -> Unit) {
    SectionCard("Performance & runtime", "Memory, speed and hardware usage.") {
        IntParam("Context window", p.contextWindow, 512..32768,
            help = "Tokens of history the model can see. Bigger = more RAM.") {
            onChange(p.copy(contextWindow = (it / 512).coerceAtLeast(1) * 512))
        }
        IntParam("Max tokens / reply", p.maxTokens, 64..4096,
            help = "Upper bound on a single response length.") {
            onChange(p.copy(maxTokens = it))
        }
        IntParam("CPU threads", p.threads.coerceIn(1, maxCores), 1..maxCores,
            help = "More threads = faster, but leave some for the UI.") {
            onChange(p.copy(threads = it))
        }
        IntParam("Batch size", p.batchSize, 32..1024,
            help = "Prompt-processing batch. Smaller uses less RAM.") {
            onChange(p.copy(batchSize = it))
        }
        IntParam("GPU layers", p.gpuLayers, 0..100,
            help = "Layers offloaded to GPU/NPU when supported (0 = CPU only).") {
            onChange(p.copy(gpuLayers = it))
        }
        SwitchParam("Use mmap", p.useMmap,
            help = "Memory-map the model file (faster load, less copy).") {
            onChange(p.copy(useMmap = it))
        }
        SwitchParam("Use mlock", p.useMlock,
            help = "Pin weights in RAM so they're never swapped (needs headroom).") {
            onChange(p.copy(useMlock = it))
        }
        SwitchParam("Flash attention", p.flashAttention,
            help = "Faster attention kernel where supported.") {
            onChange(p.copy(flashAttention = it))
        }
    }
}

@Composable
private fun BehaviorCard(p: InferenceParams, onChange: (InferenceParams) -> Unit) {
    SectionCard("Behaviour", "System prompt, stop strings and streaming.", startExpanded = false) {
        OutlinedTextField(
            value = p.systemPrompt,
            onValueChange = { onChange(p.copy(systemPrompt = it)) },
            label = { Text("Default system prompt") },
            placeholder = { Text("You are a helpful assistant…") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = p.stopSequences,
            onValueChange = { onChange(p.copy(stopSequences = it)) },
            label = { Text("Stop sequences (one per line)") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
        )
        Spacer(Modifier.height(8.dp))
        SwitchParam("Stream responses", p.streamResponses,
            help = "Show tokens as they arrive instead of all at once.") {
            onChange(p.copy(streamResponses = it))
        }
    }
}

@Composable
private fun AgentSettingsCard(
    enabled: Boolean,
    mode: AgentMode,
    permissions: AgentToolPermissions,
    onEnabledChange: (Boolean) -> Unit,
    onModeChange: (AgentMode) -> Unit,
    onPermissionChange: (AgentTool, Boolean) -> Unit,
) {
    Card {
        Column(Modifier.padding(16.dp)) {
            SwitchParam(
                "Agent mode",
                enabled,
                help = "Let the AI act on your device — files, apps, shell commands.",
                onChange = onEnabledChange,
            )
            if (enabled) {
                Spacer(Modifier.height(8.dp))
                Text("Autonomy", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
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

                Spacer(Modifier.height(12.dp))
                Text("Allowed actions", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(
                    "Turn off any capability you don't want the agent to ever use.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                AgentTool.entries.forEach { tool ->
                    SwitchParam(
                        label = tool.label + if (tool.destructive) "  ⚠️" else "",
                        checked = permissions.isAllowed(tool),
                        help = tool.description,
                        onChange = { onPermissionChange(tool, it) },
                    )
                }
            }
        }
    }
}

@Composable
private fun AppearanceCard(
    theme: AppTheme,
    dynamicColor: Boolean,
    fontScale: Float,
    showTimestamps: Boolean,
    onTheme: (AppTheme) -> Unit,
    onDynamic: (Boolean) -> Unit,
    onFontScale: (Float) -> Unit,
    onTimestamps: (Boolean) -> Unit,
) {
    SectionCard("Appearance", "Theme, colours and text size.", startExpanded = false) {
        Text("Theme", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        AppTheme.entries.forEach { t ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .selectable(selected = t == theme, onClick = { onTheme(t) })
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = t == theme, onClick = { onTheme(t) })
                Text(t.label, Modifier.padding(start = 4.dp))
            }
        }
        SwitchParam("Dynamic colour (Material You)", dynamicColor,
            help = "Use wallpaper-based colours on Android 12+.") {
            onDynamic(it)
        }
        FloatParam("Font scale", fontScale, 0.8f..1.6f,
            help = "Scales chat text size.") { onFontScale(it) }
        SwitchParam("Show message timestamps", showTimestamps) { onTimestamps(it) }
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
private fun SmartResultDialog(
    rec: SmartRecommendation,
    onApply: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onApply) { Text("Apply to my defaults") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } },
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
                InfoRow("mlock", if (rec.params.useMlock) "on" else "off")
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
