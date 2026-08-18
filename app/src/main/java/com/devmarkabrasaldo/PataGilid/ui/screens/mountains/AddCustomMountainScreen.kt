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
import androidx.compose.ui.draw.alpha
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
import android.location.Geocoder
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import com.devmarkabrasaldo.PataGilid.BuildConfig

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
    onNavigateBack: () -> Unit,
    onMountainAdded: (String, Boolean, Boolean) -> Unit
) {
    val repository = container.mountainRepository
    val photoUploadService = container.photoUploadService
    val userPhotoService = container.userMountainPhotoService
    val context = androidx.compose.ui.platform.LocalContext.current

    val coroutineScope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var elevation by remember { mutableStateOf("") }
    var selectedIsland by remember { mutableStateOf(IslandGroup.LUZON) }
    var region by remember { mutableStateOf("CAR (Cordillera Administrative Region)") }
    var difficulty by remember { mutableStateOf("3/9 (Minor)") }
    var trailClass by remember { mutableStateOf("Class 1-2") }
    var description by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    var showRegionDialog by remember { mutableStateOf(false) }
    var showDifficultyDialog by remember { mutableStateOf(false) }
    var showTrailClassDialog by remember { mutableStateOf(false) }
    
    var showSubmitBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    var showMapDialog by remember { mutableStateOf(false) }
    var pinnedLocation by remember { mutableStateOf<LatLng?>(null) }
    var showOutsidePHAlert by remember { mutableStateOf(false) }
    var showInfoCard by remember { mutableStateOf(true) }
    var isFetchingElevation by remember { mutableStateOf(false) }

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

    val isFormValid = name.isNotBlank() && elevation.toIntOrNull() != null && !isSubmitting

    val submitAction: (Boolean, Boolean) -> Unit = { navigateToMountain, openLog ->
        val elevInt = elevation.toIntOrNull()
        
        val cleanInputName = name.replace(Regex("(?i)Mt\\.?|Mount"), "").trim()
        val isDuplicate = allMountains.any { peak ->
            val cleanDbName = peak.name.replace(Regex("(?i)Mt\\.?|Mount"), "").trim()
            cleanDbName.equals(cleanInputName, ignoreCase = true)
        }

        if (name.isBlank() || elevInt == null) {
            errorMessage = "Please enter a valid Mountain Name and numerical Elevation in MASL."
        } else if (isDuplicate) {
            errorMessage = "\"${name.trim()}\" is already on PataGilid. Please check the name or try another peak."
        } else {
            isSubmitting = true
            errorMessage = null
            coroutineScope.launch {
                try {
                    val mountainId = repository.submitCustomMountain(
                        name = name.trim(),
                        description = description.trim(),
                        elevationMASL = elevInt,
                        latitude = pinnedLocation?.latitude,
                        longitude = pinnedLocation?.longitude,
                        region = region,
                        islandGroup = selectedIsland.displayName,
                        difficultyLevel = difficulty,
                        trailClass = trailClass
                    )
                    isSubmitting = false
                    onMountainAdded(mountainId, navigateToMountain, openLog)
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
            CenterAlignedTopAppBar(
                title = { Text(text = "Contribute Mountain", color = Color(0xFF1A1A1A), fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = Color.Transparent,
                        modifier = Modifier
                            .padding(start = 12.dp)
                            .height(48.dp)
                            .clip(RoundedCornerShape(24.dp))
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
                },
                actions = {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = Color.Transparent,
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .height(48.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .clickable(enabled = isFormValid && !isSubmitting) { showSubmitBottomSheet = true }
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 18.dp)
                        ) {
                            // Invisible text ensures the Box maintains the exact width of the "Done" text
                            Text(
                                text = "Done",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.alpha(0f)
                            )
                            
                            androidx.compose.animation.Crossfade(
                                targetState = isSubmitting, 
                                label = "SubmitAnimation"
                            ) { submitting ->
                                if (submitting) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = Color(0xFF1A73E8)
                                    )
                                } else {
                                    Text(
                                        text = "Done",
                                        color = if (isFormValid) Color(0xFF1A73E8) else Color(0xFFB0C4DE),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFFF2F4F8)
                )
            )
        }
    ) { padding ->
        if (showSubmitBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { showSubmitBottomSheet = false },
                sheetState = sheetState
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Submit Mountain",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Would you like to just submit this mountain or also record a hike for it?",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Button(
                        onClick = {
                            showSubmitBottomSheet = false
                            submitAction(false, false)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5))
                    ) {
                        Text("Submit", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Button(
                        onClick = {
                            showSubmitBottomSheet = false
                            submitAction(true, true)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5))
                    ) {
                        Text("Submit & Add Hike", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            // Glider Blue Info Card ("Contributing to PataGilid List")
            if (showInfoCard) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFFE8F0FE),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.Top) {
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
                                    text = "Submit an unlisted mountain. It will be reviewed by admins before becoming public.",
                                    color = Color(0xFF5F6368),
                                    fontSize = 13.5.sp,
                                    lineHeight = 19.sp
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFF5F6368),
                            modifier = Modifier
                                .size(20.dp)
                                .clickable { showInfoCard = false }
                        )
                    }
                }
            }

            // Section 1: Mountain Information
            Column {
                Text(
                    text = "Mountain Information",
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
                        // Pin Map Button
                        Button(
                            onClick = { showMapDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .padding(top = 16.dp, bottom = if (pinnedLocation != null) 4.dp else 16.dp)
                                .height(52.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1A73E8),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Map,
                                contentDescription = "Pin Location on Map",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (pinnedLocation != null) "Edit Pinned Location" else "Pin Location on Map",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        
                        if (pinnedLocation != null) {
                            Text(
                                text = String.format("Pinned: %.4f, %.4f", pinnedLocation!!.latitude, pinnedLocation!!.longitude),
                                color = Color(0xFF5F6368),
                                fontSize = 13.sp,
                                modifier = Modifier
                                    .padding(horizontal = 16.dp)
                                    .padding(bottom = 16.dp)
                            )
                        }
                        
                        HorizontalDivider(color = Color(0xFFEAEDF1), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
                        
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
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            trailingIcon = if (isFetchingElevation) {
                                { CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color(0xFF1A73E8)) }
                            } else null
                        )
                        
                        HorizontalDivider(color = Color(0xFFEAEDF1), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))

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

            // Section 3: Difficulty & Terrain
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Text(
                    text = "Difficulty & Terrain",
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

            Spacer(modifier = Modifier.height(4.dp))



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

        // Map Dialog
        if (showMapDialog) {
            MapCalibrationDialog(
                onDismiss = { showMapDialog = false },
                onLocationPinned = { loc, placeName ->
                    pinnedLocation = loc
                    showMapDialog = false
                    
                    if (!placeName.isNullOrBlank()) {
                        name = placeName
                    }
                    
                    // Reverse geocode
                    coroutineScope.launch {
                        val helper = RegionHelper
                        try {
                            val geocoder = android.location.Geocoder(context, java.util.Locale.getDefault())
                            val addresses = withContext(Dispatchers.IO) {
                                geocoder.getFromLocation(loc.latitude, loc.longitude, 1)
                            }
                            
                            if (!addresses.isNullOrEmpty()) {
                                val address = addresses[0]
                                val countryCode = address.countryCode
                                
                                if (countryCode?.lowercase() == "ph") {
                                    val (newRegion, newIslandGroup) = helper.mapToInternalRegion(
                                        address.adminArea ?: "", 
                                        address.subAdminArea ?: "", 
                                        address.locality ?: ""
                                    )
                                    
                                    if (newRegion != null && newIslandGroup != null) {
                                        selectedIsland = newIslandGroup
                                        region = newRegion
                                        
                                        if (name.isBlank() && !address.featureName.isNullOrBlank() && !address.featureName.contains("+")) {
                                            name = address.featureName
                                        }
                                        
                                        // Fetch elevation
                                        isFetchingElevation = true
                                        try {
                                            val elevStr = withContext(Dispatchers.IO) {
                                                val apiKey = BuildConfig.GOOGLE_MAPS_API_KEY
                                                val urlString = "https://maps.googleapis.com/maps/api/elevation/json?locations=${loc.latitude},${loc.longitude}&key=$apiKey"
                                                val url = URL(urlString)
                                                val connection = url.openConnection() as HttpURLConnection
                                                connection.requestMethod = "GET"
                                                connection.connect()
                                                
                                                if (connection.responseCode == 200) {
                                                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                                                    val json = JSONObject(response)
                                                    if (json.getString("status") == "OK") {
                                                        val results = json.getJSONArray("results")
                                                        if (results.length() > 0) {
                                                            return@withContext results.getJSONObject(0).getDouble("elevation").toInt().toString()
                                                        }
                                                    }
                                                }
                                                null
                                            }
                                            if (elevStr != null && elevation.isBlank()) {
                                                elevation = elevStr
                                            }
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        } finally {
                                            isFetchingElevation = false
                                        }
                                    } else {
                                        pinnedLocation = null
                                        showOutsidePHAlert = true
                                    }
                                } else {
                                    pinnedLocation = null
                                    showOutsidePHAlert = true
                                }
                            } else {
                                pinnedLocation = null
                                showOutsidePHAlert = true
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            pinnedLocation = null
                            showOutsidePHAlert = true
                        }
                    }
                },
                initialLocation = pinnedLocation
            )
        }
        
        if (showOutsidePHAlert) {
            AlertDialog(
                onDismissRequest = { showOutsidePHAlert = false },
                containerColor = Color.White,
                titleContentColor = Color.Black,
                textContentColor = Color(0xFF202124),
                title = { Text("Invalid Location", fontWeight = FontWeight.Bold) },
                text = { Text("The pinned location appears to be outside the Philippines or in an undefined area. Please pin a valid location on land within the country.") },
                confirmButton = {
                    TextButton(onClick = { showOutsidePHAlert = false }) {
                        Text("OK", color = Color(0xFF1A73E8), fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    }
    
    if (errorMessage != null) {
        val isDuplicate = errorMessage!!.contains("already on PataGilid")
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            containerColor = Color.White,
            titleContentColor = Color.Black,
            textContentColor = Color(0xFF202124),
            title = { 
                Text(
                    text = if (isDuplicate) "Teka, sandali!" else "Notice",
                    color = Color(0xFF1A73E8),
                    fontWeight = FontWeight.Bold
                ) 
            },
            text = { 
                Text(
                    text = errorMessage!!,
                    color = Color(0xFF1A1A1A),
                    fontSize = 15.sp
                ) 
            },
            confirmButton = {
                TextButton(onClick = { errorMessage = null }) {
                    Text("OK", color = Color(0xFF1A73E8), fontWeight = FontWeight.Bold)
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
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
    minLines: Int = 1,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(placeholder, color = Color(0xFF9AA0A6), fontSize = 13.sp) },
        textStyle = TextStyle(fontSize = 16.sp, color = Color(0xFF1A1A1A), fontWeight = FontWeight.Medium),
        singleLine = singleLine,
        minLines = minLines,
        keyboardOptions = keyboardOptions,
        trailingIcon = trailingIcon,
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
