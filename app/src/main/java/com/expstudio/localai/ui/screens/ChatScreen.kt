package com.expstudio.localai.ui.screens

import android.widget.Toast
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.expstudio.localai.agent.AgentMode
import com.expstudio.localai.agent.AgentPermissions
import com.expstudio.localai.agent.AgentTool
import com.expstudio.localai.agent.PendingApproval
import com.expstudio.localai.data.db.entities.ChatMessage
import com.expstudio.localai.data.db.entities.ChatSession
import com.expstudio.localai.data.db.entities.InstalledModel
import com.expstudio.localai.data.db.entities.Role
import com.expstudio.localai.data.model.InferenceParams
import com.expstudio.localai.data.settings.AgentToolPermissions
import com.expstudio.localai.ui.components.FloatParam
import com.expstudio.localai.ui.components.IntParam
import com.expstudio.localai.ui.components.SwitchParam
import com.expstudio.localai.ui.vm.AppViewModelFactory
import com.expstudio.localai.ui.vm.ChatViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    sessionId: Long?,
    onBack: () -> Unit,
    vm: ChatViewModel = viewModel(factory = AppViewModelFactory),
) {
    LaunchedEffect(sessionId) { sessionId?.let { vm.openSession(it) } }

    val messages by vm.messages.collectAsStateSafe()
    val streaming by vm.streaming.collectAsStateSafe()
    val agentEnabled by vm.agentEnabled.collectAsStateSafe()
    val agentMode by vm.agentMode.collectAsStateSafe()
    val agentPermissions by vm.agentPermissions.collectAsStateSafe()
    val pendingApproval by vm.pendingApproval.collectAsStateSafe()
    val session by vm.currentSession.collectAsStateSafe()
    val installed by vm.installedModels.collectAsStateSafe()
    val showTimestamps by vm.showTimestamps.collectAsStateSafe()
    val fontScale by vm.fontScale.collectAsStateSafe()
    val loadingModel by vm.loadingModel.collectAsStateSafe()

    var input by remember { mutableStateOf("") }
    var showSettings by remember { mutableStateOf(false) }
    var showAgent by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    // Auto-scroll to the newest content as it streams in.
    LaunchedEffect(messages.size, streaming) {
        val target = messages.size + if (streaming != null) 1 else 0
        if (target > 0) listState.animateScrollToItem(target - 1)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(session?.title ?: "Chat", maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (agentEnabled) {
                        IconButton(onClick = { showAgent = true }) {
                            Icon(
                                Icons.Filled.SmartToy,
                                contentDescription = "Agent options",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Filled.Tune, contentDescription = "Chat settings")
                    }
                },
            )
        },
        bottomBar = {
            InputBar(
                value = input,
                onValueChange = { input = it },
                isGenerating = streaming != null,
                agentMode = agentEnabled,
                onSend = { if (input.isNotBlank()) { vm.send(input); input = "" } },
                onStop = { vm.stopGeneration() },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // The visible Chat ⇄ Agent switch.
            ModeSwitch(
                agentEnabled = agentEnabled,
                onSelect = { agent ->
                    vm.setAgentEnabled(agent)
                    if (agent) showAgent = true
                },
            )

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(messages, key = { it.id }) { msg ->
                    MessageBubble(msg, showTimestamps, fontScale)
                }
                loadingModel?.let { name ->
                    item(key = "loading") { ModelLoadingCard(name) }
                }
                streaming?.let { partial ->
                    item(key = "streaming") {
                        MessageBubble(
                            ChatMessage(
                                id = -1, sessionId = 0, role = Role.ASSISTANT,
                                content = partial.ifBlank { "…" },
                            ),
                            showTimestamps = false,
                            fontScale = fontScale,
                        )
                    }
                }
            }
        }
    }

    if (showSettings) {
        ChatSettingsSheet(
            session = session,
            installed = installed,
            onSelectModel = { vm.selectModel(it) },
            onApplyParams = { vm.updateSessionParams(it) },
            onDismiss = { showSettings = false },
        )
    }

    if (showAgent) {
        AgentSheet(
            enabled = agentEnabled,
            mode = agentMode,
            permissions = agentPermissions,
            onEnabledChange = { vm.setAgentEnabled(it) },
            onModeChange = { vm.setAgentMode(it) },
            onPermissionChange = { tool, allowed -> vm.setAgentPermission(tool, allowed) },
            onDismiss = { showAgent = false },
        )
    }

    pendingApproval?.let { approval ->
        ApprovalDialog(
            approval = approval,
            onApprove = { vm.resolveApproval(true) },
            onDeny = { vm.resolveApproval(false) },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModeSwitch(agentEnabled: Boolean, onSelect: (Boolean) -> Unit) {
    SingleChoiceSegmentedButtonRow(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        SegmentedButton(
            selected = !agentEnabled,
            onClick = { onSelect(false) },
            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
        ) { Text("💬 Chat") }
        SegmentedButton(
            selected = agentEnabled,
            onClick = { onSelect(true) },
            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
        ) { Text("🤖 Agent") }
    }
}

@Composable
private fun MessageBubble(msg: ChatMessage, showTimestamps: Boolean, fontScale: Float) {
    if (msg.role == Role.SYSTEM) {
        ToolMessage(msg.content, fontScale)
        return
    }
    val isUser = msg.role == Role.USER
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val bubbleColor =
        if (isUser) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceVariant
    val base = MaterialTheme.typography.bodyMedium
    val scaled = base.copy(fontSize = (base.fontSize.value * fontScale).sp)

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = bubbleColor),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.widthIn(max = 320.dp),
        ) {
            Column(Modifier.padding(12.dp)) {
                Text(msg.content, style = scaled)
                if (showTimestamps && msg.id >= 0) {
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            timeFormat.format(Date(msg.timestamp)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(
                            onClick = {
                                clipboard.setText(AnnotatedString(msg.content))
                                Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.height(20.dp),
                        ) {
                            Icon(
                                Icons.Filled.ContentCopy,
                                contentDescription = "Copy",
                                modifier = Modifier.height(16.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Shown while a model is being loaded into memory before the first token. */
@Composable
private fun ModelLoadingCard(name: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Text(
                    "  Loading $name into memory…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(Modifier.fillMaxWidth())
            Text(
                "First load can take a little while on phones — it's cached after this.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

/** Tool-result / agent system message — full width, monospace, tinted. */
@Composable
private fun ToolMessage(content: String, fontScale: Float) {
    val base = MaterialTheme.typography.bodySmall
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            content,
            modifier = Modifier.padding(12.dp),
            style = base.copy(fontSize = (base.fontSize.value * fontScale).sp),
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatSettingsSheet(
    session: ChatSession?,
    installed: List<InstalledModel>,
    onSelectModel: (String) -> Unit,
    onApplyParams: (InferenceParams) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, bottom = 32.dp),
        ) {
            Text("Chat settings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

            Spacer(Modifier.height(12.dp))
            Text("Model", style = MaterialTheme.typography.titleMedium)
            if (installed.isEmpty()) {
                Text(
                    "No models installed yet — download one from the Models tab. " +
                        "You can still chat in simulation mode.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    installed.forEach { m ->
                        FilterChip(
                            selected = session?.modelId == m.id,
                            onClick = { onSelectModel(m.id) },
                            label = { Text(m.displayName, maxLines = 1) },
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("Generation parameters", style = MaterialTheme.typography.titleMedium)
            Text(
                "Quick per-chat overrides. The full set lives in Settings.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val s = session
            if (s == null) {
                Text("Open a chat to edit its parameters.")
            } else {
                val params = InferenceParams(
                    temperature = s.temperature,
                    topP = s.topP,
                    repeatPenalty = s.repeatPenalty,
                    contextWindow = s.contextWindow,
                    maxTokens = s.maxTokens,
                )
                FloatParam("Temperature", params.temperature, 0f..2f) {
                    onApplyParams(params.copy(temperature = it))
                }
                FloatParam("Top-P", params.topP, 0f..1f) {
                    onApplyParams(params.copy(topP = it))
                }
                FloatParam("Repeat penalty", params.repeatPenalty, 1f..2f) {
                    onApplyParams(params.copy(repeatPenalty = it))
                }
                IntParam("Context window", params.contextWindow, 512..32768) {
                    onApplyParams(params.copy(contextWindow = (it / 512).coerceAtLeast(1) * 512))
                }
                IntParam("Max tokens / reply", params.maxTokens, 64..4096) {
                    onApplyParams(params.copy(maxTokens = it))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AgentSheet(
    enabled: Boolean,
    mode: AgentMode,
    permissions: AgentToolPermissions,
    onEnabledChange: (Boolean) -> Unit,
    onModeChange: (AgentMode) -> Unit,
    onPermissionChange: (AgentTool, Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, bottom = 32.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Agent mode", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        "Let the AI act on your device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = enabled, onCheckedChange = onEnabledChange)
            }

            if (enabled) {
                Spacer(Modifier.height(12.dp))
                Text("Autonomy", style = MaterialTheme.typography.titleMedium)
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
                Text("Allowed actions", style = MaterialTheme.typography.titleMedium)
                AgentTool.entries.forEach { tool ->
                    SwitchParam(
                        label = tool.label + if (tool.destructive) "  ⚠️" else "",
                        checked = permissions.isAllowed(tool),
                        help = tool.description,
                        onChange = { onPermissionChange(tool, it) },
                    )
                }

                Spacer(Modifier.height(12.dp))
                Text("Storage permission", style = MaterialTheme.typography.titleMedium)
                val hasFiles = AgentPermissions.hasAllFilesAccess()
                Text(
                    if (hasFiles) "✅ Full file access granted."
                    else "⚠️ Only the app's own folder is accessible. Grant full access to let " +
                        "the agent work across your storage.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (!hasFiles) {
                    OutlinedButton(
                        onClick = {
                            val intent = AgentPermissions.allFilesAccessIntent(context)
                            if (intent != null) context.startActivity(intent)
                            else Toast.makeText(
                                context,
                                "Grant storage permission from app info.",
                                Toast.LENGTH_LONG,
                            ).show()
                        },
                        modifier = Modifier.padding(top = 8.dp),
                    ) { Text("Grant full file access") }
                }
            }
        }
    }
}

@Composable
private fun ApprovalDialog(
    approval: PendingApproval,
    onApprove: () -> Unit,
    onDeny: () -> Unit,
) {
    val call = approval.call
    AlertDialog(
        onDismissRequest = onDeny,
        confirmButton = { TextButton(onClick = onApprove) { Text("Approve") } },
        dismissButton = { TextButton(onClick = onDeny) { Text("Deny") } },
        title = { Text("Agent wants to: ${call.tool.label}") },
        text = {
            Column {
                if (call.tool.destructive) {
                    AssistChip(onClick = {}, label = { Text("⚠️ Destructive") })
                    Spacer(Modifier.height(8.dp))
                }
                Text(call.tool.description, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                Text(
                    call.summary,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                )
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InputBar(
    value: String,
    onValueChange: (String) -> Unit,
    isGenerating: Boolean,
    agentMode: Boolean,
    onSend: () -> Unit,
    onStop: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text(if (agentMode) "Tell the agent what to do…" else "Message") },
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSend = { onSend() }),
            maxLines = 4,
        )
        IconButton(onClick = if (isGenerating) onStop else onSend) {
            if (isGenerating) {
                Icon(Icons.Filled.Stop, contentDescription = "Stop")
            } else {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
            }
        }
    }
}

private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
