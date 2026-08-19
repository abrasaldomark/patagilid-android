package com.devmarkabrasaldo.PataGilid.ui.screens.mountains

import androidx.compose.material3.MaterialTheme

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch

@Composable
fun MapCalibrationDialog(
    onDismiss: () -> Unit,
    onLocationPinned: (LatLng, String?) -> Unit,
    initialLocation: LatLng? = null
) {
    val context = LocalContext.current

    // Geographic center of the Philippines as fallback
    val defaultLocation = LatLng(12.8797, 121.7740)
    val startLocation = initialLocation ?: defaultLocation
    
    // Zoom out further if it's the default country view, otherwise zoom closer to the specific mountain
    val initialZoom = if (initialLocation != null) 12.0f else 5.5f
    
    var pinnedLocation by remember { mutableStateOf<LatLng?>(initialLocation) }
    var selectedPlaceName by remember { mutableStateOf<String?>(null) }
    var showInstruction by remember { mutableStateOf(true) }

    var searchText by remember { mutableStateOf("") }
    var searchPredictions by remember { mutableStateOf<List<com.google.android.libraries.places.api.model.AutocompletePrediction>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    
    val placesClient = remember { Places.createClient(context) }
    
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(startLocation, initialZoom)
    }

    fun fetchPredictions(query: String) {
        if (query.isBlank()) {
            searchPredictions = emptyList()
            return
        }
        val token = AutocompleteSessionToken.newInstance()
        val request = FindAutocompletePredictionsRequest.builder()
            .setQuery(query)
            .setCountry("PH")
            .setSessionToken(token)
            .build()
            
        placesClient.findAutocompletePredictions(request)
            .addOnSuccessListener { response ->
                searchPredictions = response.autocompletePredictions
            }
            .addOnFailureListener {
                searchPredictions = emptyList()
            }
    }

    fun performSearch() {
        if (searchText.isBlank()) return
        isSearching = true
        searchError = null

        val token = AutocompleteSessionToken.newInstance()
        val request = FindAutocompletePredictionsRequest.builder()
            .setQuery(searchText)
            .setCountry("PH")
            .setSessionToken(token)
            .build()
            
        placesClient.findAutocompletePredictions(request)
            .addOnSuccessListener { response ->
                val predictions = response.autocompletePredictions
                if (predictions.isNotEmpty()) {
                    val bestMatch = predictions[0]
                    val placeName = bestMatch.getPrimaryText(null).toString()
                    val placeRequest = FetchPlaceRequest.builder(bestMatch.placeId, listOf(Place.Field.LAT_LNG)).build()
                    placesClient.fetchPlace(placeRequest)
                        .addOnSuccessListener { placeResp ->
                            val latLng = placeResp.place.latLng
                            if (latLng != null) {
                                pinnedLocation = latLng
                                selectedPlaceName = placeName
                                coroutineScope.launch {
                                    cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(latLng, 12f))
                                }
                            } else {
                                searchError = "Location found but coordinates invalid."
                            }
                            isSearching = false
                        }
                        .addOnFailureListener {
                            searchError = "Failed to fetch place details."
                            isSearching = false
                        }
                } else {
                    searchError = "Location not found. Try a simpler name."
                    isSearching = false
                }
            }
            .addOnFailureListener { exception ->
                searchError = "Search failed: ${exception.localizedMessage ?: exception.message ?: "Unknown error"}"
                isSearching = false
            }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.95f)
                    .align(Alignment.BottomCenter),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                color = Color(0xFFF3F4F6)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    
                    val mapProperties by remember { mutableStateOf(MapProperties(mapType = MapType.TERRAIN)) }
                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cameraPositionState,
                        properties = mapProperties,
                        onMapClick = { latLng ->
                            pinnedLocation = latLng
                            selectedPlaceName = null
                            
                            // Reverse geocode to get island group and region
                            coroutineScope.launch {
                                try {
                                    val geocoder = android.location.Geocoder(context, java.util.Locale.getDefault())
                                    val addresses = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                        geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
                                    }
                                    if (!addresses.isNullOrEmpty()) {
                                        val address = addresses[0]
                                        val (fetchedRegion, islandGroup) = com.devmarkabrasaldo.PataGilid.domain.models.RegionHelper.mapToInternalRegion(
                                            address.adminArea ?: "",
                                            address.subAdminArea ?: "",
                                            address.locality ?: ""
                                        )
                                        if (fetchedRegion != null) {
                                            selectedPlaceName = "${islandGroup?.name ?: ""}, $fetchedRegion"
                                        }
                                    }
                                } catch (e: Exception) {
                                    // Ignore reverse geocoding errors
                                }
                            }
                        }
                    ) {
                        pinnedLocation?.let { loc ->
                            Marker(
                                state = MarkerState(position = loc),
                                title = "Pinned Summit"
                            )
                        }
                    }

                    // Top Overlays
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Color(0xFFE0E7FF),
                                modifier = Modifier.clickable { onDismiss() }
                            ) {
                                Text(
                                    "Cancel", 
                                    color = Color(0xFF3B82F6), 
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), 
                                    fontSize = 15.sp
                                )
                            }
                            
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                "Pin Summit Location", 
                                fontWeight = FontWeight.Bold, 
                                fontSize = 18.sp, 
                                color = MaterialTheme.colorScheme.onBackground, 
                                modifier = Modifier.padding(end = 40.dp)
                            )
                            Spacer(modifier = Modifier.weight(1f))
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White,
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(4.dp, RoundedCornerShape(12.dp))
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF3B82F6), modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    TextField(
                                        value = searchText,
                                        onValueChange = { 
                                            searchText = it
                                            fetchPredictions(it)
                                        },
                                        placeholder = { Text("Search mountain, summit, or trail...", color = Color(0xFF9CA3AF), fontSize = 15.sp) },
                                        colors = TextFieldDefaults.colors(
                                            focusedTextColor = Color.Black,
                                            unfocusedTextColor = Color.Black,
                                            cursorColor = Color(0xFF3B82F6),
                                            focusedContainerColor = Color.Transparent,
                                            unfocusedContainerColor = Color.Transparent,
                                            focusedIndicatorColor = Color.Transparent,
                                            unfocusedIndicatorColor = Color.Transparent
                                        ),
                                        modifier = Modifier.weight(1f).heightIn(min = 20.dp),
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                        keyboardActions = KeyboardActions(onSearch = { performSearch() }),
                                        singleLine = true
                                    )
                                    if (isSearching) {
                                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                    } else if (searchText.isNotEmpty()) {
                                        Text("Go", color = Color(0xFF3B82F6), fontWeight = FontWeight.Bold, modifier = Modifier.clickable { performSearch() })
                                    }
                                }

                                if (searchPredictions.isNotEmpty()) {
                                    HorizontalDivider(color = Color(0xFFE5E7EB))
                                    LazyColumn(
                                        modifier = Modifier.heightIn(max = 200.dp)
                                    ) {
                                        items(searchPredictions) { prediction ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        searchText = prediction.getPrimaryText(null).toString()
                                                        searchPredictions = emptyList()
                                                        
                                                        isSearching = true
                                                        searchError = null
                                                        val placeRequest = FetchPlaceRequest.builder(prediction.placeId, listOf(Place.Field.LAT_LNG)).build()
                                                        placesClient.fetchPlace(placeRequest)
                                                            .addOnSuccessListener { placeResp ->
                                                                val latLng = placeResp.place.latLng
                                                                if (latLng != null) {
                                                                    pinnedLocation = latLng
                                                                    selectedPlaceName = prediction.getPrimaryText(null).toString()
                                                                    coroutineScope.launch {
                                                                        cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(latLng, 12f))
                                                                    }
                                                                } else {
                                                                    searchError = "Location found but coordinates invalid."
                                                                }
                                                                isSearching = false
                                                            }
                                                            .addOnFailureListener {
                                                                searchError = "Failed to fetch place details."
                                                                isSearching = false
                                                            }
                                                    }
                                                    .padding(16.dp)
                                            ) {
                                                Column {
                                                    Text(prediction.getPrimaryText(null).toString(), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                                                    Text(prediction.getSecondaryText(null).toString(), fontSize = 12.sp, color = Color.Gray)
                                                }
                                            }
                                            HorizontalDivider(color = Color(0xFFF3F4F6))
                                        }
                                    }
                                }
                            }
                        }
                        
                        if (searchError != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(searchError!!, color = Color.White, fontSize = 12.sp, modifier = Modifier.background(Color.Red.copy(alpha=0.8f), CircleShape).padding(horizontal = 12.dp, vertical = 4.dp))
                        }
                    }

                    if (showInstruction) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(horizontal = 24.dp, vertical = 40.dp)
                                .shadow(12.dp, RoundedCornerShape(16.dp))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.TouchApp, contentDescription = null, tint = Color(0xFF3B82F6), modifier = Modifier.size(28.dp))
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    "Tap anywhere on the map to place a pin on the mountain's summit.",
                                    color = MaterialTheme.colorScheme.onBackground,
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(Color(0xFF9CA3AF), CircleShape)
                                        .clickable { showInstruction = false },
                                        contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                    
                    if (pinnedLocation != null) {
                        Button(
                            onClick = { 
                                pinnedLocation?.let { onLocationPinned(it, selectedPlaceName) }
                                onDismiss()
                            },
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(horizontal = 24.dp, vertical = if (showInstruction) 120.dp else 40.dp)
                                .fillMaxWidth()
                                .height(56.dp)
                                .shadow(8.dp, RoundedCornerShape(24.dp))
                        ) {
                            Text("Confirm Pin Location", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}
