package com.devmarkabrasaldo.PataGilid.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
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
import com.devmarkabrasaldo.PataGilid.ui.screens.mountains.MountainsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserContributionsScreen(
    mountainRepository: MountainRepository,
    authRepository: AuthRepository,
    onNavigateBack: () -> Unit
) {
    val factory = remember { MountainsViewModel.Factory(mountainRepository) }
    val viewModel: MountainsViewModel = viewModel(factory = factory)
    
    val userEmail = remember { authRepository.currentUser.value?.email ?: "" }
    
    val pendingMountains by viewModel.userPendingMountains(userEmail).collectAsState(initial = emptyList())
    val pendingGps by viewModel.userPendingGps(userEmail).collectAsState(initial = emptyList())
    val approvedMountains by viewModel.userApprovedMountains(userEmail).collectAsState(initial = emptyList())

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
                Text("Please sign in to view your contributions.", color = Color.Gray)
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
                    tint = Color.Gray
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No Contributions Yet",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "When you submit new mountains or calibrate GPS coordinates, they will appear here.",
                    color = Color.Gray,
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
                            color = Color.Gray,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    items(pendingMountains) { peak ->
                        ContributionCard(peak = peak, status = "Pending", statusColor = Color(0xFFFFA500))
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }

                if (pendingGps.isNotEmpty()) {
                    item {
                        Text(
                            text = "Pending GPS Calibrations (${pendingGps.size})",
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Gray,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    items(pendingGps) { peak ->
                        ContributionCard(peak = peak, status = "Pending", statusColor = Color(0xFFFFA500))
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }

                if (approvedMountains.isNotEmpty()) {
                    item {
                        Text(
                            text = "Approved Contributions (${approvedMountains.size})",
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Gray,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    items(approvedMountains) { peak ->
                        ContributionCard(peak = peak, status = "Approved", statusColor = Color(0xFF4CAF50))
                    }
                }
            }
        }
    }
}

@Composable
fun ContributionCard(peak: Mountain, status: String, statusColor: Color) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
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
                        color = Color.Black
                    )
                    Text(
                        text = "📍 ${peak.region}",
                        fontSize = 12.sp,
                        color = Color.Gray
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
                color = Color.Gray,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
