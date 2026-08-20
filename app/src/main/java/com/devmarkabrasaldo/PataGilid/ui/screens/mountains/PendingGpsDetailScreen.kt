package com.devmarkabrasaldo.PataGilid.ui.screens.mountains

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devmarkabrasaldo.PataGilid.di.AppContainer
import com.devmarkabrasaldo.PataGilid.domain.models.CoordinateSubmission
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PendingGpsDetailScreen(
    submissionId: String,
    container: AppContainer,
    onNavigateBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var submission by remember { mutableStateOf<CoordinateSubmission?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var showMapCalibration by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current

    val loadSubmission = {
        coroutineScope.launch {
            isLoading = true
            submission = container.mountainRepository.getCoordinateSubmission(submissionId)
            isLoading = false
        }
    }

    LaunchedEffect(submissionId) {
        loadSubmission()
    }

    if (showMapCalibration && submission != null) {
        MapCalibrationDialog(
            onDismiss = { showMapCalibration = false },
            initialLocation = LatLng(submission!!.latitude, submission!!.longitude),
            onLocationPinned = { latLng, _ ->
                coroutineScope.launch {
                    showMapCalibration = false
                    isLoading = true
                    
                    var regionStr = submission!!.region
                    try {
                        val geocoder = android.location.Geocoder(context, java.util.Locale.getDefault())
                        val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
                        if (!addresses.isNullOrEmpty()) {
                            val address = addresses[0]
                            val adminArea = address.adminArea ?: ""
                            val subAdminArea = address.subAdminArea ?: ""
                            val locality = address.locality ?: ""
                            val regionResult = com.devmarkabrasaldo.PataGilid.domain.models.RegionHelper.mapToInternalRegion(adminArea, subAdminArea, locality)
                            if (regionResult.first != null) {
                                regionStr = regionResult.first!!
                            }
                        }
                    } catch (e: Exception) {
                        // Fallback to previous region if geocoder fails
                    }
                    
                    container.mountainRepository.updateCoordinateSubmission(
                        submissionId = submission!!.id,
                        lat = latLng.latitude,
                        lon = latLng.longitude,
                        region = regionStr
                    )
                    loadSubmission()
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("GPS Calibration", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .background(Color(0xFFF3F4F6), CircleShape)
                            .size(40.dp)
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", modifier = Modifier.size(20.dp), tint = Color.Black)
                    }
                },
                actions = {
                    val currentUserEmail = container.authRepository.currentUser.value?.email
                    if (submission?.status == "PENDING" && submission?.contributorEmail == currentUserEmail && currentUserEmail != null) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White,
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .clickable { showMapCalibration = true },
                            shadowElevation = 2.dp
                        ) {
                            Text(
                                text = "Edit",
                                color = Color(0xFF007AFF),
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF3B82F6))
            }
        } else if (submission == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Calibration not found.")
            }
        } else {
            val sub = submission!!
            val latLng = LatLng(sub.latitude, sub.longitude)
            val cameraPositionState = rememberCameraPositionState {
                position = CameraPosition.fromLatLngZoom(latLng, 14f)
            }
            
            LaunchedEffect(latLng) {
                cameraPositionState.position = CameraPosition.fromLatLngZoom(latLng, 14f)
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cameraPositionState,
                        properties = MapProperties(mapType = MapType.TERRAIN),
                        uiSettings = MapUiSettings(zoomControlsEnabled = false)
                    ) {
                        Marker(
                            state = MarkerState(position = latLng),
                            title = "Submitted Location"
                        )
                    }
                }
                
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text("Region", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(sub.region, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 4.dp))
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text("Coordinates", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(String.format("%.6f, %.6f", sub.latitude, sub.longitude), fontSize = 16.sp, modifier = Modifier.padding(top = 4.dp))
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text("Status", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = sub.status,
                            color = when(sub.status) {
                                "APPROVED" -> Color(0xFF4CAF50)
                                "REJECTED", "DUPLICATE" -> Color(0xFFF44336)
                                else -> Color(0xFFFFA500)
                            },
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .background(
                                    color = when(sub.status) {
                                        "APPROVED" -> Color(0xFF4CAF50).copy(alpha=0.15f)
                                        "REJECTED", "DUPLICATE" -> Color(0xFFF44336).copy(alpha=0.15f)
                                        else -> Color(0xFFFFA500).copy(alpha=0.15f)
                                    },
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}
