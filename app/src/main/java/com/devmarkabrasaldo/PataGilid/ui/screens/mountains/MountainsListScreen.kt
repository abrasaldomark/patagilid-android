package com.devmarkabrasaldo.PataGilid.ui.screens.mountains

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.devmarkabrasaldo.PataGilid.ui.components.*
import com.devmarkabrasaldo.PataGilid.R
import com.devmarkabrasaldo.PataGilid.data.repository.MountainRepository
import com.devmarkabrasaldo.PataGilid.data.repository.AuthRepository
import com.devmarkabrasaldo.PataGilid.domain.models.IslandGroup
import com.devmarkabrasaldo.PataGilid.domain.models.Mountain
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MountainsListScreen(
    repository: MountainRepository,
    authRepository: AuthRepository,
    modifier: Modifier = Modifier,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToAddCustom: () -> Unit,
    onNavigateToAdminQueue: () -> Unit,
    vm: MountainsViewModel = viewModel(factory = MountainsViewModel.Factory(repository))
) {
    val mountains by vm.mountains.collectAsState()
    val totalCount by vm.totalMountainsCount.collectAsState()
    val pendingReviewsCount by vm.pendingReviewsCount.collectAsState()
    val searchQuery by vm.searchQuery.collectAsState()
    val selectedIsland by vm.selectedIslandGroup.collectAsState()
    val selectedRegion by vm.selectedRegion.collectAsState()
    val selectedSort by vm.sortType.collectAsState()
    val availableRegions by vm.availableRegions.collectAsState()
    val isSyncing by vm.isSyncing.collectAsState()
    val syncProgress by vm.syncProgress.collectAsState()

    val isAdmin = remember { authRepository.isAdmin }

    var sortMenuExpanded by remember { mutableStateOf(false) }
    var isSearchVisible by remember { mutableStateOf(false) }
    val searchFocusRequester = remember { FocusRequester() }

    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.US) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(top = 8.dp)
            ) {
                // Top Action Toolbar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    // Top-Right Icons (Search & Filter/Sort Menu)
                    SearchFilterToolbar(
                        isSearchVisible = isSearchVisible,
                        onToggleSearch = { isSearchVisible = !isSearchVisible },
                        isMenuExpanded = sortMenuExpanded,
                        onToggleMenu = { sortMenuExpanded = it },
                        onAdd = { onNavigateToAddCustom() },
                        menuContent = {
                            SortOrderMenuSection(
                                items = SortType.entries.map { type ->
                                    SortMenuItem(
                                        label = type.label,
                                        isSelected = selectedSort == type,
                                        onClick = {
                                            vm.sortType.value = type
                                            sortMenuExpanded = false
                                        }
                                    )
                                }
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 6.dp),
                                color = Color(0xFFE8EAED)
                            )

                            RegionFilterMenuSection(
                                availableRegions = availableRegions,
                                selectedRegion = selectedRegion,
                                onSelectRegion = {
                                    vm.selectRegion(it)
                                    sortMenuExpanded = false
                                }
                            )
                        }
                    )
                }

                // Title Text matching iOS exactly
                Text(
                    text = "Philippine Mountains",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF202124),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )

                // Admin Review Queue Banner
                if (isAdmin) {
                    val hasPendingReviews = pendingReviewsCount > 0
                    val bannerColors = if (hasPendingReviews) {
                        listOf(Color(0xFFFF9500), Color(0xFFFF3B30)) // Orange to Red
                    } else {
                        listOf(Color(0xFF34C759), Color(0xFF1E824C)) // Greenish for all clear (mimicking iOS GliderBlue/SummitSteel, but android doesn't have those exact named colors, let's use blue/steel)
                    }
                    val actualBannerColors = if (hasPendingReviews) {
                        listOf(Color(0xFF3A82F5), Color(0xFF1A73E8)) // GliderBlue to Google Blue
                    } else {
                        listOf(Color(0xFF67B5FF), Color(0xFF5A728C))
                    }
                    val iconVector = if (hasPendingReviews) Icons.Default.Security else Icons.Default.VerifiedUser
                    
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 0.dp) // iOS banner spans full width or has some padding? Screenshot shows it full width. Wait, screenshot shows full width gradient.
                            .clickable { onNavigateToAdminQueue() },
                        shadowElevation = 4.dp,
                        color = Color.Transparent
                    ) {
                        Row(
                            modifier = Modifier
                                .background(androidx.compose.ui.graphics.Brush.horizontalGradient(actualBannerColors))
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = iconVector,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (hasPendingReviews) "🔔 Admin Review Queue" else "Admin Superpowers Active",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (hasPendingReviews) "$pendingReviewsCount community submission(s) awaiting verification\n• Tap to Action" else "0 pending community submissions\n• All clear!",
                                    color = Color.White.copy(alpha = 0.95f),
                                    fontSize = 11.sp
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Search Bar (shown when toggled or active)
                if (isSearchVisible || searchQuery.isNotBlank()) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { vm.searchQuery.value = it },
                        placeholder = { Text("Search by Name, Region (e.g. Region 6), or Details", color = Color(0xFF9AA0A6), fontSize = 14.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF5F6368)) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { vm.searchQuery.value = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color(0xFF5F6368))
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .focusRequester(searchFocusRequester),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF1A73E8),
                            unfocusedBorderColor = Color(0xFFDADCE0),
                            focusedTextColor = Color(0xFF202124),
                            unfocusedTextColor = Color(0xFF202124),
                            focusedContainerColor = Color(0xFFF8F9FA),
                            unfocusedContainerColor = Color(0xFFF8F9FA)
                        )
                    )
                    LaunchedEffect(Unit) {
                        searchFocusRequester.requestFocus()
                    }
                }

                IslandGroupFilterBar(
                    allCount = totalCount,
                    isAllSelected = selectedIsland == null && selectedRegion == null,
                    selectedIslandGroup = selectedIsland,
                    onResetFilters = { vm.resetFilters() },
                    onSelectIslandGroup = { vm.selectIslandGroup(it) },
                    extraBadges = {
                        if (selectedRegion != null) {
                            DismissableBadge(
                                text = selectedRegion!!,
                                onDismiss = { vm.selectRegion(null) }
                            )
                        }
                    }
                )

                // Peak Count Subtitle
                CountBanner(
                    filteredCount = mountains.size,
                    totalCount = totalCount,
                    noun = "Mountains",
                    showDivider = false
                )

                // Subtle Sync Progress Indicator
                if (syncProgress != null) {
                    LinearProgressIndicator(
                        progress = syncProgress!!,
                        color = Color(0xFF1A73E8),
                        trackColor = Color(0xFFE8F0FE),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                    )
                } else if (isSyncing) {
                    LinearProgressIndicator(
                        color = Color(0xFF1A73E8),
                        trackColor = Color(0xFFE8F0FE),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                    )
                } else {
                    Spacer(modifier = Modifier.height(3.dp))
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().background(Color.White)) {
            if (mountains.isEmpty() && isSyncing) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(6) {
                        SkeletonMountainRow()
                    }
                }
            } else if (mountains.isEmpty() && !isSyncing) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    VStackEmptyState(searchQuery = searchQuery, onContribute = onNavigateToAddCustom)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(mountains, key = { it.id }) { mountain ->
                        MountainRowView(
                            mountain = mountain,
                            numberFormat = numberFormat,
                            onClick = { onNavigateToDetail(mountain.id) }
                        )
                    }
                }
            }
        }
    }
}



