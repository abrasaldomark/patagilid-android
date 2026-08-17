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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import com.devmarkabrasaldo.PataGilid.R
import com.devmarkabrasaldo.PataGilid.data.repository.MountainRepository
import com.devmarkabrasaldo.PataGilid.domain.models.HikeLog
import com.devmarkabrasaldo.PataGilid.ui.components.*
import com.devmarkabrasaldo.PataGilid.domain.models.IslandGroup
import com.devmarkabrasaldo.PataGilid.domain.models.Mountain
import kotlinx.coroutines.flow.combine
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
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
    onNavigateToDetail: (String) -> Unit,
    vm: ClimbsListViewModel = viewModel(factory = ClimbsListViewModel.Factory(repository))
) {
    val hikeLogs by vm.hikeLogs.collectAsState()
    val mountainMap by vm.mountainMap.collectAsState()
    
    // Check if initial load is still happening (both empty)
    val isLoading = hikeLogs.isEmpty() && mountainMap.isEmpty()
    var showLoadingUI by remember { mutableStateOf(false) }

    var searchText by remember { mutableStateOf("") }
    var isSearchVisible by remember { mutableStateOf(false) }
    val searchFocusRequester = remember { FocusRequester() }
    var sortOrder by remember { mutableStateOf(LogSortOrder.MOST_RECENT) }
    var selectedOutcome by remember { mutableStateOf(LogOutcomeFilter.ALL) }
    var selectedRegion by remember { mutableStateOf<String?>(null) }
    var selectedIsland by remember { mutableStateOf<IslandGroup?>(null) }
    var menuExpanded by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(isLoading) {
        if (isLoading) {
            kotlinx.coroutines.delay(400)
            if (isLoading) showLoadingUI = true
        } else {
            showLoadingUI = false
        }
    }

    val availableRegions = remember(selectedIsland) {
        if (selectedIsland != null) {
            RegionHelper.canonicalRegionsByIslandGroup[selectedIsland] ?: emptyList()
        } else {
            RegionHelper.allCanonicalRegions
        }
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
        containerColor = Color.White,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(top = 8.dp)
            ) {
                // Upper right floating search & filter buttons in white capsule
                FloatingSearchFilterToolbar(
                    isSearchVisible = isSearchVisible,
                    onToggleSearch = { isSearchVisible = !isSearchVisible },
                    isMenuExpanded = menuExpanded,
                    onToggleMenu = { menuExpanded = it },
                    menuContent = {
                        SortOrderMenuSection(
                            items = LogSortOrder.entries.map { order ->
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

                        HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = Color(0xFFE8EAED))

                        CustomFilterMenuSection(
                            title = "Filter by Outcome",
                            items = LogOutcomeFilter.entries.map { outcome ->
                                SortMenuItem(
                                    label = outcome.label,
                                    isSelected = selectedOutcome == outcome,
                                    onClick = {
                                        selectedOutcome = outcome
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

                // Large Title
                Text(
                    text = "Climbs",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF202124),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )

                // Animated Search Field
                AnimatedVisibility(visible = isSearchVisible) {
                    OutlinedTextField(
                        value = searchText,
                        onValueChange = { searchText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .focusRequester(searchFocusRequester),
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
                    LaunchedEffect(Unit) {
                        searchFocusRequester.requestFocus()
                    }
                }

                IslandGroupFilterBar(
                    allCount = hikeLogs.size,
                    isAllSelected = selectedIsland == null && selectedRegion == null && selectedOutcome == LogOutcomeFilter.ALL,
                    selectedIslandGroup = selectedIsland,
                    onResetFilters = {
                        selectedIsland = null
                        selectedRegion = null
                        selectedOutcome = LogOutcomeFilter.ALL
                    },
                    onSelectIslandGroup = { island ->
                        selectedIsland = island
                        if (island != null) selectedRegion = null
                    },
                    extraBadges = {
                        if (selectedOutcome != LogOutcomeFilter.ALL) {
                            DismissableBadge(
                                text = if (selectedOutcome == LogOutcomeFilter.SUMMITED) "Summited Only" else "Backed Out",
                                onDismiss = { selectedOutcome = LogOutcomeFilter.ALL }
                            )
                        }
                        if (selectedRegion != null) {
                            DismissableBadge(
                                text = selectedRegion!!,
                                onDismiss = { selectedRegion = null }
                            )
                        }
                    }
                )
    
                CountBanner(
                    filteredCount = filteredLogs.size,
                    totalCount = hikeLogs.size,
                    noun = "Climbs",
                    showDivider = true
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color.White)
        ) {
            if (isLoading) {
                if (showLoadingUI) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF1A73E8))
                    }
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
                                text = "Akyat na!",
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
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredLogs, key = { it.id ?: UUID.randomUUID().toString() }) { log ->
                        val mountain = mountainMap[log.mountainId]
                        HikeLogRow(log = log, mountain = mountain, onClick = {
                            log.id?.let { onNavigateToDetail(it) }
                        })
                    }
                }
            }
        }
    }
}



@Composable
fun HikeLogRow(log: HikeLog, mountain: Mountain?, onClick: () -> Unit) {
    val mountainName = mountain?.name ?: (if (log.mountainId.contains("_")) {
        log.mountainId.substringAfter("_").replace("-", " ").replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    } else "Philippine Peak")

    val dateString = formatDateRange(log.dateTimeStart, log.dateTimeEnd)
    val elevation = mountain?.elevationMASL
    val numberFormat = java.text.NumberFormat.getNumberInstance(Locale.US)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .background(Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Thumbnail matching Mountains screen
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFF1F3F4),
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = Icons.Default.Terrain,
                        contentDescription = null,
                        tint = Color(0xFFBDC1C6),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Details column
            Column(modifier = Modifier.weight(1f)) {
                // Mountain Name
                // Mountain Name
                Text(
                    text = mountainName,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF202124),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Elevation and Date
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (elevation != null && elevation > 0) {
                        Text(
                            text = "${numberFormat.format(elevation)} MASL",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1A73E8)
                        )
                        Text(
                            text = "  ·  ",
                            fontSize = 13.sp,
                            color = Color(0xFF9AA0A6)
                        )
                    }
                    Text(
                        text = dateString,
                        fontSize = 13.sp,
                        color = Color(0xFF5F6368),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Region
                if (!mountain?.region.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = mountain?.region ?: "",
                        fontSize = 13.sp,
                        color = Color(0xFF5F6368),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                
                // Badges Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Trail / Route Badge
                    if (!log.trailName.isNullOrBlank()) {
                        val trailText = if (log.routeType == "Traverse" && log.exitTrailName.isNotBlank()) {
                            "${log.trailName} → ${log.exitTrailName} (Traverse)"
                        } else if (log.routeType == "Traverse") {
                            "${log.trailName} (Traverse)"
                        } else if (log.routeType == "Circuit") {
                            "${log.trailName} (Circuit)"
                        } else {
                            "${log.trailName} (Back Trail)"
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
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

                    // Summited / Backed Out status
                    Text(
                        text = if (log.didSummit) "Summited" else "Backed Out",
                        color = if (log.didSummit) Color(0xFF1A73E8) else Color(0xFFD93025), // Blue for Summited, Red for Backed Out
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Right Chevron Arrow
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color(0xFF9AA0A6),
                modifier = Modifier
                    .size(20.dp)
                    .align(Alignment.CenterVertically)
            )
        }
        HorizontalDivider(
            color = Color(0xFFF1F3F4),
            thickness = 1.dp,
            modifier = Modifier.padding(start = 86.dp)
        )
    }
}

fun formatDateRange(startMillis: Long, endMillis: Long): String {
    val startDate = Date(startMillis)
    val endDate = Date(if (endMillis > 0) endMillis else startMillis)

    val calendarStart = java.util.Calendar.getInstance().apply { time = startDate }
    val calendarEnd = java.util.Calendar.getInstance().apply { time = endDate }

    val startYear = calendarStart.get(java.util.Calendar.YEAR)
    val endYear = calendarEnd.get(java.util.Calendar.YEAR)
    val startMonth = calendarStart.get(java.util.Calendar.MONTH)
    val endMonth = calendarEnd.get(java.util.Calendar.MONTH)
    val startDay = calendarStart.get(java.util.Calendar.DAY_OF_MONTH)
    val endDay = calendarEnd.get(java.util.Calendar.DAY_OF_MONTH)

    val monthFormat = SimpleDateFormat("MMMM", Locale.getDefault())

    return if (startYear == endYear) {
        if (startMonth == endMonth) {
            if (startDay == endDay) {
                "${monthFormat.format(startDate)} $startDay, $startYear"
            } else {
                "${monthFormat.format(startDate)} $startDay to $endDay, $startYear"
            }
        } else {
            "${monthFormat.format(startDate)} $startDay to ${monthFormat.format(endDate)} $endDay, $startYear"
        }
    } else {
        "${monthFormat.format(startDate)} $startDay, $startYear to ${monthFormat.format(endDate)} $endDay, $endYear"
    }
}
