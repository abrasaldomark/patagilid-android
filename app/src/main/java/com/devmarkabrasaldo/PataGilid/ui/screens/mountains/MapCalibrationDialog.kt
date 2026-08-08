package com.devmarkabrasaldo.PataGilid.ui.screens.mountains

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.net.URLEncoder

@Composable
fun MapCalibrationDialog(
    onDismiss: () -> Unit,
    onLocationPinned: (LatLng) -> Unit,
    initialLocation: LatLng? = null
) {
    val context = LocalContext.current
    Configuration.getInstance().userAgentValue = "PataGilid-Android-App/1.0 (contact@patagilid.app)"

    // Geographic center of the Philippines as fallback
    val defaultLocation = GeoPoint(12.8797, 121.7740)
    val startLocation = initialLocation?.let { GeoPoint(it.latitude, it.longitude) } ?: defaultLocation
    
    // Zoom out further if it's the default country view, otherwise zoom closer to the specific mountain
    val initialZoom = if (initialLocation != null) 12.0 else 5.5
    
    var pinnedLocation by remember { mutableStateOf<LatLng?>(initialLocation) }
    var showInstruction by remember { mutableStateOf(true) }

    var searchText by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val okHttpClient = remember { OkHttpClient() }
    
    var mapContainer by remember { mutableStateOf<MapView?>(null) }
    var mapMarker by remember { mutableStateOf<Marker?>(null) }

    fun performSearch() {
        if (searchText.isBlank()) return
        isSearching = true
        searchError = null

        coroutineScope.launch {
            try {
                val query = URLEncoder.encode(searchText, "UTF-8")
                val url = "https://nominatim.openstreetmap.org/search?q=$query&format=json&limit=5&countrycodes=ph"
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "PataGilid-Android-App/1.0 (contact@patagilid.app)")
                    .build()
                
                val response = withContext(Dispatchers.IO) {
                    okHttpClient.newCall(request).execute()
                }

                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    if (responseBody != null) {
                        val jsonArray = JSONArray(responseBody)
                        if (jsonArray.length() > 0) {
                            val firstResult = jsonArray.getJSONObject(0)
                            val lat = firstResult.getString("lat").toDoubleOrNull()
                            val lon = firstResult.getString("lon").toDoubleOrNull()
                            if (lat != null && lon != null) {
                                val coordinate = LatLng(lat, lon)
                                pinnedLocation = coordinate
                                mapContainer?.controller?.animateTo(GeoPoint(lat, lon), 12.0, 1000L)
                            } else {
                                searchError = "Location found but coordinates invalid."
                            }
                        } else {
                            searchError = "Location not found in OpenStreetMap. Try a simpler name."
                        }
                    }
                } else {
                    searchError = "Search failed: ${response.code}"
                }
            } catch (e: Exception) {
                searchError = "Connection failed."
            } finally {
                isSearching = false
            }
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
                    
                    AndroidView(
                        factory = { ctx ->
                            // Esri World Topo tiles use z/y/x order (not z/x/y)
                            val esriSource = object : OnlineTileSourceBase(
                                "EsriWorldTopo",
                                0, 19, 256, ".png",
                                arrayOf("https://server.arcgisonline.com/ArcGIS/rest/services/World_Topo_Map/MapServer/tile/")
                            ) {
                                override fun getTileURLString(pMapTileIndex: Long): String {
                                    val zoom = MapTileIndex.getZoom(pMapTileIndex)
                                    val x = MapTileIndex.getX(pMapTileIndex)
                                    val y = MapTileIndex.getY(pMapTileIndex)
                                    return "${baseUrl}$zoom/$y/$x"
                                }
                            }

                            // Subclass MapView so we can reliably center after layout.
                            // onLayout is the only callback guaranteed to fire after the
                            // view has real pixel dimensions, even inside a Dialog window.
                            object : MapView(ctx) {
                                private var hasInitialized = false
                                override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
                                    super.onLayout(changed, left, top, right, bottom)
                                    if (!hasInitialized && (right - left) > 0 && (bottom - top) > 0) {
                                        hasInitialized = true
                                        controller.setZoom(if (initialLocation != null) 12.0 else 7.2)
                                        controller.setCenter(startLocation)
                                    }
                                }
                            }.apply {
                                setTileSource(esriSource)
                                setMultiTouchControls(true)

                                // Add transparent label overlay on top of topo tiles
                                val labelSource = object : OnlineTileSourceBase(
                                    "CartoLabels",
                                    0, 19, 256, ".png",
                                    arrayOf(
                                        "https://cartodb-basemaps-a.global.ssl.fastly.net/light_only_labels/",
                                        "https://cartodb-basemaps-b.global.ssl.fastly.net/light_only_labels/",
                                        "https://cartodb-basemaps-c.global.ssl.fastly.net/light_only_labels/"
                                    )
                                ) {
                                    override fun getTileURLString(pMapTileIndex: Long): String {
                                        val zoom = MapTileIndex.getZoom(pMapTileIndex)
                                        val x = MapTileIndex.getX(pMapTileIndex)
                                        val y = MapTileIndex.getY(pMapTileIndex)
                                        return "${baseUrl}$zoom/$x/$y.png"
                                    }
                                }
                                val labelProvider = org.osmdroid.tileprovider.MapTileProviderBasic(ctx, labelSource)
                                val labelOverlay = org.osmdroid.views.overlay.TilesOverlay(labelProvider, ctx)
                                labelOverlay.loadingBackgroundColor = android.graphics.Color.TRANSPARENT
                                labelOverlay.loadingLineColor = android.graphics.Color.TRANSPARENT
                                overlays.add(labelOverlay)

                                val tapOverlay = object : org.osmdroid.views.overlay.Overlay() {
                                    override fun onSingleTapConfirmed(e: android.view.MotionEvent, mapView: MapView): Boolean {
                                        val proj = mapView.projection
                                        val geoPoint = proj.fromPixels(e.x.toInt(), e.y.toInt()) as GeoPoint
                                        pinnedLocation = LatLng(geoPoint.latitude, geoPoint.longitude)
                                        return true
                                    }
                                }
                                overlays.add(tapOverlay)
                                mapContainer = this
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Marker update effect
                    LaunchedEffect(pinnedLocation) {
                        val loc = pinnedLocation
                        val map = mapContainer
                        if (loc != null && map != null) {
                            var marker = mapMarker
                            if (marker == null) {
                                marker = Marker(map)
                                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                marker.title = "Pinned Summit"
                                map.overlays.add(marker)
                                mapMarker = marker
                            }
                            marker.position = GeoPoint(loc.latitude, loc.longitude)
                            map.invalidate()
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
                                color = Color.Black, 
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
                                    onValueChange = { searchText = it },
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
                                    color = Color.Black,
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
                                pinnedLocation?.let { onLocationPinned(it) }
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
