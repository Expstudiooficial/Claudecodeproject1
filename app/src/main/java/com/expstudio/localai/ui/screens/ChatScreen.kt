package com.expstudio.localai.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.expstudio.localai.agent.AgentMode
import com.expstudio.localai.agent.AgentPermissions
import com.expstudio.localai.agent.PendingApproval
import com.expstudio.localai.data.db.entities.ChatMessage
import com.expstudio.localai.data.db.entities.ChatSession
import com.expstudio.localai.data.db.entities.InstalledModel
import com.expstudio.localai.data.db.entities.Role
import com.expstudio.localai.data.model.InferenceParams
import com.expstudio.localai.ui.components.FloatParam
import com.expstudio.localai.ui.components.IntParam
import com.expstudio.localai.ui.util.collectAsStateSafe
import com.expstudio.localai.ui.vm.AppViewModelFactory
import com.expstudio.localai.ui.vm.ChatViewModel

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
    val pendingApproval by vm.pendingApproval.collectAsStateSafe()
    val session by vm.currentSession.collectAsStateSafe()
    val installed by vm.installedModels.collectAsStateSafe()

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
                title = { Text(if (agentEnabled) "Chat · Agent" else "Chat", maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showAgent = true }) {
                        Icon(
                            Icons.Filled.SmartToy,
                            contentDescription = "Agent",
                            tint = if (agentEnabled) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
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
                onSend = { if (input.isNotBlank()) { vm.send(input); input = "" } },
                onStop = { vm.stopGeneration() },
            )
        },
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(messages, key = { it.id }) { msg -> MessageBubble(msg.role, msg.content) }
            streaming?.let { partial ->
                item(key = "streaming") { MessageBubble(Role.ASSISTANT, partial.ifBlank { "…" }) }
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
            onEnabledChange = { vm.setAgentEnabled(it) },
            onModeChange = { vm.setAgentMode(it) },
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

@Composable
private fun MessageBubble(role: Role, content: String) {
    if (role == Role.SYSTEM) {
        ToolMessage(content)
        return
    }
    val isUser = role == Role.USER
    val bubbleColor =
        if (isUser) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceVariant
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = bubbleColor),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.widthIn(max = 320.dp),
        ) {
            Text(content, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/** Tool-result / agent system message — full width, monospace, tinted. */
@Composable
private fun ToolMessage(content: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            content,
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.bodySmall,
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
                        .horizontalScrollRow(),
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
    onEnabledChange: (Boolean) -> Unit,
    onModeChange: (AgentMode) -> Unit,
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
                Text("Permissions", style = MaterialTheme.typography.titleMedium)
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
            placeholder = { Text("Message") },
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

/** Horizontal scroll helper kept local to avoid an extra import at call sites. */
@Composable
private fun Modifier.horizontalScrollRow(): Modifier =
    this.then(androidx.compose.foundation.horizontalScroll(rememberScrollState()))
