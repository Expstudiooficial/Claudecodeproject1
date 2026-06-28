package com.expstudio.localai.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.expstudio.localai.data.model.CatalogModel
import com.expstudio.localai.download.DownloadState
import com.expstudio.localai.ui.util.collectAsStateSafe
import com.expstudio.localai.ui.vm.AppViewModelFactory
import com.expstudio.localai.ui.vm.ModelManagerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelManagerScreen(
    vm: ModelManagerViewModel = viewModel(factory = AppViewModelFactory),
) {
    val states by vm.downloadStates.collectAsStateSafe()
    val installed by vm.installedIds.collectAsStateSafe()

    val recommended = vm.catalog.filter { it.isRecommended }
    val others = vm.catalog.filterNot { it.isRecommended }

    Scaffold(topBar = { TopAppBar(title = { Text("Models") }) }) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item { SectionHeader("Recommended for mobile") }
            items(recommended, key = { it.id }) { model ->
                ModelCard(model, states[model.id], model.id in installed,
                    onDownload = { vm.download(model) },
                    onCancel = { vm.cancelDownload(model) },
                    onDelete = { vm.delete(model) })
            }
            item { SectionHeader("More models") }
            items(others, key = { it.id }) { model ->
                ModelCard(model, states[model.id], model.id in installed,
                    onDownload = { vm.download(model) },
                    onCancel = { vm.cancelDownload(model) },
                    onDelete = { vm.delete(model) })
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
    )
}

@Composable
private fun ModelCard(
    model: CatalogModel,
    state: DownloadState?,
    isInstalled: Boolean,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (model.isRecommended) {
                    Icon(
                        Icons.Filled.Star,
                        contentDescription = "Recommended",
                        tint = MaterialTheme.colorScheme.tertiary,
                    )
                }
                Text(
                    model.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = if (model.isRecommended) 6.dp else 0.dp),
                )
            }
            Text(
                "${model.publisher} • ${model.sizeGbText} • ${model.quantization}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                model.description,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 6.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                AssistChip(onClick = {}, label = { Text("~${model.recommendedRamMb} MB RAM") })
                AssistChip(onClick = {}, label = { Text("${model.maxContextWindow / 1024}K ctx") })
            }

            androidx.compose.foundation.layout.Spacer(Modifier.padding(4.dp))

            when {
                isInstalled -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.CheckCircle, null, tint = MaterialTheme.colorScheme.secondary)
                    Text("Installed", modifier = Modifier.padding(start = 6.dp).weight(1f))
                    OutlinedButton(onClick = onDelete) { Text("Delete") }
                }

                state is DownloadState.Downloading -> Column {
                    LinearProgressIndicator(
                        progress = { state.fraction },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(
                        Modifier.fillMaxWidth().padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "${state.percent}% • ${state.bytesPerSecond / 1024 / 1024} MB/s",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedButton(onClick = onCancel) {
                            Icon(Icons.Filled.Close, null); Text("Cancel")
                        }
                    }
                }

                state is DownloadState.Verifying ->
                    Text("Verifying…", style = MaterialTheme.typography.bodySmall)

                state is DownloadState.Failed -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Failed: ${state.message}",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f),
                    )
                    Button(onClick = onDownload) { Text("Retry") }
                }

                else -> Button(onClick = onDownload, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Download, null)
                    Text("  Download", )
                }
            }
        }
    }
}
