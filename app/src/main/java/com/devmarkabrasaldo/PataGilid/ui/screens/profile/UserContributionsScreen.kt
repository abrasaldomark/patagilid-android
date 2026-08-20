package com.devmarkabrasaldo.PataGilid.ui.screens.profile

import com.devmarkabrasaldo.PataGilid.ui.theme.GliderBlue
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import kotlinx.coroutines.launch
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.devmarkabrasaldo.PataGilid.data.repository.AuthRepository
import com.devmarkabrasaldo.PataGilid.data.repository.MountainRepository
import com.devmarkabrasaldo.PataGilid.domain.models.Mountain
import com.devmarkabrasaldo.PataGilid.domain.models.CoordinateSubmission
import com.devmarkabrasaldo.PataGilid.ui.screens.mountains.MountainsViewModel
import com.devmarkabrasaldo.PataGilid.ui.components.SwipeToReveal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserContributionsScreen(
    mountainRepository: MountainRepository,
    authRepository: AuthRepository,
    onNavigateBack: () -> Unit,
    onEditMountain: (String) -> Unit,
    onViewMountain: (String) -> Unit,
    onViewGpsSubmission: (String) -> Unit
) {
    val factory = remember { MountainsViewModel.Factory(mountainRepository) }
    val viewModel: MountainsViewModel = viewModel(factory = factory)
    
    val userEmail = remember { authRepository.currentUser.value?.email ?: "" }
    val coroutineScope = rememberCoroutineScope()
    
    val pendingMountains by viewModel.userPendingMountains(userEmail).collectAsState(initial = emptyList())
    var pendingGps by remember { mutableStateOf<List<CoordinateSubmission>>(emptyList()) }
    LaunchedEffect(userEmail) {
        if (userEmail.isNotBlank()) {
            pendingGps = mountainRepository.getUserCoordinateSubmissions(userEmail)
        }
    }
    val approvedMountains by viewModel.userApprovedMountains(userEmail).collectAsState(initial = emptyList())

    var selectedPendingMountain by remember { mutableStateOf<Mountain?>(null) }
    var selectedPendingGps by remember { mutableStateOf<CoordinateSubmission?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }


    val refreshGps = {
        coroutineScope.launch {
            if (userEmail.isNotBlank()) {
                pendingGps = mountainRepository.getUserCoordinateSubmissions(userEmail)
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Confirm Deletion") },
            text = { Text("Are you sure you want to delete this pending request? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        if (selectedPendingMountain != null) {
                            viewModel.deletePendingMountain(
                                mountainId = selectedPendingMountain!!.id,
                                onSuccess = { selectedPendingMountain = null },
                                onError = {}
                            )
                        } else if (selectedPendingGps != null) {
                            viewModel.deleteCoordinateSubmission(
                                submissionId = selectedPendingGps!!.id,
                                onSuccess = { 
                                    selectedPendingGps = null
                                    refreshGps()
                                },
                                onError = {}
                            )
                        }
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }



    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Contributions") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (userEmail.isBlank()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Please sign in to view your contributions.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else if (pendingMountains.isEmpty() && pendingGps.isEmpty() && approvedMountains.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 40.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    modifier = Modifier.size(60.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No Contributions Yet",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "When you submit new mountains or calibrate GPS coordinates, they will appear here.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp)
            ) {
                if (pendingMountains.isNotEmpty()) {
                    item {
                        Text(
                            text = "Pending Mountains (${pendingMountains.size})",
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    items(pendingMountains) { peak ->
                        SwipeToReveal(
                            modifier = Modifier.padding(vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp),
                            onDelete = {
                                selectedPendingMountain = peak
                                selectedPendingGps = null
                                showDeleteConfirm = true
                            }
                        ) {
                            ContributionCard(
                                peak = peak, 
                                status = "Pending", 
                                statusColor = Color(0xFFFFA500),
                                modifier = Modifier.clickable { onViewMountain(peak.id) }
                            )
                        }
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }

                if (pendingGps.isNotEmpty()) {
                    item {
                        Text(
                            text = "GPS Calibrations (${pendingGps.size})",
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    items(pendingGps) { submission ->
                        if (submission.status == "PENDING") {
                            SwipeToReveal(
                                modifier = Modifier.padding(vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp),
                                onDelete = {
                                    selectedPendingGps = submission
                                    selectedPendingMountain = null
                                    showDeleteConfirm = true
                                }
                            ) {
                                GpsContributionCard(
                                    submission = submission, 
                                    status = submission.status, 
                                    statusColor = Color(0xFFFFA500),
                                    modifier = Modifier.clickable { onViewGpsSubmission(submission.id) }
                                )
                            }
                        } else {
                            Box(modifier = Modifier.padding(vertical = 4.dp)) {
                                GpsContributionCard(
                                    submission = submission, 
                                    status = submission.status, 
                                    statusColor = when(submission.status) {
                                        "APPROVED" -> Color(0xFF4CAF50)
                                        "REJECTED", "DUPLICATE" -> Color(0xFFF44336)
                                        else -> Color.Gray
                                    },
                                    modifier = Modifier.clickable { onViewGpsSubmission(submission.id) }
                                )
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }

                if (approvedMountains.isNotEmpty()) {
                    item {
                        Text(
                            text = "Approved Contributions (${approvedMountains.size})",
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    items(approvedMountains) { peak ->
                        Box(modifier = Modifier.padding(vertical = 4.dp)) {
                            ContributionCard(
                                peak = peak, 
                                status = "Approved", 
                                statusColor = Color(0xFF4CAF50),
                                modifier = Modifier.clickable { onViewMountain(peak.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ContributionCard(peak: Mountain, status: String, statusColor: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = peak.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "📍 ${peak.region}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = status,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = statusColor,
                    modifier = Modifier
                        .background(statusColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = peak.descriptionText,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun GpsContributionCard(submission: CoordinateSubmission, status: String, statusColor: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "GPS Calibration",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "📍 ${submission.region}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = status,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = statusColor,
                    modifier = Modifier
                        .background(statusColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = String.format("Coordinates: %.6f, %.6f", submission.latitude, submission.longitude),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
