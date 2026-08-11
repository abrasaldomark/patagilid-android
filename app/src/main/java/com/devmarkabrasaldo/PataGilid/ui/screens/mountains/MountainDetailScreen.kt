package com.devmarkabrasaldo.PataGilid.ui.screens.mountains

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Terrain
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.devmarkabrasaldo.PataGilid.ui.theme.GlobeLocationPin
import com.devmarkabrasaldo.PataGilid.ui.theme.Elevation
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.android.gms.maps.model.LatLng
import coil.compose.AsyncImage
import com.devmarkabrasaldo.PataGilid.di.AppContainer
import kotlinx.coroutines.launch

val GliderBlue = Color(0xFF3B82F6)
val SummitSteel = Color(0xFF6B7280)
val DarkHero = Color(0xFF1F2937)
val LightCard = Color(0xFFF3F4F6)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MountainDetailScreen(
    mountainId: String,
    container: AppContainer,
    onNavigateBack: () -> Unit,
    onNavigateToLogClimb: (String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val mountainFlow = remember { container.mountainRepository.observeMountain(mountainId) }
    val mountain by mountainFlow.collectAsState(initial = null)
    val isAdmin = remember { container.authRepository.isAdmin }
    
    val customPhotos by container.userMountainPhotoService.customPhotos.collectAsState()
    val personalPhotoUrl = customPhotos[mountainId]
    var isUploadingPhoto by remember { mutableStateOf(false) }

    var showCalibrateDialog by remember { mutableStateOf(false) }
    var showMapDialog by remember { mutableStateOf(false) }
    var showCoordinatesBottomSheet by remember { mutableStateOf(false) }
    var pinnedLocation by remember { mutableStateOf<LatLng?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            isUploadingPhoto = true
            coroutineScope.launch {
                try {
                    val asset = container.photoUploadService.resolveAssetFromUri(uri)
                    if (asset != null) {
                        val urls = container.photoUploadService.uploadPhotos(listOf(asset))
                        if (urls.isNotEmpty()) {
                            container.userMountainPhotoService.savePhoto(mountainId, urls.first())
                        }
                    }
                } catch (e: Exception) {
                    // Handle error if needed
                } finally {
                    isUploadingPhoto = false
                }
            }
        }
    }

    Scaffold(
        containerColor = Color.White,
        topBar = {
            TopAppBar(
                title = { 
                    Text(mountain?.name ?: "", fontWeight = FontWeight.Bold, color = Color.Black) 
                },
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
                    Surface(
                        shape = CircleShape,
                        color = Color.Transparent,
                        border = BorderStroke(1.dp, GliderBlue.copy(alpha = 0.2f)),
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .clip(CircleShape)
                            .clickable { onNavigateToLogClimb(mountainId) }
                    ) {
                        Text(
                            "Add Climb",
                            color = GliderBlue,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        }
    ) { padding ->
        if (mountain == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = GliderBlue)
            }
            return@Scaffold
        }
        val peak = mountain!!

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            // Hero Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkHero)
            ) {
                if (personalPhotoUrl != null) {
                    AsyncImage(
                        model = personalPhotoUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        alpha = 0.6f
                    )
                } else {
                    Icon(
                        Icons.Outlined.Terrain,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.05f),
                        modifier = Modifier
                            .size(200.dp)
                            .align(Alignment.Center)
                            .offset(y = 20.dp)
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Surface(shape = CircleShape, color = GliderBlue) {
                            Text(
                                text = peak.islandGroup.uppercase(),
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }

                        if (!peak.isPubliclyApproved) {
                            Surface(shape = CircleShape, color = Color(0xFFF59E0B)) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Schedule, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Pending Review", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Column {
                        Text(
                            text = peak.name,
                            color = Color.White,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Elevation, contentDescription = null, tint = SummitSteel, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("${peak.elevationMASL} MASL", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Place, contentDescription = null, tint = GliderBlue, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(peak.region, color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Specs
            Text("Mountain Details", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.Black)
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SpecCard(
                    modifier = Modifier.weight(1f),
                    title = "Difficulty",
                    value = peak.difficultyLevel.ifBlank { "Unspecified" },
                    icon = Icons.Default.Speed,
                    iconColor = GliderBlue
                )
                SpecCard(
                    modifier = Modifier.weight(1f),
                    title = "Trail Class",
                    value = peak.trailClass.ifBlank { "Class 1-3" },
                    icon = Icons.Default.Hiking,
                    iconColor = SummitSteel
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            
            if (peak.latitude != null && peak.longitude != null && peak.latitude != 0.0) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = LightCard,
                    modifier = Modifier.fillMaxWidth().clickable { showCoordinatesBottomSheet = true }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(shape = CircleShape, color = GliderBlue, modifier = Modifier.size(36.dp)) {
                            Icon(GlobeLocationPin, contentDescription = null, tint = Color.White, modifier = Modifier.padding(8.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Coordinates", color = SummitSteel, fontSize = 12.sp)
                            Text("${peak.latitude}, ${peak.longitude}", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        Surface(shape = CircleShape, color = GliderBlue, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.CallMade, contentDescription = null, tint = Color.White, modifier = Modifier.padding(6.dp))
                        }
                    }
                }

                if (peak.isVerifiedByCommunity) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFEFF6FF), // Light blue background
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Verified, contentDescription = null, tint = GliderBlue, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("GPS Verified by ${peak.communityVerifications} Explorer${if (peak.communityVerifications == 1) "" else "s"}", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            } else if (peak.pendingLatitude != null && peak.pendingLongitude != null) {
                Spacer(modifier = Modifier.height(12.dp))
                // Pending Calibration Banner
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFFF4EB),
                    border = BorderStroke(1.dp, Color(0xFFFFDAB9)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(Icons.Outlined.Schedule, contentDescription = null, tint = Color(0xFFF57C00), modifier = Modifier.size(28.dp).padding(top = 2.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("GPS Calibration Pending", fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 15.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("A summit location has been submitted for this mountain and is currently awaiting admin verification.", color = Color(0xFF6B7280), fontSize = 13.sp, lineHeight = 18.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        val isSubmitter = container.authRepository.currentUser.value?.email == peak.pendingContributorEmail
                        val pillText = if (isAdmin) {
                            "Approve Pending Review"
                        } else if (isSubmitter) {
                            "Submitted by You • Pending Review"
                        } else {
                            "Submitted by ${peak.displayPendingContributorName ?: "Explorer"} • Pending Review"
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFEBE5DF),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .clickable(enabled = isAdmin) {
                                    if (isAdmin) {
                                        coroutineScope.launch { container.mountainRepository.applyGpsCalibration(mountainId) }
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Outlined.HourglassEmpty, contentDescription = null, tint = Color(0xFF6B7280), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(pillText, color = Color(0xFF6B7280), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFEFF6FF),
                    border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFF3B82F6), modifier = Modifier.size(28.dp).padding(top = 2.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Help Map This Summit", fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 15.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Be the first hiker to pin this mountain! Locate its summit directly on the map to help complete our mountain list.", color = Color(0xFF6B7280), fontSize = 13.sp, lineHeight = 18.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { showCalibrateDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Icon(Icons.Default.AddCircle, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Pin Summit Location", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Mountain Overview
            Text("Mountain Overview", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.Black)
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = LightCard,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = peak.descriptionText.ifBlank { "No detailed trail description recorded yet. Pioneer this route and contribute your trip reports!" },
                    color = SummitSteel,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    modifier = Modifier.padding(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Mountain Photography
            Text("Mountain Photography", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.Black)
            Spacer(modifier = Modifier.height(12.dp))
            
            Surface(
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, LightCard),
                color = Color.White,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = GliderBlue, modifier = Modifier.size(24.dp).padding(top = 2.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(if (personalPhotoUrl == null) "Add Personal Cover Photo" else "Personal Cover Photo Set", fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 15.sp)
                            Text("Set a private cover photo for this mountain. This image is visible only on your account.", color = SummitSteel, fontSize = 12.sp, lineHeight = 16.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { photoPickerLauncher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = GliderBlue),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isUploadingPhoto
                    ) {
                        if (isUploadingPhoto) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Saving photo...", fontWeight = FontWeight.Bold)
                        } else {
                            Icon(if (personalPhotoUrl == null) Icons.Default.PhotoCamera else Icons.Default.Edit, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (personalPhotoUrl == null) "Upload Photo" else "Update Photo", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }

        if (showCalibrateDialog) {
            Dialog(
                onDismissRequest = { showCalibrateDialog = false },
                properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                    Surface(
                        modifier = Modifier.fillMaxWidth().fillMaxHeight(0.95f),
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                        color = Color(0xFFF3F4F6)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            // Top Bar
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = Color.White,
                                    modifier = Modifier.clickable { showCalibrateDialog = false }
                                ) {
                                    Text("Cancel", color = Color(0xFF3B82F6), modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), fontSize = 15.sp)
                                }
                                
                                Spacer(modifier = Modifier.weight(1f))
                                Text("Contribute GPS", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.Black, modifier = Modifier.padding(end = 40.dp))
                                Spacer(modifier = Modifier.weight(1f))
                            }
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            Text("Target Summit", color = Color(0xFF9CA3AF), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color.White,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Mountain Name", color = Color(0xFF9CA3AF), fontSize = 16.sp)
                                        Text(peak.name, color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    }
                                    Divider(color = Color(0xFFF3F4F6), modifier = Modifier.padding(vertical = 12.dp))
                                    Text("Region / Province", color = Color.Black, fontSize = 16.sp)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(peak.region, color = Color(0xFF3B82F6), fontSize = 15.sp)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(Icons.Default.UnfoldMore, contentDescription = null, tint = Color(0xFF3B82F6), modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Submissions are reviewed by our moderation team to verify map integrity before updating public mountain markers.", color = Color(0xFF9CA3AF), fontSize = 12.sp, lineHeight = 16.sp)
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            Text("Summit Location", color = Color(0xFF9CA3AF), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color.White,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column {
                                    Surface(
                                        color = Color(0xFF3B82F6),
                                        modifier = Modifier.fillMaxWidth().clickable { showMapDialog = true }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(16.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text("Pin on Map", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                            Spacer(modifier = Modifier.weight(1f))
                                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                        }
                                    }
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (pinnedLocation == null) {
                                            Icon(Icons.Default.LocationOff, contentDescription = null, tint = Color(0xFF9CA3AF), modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text("No location pinned yet. Tap above to select on map.", color = Color(0xFF9CA3AF), fontSize = 13.sp)
                                        } else {
                                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(String.format("Pinned at: %.5f, %.5f", pinnedLocation!!.latitude, pinnedLocation!!.longitude), color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Tap 'Pin on Map' to visually locate and pin the mountain summit.", color = Color(0xFF9CA3AF), fontSize = 12.sp, lineHeight = 16.sp)
                            
                            Spacer(modifier = Modifier.weight(1f))
                            
                            Button(
                                onClick = {
                                    pinnedLocation?.let { loc ->
                                        coroutineScope.launch {
                                            container.mountainRepository.submitGpsCalibration(
                                                mountainId = mountainId,
                                                latitude = loc.latitude,
                                                longitude = loc.longitude,
                                                region = peak.region
                                            )
                                            showCalibrateDialog = false
                                        }
                                    }
                                },
                                enabled = pinnedLocation != null,
                                shape = RoundedCornerShape(24.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF3B82F6),
                                    disabledContainerColor = Color.White, 
                                    disabledContentColor = Color(0xFFD1D5DB)
                                ),
                                modifier = Modifier.fillMaxWidth().height(56.dp)
                            ) {
                                Text("Submit for Admin Review", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = if (pinnedLocation != null) Color.White else Color.Unspecified)
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }
        }

        if (showMapDialog) {
            MapCalibrationDialog(
                onDismiss = { showMapDialog = false },
                onLocationPinned = { loc, _ -> pinnedLocation = loc },
                initialLocation = pinnedLocation
            )
        }
        
        if (showCoordinatesBottomSheet && peak.latitude != null && peak.longitude != null) {
            val sheetState = rememberModalBottomSheetState()
            ModalBottomSheet(
                onDismissRequest = { showCoordinatesBottomSheet = false },
                sheetState = sheetState,
                containerColor = Color.White
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp)
                ) {
                    Text(
                        text = "More Coordinate Options",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                        color = Color.Black
                    )
                    Divider(color = Color.LightGray.copy(alpha = 0.5f))
                    
                    ListItem(
                        headlineContent = { Text("Copy Coordinates to Clipboard", fontSize = 16.sp, color = Color.Black) },
                        leadingContent = { Icon(Icons.Default.ContentCopy, contentDescription = null, tint = GliderBlue) },
                        modifier = Modifier.clickable {
                            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            val clip = android.content.ClipData.newPlainText("Coordinates", "${peak.latitude}, ${peak.longitude}")
                            clipboard.setPrimaryClip(clip)
                            android.widget.Toast.makeText(context, "Coordinates Copied!", android.widget.Toast.LENGTH_SHORT).show()
                            showCoordinatesBottomSheet = false
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.White)
                    )
                    
                    ListItem(
                        headlineContent = { Text("View on Map", fontSize = 16.sp, color = Color.Black) },
                        leadingContent = { Icon(Icons.Default.Map, contentDescription = null, tint = GliderBlue) },
                        modifier = Modifier.clickable {
                            val gmmIntentUri = Uri.parse("geo:${peak.latitude},${peak.longitude}?q=${peak.latitude},${peak.longitude}(${peak.name})")
                            val mapIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, gmmIntentUri)
                            try {
                                if (mapIntent.resolveActivity(context.packageManager) != null) {
                                    context.startActivity(mapIntent)
                                } else {
                                    val fallbackIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, gmmIntentUri)
                                    context.startActivity(fallbackIntent)
                                }
                            } catch (e: Exception) {
                                android.widget.Toast.makeText(context, "No map application found. Opening browser.", android.widget.Toast.LENGTH_SHORT).show()
                                val browserIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse("https://maps.google.com/?q=${peak.latitude},${peak.longitude}"))
                                try {
                                    context.startActivity(browserIntent)
                                } catch (e2: Exception) {
                                    android.widget.Toast.makeText(context, "Unable to open map.", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                            showCoordinatesBottomSheet = false
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.White)
                    )
                }
            }
        }
    }
}

@Composable
fun SpecCard(modifier: Modifier = Modifier, title: String, value: String, icon: ImageVector, iconColor: Color) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = LightCard,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(title, color = SummitSteel, fontSize = 12.sp)
                Text(value, color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}
