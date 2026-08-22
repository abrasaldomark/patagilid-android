package com.devmarkabrasaldo.PataGilid.ui.screens.lists

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devmarkabrasaldo.PataGilid.domain.models.MountainList
import com.devmarkabrasaldo.PataGilid.ui.components.SearchFilterToolbar
import com.devmarkabrasaldo.PataGilid.ui.components.SortMenuItem
import com.devmarkabrasaldo.PataGilid.ui.components.SortOrderMenuSection

enum class ListSortOrder(val label: String) {
    ASCENDING("Ascending"),
    DESCENDING("Descending")
}

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
    var showLoadingUI by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isLoading) {
        if (uiState.isLoading) {
            kotlinx.coroutines.delay(400)
            if (uiState.isLoading) showLoadingUI = true
        } else {
            showLoadingUI = false
        }
    }

    var isSearchVisible by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }
    var sortOrder by remember { mutableStateOf(ListSortOrder.ASCENDING) }
    
    val sortedLists = remember(lists, sortOrder) {
        when (sortOrder) {
            ListSortOrder.ASCENDING -> lists.sortedBy { it.name }
            ListSortOrder.DESCENDING -> lists.sortedByDescending { it.name }
        }
    }

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
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(top = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SearchFilterToolbar(
                        isSearchVisible = isSearchVisible,
                        onToggleSearch = { isSearchVisible = !isSearchVisible },
                        isMenuExpanded = menuExpanded,
                        onToggleMenu = { menuExpanded = it },
                        onAdd = { showCreateDialog = true },
                        menuContent = {
                            SortOrderMenuSection(
                                items = ListSortOrder.entries.map { order ->
                                    SortMenuItem(
                                        label = order.label,
                                        isSelected = sortOrder == order,
                                        onClick = {
                                            sortOrder = order
                                            menuExpanded = false
                                        }
                                    )
                                }
                            )
                        }
                    )
                }

                Text(
                    text = "Lists",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF202124),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {


            // Body
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(SurfaceGray)
            ) {
                com.devmarkabrasaldo.PataGilid.ui.components.CountBanner(
                    filteredCount = lists.size,
                    totalCount = lists.size,
                    noun = "Lists",
                    showDivider = true
                )

                if (showLoadingUI && lists.isEmpty()) {
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
                        items(sortedLists, key = { it.id }) { list ->
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
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = Color.Black,
            textContentColor = Color(0xFF202124),
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

@Composable
private fun MountainListCard(
    list: MountainList,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    
    val buttonWidth = 140.dp
    val buttonWidthPx = with(LocalDensity.current) { buttonWidth.toPx() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        coroutineScope.launch {
                            val newOffset = (offsetX.value + dragAmount).coerceIn(-buttonWidthPx, 0f)
                            offsetX.snapTo(newOffset)
                        }
                    },
                    onDragEnd = {
                        coroutineScope.launch {
                            if (offsetX.value < -buttonWidthPx / 2) {
                                offsetX.animateTo(-buttonWidthPx)
                            } else {
                                offsetX.animateTo(0f)
                            }
                        }
                    }
                )
            }
    ) {
        // Background Buttons
        Row(
            modifier = Modifier
                .matchParentSize()
                .padding(end = 16.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    coroutineScope.launch { offsetX.animateTo(0f) }
                    onEdit()
                }
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(GliderBlue),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Rename", tint = Color.White)
                }
                Spacer(Modifier.height(4.dp))
                Text("Rename", fontSize = 12.sp, color = Color(0xFF5F6368))
            }
            
            Spacer(Modifier.width(16.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    coroutineScope.launch { offsetX.animateTo(0f) }
                    onDelete()
                }
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFF3B30)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White)
                }
                Spacer(Modifier.height(4.dp))
                Text("Delete", fontSize = 12.sp, color = Color(0xFF5F6368))
            }
        }

        // Foreground Card
        Card(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.value.roundToInt(), 0) },
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
                        .size(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(GliderBlue.copy(alpha = 0.10f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(list.emoji, fontSize = 28.sp)
                }

                Spacer(Modifier.width(14.dp))

                Column(Modifier.weight(1f)) {
                    Text(
                        text = list.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = Color(0xFF1C1B1F),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = if (list.mountainCount == 1) "1 mountain" else "${list.mountainCount} mountains",
                        fontSize = 14.sp,
                        color = Color(0xFF5F6368)
                    )
                }
                
                Spacer(Modifier.width(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Peak count badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFF1F3F4)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = list.mountainCount.toString(),
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color(0xFF1C1B1F)
                            )
                            Text(
                                text = "peaks",
                                fontSize = 10.sp,
                                color = Color(0xFF5F6368)
                            )
                        }
                    }
                    
                    Spacer(Modifier.width(8.dp))
                    
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Options",
                        tint = Color(0xFFBDC1C6),
                        modifier = Modifier.size(20.dp)
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
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    val quickEmojis = listOf("🏔️", "⛰️", "🌋", "🗻", "🌿", "🧭", "🎒", "🥾", "🏕️", "📍", "⭐", "❤️")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.background,
        titleContentColor = Color.Black,
        textContentColor = Color.Black,
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
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GliderBlue,
                        unfocusedBorderColor = Color(0xFFE8EAED),
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        focusedLabelColor = GliderBlue,
                        unfocusedLabelColor = Color(0xFF5F6368),
                        cursorColor = GliderBlue
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onConfirm(name.trim(), emoji) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = GliderBlue,
                    contentColor = Color.White
                ),
                enabled = name.isNotBlank()
            ) {
                Text("Save", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { 
                Text("Cancel", color = GliderBlue) 
            }
        }
    )
}