@Composable
private fun MountainRowView(
    mountain: Mountain,
    numberFormat: NumberFormat,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .background(Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail Image Box
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

            // Mountain Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = mountain.name,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF202124),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${mountain.region} • ${mountain.islandGroup}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF5F6368),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Elevation Box
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFF1F3F4)
            ) {
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = numberFormat.format(mountain.elevationMASL),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF202124)
                    )
                    Text(
                        text = "MASL",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF5F6368)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Right Chevron Arrow
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color(0xFF9AA0A6),
                modifier = Modifier.size(20.dp)
            )
        }
        HorizontalDivider(
            color = Color(0xFFF1F3F4),
            thickness = 1.dp,
            modifier = Modifier.padding(start = 86.dp)
        )
    }
}

@Composable
private fun VStackEmptyState(searchQuery: String, onContribute: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(32.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Terrain,
            contentDescription = null,
            tint = Color(0xFFBDC1C6),
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (searchQuery.isBlank()) "No mountains found" else "No mountains matched '$searchQuery'",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF202124)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Can't find the local mountain or trail you climbed? Contribute it directly to PataGilid!",
            fontSize = 14.sp,
            color = Color(0xFF5F6368),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = onContribute,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A73E8)),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Icon(Icons.Default.AddCircle, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Contribute Missing Mountain", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun SkeletonMountainRow() {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(alpha)
            .background(Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .background(Color(0xFFE8EAED), shape = RoundedCornerShape(12.dp))
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .height(20.dp)
                        .fillMaxWidth(0.6f)
                        .background(Color(0xFFE8EAED), shape = RoundedCornerShape(4.dp))
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .height(14.dp)
                        .fillMaxWidth(0.4f)
                        .background(Color(0xFFF1F3F4), shape = RoundedCornerShape(4.dp))
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Box(
                modifier = Modifier
                    .height(30.dp)
                    .width(48.dp)
                    .background(Color(0xFFE8EAED), shape = RoundedCornerShape(4.dp))
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .background(Color(0xFFF1F3F4), shape = CircleShape)
            )
        }
        HorizontalDivider(
            color = Color(0xFFF1F3F4),
            thickness = 1.dp,
            modifier = Modifier.padding(start = 86.dp)
        )
    }
}
