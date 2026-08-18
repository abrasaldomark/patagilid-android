package com.devmarkabrasaldo.PataGilid.ui.screens.climbs

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Color
import com.devmarkabrasaldo.PataGilid.ui.theme.UTurnLeft
import com.devmarkabrasaldo.PataGilid.ui.theme.Start
import com.devmarkabrasaldo.PataGilid.ui.theme.FlagCheck
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.devmarkabrasaldo.PataGilid.di.AppContainer
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HikeLogCreationScreen(
    mountainId: String,
    container: AppContainer,
    onNavigateBack: () -> Unit,
    onLogSuccess: () -> Unit,
    logToEdit: com.devmarkabrasaldo.PataGilid.domain.models.HikeLog? = null,
    vm: HikeLogViewModel = viewModel(factory = HikeLogViewModel.Factory(container.mountainRepository, container.photoUploadService, mountainId))
) {
    val mountain by vm.mountain.collectAsState()
    val trailName by vm.trailName.collectAsState()
    val didSummit by vm.didSummit.collectAsState()
    val routeType by vm.routeType.collectAsState()
    val activeRouteColor = Color(0xFF3B82F6)
    val activeRouteBg = Color(0xFFEFF6FF)
    val exitTrailName by vm.exitTrailName.collectAsState()
    val waypoints by vm.waypoints.collectAsState()
    val climbNotes by vm.climbNotes.collectAsState()
    val selectedAssets by vm.selectedAssets.collectAsState()
    val isSubmitting by vm.isSubmitting.collectAsState()
    val uploadProgress by vm.uploadProgress.collectAsState()
    val errorMessage by vm.errorMessage.collectAsState()
    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(logToEdit) {
        logToEdit?.let {
            vm.setupForEditing(it)
        }
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            vm.clearError()
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 20)
    ) { uris ->
        vm.onPhotosSelected(uris)
    }

    val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    val startDateStr = dateFormat.format(Date(vm.startDate.value))
    val startTimeStr = timeFormat.format(Date(vm.startDate.value))
    val endDateStr = dateFormat.format(Date(vm.endDate.value))
    val endTimeStr = timeFormat.format(Date(vm.endDate.value))

    Scaffold(
        containerColor = Color.White,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = "New Hike", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 17.sp) },
                navigationIcon = {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = Color(0xFFE5ECF4),
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
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .height(48.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .clickable(enabled = !isSubmitting) { vm.submitHikeLog(onSuccess = onLogSuccess) }
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 18.dp)
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = Color(0xFF1A73E8)
                                )
                            } else {
                                Text(
                                    text = "Save",
                                    color = Color(0xFF1A73E8),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(onTap = {
                        focusManager.clearFocus()
                    })
                }
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Target Summit Hero
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFF9FAFB),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFE0E7FF),
                        modifier = Modifier.size(64.dp)
                    ) {
                        Icon(
                            Icons.Default.Terrain,
                            contentDescription = null,
                            tint = Color(0xFF3A82F5),
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxSize()
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(mountain?.name ?: "Peak", color = Color.Black, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                        Text("${mountain?.elevationMASL ?: 0} MASL", color = Color(0xFF94A3B8), fontSize = 13.sp)
                        Text(mountain?.region ?: "", color = Color(0xFF94A3B8), fontSize = 13.sp)
                    }
                }
            }

            // Climb Duration
            Column {
                Text("Climb Duration", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.Black)
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFF3F4F6)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Start Date
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Start", color = Color.Black, fontSize = 16.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Surface(shape = RoundedCornerShape(16.dp), color = Color(0xFFF3F4F6)) {
                                    Text(startDateStr, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), color = Color.Black, fontSize = 14.sp)
                                }
                                Surface(shape = RoundedCornerShape(16.dp), color = Color(0xFFF3F4F6)) {
                                    Text(startTimeStr, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), color = Color.Black, fontSize = 14.sp)
                                }
                            }
                        }
                        
                        Divider(color = Color(0xFFF3F4F6), modifier = Modifier.padding(vertical = 16.dp))
                        
                        // End Date
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("End", color = Color.Black, fontSize = 16.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Surface(shape = RoundedCornerShape(16.dp), color = Color(0xFFF3F4F6)) {
                                    Text(endDateStr, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), color = Color.Black, fontSize = 14.sp)
                                }
                                Surface(shape = RoundedCornerShape(16.dp), color = Color(0xFFF3F4F6)) {
                                    Text(endTimeStr, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), color = Color.Black, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Trail Details
            Column {
                Text("Trail Details", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.Black)
                Spacer(modifier = Modifier.height(12.dp))
                
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF9FAFB),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Route Type", color = Color(0xFF9CA3AF), fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Back Trail Button
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (routeType == "Back Trail") activeRouteBg else Color(0xFFF3F4F6),
                                border = BorderStroke(2.dp, if (routeType == "Back Trail") activeRouteColor else Color.Transparent),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(90.dp)
                                    .clickable { vm.routeType.value = "Back Trail" }
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp, vertical = 8.dp)
                                ) {
                                    Icon(UTurnLeft, contentDescription = null, tint = if (routeType == "Back Trail") activeRouteColor else Color(0xFF9CA3AF), modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Back Trail", color = if (routeType == "Back Trail") activeRouteColor else Color(0xFF9CA3AF), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text("Same Start & Exit", color = if (routeType == "Back Trail") Color(0xFF9CA3AF) else Color(0xFFD1D5DB), fontSize = 10.sp, textAlign = TextAlign.Center)
                                }
                            }
                            
                            // Traverse Button
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (routeType == "Traverse") activeRouteBg else Color(0xFFF3F4F6),
                                border = BorderStroke(2.dp, if (routeType == "Traverse") activeRouteColor else Color.Transparent),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(90.dp)
                                    .clickable { vm.routeType.value = "Traverse" }
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp, vertical = 8.dp)
                                ) {
                                    Icon(Icons.Default.Route, contentDescription = null, tint = if (routeType == "Traverse") activeRouteColor else Color(0xFF9CA3AF), modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Traverse", color = if (routeType == "Traverse") activeRouteColor else Color(0xFF9CA3AF), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text("Different Exit", color = if (routeType == "Traverse") Color(0xFF9CA3AF) else Color(0xFFD1D5DB), fontSize = 10.sp, textAlign = TextAlign.Center)
                                }
                            }

                            // Circuit Button
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (routeType == "Circuit") activeRouteBg else Color(0xFFF3F4F6),
                                border = BorderStroke(2.dp, if (routeType == "Circuit") activeRouteColor else Color.Transparent),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(90.dp)
                                    .clickable { vm.routeType.value = "Circuit" }
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp, vertical = 8.dp)
                                ) {
                                    Icon(Icons.Default.Loop, contentDescription = null, tint = if (routeType == "Circuit") activeRouteColor else Color(0xFF9CA3AF), modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Circuit", color = if (routeType == "Circuit") activeRouteColor else Color(0xFF9CA3AF), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text("Loop to Start", color = if (routeType == "Circuit") Color(0xFF9CA3AF) else Color(0xFFD1D5DB), fontSize = 10.sp, textAlign = TextAlign.Center)
                                }
                            }
                        }
                        
                        Divider(color = Color(0xFFE5E7EB), modifier = Modifier.padding(vertical = 16.dp))
                        
                        // Trail Names
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(if (routeType == "Back Trail") UTurnLeft else if (routeType == "Circuit") Icons.Default.Loop else Start, contentDescription = null, tint = Color(0xFF9CA3AF), modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    if (routeType == "Back Trail") "Trail Name (Entry & Exit)" 
                                    else if (routeType == "Circuit") "Entry & Exit"
                                    else "Entry Trail", 
                                    color = Color(0xFF9CA3AF), fontSize = 13.sp
                                )
                                BasicTextField(
                                    value = trailName,
                                    onValueChange = { vm.trailName.value = it },
                                    textStyle = TextStyle(color = Color.Black, fontSize = 16.sp),
                                    cursorBrush = SolidColor(Color(0xFF3B82F6)),
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                                    decorationBox = { innerTextField ->
                                        if (trailName.isEmpty()) {
                                            Text(if (routeType == "Circuit") "e.g. Sta. Cruz Circuit" else "e.g. Salacafe Trail", color = Color(0xFFD1D5DB), fontSize = 16.sp)
                                        }
                                        innerTextField()
                                    }
                                )
                            }
                        }
                        
                        if (routeType == "Traverse") {
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(FlagCheck, contentDescription = null, tint = Color(0xFF9CA3AF), modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text("Exit Trail", color = Color(0xFF9CA3AF), fontSize = 13.sp)
                                    BasicTextField(
                                        value = exitTrailName,
                                        onValueChange = { vm.exitTrailName.value = it },
                                        textStyle = TextStyle(color = Color.Black, fontSize = 16.sp),
                                        cursorBrush = SolidColor(Color(0xFF3B82F6)),
                                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                                        decorationBox = { innerTextField ->
                                            if (exitTrailName.isEmpty()) {
                                                Text("e.g. Ambangeg Trail", color = Color(0xFFD1D5DB), fontSize = 16.sp)
                                            }
                                            innerTextField()
                                        }
                                    )
                                }
                            }
                        }
                        
                        if (routeType == "Circuit") {
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(verticalAlignment = Alignment.Top) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(Color(0xFF9CA3AF), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text("Waypoints", color = Color(0xFF9CA3AF), fontSize = 13.sp)
                                        TextButton(
                                            onClick = { vm.addWaypoint() }, 
                                            contentPadding = PaddingValues(0.dp),
                                            modifier = Modifier.height(24.dp).defaultMinSize(minWidth = 1.dp, minHeight = 1.dp)
                                        ) {
                                            Text("+ Add", color = Color(0xFF3B82F6), fontSize = 14.sp)
                                        }
                                    }
                                    
                                    waypoints.forEachIndexed { index, waypoint ->
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                                        ) {
                                            BasicTextField(
                                                value = waypoint,
                                                onValueChange = { vm.updateWaypoint(index, it) },
                                                textStyle = TextStyle(color = Color.Black, fontSize = 16.sp),
                                                cursorBrush = SolidColor(Color(0xFF3B82F6)),
                                                modifier = Modifier.weight(1f),
                                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                                                decorationBox = { innerTextField ->
                                                    if (waypoint.isEmpty()) {
                                                        Text("e.g. Lake Venado", color = Color(0xFFD1D5DB), fontSize = 16.sp)
                                                    }
                                                    innerTextField()
                                                }
                                            )
                                            IconButton(onClick = { vm.removeWaypoint(index) }, modifier = Modifier.size(24.dp)) {
                                                Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color(0xFF9CA3AF))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        
                        Divider(color = Color(0xFFE5E7EB), modifier = Modifier.padding(vertical = 16.dp))
                        
                        // Experienced Difficulty
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Speed, contentDescription = null, tint = Color(0xFF9CA3AF), modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text("Experienced Difficulty", color = Color(0xFF9CA3AF), fontSize = 13.sp)
                                Text(mountain?.difficultyLevel ?: "N/A", color = Color.Black, fontSize = 16.sp)
                            }
                        }
                        
                        Divider(color = Color(0xFFE5E7EB), modifier = Modifier.padding(vertical = 16.dp))
                        
                        // Technical Trail Class
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Hiking, contentDescription = null, tint = Color(0xFF9CA3AF), modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text("Technical Trail Class", color = Color(0xFF9CA3AF), fontSize = 13.sp)
                                Text(mountain?.trailClass ?: "N/A", color = Color.Black, fontSize = 16.sp)
                            }
                        }
                    }
                }
            }

            // Climb Outcome
            Column {
                Text("Climb Outcome", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.Black)
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFF3F4F6)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            // Summited Button
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (didSummit) activeRouteBg else Color(0xFFF9FAFB),
                                border = BorderStroke(2.dp, if (didSummit) activeRouteColor else Color.Transparent),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(90.dp)
                                    .clickable { vm.didSummit.value = true }
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Icon(Icons.Default.Terrain, contentDescription = null, tint = if (didSummit) activeRouteColor else Color(0xFF9CA3AF), modifier = Modifier.size(28.dp))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Summited", color = if (didSummit) activeRouteColor else Color(0xFF9CA3AF), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("Reached Top", color = if (didSummit) Color(0xFF9CA3AF) else Color(0xFFD1D5DB), fontSize = 11.sp)
                                }
                            }
                            
                            // DNF Button
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (!didSummit) activeRouteBg else Color(0xFFF9FAFB),
                                border = BorderStroke(2.dp, if (!didSummit) activeRouteColor else Color.Transparent),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(90.dp)
                                    .clickable { vm.didSummit.value = false }
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Icon(Icons.Default.Undo, contentDescription = null, tint = if (!didSummit) activeRouteColor else Color(0xFF9CA3AF), modifier = Modifier.size(28.dp))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Backed Out (DNF)", color = if (!didSummit) activeRouteColor else Color(0xFF9CA3AF), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("Did Not Finish", color = if (!didSummit) Color(0xFF9CA3AF) else Color(0xFFD1D5DB), fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Climb Notes
            Column {
                Text("Climb Notes", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.Black)
                Spacer(modifier = Modifier.height(12.dp))
                
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF9FAFB),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        if (!didSummit) {
                            Text("Why did you back out? (Optional)", color = Color(0xFF9CA3AF), fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                val chips = listOf("⛈️ Bad Weather", "🤕 Injury / Sickness", "⏰ Time Constraint", "🥾 Trail Conditions", "🛑 Group / Safety Call")
                                items(chips) { chip ->
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = Color(0xFFE5E7EB),
                                        modifier = Modifier.clickable {
                                            val currentText = vm.climbNotes.value
                                            if (currentText.isNotEmpty()) {
                                                vm.climbNotes.value = currentText + "\n" + chip
                                            } else {
                                                vm.climbNotes.value = chip
                                            }
                                        }
                                    ) {
                                        Text(
                                            text = chip,
                                            fontSize = 12.sp,
                                            color = Color.Black,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                        
                        OutlinedTextField(
                            value = climbNotes,
                            onValueChange = { vm.climbNotes.value = it },
                            placeholder = { 
                                Text(
                                    if (didSummit) "Journal your climb experience... (Optional)" else "Additional notes about the climb... (Optional)", 
                                    color = Color(0xFFD1D5DB), 
                                    fontSize = 15.sp
                                ) 
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 100.dp),
                            textStyle = TextStyle(color = Color.Black, fontSize = 15.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFE5E7EB),
                                unfocusedBorderColor = Color(0xFFE5E7EB),
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
            }

            // Climb Photos
            Column {
                Text("Climb Photos", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.Black)
                Spacer(modifier = Modifier.height(12.dp))
                
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF9FAFB),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Button(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE0E7FF), contentColor = Color(0xFF3B82F6)),
                            modifier = Modifier.fillMaxWidth().height(56.dp)
                        ) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (selectedAssets.isEmpty()) "Add Photos" else "Add More Photos", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        }

                        if (selectedAssets.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(3),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.height(180.dp)
                            ) {
                                items(selectedAssets) { asset ->
                                    Box(modifier = Modifier.size(100.dp).clip(RoundedCornerShape(8.dp))) {
                                        Image(
                                            painter = rememberAsyncImagePainter(model = asset.uri),
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(4.dp)
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(Color.Black.copy(alpha = 0.7f))
                                                .clickable { vm.removePhoto(asset) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.White, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            if (isSubmitting && uploadProgress != null) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                    CircularProgressIndicator(color = Color(0xFF3A82F5), modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(uploadProgress!!, color = Color(0xFF3A82F5), fontSize = 13.sp)
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp)) // Extra padding at the bottom
        }
    }
}
