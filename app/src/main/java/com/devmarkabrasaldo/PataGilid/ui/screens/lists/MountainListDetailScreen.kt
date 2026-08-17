package com.devmarkabrasaldo.PataGilid.ui.screens.lists

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Clear
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
import com.devmarkabrasaldo.PataGilid.domain.models.IslandGroup
import com.devmarkabrasaldo.PataGilid.domain.models.RegionHelper
import com.devmarkabrasaldo.PataGilid.ui.components.*
import com.devmarkabrasaldo.PataGilid.ui.screens.mountains.SortType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.animation.AnimatedVisibility
import java.util.*

private val GliderBlue = Color(0xFF1A73E8)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MountainListDetailScreen(
    list: MountainList,
    mountainRepository: MountainRepository,
    viewModel: MountainListsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToMountainDetail: (String) -> Unit,
    onBrowseMountains: () -> Unit,
    modifier: Modifier = Modifier
) {
    val allMountains by viewModel.allMountains.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var searchText by remember { mutableStateOf("") }
    var isSearchVisible by remember { mutableStateOf(false) }
    var sortType by remember { mutableStateOf(SortType.ELEVATION_HIGH_TO_LOW) }
    var selectedRegion by remember { mutableStateOf<String?>(null) }
    var selectedIsland by remember { mutableStateOf<IslandGroup?>(null) }
    var menuExpanded by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    val availableRegions = remember(selectedIsland) {
        if (selectedIsland != null) {
            RegionHelper.canonicalRegionsByIslandGroup[selectedIsland] ?: emptyList()
        } else {
            RegionHelper.allCanonicalRegions
        }
    }

    // Resolve the mountains that belong to this list (preserving list order)
    val baseListMountains: List<Mountain> = remember(list.mountainIds, allMountains) {
        list.mountainIds.mapNotNull { id -> allMountains.firstOrNull { it.id == id } }
    }

    val listMountains = remember(baseListMountains, searchText, selectedIsland, selectedRegion, sortType) {
        baseListMountains.filter { mountain ->
            val matchesSearch = if (searchText.isBlank()) true else {
                val q = searchText.trim().lowercase(Locale.getDefault())
                mountain.name.lowercase(Locale.getDefault()).contains(q) ||
                mountain.region.lowercase(Locale.getDefault()).contains(q) ||
                mountain.descriptionText.lowercase(Locale.getDefault()).contains(q)
            }
            val matchesIsland = if (selectedIsland == null) true else mountain.islandGroupEnum == selectedIsland
            val matchesRegion = if (selectedRegion == null) true else mountain.region == selectedRegion
            matchesSearch && matchesIsland && matchesRegion
        }.let { filtered ->
            when (sortType) {
                SortType.ELEVATION_HIGH_TO_LOW -> filtered.sortedByDescending { it.elevationMASL }
                SortType.ELEVATION_LOW_TO_HIGH -> filtered.sortedBy { it.elevationMASL }
                SortType.NAME_AZ -> filtered.sortedBy { it.name.lowercase(Locale.getDefault()) }
            }
        }
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
            Column(modifier = Modifier.fillMaxWidth().background(Color.White)) {
                TopAppBar(
                    title = {
                        Text(
                            text = list.displayTitle,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .background(Color(0xFFF3F4F6), androidx.compose.foundation.shape.CircleShape)
                                .size(40.dp)
                        ) {
                            Icon(androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", modifier = Modifier.size(20.dp), tint = Color.Black)
                        }
                    },
                    actions = {
                        if (baseListMountains.isNotEmpty()) {
                            TopBarSearchFilterAction(
                                isSearchVisible = isSearchVisible,
                                onToggleSearch = { isSearchVisible = !isSearchVisible },
                                isMenuExpanded = menuExpanded,
                                onToggleMenu = { menuExpanded = it },
                                menuContent = {
                                    SortOrderMenuSection(
                                        items = SortType.entries.map { order ->
                                            SortMenuItem(
                                                label = order.label,
                                                isSelected = sortType == order,
                                                onClick = {
                                                    sortType = order
                                                    menuExpanded = false
                                                }
                                            )
                                        }
                                    )

                                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = Color(0xFFE8EAED))

                                    RegionFilterMenuSection(
                                        availableRegions = availableRegions,
                                        selectedRegion = selectedRegion,
                                        onSelectRegion = {
                                            selectedRegion = it
                                            selectedIsland = null
                                            menuExpanded = false
                                        }
                                    )
                                }
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.White,
                        titleContentColor = Color(0xFF1C1B1F),
                        navigationIconContentColor = Color.Black
                    )
                )

                // Animated Search Field
                AnimatedVisibility(visible = isSearchVisible) {
                    OutlinedTextField(
                        value = searchText,
                        onValueChange = { searchText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        placeholder = { Text("Search Mountain or Region...", color = Color(0xFF5F6368)) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF5F6368)) },
                        trailingIcon = {
                            if (searchText.isNotEmpty()) {
                                IconButton(onClick = { searchText = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color(0xFF5F6368))
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF1A73E8),
                            unfocusedBorderColor = Color(0xFFE8EAED),
                            focusedContainerColor = Color(0xFFF8F9FA),
                            unfocusedContainerColor = Color(0xFFF8F9FA)
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                        singleLine = true
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (baseListMountains.isNotEmpty()) {
                IslandGroupFilterBar(
                    allCount = baseListMountains.size,
                    isAllSelected = selectedIsland == null && selectedRegion == null,
                    selectedIslandGroup = selectedIsland,
                    onResetFilters = {
                        selectedIsland = null
                        selectedRegion = null
                    },
                    onSelectIslandGroup = { island ->
                        selectedIsland = island
                        if (island != null) selectedRegion = null
                    },
                    extraBadges = {
                        if (selectedRegion != null) {
                            DismissableBadge(
                                text = selectedRegion!!,
                                onDismiss = { selectedRegion = null }
                            )
                        }
                    }
                )

                CountBanner(
                    filteredCount = listMountains.size,
                    totalCount = baseListMountains.size,
                    noun = "Mountains",
                    showDivider = true
                )
            }

            if (baseListMountains.isEmpty()) {
                EmptyListDetail(onBrowse = onBrowseMountains)
            } else if (listMountains.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(Icons.Default.Terrain, contentDescription = null, tint = Color(0xFFBDC1C6), modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (searchText.isBlank()) "No mountains match filters" else "No mountains matched '$searchText'",
                            color = Color(0xFF202124),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
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
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun ListMountainCard(
    mountain: Mountain,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    var showConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = { showConfirm = true }
            ),
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
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF1F3F4)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Terrain, contentDescription = null, tint = Color(0xFF5F6368), modifier = Modifier.size(24.dp))
            }

            Spacer(Modifier.width(14.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    text = mountain.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = Color(0xFF1C1B1F),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = mountain.region,
                    fontSize = 13.sp,
                    color = Color(0xFF5F6368),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.width(12.dp))

            // Elevation and chevron
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.End) {
                    val numberFormat = java.text.NumberFormat.getNumberInstance(Locale.US)
                    Text(
                        text = numberFormat.format(mountain.elevationMASL),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFF1C1B1F)
                    )
                    Text(
                        text = "MASL",
                        fontSize = 10.sp,
                        color = Color(0xFF5F6368)
                    )
                }
                Spacer(Modifier.width(6.dp))
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Color(0xFFBDC1C6),
                    modifier = Modifier.size(20.dp)
                )
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
            Text("This list is empty", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(
                "Open a mountain's detail page and\ntap the Heart icon to add it here.",
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
