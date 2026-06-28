package com.expstudio.localai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.expstudio.localai.data.db.entities.ChatSession
import com.expstudio.localai.data.db.entities.Project
import com.expstudio.localai.ui.vm.AppViewModelFactory
import com.expstudio.localai.ui.vm.ChatViewModel

/** A small palette so each project gets a distinct accent dot. */
private val projectColors = listOf(
    Color(0xFF3D5AFE), Color(0xFF00BFA5), Color(0xFF7C4DFF),
    Color(0xFFFF6D00), Color(0xFFD500F9), Color(0xFF00B0FF),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenChat: (Long) -> Unit,
    onBrowseModels: () -> Unit,
    vm: ChatViewModel = viewModel(factory = AppViewModelFactory),
) {
    val sessions by vm.sessions.collectAsStateSafe()
    val projects by vm.projects.collectAsStateSafe()
    var showNewProject by remember { mutableStateOf(false) }

    val byProject = sessions.groupBy { it.projectId }
    val loose = byProject[null].orEmpty()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("LocalAI") },
                actions = {
                    IconButton(onClick = { showNewProject = true }) {
                        Icon(Icons.Filled.CreateNewFolder, contentDescription = "New project")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { vm.newSession(null) { id -> onOpenChat(id) } }) {
                Icon(Icons.Filled.Add, contentDescription = "New chat")
            }
        },
    ) { padding ->
        if (sessions.isEmpty() && projects.isEmpty()) {
            EmptyHome(Modifier.padding(padding), onBrowseModels)
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (projects.isNotEmpty()) {
                    item { SectionHeader("Projects") }
                    items(projects, key = { "p${it.id}" }) { project ->
                        ProjectCard(
                            project = project,
                            chats = byProject[project.id].orEmpty(),
                            onOpenChat = onOpenChat,
                            onNewChat = { vm.newSession(project.id) { id -> onOpenChat(id) } },
                            onDeleteProject = { vm.deleteProject(project.id) },
                            onDeleteChat = { vm.deleteSession(it) },
                        )
                    }
                }

                item { SectionHeader(if (projects.isEmpty()) "Chats" else "Other chats") }
                if (loose.isEmpty()) {
                    item {
                        Text(
                            "No loose chats. Tap + to start one.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(8.dp),
                        )
                    }
                } else {
                    items(loose, key = { it.id }) { session ->
                        SessionRow(
                            session = session,
                            onClick = { onOpenChat(session.id) },
                            onDelete = { vm.deleteSession(session.id) },
                        )
                    }
                }
            }
        }
    }

    if (showNewProject) {
        NewProjectDialog(
            onConfirm = { name ->
                vm.createProject(name)
                showNewProject = false
            },
            onDismiss = { showNewProject = false },
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp),
    )
}

@Composable
private fun ProjectCard(
    project: Project,
    chats: List<ChatSession>,
    onOpenChat: (Long) -> Unit,
    onNewChat: () -> Unit,
    onDeleteProject: () -> Unit,
    onDeleteChat: (Long) -> Unit,
) {
    val accent = projectColors[project.colorIndex % projectColors.size]
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(14.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(accent),
                )
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(project.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        "${chats.size} chat${if (chats.size == 1) "" else "s"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onNewChat) {
                    Icon(Icons.Filled.Add, contentDescription = "New chat in project")
                }
                IconButton(onClick = onDeleteProject) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete project")
                }
            }
            chats.forEach { chat ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onOpenChat(chat.id) }
                        .padding(start = 24.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Chat,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        chat.title,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(horizontal = 10.dp),
                    )
                    IconButton(onClick = { onDeleteChat(chat.id) }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete chat", modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionRow(session: ChatSession, onClick: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
            ) {
                Text(
                    session.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    session.modelId ?: "No model selected",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete")
            }
        }
    }
}

@Composable
private fun NewProjectDialog(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { onConfirm(name) }) { Text("Create") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        icon = { Icon(Icons.Filled.Folder, contentDescription = null) },
        title = { Text("New project") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Project name") },
                singleLine = true,
            )
        },
    )
}

@Composable
private fun EmptyHome(modifier: Modifier, onBrowseModels: () -> Unit) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("No conversations yet", style = MaterialTheme.typography.titleLarge)
            Text(
                "Tap + to start a chat, or the folder icon to make a project.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(8.dp),
            )
            TextButton(onClick = onBrowseModels) { Text("Browse models") }
        }
    }
}
