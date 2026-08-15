package com.devmarkabrasaldo.PataGilid.ui.screens.lists

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devmarkabrasaldo.PataGilid.data.repository.MountainRepository
import com.devmarkabrasaldo.PataGilid.domain.models.Mountain
import com.devmarkabrasaldo.PataGilid.domain.models.MountainList

private val GliderBlue = Color(0xFF1A73E8)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MountainListDetailScreen(
    list: MountainList,
    mountainRepository: MountainRepository,
    viewModel: MountainListsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToMountainDetail: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val allMountains by mountainRepository.allMountainsByName.collectAsStateWithLifecycle(emptyList())
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Resolve the mountains that belong to this list (preserving list order)
    val listMountains: List<Mountain> = remember(list.mountainIds, allMountains) {
        list.mountainIds.mapNotNull { id -> allMountains.firstOrNull { it.id == id } }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = Color(0xFFF8F9FA),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = list.displayTitle,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (list.mountainCount == 1) "1 mountain" else "${list.mountainCount} mountains",
                            fontSize = 12.sp,
                            color = Color(0xFF5F6368)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .background(Color(0xFFF3F4F6), androidx.compose.foundation.shape.CircleShape)
                            .size(40.dp)
                    ) {
                        Icon(androidx.compose.material.icons.Icons.Default.ArrowBack, contentDescription = "Back", modifier = Modifier.size(20.dp), tint = Color.Black)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color(0xFF1C1B1F),
                    navigationIconContentColor = Color.Black
                )
            )
        }
    ) { padding ->
        if (listMountains.isEmpty()) {
            EmptyListDetail(onBrowse = onNavigateBack)
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                items(listMountains, key = { it.id }) { mountain ->
                    ListMountainCard(
                        mountain = mountain,
                        onClick = { onNavigateToMountainDetail(mountain.id) },
                        onRemove = { viewModel.removeMountain(list.id, mountain.id) }
                    )
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ListMountainCard(
    mountain: Mountain,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    var showConfirm by remember { mutableStateOf(false) }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Mountain icon badge
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(GliderBlue.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Terrain, contentDescription = null, tint = GliderBlue, modifier = Modifier.size(22.dp))
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    text = mountain.name,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${mountain.elevationMASL} MASL · ${mountain.region}",
                    fontSize = 12.sp,
                    color = Color(0xFF5F6368)
                )
            }

            TextButton(
                onClick = { showConfirm = true },
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Remove", fontSize = 13.sp)
            }
        }
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("Remove from list?") },
            text = { Text("${mountain.name} will be removed from this list. Your climb logs are not affected.") },
            confirmButton = {
                TextButton(onClick = { onRemove(); showConfirm = false }) {
                    Text("Remove", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun EmptyListDetail(onBrowse: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("🗺️", fontSize = 52.sp)
            Text("This list is empty", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(
                "Open a mountain's detail page and\ntap \"Save to List\" to add it here.",
                fontSize = 14.sp,
                color = Color(0xFF5F6368),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            FilledTonalButton(onClick = onBrowse) {
                Text("Browse Mountains")
            }
        }
    }
}
