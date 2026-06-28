package com.expstudio.localai.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.expstudio.localai.agent.AgentPermissions
import com.expstudio.localai.data.model.ModelCatalog
import com.expstudio.localai.download.DownloadState
import com.expstudio.localai.ui.vm.AppViewModelFactory
import com.expstudio.localai.ui.vm.ModelManagerViewModel

/**
 * First-launch flow. Welcomes the user, downloads a starter model in the
 * background, and requests the permissions the app (and agent mode) need — so
 * nobody ever has to touch a terminal. No command line exists on Android, and
 * the app should never pretend otherwise.
 */
@Composable
fun OnboardingScreen(
    onDone: () -> Unit,
    vm: ModelManagerViewModel = viewModel(factory = AppViewModelFactory),
) {
    var step by remember { mutableIntStateOf(0) }
    val steps = 4

    Scaffold { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Progress dots
            Row(
                Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                repeat(steps) { i ->
                    Box(
                        Modifier
                            .padding(horizontal = 4.dp)
                            .size(if (i == step) 10.dp else 8.dp)
                    ) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = if (i <= step) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }

            Box(Modifier.weight(1f).fillMaxWidth()) {
                when (step) {
                    0 -> WelcomeStep()
                    1 -> DependencyStep(vm)
                    2 -> PermissionsStep()
                    else -> DoneStep()
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                if (step > 0) {
                    TextButton(onClick = { step-- }) { Text("Back") }
                } else {
                    Spacer(Modifier.height(1.dp))
                }
                Button(onClick = { if (step < steps - 1) step++ else onDone() }) {
                    Text(
                        when (step) {
                            0 -> "Get started"
                            steps - 1 -> "Enter LocalAI"
                            else -> "Continue"
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun WelcomeStep() {
    CenteredColumn {
        Icon(
            Icons.Filled.AutoAwesome,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(72.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text("Welcome to LocalAI", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "Run powerful AI models fully offline on your phone — private, free and " +
                "yours. Let's set everything up in a few taps.\n\n" +
                "No computer or terminal needed: the app downloads and configures " +
                "everything itself.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DependencyStep(vm: ModelManagerViewModel) {
    val states by vm.downloadStates.collectAsStateSafe()
    val installed by vm.installedIds.collectAsStateSafe()
    val starter = remember { ModelCatalog.byId("qwen25-1_5b-it-q4") ?: ModelCatalog.models.first() }
    val state = states[starter.id]
    val isInstalled = starter.id in installed

    CenteredColumn {
        Icon(
            Icons.Filled.CloudDownload,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(64.dp),
        )
        Spacer(Modifier.height(12.dp))
        Text("Get a starter model", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "We recommend ${starter.displayName} (${starter.sizeGbText}) — fast and light. " +
                "It downloads in the background, so you can keep going while it finishes.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))

        when {
            isInstalled || state is DownloadState.Completed -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("Downloaded — you're ready to chat!")
                }
            }
            state is DownloadState.Downloading -> {
                Column(Modifier.fillMaxWidth()) {
                    LinearProgressIndicator(
                        progress = { state.fraction },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "${state.percent}%  •  ${state.bytesPerSecond / 1024} KB/s  (downloading in background)",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            state is DownloadState.Verifying -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Verifying…")
                }
            }
            state is DownloadState.Failed -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Download failed: ${state.message}", color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { vm.download(starter) }) { Text("Retry") }
                }
            }
            else -> {
                Button(onClick = { vm.download(starter) }) {
                    Icon(Icons.Filled.CloudDownload, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Download ${starter.sizeGbText}")
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "You can skip this and pick any model later from the Models tab.",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun PermissionsStep() {
    val context = androidx.compose.ui.platform.LocalContext.current
    var notifGranted by remember { mutableStateOf(false) }
    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> notifGranted = granted }

    CenteredColumn {
        Icon(
            Icons.Filled.Security,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(64.dp),
        )
        Spacer(Modifier.height(12.dp))
        Text("Permissions", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "Grant these so downloads can notify you and so agent mode can work across " +
                "your device. You stay in control — agent actions still ask before running.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))

        PermissionCard(
            title = "Notifications",
            body = "Show download progress while you use other apps.",
            granted = notifGranted || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU,
            onGrant = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else notifGranted = true
            },
        )
        Spacer(Modifier.height(8.dp))
        PermissionCard(
            title = "All-files access (for Agent mode)",
            body = "Lets the AI read/write files across your storage. Optional — the agent " +
                "works in its own folder without it.",
            granted = AgentPermissions.hasAllFilesAccess(),
            onGrant = {
                AgentPermissions.allFilesAccessIntent(context)?.let { context.startActivity(it) }
            },
        )
    }
}

@Composable
private fun PermissionCard(title: String, body: String, granted: Boolean, onGrant: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Medium)
                Text(
                    body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(12.dp))
            if (granted) {
                Icon(Icons.Filled.CheckCircle, "Granted", tint = MaterialTheme.colorScheme.primary)
            } else {
                OutlinedButton(onClick = onGrant) { Text("Grant") }
            }
        }
    }
}

@Composable
private fun DoneStep() {
    CenteredColumn {
        Icon(
            Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(72.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text("All set!", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "You're ready to chat — fully offline and private. Create projects to organise " +
                "chats, flip on Agent mode to let the AI act on your device, and tune " +
                "everything in Settings.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CenteredColumn(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) { content() }
}
