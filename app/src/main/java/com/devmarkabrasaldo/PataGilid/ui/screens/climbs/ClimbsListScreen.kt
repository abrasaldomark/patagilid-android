package com.devmarkabrasaldo.PataGilid.ui.screens.climbs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import com.devmarkabrasaldo.PataGilid.R
import com.devmarkabrasaldo.PataGilid.data.repository.MountainRepository
import com.devmarkabrasaldo.PataGilid.domain.models.HikeLog
import com.devmarkabrasaldo.PataGilid.domain.models.IslandGroup
import com.devmarkabrasaldo.PataGilid.domain.models.Mountain
import com.devmarkabrasaldo.PataGilid.domain.models.RegionHelper
import java.text.SimpleDateFormat
import java.util.*

enum class LogSortOrder(val label: String) {
    MOST_RECENT("Most Recent"),
    OLDEST_FIRST("Oldest First"),
    HIGHEST_ELEVATION("Highest Peak"),
    ALPHABETICAL("Mountain Name (A-Z)")
}

enum class LogOutcomeFilter(val label: String) {
    ALL("All Outcomes"),
    SUMMITED("Summited Only"),
    BACKED_OUT("Backed Out")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClimbsListScreen(
    repository: MountainRepository,
    modifier: Modifier = Modifier,
    onNavigateToDetail: (String) -> Unit
) {
    var hikeLogs by remember { mutableStateOf<List<HikeLog>>(emptyList()) }
    var mountainMap by remember { mutableStateOf<Map<String, Mountain?>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }

    var searchText by remember { mutableStateOf("") }
    var isSearchVisible by remember { mutableStateOf(false) }
    var sortOrder by remember { mutableStateOf(LogSortOrder.MOST_RECENT) }
    var selectedOutcome by remember { mutableStateOf(LogOutcomeFilter.ALL) }
    var selectedRegion by remember { mutableStateOf<String?>(null) }
    var selectedIsland by remember { mutableStateOf<IslandGroup?>(null) }
    var menuExpanded by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(Unit) {
        isLoading = true
        val logs = repository.getUserHikeLogs()
        val map = mutableMapOf<String, Mountain?>()
        for (log in logs) {
            if (!map.containsKey(log.mountainId)) {
                map[log.mountainId] = repository.getMountain(log.mountainId)
            }
        }
        hikeLogs = logs
        mountainMap = map
        isLoading = false
    }

    val availableRegions = remember(mountainMap) {
        RegionHelper.sortRegions(mountainMap.values.mapNotNull { it?.region })
    }

    val filteredLogs = remember(
        hikeLogs, mountainMap, searchText, selectedIsland, selectedRegion, selectedOutcome, sortOrder
    ) {
        hikeLogs.filter { log ->
            val mountain = mountainMap[log.mountainId]
            val matchesSearch = if (searchText.isBlank()) true else {
                val q = searchText.trim().lowercase(Locale.getDefault())
                (mountain?.name?.lowercase(Locale.getDefault())?.contains(q) == true) ||
                (mountain?.region?.lowercase(Locale.getDefault())?.contains(q) == true) ||
                (log.trailName?.lowercase(Locale.getDefault())?.contains(q) == true)
            }
            val matchesIsland = if (selectedIsland == null) true else mountain?.islandGroupEnum == selectedIsland
            val matchesRegion = if (selectedRegion == null) true else mountain?.region == selectedRegion
            val matchesOutcome = when (selectedOutcome) {
                LogOutcomeFilter.ALL -> true
                LogOutcomeFilter.SUMMITED -> log.didSummit
                LogOutcomeFilter.BACKED_OUT -> !log.didSummit
            }
            matchesSearch && matchesIsland && matchesRegion && matchesOutcome
        }.let { list ->
            when (sortOrder) {
                LogSortOrder.MOST_RECENT -> list.sortedByDescending { it.dateTimeStart }
                LogSortOrder.OLDEST_FIRST -> list.sortedBy { it.dateTimeStart }
                LogSortOrder.HIGHEST_ELEVATION -> list.sortedByDescending { mountainMap[it.mountainId]?.elevationMASL ?: 0 }
                LogSortOrder.ALPHABETICAL -> list.sortedBy { mountainMap[it.mountainId]?.name ?: "" }
            }
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    focusManager.clearFocus()
                })
            },
        containerColor = Color(0xFFF5F6F8),
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
            ) {
                // Upper right floating search & filter buttons in white capsule
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    if (hikeLogs.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = Color.White,
                            shadowElevation = 6.dp
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                // Search Toggle Button
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFF1A73E8),
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clickable { isSearchVisible = !isSearchVisible }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Search,
                                            contentDescription = "Search",
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                // Sort & Filter Dropdown Trigger Button
                                Box {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFF1A73E8),
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clickable { menuExpanded = true }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.FilterList,
                                                contentDescription = "Filter and Sort Menu",
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }

                                    DropdownMenu(
                                        expanded = menuExpanded,
                                        onDismissRequest = { menuExpanded = false },
                                        modifier = Modifier.background(Color.White),
                                        shape = RoundedCornerShape(16.dp),
                                        shadowElevation = 8.dp
                                    ) {
                                        // Sort Order
                                        Text(
                                            text = "Sort Order",
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF5F6368)
                                        )
                                        LogSortOrder.entries.forEach { order ->
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        text = order.label,
                                                        color = Color(0xFF202124),
                                                        fontSize = 15.sp,
                                                        fontWeight = if (sortOrder == order) FontWeight.SemiBold else FontWeight.Normal
                                                    )
                                                },
                                                leadingIcon = {
                                                    if (sortOrder == order) {
                                                        Icon(
                                                            imageVector = Icons.Default.Check,
                                                            contentDescription = null,
                                                            tint = Color(0xFF1A73E8),
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    } else {
                                                        Spacer(modifier = Modifier.size(20.dp))
                                                    }
                                                },
                                                onClick = {
                                                    sortOrder = order
                                                    menuExpanded = false
                                                },
                                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
                                            )
                                        }

                                        HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = Color(0xFFE8EAED))

                                        // Filter by Outcome
                                        Text(
                                            text = "Filter by Outcome",
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF5F6368)
                                        )
                                        LogOutcomeFilter.entries.forEach { outcome ->
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        text = outcome.label,
                                                        color = Color(0xFF202124),
                                                        fontSize = 15.sp,
                                                        fontWeight = if (selectedOutcome == outcome) FontWeight.SemiBold else FontWeight.Normal
                                                    )
                                                },
                                                leadingIcon = {
                                                    if (selectedOutcome == outcome) {
                                                        Icon(
                                                            imageVector = Icons.Default.Check,
                                                            contentDescription = null,
                                                            tint = Color(0xFF1A73E8),
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    } else {
                                                        Spacer(modifier = Modifier.size(20.dp))
                                                    }
                                                },
                                                onClick = {
                                                    selectedOutcome = outcome
                                                    menuExpanded = false
                                                },
                                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
                                            )
                                        }

                                        HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = Color(0xFFE8EAED))

                                        // Filter by Region
                                        Text(
                                            text = "Filter by Region",
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF5F6368)
                                        )
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = "All Regions",
                                                    color = Color(0xFF202124),
                                                    fontSize = 15.sp,
                                                    fontWeight = if (selectedRegion == null) FontWeight.SemiBold else FontWeight.Normal
                                                )
                                            },
                                            leadingIcon = {
                                                if (selectedRegion == null) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = Color(0xFF1A73E8),
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                } else {
                                                    Spacer(modifier = Modifier.size(20.dp))
                                                }
                                            },
                                            onClick = {
                                                selectedRegion = null
                                                menuExpanded = false
                                            },
                                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
                                        )

                                        availableRegions.forEach { region ->
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        text = region,
                                                        color = Color(0xFF202124),
                                                        fontSize = 15.sp,
                                                        fontWeight = if (selectedRegion == region) FontWeight.SemiBold else FontWeight.Normal
                                                    )
                                                },
                                                leadingIcon = {
                                                    if (selectedRegion == region) {
                                                        Icon(
                                                            imageVector = Icons.Default.Check,
                                                            contentDescription = null,
                                                            tint = Color(0xFF1A73E8),
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    } else {
                                                        Spacer(modifier = Modifier.size(20.dp))
                                                    }
                                                },
                                                onClick = {
                                                    selectedRegion = region
                                                    selectedIsland = null
                                                    menuExpanded = false
                                                },
                                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Large Title
                Text(
                    text = "My Summit Logs",
                    fontSize = 34.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF202124),
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 10.dp)
                )

                // Animated Search Field
                AnimatedVisibility(visible = isSearchVisible) {
                    OutlinedTextField(
                        value = searchText,
                        onValueChange = { searchText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        placeholder = { Text("Search Mountain, Region, or Trail...", color = Color(0xFF5F6368)) },
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
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFFF5F6F8))
        ) {
            if (hikeLogs.isNotEmpty()) {
                // Horizontal Island & Active Badges Filter Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF1F3F4))
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // All (Count)
                    FilterPill(
                        text = "All (${hikeLogs.size})",
                        iconResId = R.drawable.philippines_icon,
                        isSelected = selectedIsland == null && selectedRegion == null && selectedOutcome == LogOutcomeFilter.ALL,
                        onClick = {
                            selectedIsland = null
                            selectedRegion = null
                            selectedOutcome = LogOutcomeFilter.ALL
                        }
                    )
    
                    // Island Groups
                    IslandGroup.entries.forEach { island ->
                        val iconRes = when (island) {
                            IslandGroup.LUZON -> R.drawable.luzon_icon
                            IslandGroup.VISAYAS -> R.drawable.visayas_icon
                            IslandGroup.MINDANAO -> R.drawable.mindanao_icon
                        }
                        FilterPill(
                            text = island.displayName,
                            iconResId = iconRes,
                            isSelected = selectedIsland == island,
                            onClick = {
                                selectedIsland = if (selectedIsland == island) null else island
                                if (selectedIsland != null) selectedRegion = null
                            }
                        )
                    }
    
                    // Active Outcome Badge indicator (Glider Blue scheme)
                    if (selectedOutcome != LogOutcomeFilter.ALL) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF1A73E8),
                            modifier = Modifier.clickable { selectedOutcome = LogOutcomeFilter.ALL }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = if (selectedOutcome == LogOutcomeFilter.SUMMITED) "Summited Only" else "Backed Out",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.Cancel,
                                    contentDescription = "Clear Outcome Filter",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
    
                    // Active Region Badge indicator (Glider Blue scheme)
                    if (selectedRegion != null) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF1A73E8),
                            modifier = Modifier.clickable { selectedRegion = null }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = selectedRegion!!,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.Cancel,
                                    contentDescription = "Clear Region Filter",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
    
                // Showing X of Y Climbs Subtitle & Divider
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Showing ${filteredLogs.size} of ${hikeLogs.size} Climbs",
                        color = Color(0xFF80868B),
                        fontSize = 13.sp
                    )
                }
                HorizontalDivider(color = Color(0xFFE8EAED), thickness = 1.dp)
            }

            // Content Body
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF1A73E8))
                }
            } else if (filteredLogs.isEmpty()) {
                val isTrulyEmpty = searchText.isBlank() && selectedIsland == null && selectedRegion == null && selectedOutcome == LogOutcomeFilter.ALL
                if (isTrulyEmpty) {
                    Box(modifier = Modifier.fillMaxSize().padding(bottom = 60.dp), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(120.dp)
                                    .background(Color(0x1A3A82F5), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Terrain,
                                    contentDescription = null,
                                    tint = Color(0x803A82F5),
                                    modifier = Modifier.size(64.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                text = "No Logs Yet",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Open any mountain on the list and tap\n\"Add Climb\" to record your first ascent.",
                                color = Color(0xFF8E8E93),
                                fontSize = 15.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 22.sp
                            )
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Icon(Icons.Default.Flag, contentDescription = null, tint = Color(0xFFBDC1C6), modifier = Modifier.size(64.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (searchText.isBlank()) "No climbs match filters" else "No climbs matched '$searchText'",
                                color = Color(0xFF202124),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 18.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Adjust your search or filters to see your recorded climbs.",
                                color = Color(0xFF5F6368),
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(filteredLogs, key = { it.id ?: UUID.randomUUID().toString() }) { log ->
                        val mountain = mountainMap[log.mountainId]
                        HikeLogCard(log = log, mountain = mountain, onClick = {
                            log.id?.let { onNavigateToDetail(it) }
                        })
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterPill(
    text: String,
    iconResId: Int? = null,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = CircleShape,
        color = if (isSelected) Color(0xFF1A73E8) else Color(0xFFE8EAED),
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            if (iconResId != null) {
                Icon(
                    painter = painterResource(id = iconResId),
                    contentDescription = null,
                    tint = if (isSelected) Color.White else Color(0xFF3C4043),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = text,
                color = if (isSelected) Color.White else Color(0xFF3C4043),
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@Composable
fun HikeLogCard(log: HikeLog, mountain: Mountain?, onClick: () -> Unit) {
    val mountainName = mountain?.name ?: (if (log.mountainId.contains("_")) {
        log.mountainId.substringAfter("_").replace("-", " ").replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    } else "Philippine Peak")

    val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    val dateString = dateFormat.format(Date(log.dateTimeStart))
    val elevation = mountain?.elevationMASL

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left circular mountain icon with light Glider Blue background tint
            Surface(
                shape = CircleShape,
                color = if (log.didSummit) Color(0xFFE8F0FE) else Color(0xFFE8EAED),
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (log.didSummit) Icons.Default.Terrain else Icons.Default.DirectionsWalk,
                        contentDescription = null,
                        tint = if (log.didSummit) Color(0xFF1A73E8) else Color(0xFF5F6368),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Center content column
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Mountain Name
                Text(
                    text = mountainName,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF202124),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Elevation & Date on a single line
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (elevation != null && elevation > 0) {
                        Text(
                            text = "$elevation MASL",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF70757A)
                        )
                        Text(
                            text = "·",
                            fontSize = 13.sp,
                            color = Color(0xFF70757A)
                        )
                    }
                    Text(
                        text = dateString,
                        fontSize = 13.sp,
                        color = Color(0xFF70757A)
                    )
                }

                // Region text
                if (!mountain?.region.isNullOrBlank()) {
                    Text(
                        text = mountain?.region ?: "",
                        fontSize = 13.sp,
                        color = Color(0xFF70757A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Trail / Route Badge
                if (!log.trailName.isNullOrBlank()) {
                    val trailText = if (log.routeType == "Traverse" && log.exitTrailName.isNotBlank()) {
                        "${log.trailName} ➔ ${log.exitTrailName} (Traverse)"
                    } else if (log.routeType == "Traverse") {
                        "${log.trailName} (Traverse)"
                    } else if (log.routeType == "Circuit") {
                        "${log.trailName} (Circuit)"
                    } else {
                        "${log.trailName} (Back Trail)"
                    }
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFE8F0FE)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timeline,
                                contentDescription = null,
                                tint = Color(0xFF1A73E8),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = trailText,
                                color = Color(0xFF1A73E8),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Status Outcome Badge (Summited / Backed Out)
                Surface(
                    shape = CircleShape,
                    color = if (log.didSummit) Color(0xFFE8F0FE) else Color(0xFFE8EAED)
                ) {
                    Text(
                        text = if (log.didSummit) "Summited" else "Backed Out",
                        color = if (log.didSummit) Color(0xFF1A73E8) else Color(0xFF5F6368),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Right Chevron Arrow
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color(0xFFBDC1C6),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
