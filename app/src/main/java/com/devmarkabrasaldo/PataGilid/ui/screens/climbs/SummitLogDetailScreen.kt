package com.devmarkabrasaldo.PataGilid.ui.screens.climbs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import com.devmarkabrasaldo.PataGilid.ui.theme.UTurnLeft
import com.devmarkabrasaldo.PataGilid.ui.theme.Start
import com.devmarkabrasaldo.PataGilid.ui.theme.FlagCheck
import com.devmarkabrasaldo.PataGilid.ui.theme.GlobeLocationPin
import com.devmarkabrasaldo.PataGilid.ui.theme.Elevation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devmarkabrasaldo.PataGilid.di.AppContainer
import com.devmarkabrasaldo.PataGilid.domain.models.HikeLog
import com.devmarkabrasaldo.PataGilid.domain.models.Mountain
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// Helper Composable for Section Label
@Composable
fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        color = Color(0xFF8E8E93),
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp)
    )
}

// Helper Composable for List Row
@Composable
fun InfoRow(
    icon: @Composable () -> Unit,
    label: String,
    value: String,
    showChevron: Boolean = false,
    valueColor: Color = Color.Black,
    onClick: (() -> Unit)? = null
) {
    val modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
            icon()
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            color = Color(0xFF8E8E93),
            fontSize = 16.sp,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            color = valueColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
        if (showChevron) {
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFFC7C7CC), modifier = Modifier.size(20.dp))
        }
    }
}

