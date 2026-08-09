package com.devmarkabrasaldo.PataGilid.ui.screens.profile

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devmarkabrasaldo.PataGilid.data.repository.MountainRepository
import com.devmarkabrasaldo.PataGilid.domain.models.Mountain
import com.devmarkabrasaldo.PataGilid.ui.screens.mountains.MountainMapView
import com.devmarkabrasaldo.PataGilid.ui.theme.GlobeLocationPin
import kotlinx.coroutines.launch

// iOS colors
private val PageBackground = Color(0xFFF2F2F7)
private val CardBackground = Color.White
private val PrimaryText = Color(0xFF1C1C1E)
private val SecondaryText = Color(0xFF8E8E93)
private val GliderBlue = Color(0xFF007AFF)
private val DestructiveRed = Color(0xFFFF3B30)
private val Orange = Color(0xFFFF9500)
private val Purple = Color(0xFFAF52DE)
private val Green = Color(0xFF34C759)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminModerationQueueScreen(
    repository: MountainRepository,
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (String) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val unapprovedPeaks by repository.unapprovedMountains.collectAsState(initial = emptyList())
    val pendingGpsPeaks by repository.pendingGpsMountains.collectAsState(initial = emptyList())
    val allPublicPeaks by repository.allMountainsByName.collectAsState(initial = emptyList())

    var isProcessing by remember { mutableStateOf(false) }
    var actionFeedback by remember { mutableStateOf<String?>(null) }
    
    var mountainToMerge by remember { mutableStateOf<Mountain?>(null) }
    
    // Feedback dialog
    if (actionFeedback != null) {
        AlertDialog(
            onDismissRequest = { actionFeedback = null },
            title = { Text("Moderation Success", fontWeight = FontWeight.Bold) },
            text = { Text(actionFeedback!!) },
            confirmButton = {
                TextButton(onClick = { actionFeedback = null }) {
                    Text("OK", color = GliderBlue, fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }
    
    // Merge Dialog
    if (mountainToMerge != null) {
        MergeMountainSelectionDialog(
            duplicatePeak = mountainToMerge!!,
            publicPeaks = allPublicPeaks,
            onDismiss = { mountainToMerge = null },
            onSelectTarget = { target ->
                mountainToMerge = null
                coroutineScope.launch {
                    isProcessing = true
                    try {
                        repository.mergeMountain(mountainToMerge!!.id, target.id)
                        actionFeedback = "🔀 Merged '${mountainToMerge!!.name}' into official '${target.name}'. All user climb logs re-linked safely!"
                    } catch (e: Exception) {
                        actionFeedback = "⚠️ Failed to merge: ${e.localizedMessage}"
                    }
                    isProcessing = false
                }
            }
        )
    }
    
    var mountainToViewMap by remember { mutableStateOf<Mountain?>(null) }
    
    mountainToViewMap?.let { peak ->
        MountainMapView(
            mountain = peak,
            isAdmin = true,
            onDismiss = { mountainToViewMap = null },
            onUpdateProposal = { lat, lng ->
                coroutineScope.launch {
                    isProcessing = true
                    try {
                        repository.updateGpsProposal(peak.id, lat, lng)
                        actionFeedback = "✏️ GPS proposal updated successfully."
                    } catch (e: Exception) {
                        actionFeedback = "⚠️ Failed to update GPS proposal: ${e.localizedMessage}"
                    }
                    isProcessing = false
                }
            },
            onApprove = { lat, lng ->
                coroutineScope.launch {
                    isProcessing = true
                    try {
                        repository.applyAdjustedGpsCalibration(peak.id, lat, lng)
                        actionFeedback = "✅ Adjusted GPS coordinates approved & broadcasted nationwide!"
                    } catch (e: Exception) {
                        actionFeedback = "⚠️ Failed to approve GPS: ${e.localizedMessage}"
                    }
                    isProcessing = false
                }
            }
        )
    }

    Scaffold(
        containerColor = PageBackground,
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🛡️", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Moderation Queue", fontWeight = FontWeight.Bold, color = PrimaryText) 
                    }
                },
                actions = {
                    TextButton(onClick = onNavigateBack) {
                        Text("Done", color = GliderBlue, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PageBackground)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (unapprovedPeaks.isEmpty() && pendingGpsPeaks.isEmpty()) {
                // Empty State
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.VerifiedUser,
                        contentDescription = null,
                        tint = GliderBlue,
                        modifier = Modifier.size(60.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Queue Empty!",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryText
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "You are completely caught up. Zero community mountain submissions or GPS calibrations waiting for review.",
                        fontSize = 15.sp,
                        color = SecondaryText,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 40.dp),
                        lineHeight = 22.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Section 1: Community Mountains
                    if (unapprovedPeaks.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = "Pending Community Mountains (${unapprovedPeaks.size})",
                                footer = "Approved mountains go live instantly on the public list. Merged mountains re-link contributor logs to an official entry and remove the duplicate."
                            )
                        }
                        
                        items(unapprovedPeaks, key = { it.id }) { peak ->
                            PendingPeakCard(
                                peak = peak,
                                onReject = {
                                    coroutineScope.launch {
                                        isProcessing = true
                                        try {
                                            repository.deleteMountain(peak.id)
                                            actionFeedback = "🗑️ ${peak.name} was rejected and removed from the review queue."
                                        } catch (e: Exception) {
                                            actionFeedback = "⚠️ Failed to decline: ${e.localizedMessage}"
                                        }
                                        isProcessing = false
                                    }
                                },
                                onMerge = { mountainToMerge = peak },
                                onApprove = {
                                    coroutineScope.launch {
                                        isProcessing = true
                                        try {
                                            repository.approveCustomMountain(peak.id)
                                            actionFeedback = "✅ ${peak.name} has been approved and is now live nationwide in PataGilid!"
                                        } catch (e: Exception) {
                                            actionFeedback = "⚠️ Failed to approve: ${e.localizedMessage}"
                                        }
                                        isProcessing = false
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }

                    // Section 2: GPS Calibrations
                    if (pendingGpsPeaks.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = "Pending GPS Calibrations (${pendingGpsPeaks.size})",
                                footer = "Approved coordinates immediately calibrate the official mountain entry and grant a verified community badge nationwide via Delta-Sync."
                            )
                        }

                        items(pendingGpsPeaks, key = { it.id + "gps" }) { peak ->
                            PendingGpsCard(
                                peak = peak,
                                onViewMap = {
                                    mountainToViewMap = peak
                                },
                                onReject = {
                                    coroutineScope.launch {
                                        isProcessing = true
                                        try {
                                            repository.declineGPS(peak.id)
                                            actionFeedback = "🗑️ GPS submission rejected."
                                        } catch (e: Exception) {
                                            actionFeedback = "⚠️ Failed to reject GPS: ${e.localizedMessage}"
                                        }
                                        isProcessing = false
                                    }
                                },
                                onApprove = {
                                    coroutineScope.launch {
                                        isProcessing = true
                                        try {
                                            repository.applyGpsCalibration(peak.id)
                                            actionFeedback = "✅ GPS coordinates for '${peak.name}' approved & broadcasted nationwide via Delta-Sync!"
                                        } catch (e: Exception) {
                                            actionFeedback = "⚠️ Failed to approve GPS: ${e.localizedMessage}"
                                        }
                                        isProcessing = false
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }
            }

            // Processing Overlay
            if (isProcessing) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.35f))
                        .clickable(enabled = false) {},
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .background(Color(0xFFF2F2F7), RoundedCornerShape(12.dp))
                            .padding(20.dp)
                    ) {
                        CircularProgressIndicator(color = GliderBlue)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Processing Moderation...", color = PrimaryText, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, footer: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title.uppercase(),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = SecondaryText,
            modifier = Modifier.padding(start = 16.dp, bottom = 6.dp)
        )
    }
}

@Composable
fun PendingPeakCard(
    peak: Mountain,
    onReject: () -> Unit,
    onMerge: () -> Unit,
    onApprove: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Info
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(peak.name, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = PrimaryText)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ArrowOutward, contentDescription = null, tint = GliderBlue, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("${peak.elevationMASL} MASL", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = PrimaryText)
                        Text(" • ${peak.islandGroupEnum.displayName}", fontSize = 13.sp, color = SecondaryText)
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("📍 ${peak.region}", fontSize = 12.sp, color = SecondaryText)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Difficulty", fontSize = 10.sp, color = SecondaryText)
                    Text(
                        text = peak.difficultyLevel.ifBlank { "N/A" },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Orange,
                        modifier = Modifier
                            .background(Orange.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Description & Contributor
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SecondaryText.copy(alpha = 0.06f), RoundedCornerShape(8.dp))
                    .padding(10.dp)
            ) {
                Text(
                    text = peak.descriptionText.ifBlank { "No description provided." },
                    fontSize = 12.sp,
                    color = SecondaryText,
                    maxLines = 3
                )
                if (!peak.displayContributorName.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = Purple, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Submitted by: ${peak.displayContributorName}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = Purple
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Actions
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Reject
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(DestructiveRed.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .clickable { onReject() }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = DestructiveRed, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reject", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DestructiveRed)
                    }
                }

                // Merge
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(GliderBlue.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .clickable { onMerge() }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CallMerge, contentDescription = null, tint = GliderBlue, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Merge", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GliderBlue)
                    }
                }

                // Approve
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(GliderBlue, RoundedCornerShape(8.dp))
                        .clickable { onApprove() }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Approve", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun PendingGpsCard(
    peak: Mountain,
    onViewMap: () -> Unit,
    onReject: () -> Unit,
    onApprove: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Info
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(peak.name, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = PrimaryText)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("📍 ${peak.pendingRegion ?: peak.region}", fontSize = 12.sp, color = SecondaryText)
                }
                Text(
                    text = "GPS Proposal",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = GliderBlue,
                    modifier = Modifier
                        .background(GliderBlue.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Coordinates & Contributor
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SecondaryText.copy(alpha = 0.06f), RoundedCornerShape(8.dp))
                    .padding(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(GlobeLocationPin, contentDescription = null, tint = Orange, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = String.format("%.6f, %.6f", peak.pendingLatitude ?: 0.0, peak.pendingLongitude ?: 0.0),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryText
                    )
                }
                
                if (!peak.displayPendingContributorName.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = Purple, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Submitted by: ${peak.displayPendingContributorName}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = Purple
                        )
                    }
                }
                
                if (peak.pendingVerifications > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = Green, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "⭐️ Upvoted by ${peak.pendingVerifications} community mountaineer" + if (peak.pendingVerifications > 1) "s" else "",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Green
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Actions
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // View Map
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(SecondaryText.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .clickable { onViewMap() }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Map, contentDescription = null, tint = PrimaryText, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("View Map", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PrimaryText)
                    }
                }

                // Reject
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(DestructiveRed.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .clickable { onReject() }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = DestructiveRed, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reject", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DestructiveRed)
                    }
                }

                // Approve
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(GliderBlue, RoundedCornerShape(8.dp))
                        .clickable { onApprove() }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Approve", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MergeMountainSelectionDialog(
    duplicatePeak: Mountain,
    publicPeaks: List<Mountain>,
    onDismiss: () -> Unit,
    onSelectTarget: (Mountain) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    
    val filteredPeaks = remember(searchQuery, publicPeaks) {
        if (searchQuery.isBlank()) {
            publicPeaks.take(50)
        } else {
            publicPeaks.filter {
                it.name.contains(searchQuery, ignoreCase = true) || 
                it.region.contains(searchQuery, ignoreCase = true)
            }.take(50)
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = PageBackground) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Alert
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DestructiveRed.copy(alpha = 0.08f))
                    .padding(16.dp)
            ) {
                Text("Merging Duplicate Entry:", fontSize = 12.sp, color = SecondaryText)
                Text(duplicatePeak.name, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = DestructiveRed)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Select the canonical, official PataGilid mountain below to merge into. All hike logs pointing to this duplicate will be re-linked without data loss.",
                    fontSize = 11.sp,
                    color = SecondaryText
                )
            }
            
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search official mountain name...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
            
            Text(
                "Official Mountain on List",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = SecondaryText,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                items(filteredPeaks, key = { it.id }) { target ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectTarget(target) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(target.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = PrimaryText)
                            Text("${target.elevationMASL} MASL • ${target.region}", fontSize = 11.sp, color = SecondaryText)
                        }
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = GliderBlue, modifier = Modifier.size(20.dp))
                    }
                    HorizontalDivider(color = SecondaryText.copy(alpha = 0.2f), thickness = 0.5.dp)
                }
            }
        }
    }
}
