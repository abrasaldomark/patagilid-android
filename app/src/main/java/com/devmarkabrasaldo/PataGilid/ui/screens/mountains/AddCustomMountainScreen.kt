package com.devmarkabrasaldo.PataGilid.ui.screens.mountains

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.rememberAsyncImagePainter
import com.devmarkabrasaldo.PataGilid.di.AppContainer
import com.devmarkabrasaldo.PataGilid.domain.models.IslandGroup
import com.devmarkabrasaldo.PataGilid.domain.models.RegionHelper
import kotlinx.coroutines.launch

private val defaultRegionsByIslandGroup = RegionHelper.canonicalRegionsByIslandGroup

private val difficultyOptions = listOf(
    "1/9 (Minor)",
    "2/9 (Minor)",
    "3/9 (Minor)",
    "4/9 (Minor)",
    "5/9 (Major)",
    "6/9 (Major)",
    "7/9 (Major)",
    "8/9 (Major)",
    "9/9 (Major)"
)

private val trailClassOptions = listOf(
    "Class 1",
    "Class 1-2",
    "Class 2",
    "Class 2-3",
    "Class 3",
    "Class 4",
    "Class 5 (Technical)"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCustomMountainScreen(
    container: AppContainer,
    onNavigateBack: () -> Unit
) {
    val repository = container.mountainRepository
    val photoUploadService = container.photoUploadService
    val userPhotoService = container.userMountainPhotoService

    val coroutineScope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var elevation by remember { mutableStateOf("") }
    var selectedIsland by remember { mutableStateOf(IslandGroup.LUZON) }
    var region by remember { mutableStateOf("CAR (Cordillera Administrative Region)") }
    var difficulty by remember { mutableStateOf("3/9 (Minor)") }
    var trailClass by remember { mutableStateOf("Class 1-2") }
    var description by remember { mutableStateOf("") }
    var selectedPhotoUri by remember { mutableStateOf<Uri?>(null) }
    
    var isSubmitting by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("Saving mountain...") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    var showRegionDialog by remember { mutableStateOf(false) }
    var showDifficultyDialog by remember { mutableStateOf(false) }
    var showTrailClassDialog by remember { mutableStateOf(false) }

    val allMountains by repository.allMountainsByName.collectAsState(initial = emptyList())
    
    val applicableRegions = remember(selectedIsland, allMountains) {
        val defaults = defaultRegionsByIslandGroup[selectedIsland] ?: emptyList()
        val dynamic = allMountains
            .filter { it.islandGroup.equals(selectedIsland.displayName, ignoreCase = true) || it.islandGroup.equals(selectedIsland.name, ignoreCase = true) }
            .map { it.region }
            .filter { it.isNotBlank() }
        RegionHelper.sortRegions(defaults + dynamic)
    }

    // Update region defaults when switching island groups if not in new group
    LaunchedEffect(applicableRegions) {
        if (region !in applicableRegions && applicableRegions.isNotEmpty()) {
            region = applicableRegions.first()
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            selectedPhotoUri = uri
        }
    }

    val isFormValid = name.isNotBlank() && elevation.toIntOrNull() != null && !isSubmitting

    val submitAction: () -> Unit = {
        val elevInt = elevation.toIntOrNull()
        if (name.isBlank() || elevInt == null) {
            errorMessage = "Please enter a valid Mountain Name and numerical Elevation in MASL."
        } else {
            isSubmitting = true
            errorMessage = null
            coroutineScope.launch {
                try {
                    statusText = "Saving mountain..."
                    val mountainId = repository.submitCustomMountain(
                        name = name.trim(),
                        description = description.trim(),
                        elevationMASL = elevInt,
                        region = region,
                        islandGroup = selectedIsland.displayName,
                        difficultyLevel = difficulty,
                        trailClass = trailClass
                    )
                    if (selectedPhotoUri != null) {
                        statusText = "Saving photo..."
                        try {
                            val asset = photoUploadService.resolveAssetFromUri(selectedPhotoUri!!)
                            if (asset != null) {
                                val uploadedUrls = photoUploadService.uploadPhotos(listOf(asset))
                                val driveUrl = uploadedUrls.firstOrNull()
                                if (driveUrl != null) {
                                    userPhotoService.savePhoto(mountainId, driveUrl)
                                }
                            }
                        } catch (photoEx: Exception) {
                            Log.e("AddCustomMountain", "Cover photo upload failed: ${photoEx.localizedMessage}", photoEx)
                        }
                    }
                    isSubmitting = false
                    onNavigateBack()
                } catch (e: Exception) {
                    isSubmitting = false
                    errorMessage = "Submission failed: ${e.localizedMessage}"
                }
            }
        }
    }

    Scaffold(
        containerColor = Color(0xFFF2F4F8),
        topBar = {
            Surface(
                color = Color(0xFFF2F4F8),
                shadowElevation = 0.dp,
                modifier = Modifier.fillMaxWidth().statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Cancel Button Capsule
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFFE5ECF4),
                        modifier = Modifier
                            .height(40.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .clickable(enabled = !isSubmitting) { onNavigateBack() }
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 18.dp)
                        ) {
                            Text(
                                text = "Cancel",
                                color = Color(0xFF1A73E8),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Title
                    Text(
                        text = "Contribute Mountain",
                        color = Color(0xFF1A1A1A),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    // Next / Submit Button Capsule
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isFormValid) Color(0xFF1A73E8) else Color(0xFFE5ECF4),
                        modifier = Modifier
                            .height(40.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .clickable(enabled = isFormValid) { submitAction() }
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 20.dp)
                        ) {
                            Text(
                                text = "Next",
                                color = if (isFormValid) Color.White else Color(0xFF9AA0A6),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            // Glider Blue Info Card ("Contributing to PataGilid List")
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFFE8F0FE),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = Color(0xFF1A73E8),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Contributing to PataGilid List",
                            color = Color(0xFF1A1A1A),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Use this form to submit an unlisted mountain or trail to the national list. Your submission will be reviewed by administrators before becoming visible to all mountaineers.",
                            color = Color(0xFF5F6368),
                            fontSize = 13.5.sp,
                            lineHeight = 19.sp
                        )
                    }
                }
            }

            // Section 1: Mountain Identification
            Column {
                Text(
                    text = "Mountain Identification",
                    color = Color(0xFF70757A),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                )
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        FormTextField(
                            value = name,
                            onValueChange = { name = it },
                            placeholder = "Mountain Name (e.g. Mt. Tagapo)"
                        )
                        HorizontalDivider(color = Color(0xFFEAEDF1), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
                        FormTextField(
                            value = elevation,
                            onValueChange = { elevation = it },
                            placeholder = "Elevation in MASL (e.g. 270)",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                }
            }

            // Section 2: Location
            Column {
                Text(
                    text = "Location",
                    color = Color(0xFF70757A),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                )
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        // Segmented Control (Sliding Island Group Switcher)
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFECEFF4),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .height(44.dp)
                        ) {
                            Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                                IslandGroup.entries.forEach { island ->
                                    val isSelected = selectedIsland == island
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .padding(3.dp)
                                            .clip(RoundedCornerShape(11.dp))
                                            .background(if (isSelected) Color.White else Color.Transparent)
                                            .clickable { selectedIsland = island },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = island.displayName,
                                            color = if (isSelected) Color(0xFF1A1A1A) else Color(0xFF5F6368),
                                            fontSize = 14.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = Color(0xFFEAEDF1), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))

                        // Region Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showRegionDialog = true }
                                .padding(horizontal = 16.dp, vertical = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Region",
                                color = Color(0xFF1A1A1A),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = region,
                                    color = Color(0xFF1A73E8),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Select Region",
                                    tint = Color(0xFF1A73E8),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Section 3: Experienced Difficulty & Terrain
            Column {
                Text(
                    text = "Experienced Difficulty & Terrain",
                    color = Color(0xFF70757A),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                )
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        // Difficulty Rating Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showDifficultyDialog = true }
                                .padding(horizontal = 16.dp, vertical = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Difficulty Rating",
                                color = Color(0xFF1A1A1A),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = difficulty,
                                    color = Color(0xFF1A73E8),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Select Difficulty",
                                    tint = Color(0xFF1A73E8),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        HorizontalDivider(color = Color(0xFFEAEDF1), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
                        // Trail Class Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showTrailClassDialog = true }
                                .padding(horizontal = 16.dp, vertical = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Trail Class",
                                color = Color(0xFF1A1A1A),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = trailClass,
                                    color = Color(0xFF1A73E8),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Select Trail Class",
                                    tint = Color(0xFF1A73E8),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Section 4: Brief Description (Optional)
            Column {
                Text(
                    text = "Brief Description (Optional)",
                    color = Color(0xFF70757A),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                )
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FormTextField(
                        value = description,
                        onValueChange = { description = it },
                        placeholder = "Enter route itinerary, jump-off points, water source notes, or guide details...",
                        singleLine = false,
                        minLines = 4,
                        modifier = Modifier.defaultMinSize(minHeight = 110.dp)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Submitted mountains are immediately available for your personal summit logs. They will display on the nationwide public list once verified by a PataGilid admin.",
                    color = Color(0xFF70757A),
                    fontSize = 12.5.sp,
                    lineHeight = 17.sp,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            // Section 5: Personal Cover Photo (Optional)
            Column {
                Text(
                    text = "Personal Cover Photo (Optional)",
                    color = Color(0xFF70757A),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                )
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (selectedPhotoUri == null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(
                                            ActivityResultContracts.PickVisualMedia.ImageOnly
                                        )
                                    )
                                }
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AddPhotoAlternate,
                                    contentDescription = null,
                                    tint = Color(0xFF1A73E8),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Add Personal Cover Photo",
                                    color = Color(0xFF1A1A1A),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = Color(0xFF9AA0A6),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    } else {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Image(
                                painter = rememberAsyncImagePainter(model = selectedPhotoUri),
                                contentDescription = "Personal Cover Photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(14.dp))
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(
                                                ActivityResultContracts.PickVisualMedia.ImageOnly
                                            )
                                        )
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AddPhotoAlternate,
                                        contentDescription = null,
                                        tint = Color(0xFF1A73E8),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Change Photo", color = Color(0xFF1A73E8), fontWeight = FontWeight.SemiBold)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                TextButton(
                                    onClick = { selectedPhotoUri = null }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = Color(0xFFD32F2F),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Remove", color = Color(0xFFD32F2F), fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Set a custom cover image for this mountain. This photo is private and visible solely on your account.",
                    color = Color(0xFF70757A),
                    fontSize = 12.5.sp,
                    lineHeight = 17.sp,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            if (errorMessage != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFDEDED),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errorMessage!!,
                        color = Color(0xFFD32F2F),
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Main Submission Button
            Button(
                onClick = submitAction,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1A73E8),
                    disabledContainerColor = Color(0xFFB0C4DE)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                enabled = isFormValid && !isSubmitting
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(statusText, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.Terrain, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Submit Mountain for Moderation", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Region Selection Dialog
        if (showRegionDialog) {
            Dialog(onDismissRequest = { showRegionDialog = false }) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Select Region (${selectedIsland.displayName})",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1A1A1A)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 380.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            applicableRegions.forEach { reg ->
                                val isSelected = (reg == region)
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) Color(0xFFE8F0FE) else Color.Transparent,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            region = reg
                                            showRegionDialog = false
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = reg,
                                            color = if (isSelected) Color(0xFF1A73E8) else Color(0xFF1A1A1A),
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 15.sp
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        TextButton(
                            onClick = { showRegionDialog = false },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Close", color = Color(0xFF5F6368), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Difficulty Selection Dialog
        if (showDifficultyDialog) {
            Dialog(onDismissRequest = { showDifficultyDialog = false }) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Select Difficulty Rating",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1A1A1A)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 380.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            difficultyOptions.forEach { diff ->
                                val isSelected = (diff == difficulty)
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) Color(0xFFE8F0FE) else Color.Transparent,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            difficulty = diff
                                            showDifficultyDialog = false
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = diff,
                                            color = if (isSelected) Color(0xFF1A73E8) else Color(0xFF1A1A1A),
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 15.sp
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        TextButton(
                            onClick = { showDifficultyDialog = false },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Close", color = Color(0xFF5F6368), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Trail Class Selection Dialog
        if (showTrailClassDialog) {
            Dialog(onDismissRequest = { showTrailClassDialog = false }) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Select Trail Class",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1A1A1A)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 380.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            trailClassOptions.forEach { tClass ->
                                val isSelected = (tClass == trailClass)
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) Color(0xFFE8F0FE) else Color.Transparent,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            trailClass = tClass
                                            showTrailClassDialog = false
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = tClass,
                                            color = if (isSelected) Color(0xFF1A73E8) else Color(0xFF1A1A1A),
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 15.sp
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        TextButton(
                            onClick = { showTrailClassDialog = false },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Close", color = Color(0xFF5F6368), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FormTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    singleLine: Boolean = true,
    minLines: Int = 1
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = Color(0xFF9AA0A6), fontSize = 15.sp) },
        textStyle = TextStyle(fontSize = 16.sp, color = Color(0xFF1A1A1A), fontWeight = FontWeight.Medium),
        singleLine = singleLine,
        minLines = minLines,
        keyboardOptions = keyboardOptions,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            cursorColor = Color(0xFF1A73E8)
        ),
        modifier = modifier.fillMaxWidth()
    )
}