// Helper for duration formatting
fun formatDuration(start: Long, end: Long): String {
    if (start == 0L || end == 0L || end < start) return "N/A"
    val diffMillis = end - start
    val hours = diffMillis / (1000 * 60 * 60)
    val mins = (diffMillis / (1000 * 60)) % 60
    if (hours > 0 && mins > 0) return "${hours}h ${mins}m"
    if (hours > 0) return "${hours}h"
    return "${mins}m"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummitLogDetailScreen(
    logId: String,
    container: AppContainer,
    onNavigateBack: () -> Unit,
    onNavigateToGallery: (Int, List<String>) -> Unit // Kept for signature compatibility
) {
    val coroutineScope = rememberCoroutineScope()
    var log by remember { mutableStateOf<HikeLog?>(null) }
    var mountain by remember { mutableStateOf<Mountain?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    val PageBackground = Color(0xFFF2F2F7) // iOS grouped list background

    LaunchedEffect(logId) {
        isLoading = true
        val logs = container.mountainRepository.getUserHikeLogs()
        val match = logs.find { it.id == logId }
        log = match
        if (match != null) {
            mountain = container.mountainRepository.getMountain(match.mountainId)
        }
        isLoading = false
    }

    Scaffold(
        containerColor = PageBackground,
        topBar = {
            TopAppBar(
                title = { Text(mountain?.name ?: "", color = Color.Black, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    Surface(
                        shape = CircleShape,
                        color = Color.White,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .size(36.dp)
                            .clickable { onNavigateBack() },
                        shadowElevation = 2.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Back", tint = Color.Black, modifier = Modifier.size(18.dp))
                        }
                    }
                },
                actions = {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .clickable { /* TODO Edit Action */ },
                        shadowElevation = 2.dp
                    ) {
                        Text(
                            text = "Edit",
                            color = Color(0xFF007AFF),
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PageBackground)
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF1A73E8))
            }
        } else if (log == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Summit log not found or deleted.", color = Color.Black)
            }
        } else {
            val hikeLog = log ?: return@Scaffold
            val mtn = mountain
            
            // Formatters
            val dateFormatter = SimpleDateFormat("EEEE, MMMM d, yyyy 'at' h:mm a", Locale.getDefault())
            val timeFormatter = SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault())
            
            val startStr = if (hikeLog.dateTimeStart > 0L) timeFormatter.format(Date(hikeLog.dateTimeStart)) else "N/A"
            val endStr = if (hikeLog.dateTimeEnd > 0L) timeFormatter.format(Date(hikeLog.dateTimeEnd)) else "N/A"
            val durationStr = formatDuration(hikeLog.dateTimeStart, hikeLog.dateTimeEnd)
            
            val dateString = if (hikeLog.dateTimeStart > 0L) dateFormatter.format(Date(hikeLog.dateTimeStart)) else "Unknown Date"

            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(12.dp))
                
                // Header Card
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(vertical = 32.dp, horizontal = 16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .background(Color(0xFFE5EFFF), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Terrain, contentDescription = null, tint = Color(0xFF007AFF), modifier = Modifier.size(40.dp))
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (hikeLog.didSummit) "Summited" else "Attempted",
                            color = if (hikeLog.didSummit) Color(0xFF007AFF) else Color(0xFFFF9500),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = dateString,
                            color = Color(0xFF8E8E93),
                            fontSize = 15.sp
                        )
                    }
                }

                // SUMMIT DETAILS Section
                SectionLabel("SUMMIT DETAILS")
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        InfoRow(
                            icon = { Icon(Icons.Default.Terrain, contentDescription = null, tint = Color(0xFF007AFF)) },
                            label = "Mountain",
                            value = mtn?.name ?: "Unknown"
                        )
                        HorizontalDivider(modifier = Modifier.padding(start = 52.dp), color = Color(0xFFE5E5EA), thickness = 0.5.dp)
                        InfoRow(
                            icon = { Icon(Elevation, contentDescription = null, tint = Color(0xFF8E8E93)) },
                            label = "Elevation",
                            value = "${mtn?.elevationMASL ?: 0} MASL"
                        )
                        HorizontalDivider(modifier = Modifier.padding(start = 52.dp), color = Color(0xFFE5E5EA), thickness = 0.5.dp)
                        InfoRow(
                            icon = { Icon(Icons.Default.Place, contentDescription = null, tint = Color(0xFFFF3B30)) },
                            label = "Region",
                            value = mtn?.region ?: "Unknown"
                        )
                        HorizontalDivider(modifier = Modifier.padding(start = 52.dp), color = Color(0xFFE5E5EA), thickness = 0.5.dp)
                        InfoRow(
                            icon = { Icon(Icons.Default.Public, contentDescription = null, tint = Color(0xFF007AFF)) },
                            label = "Island Group",
                            value = mtn?.islandGroup ?: "Unknown"
                        )
                        HorizontalDivider(modifier = Modifier.padding(start = 52.dp), color = Color(0xFFE5E5EA), thickness = 0.5.dp)
                        val latLng = if (mtn?.latitude != null && mtn.longitude != null) "${mtn.latitude}, ${mtn.longitude}" else "N/A"
                        InfoRow(
                            icon = { Icon(GlobeLocationPin, contentDescription = null, tint = Color(0xFF007AFF)) },
                            label = "Coordinates",
                            value = latLng,
                            showChevron = true
                        )
                    }
                }

                // ROUTE EXPERIENCED Section
                SectionLabel("ROUTE EXPERIENCED")
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        if (hikeLog.isTraverse == true) {
                            InfoRow(
                                icon = { Icon(Icons.Default.Route, contentDescription = null, tint = Color(0xFFAF52DE)) },
                                label = "Climb Style",
                                value = "Traverse"
                            )
                            HorizontalDivider(modifier = Modifier.padding(start = 52.dp), color = Color(0xFFE5E5EA), thickness = 0.5.dp)
                            InfoRow(
                                icon = { Icon(Start, contentDescription = null, tint = Color(0xFF8E8E93)) },
                                label = "Start Trail",
                                value = hikeLog.trailName ?: "N/A"
                            )
                            HorizontalDivider(modifier = Modifier.padding(start = 52.dp), color = Color(0xFFE5E5EA), thickness = 0.5.dp)
                            InfoRow(
                                icon = { Icon(FlagCheck, contentDescription = null, tint = Color(0xFFFF3B30)) },
                                label = "Exit Trail",
                                value = hikeLog.exitTrailName ?: "N/A"
                            )
                        } else {
                            InfoRow(
                                icon = { Icon(UTurnLeft, contentDescription = null, tint = Color(0xFF007AFF)) },
                                label = "Climb Style",
                                value = "Back Trail"
                            )
                            HorizontalDivider(modifier = Modifier.padding(start = 52.dp), color = Color(0xFFE5E5EA), thickness = 0.5.dp)
                            InfoRow(
                                icon = { Icon(Icons.Default.SwapVert, contentDescription = null, tint = Color(0xFF8E8E93)) },
                                label = "Route Name",
                                value = hikeLog.trailName ?: "N/A"
                            )
                        }
                        HorizontalDivider(modifier = Modifier.padding(start = 52.dp), color = Color(0xFFE5E5EA), thickness = 0.5.dp)
                        InfoRow(
                            icon = { Icon(Icons.Default.Speed, contentDescription = null, tint = Color(0xFF007AFF)) },
                            label = "Experienced Difficulty",
                            value = hikeLog.trailDifficulty ?: "N/A"
                        )
                        HorizontalDivider(modifier = Modifier.padding(start = 52.dp), color = Color(0xFFE5E5EA), thickness = 0.5.dp)
                        InfoRow(
                            icon = { Icon(Icons.Default.Hiking, contentDescription = null, tint = Color(0xFF8E8E93)) },
                            label = "Technical Trail Class",
                            value = hikeLog.trailClass ?: "N/A"
                        )
                    }
                }

                // ATTEMPT RECORD Section
                SectionLabel("ATTEMPT RECORD")
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        InfoRow(
                            icon = { Icon(Icons.Default.PlayCircle, contentDescription = null, tint = Color(0xFF8E8E93)) },
                            label = "Start",
                            value = startStr
                        )
                        HorizontalDivider(modifier = Modifier.padding(start = 52.dp), color = Color(0xFFE5E5EA), thickness = 0.5.dp)
                        InfoRow(
                            icon = { Icon(Icons.Default.StopCircle, contentDescription = null, tint = Color(0xFFFF3B30)) },
                            label = "End",
                            value = endStr
                        )
                        HorizontalDivider(modifier = Modifier.padding(start = 52.dp), color = Color(0xFFE5E5EA), thickness = 0.5.dp)
                        InfoRow(
                            icon = { Icon(Icons.Outlined.Timer, contentDescription = null, tint = Color(0xFFAF52DE)) },
                            label = "Duration",
                            value = durationStr
                        )
                        HorizontalDivider(modifier = Modifier.padding(start = 52.dp), color = Color(0xFFE5E5EA), thickness = 0.5.dp)
                        val outcomeText = if (hikeLog.didSummit) "Successful Summit" else "Backed Out"
                        val outcomeColor = if (hikeLog.didSummit) Color(0xFF007AFF) else Color(0xFFFF3B30)
                        InfoRow(
                            icon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = outcomeColor) },
                            label = "Outcome",
                            value = outcomeText
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Delete Button
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFFE5E5),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            coroutineScope.launch {
                                container.mountainRepository.deleteHikeLog(logId)
                                onNavigateBack()
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.Delete, contentDescription = null, tint = Color(0xFFFF3B30), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Delete Climb Log",
                            color = Color(0xFFFF3B30),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(60.dp))
            }
        }
    }
}
