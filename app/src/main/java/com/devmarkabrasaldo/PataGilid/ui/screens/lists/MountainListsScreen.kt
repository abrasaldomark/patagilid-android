package com.devmarkabrasaldo.PataGilid.ui.screens.lists

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devmarkabrasaldo.PataGilid.domain.models.MountainList

private val GliderBlue = Color(0xFF1A73E8)
private val SurfaceGray = Color(0xFFF8F9FA)
private val CardBackground = Color(0xFFFFFFFF)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MountainListsScreen(
    viewModel: MountainListsViewModel,
    onNavigateToDetail: (MountainList) -> Unit,
    modifier: Modifier = Modifier
) {
    val lists by viewModel.lists.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showCreateDialog by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<MountainList?>(null) }
    var deleteTarget by remember { mutableStateOf<MountainList?>(null) }

    // Error snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = SurfaceGray,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "My Lists",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1C1B1F)
                    )
                    Text(
                        text = "Your personal mountain collections",
                        fontSize = 14.sp,
                        color = Color(0xFF5F6368)
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = Color.Transparent,
                    border = BorderStroke(1.dp, GliderBlue.copy(alpha = 0.2f)),
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable { showCreateDialog = true }
                ) {
                    Text(
                        "Add List",
                        color = GliderBlue,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }

            Divider(color = Color(0xFFE0E0E0), thickness = 0.5.dp)

            if (uiState.isLoading && lists.isEmpty()) {
                // Loading shimmer placeholder
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = GliderBlue)
                }
            } else if (lists.isEmpty()) {
                EmptyListsPlaceholder()
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(lists, key = { it.id }) { list ->
                        MountainListCard(
                            list = list,
                            onClick = { onNavigateToDetail(list) },
                            onEdit = { editTarget = list },
                            onDelete = { deleteTarget = list }
                        )
                    }
                    item { Spacer(Modifier.height(80.dp)) } // FAB clearance
                }
            }
        }
    }

    // Create dialog
    if (showCreateDialog) {
        ListNameDialog(
            title = "New List",
            initialName = "",
            initialEmoji = "🏔️",
            onConfirm = { name, emoji ->
                viewModel.createList(name, emoji)
                showCreateDialog = false
            },
            onDismiss = { showCreateDialog = false }
        )
    }

    // Edit dialog
    editTarget?.let { target ->
        ListNameDialog(
            title = "Rename List",
            initialName = target.name,
            initialEmoji = target.emoji,
            onConfirm = { name, emoji ->
                viewModel.renameList(target.id, name, emoji)
                editTarget = null
            },
            onDismiss = { editTarget = null }
        )
    }

    // Delete confirmation
    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete \"${target.name}\"?") },
            text = { Text("This will permanently remove the list and all ${target.mountainCount} saved mountains from it. Your actual climb logs are unaffected.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteList(target)
                    deleteTarget = null
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MountainListCard(
    list: MountainList,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Emoji badge
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(GliderBlue.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                Text(list.emoji, fontSize = 24.sp)
            }

            Spacer(Modifier.width(14.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    text = list.name,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = Color(0xFF1C1B1F),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = if (list.mountainCount == 1) "1 mountain" else "${list.mountainCount} mountains",
                    fontSize = 13.sp,
                    color = Color(0xFF5F6368)
                )
            }

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        Icons.Default.FormatListBulleted,
                        contentDescription = "Options",
                        tint = Color(0xFF5F6368)
                    )
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Rename") },
                        leadingIcon = { Icon(Icons.Default.Edit, null) },
                        onClick = { showMenu = false; onEdit() }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                        onClick = { showMenu = false; onDelete() }
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyListsPlaceholder() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("🏔️", fontSize = 56.sp)
            Text(
                "No lists yet",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1C1B1F)
            )
            Text(
                "Create your first mountain collection —\nlike \"Luzon Trip\" or \"CAR Peaks\".",
                fontSize = 14.sp,
                color = Color(0xFF5F6368),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun ListNameDialog(
    title: String,
    initialName: String,
    initialEmoji: String,
    onConfirm: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var emoji by remember { mutableStateOf(initialEmoji) }

    val quickEmojis = listOf("🏔️", "⛰️", "🌋", "🗻", "🌿", "🧭", "🎒", "🥾", "🏕️", "📍", "⭐", "❤️")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Emoji quick-pick row
                Text("Icon", fontWeight = FontWeight.Medium, fontSize = 13.sp, color = Color(0xFF5F6368))
                androidx.compose.foundation.lazy.LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(quickEmojis) { e ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (emoji == e) GliderBlue.copy(alpha = 0.15f) else Color(0xFFF1F3F4))
                                .then(
                                    if (emoji != e) Modifier else Modifier.background(GliderBlue.copy(alpha = 0.15f))
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            TextButton(onClick = { emoji = e }, contentPadding = PaddingValues(0.dp)) {
                                Text(e, fontSize = 20.sp)
                            }
                        }
                    }
                }

                // Name field
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("List Name") },
                    placeholder = { Text("e.g. Luzon Trip 2026") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onConfirm(name.trim(), emoji) },
                colors = ButtonDefaults.buttonColors(containerColor = GliderBlue),
                enabled = name.isNotBlank()
            ) {
                Text("Save", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
