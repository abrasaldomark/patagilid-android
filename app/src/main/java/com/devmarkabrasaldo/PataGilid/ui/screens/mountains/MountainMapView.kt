package com.devmarkabrasaldo.PataGilid.ui.screens.mountains

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Terrain
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.devmarkabrasaldo.PataGilid.domain.models.Mountain
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import androidx.core.content.ContextCompat
import com.devmarkabrasaldo.PataGilid.R
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.views.overlay.MapEventsOverlay
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable

private val OrangeMap = Color(0xFFFF9500)

@Composable
fun MountainMapView(
    mountain: Mountain,
    isAdmin: Boolean = false,
    onUpdateProposal: ((lat: Double, lng: Double) -> Unit)? = null,
    onApprove: ((lat: Double, lng: Double) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val ctx = LocalContext.current
    var mapContainer by remember { mutableStateOf<MapView?>(null) }
    var mapMarker by remember { mutableStateOf<Marker?>(null) }

    val isPending = mountain.latitude == null && mountain.pendingLatitude != null
    val displayLat = mountain.pendingLatitude ?: mountain.latitude
    val displayLng = mountain.pendingLongitude ?: mountain.longitude
    
    val initialLocation = if (displayLat != null && displayLng != null) GeoPoint(displayLat, displayLng) else GeoPoint(12.8797, 121.7740)

    var pinnedLocation by remember { mutableStateOf<GeoPoint?>(initialLocation) }
    var adjustedLocation by remember { mutableStateOf<GeoPoint?>(null) }

    // Initialize osmdroid configuration if needed
    LaunchedEffect(Unit) {
        val sharedPrefs = ctx.getSharedPreferences("osmdroid", Context.MODE_PRIVATE)
        Configuration.getInstance().load(ctx, sharedPrefs)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
            AndroidView(
                factory = { context ->
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

                    object : MapView(context) {
                        private var hasInitialized = false
                        override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
                            super.onLayout(changed, left, top, right, bottom)
                            if (!hasInitialized && (right - left) > 0 && (bottom - top) > 0) {
                                hasInitialized = true
                                if (displayLat != null && displayLng != null) {
                                    controller.setZoom(13.0)
                                } else {
                                    controller.setZoom(6.0)
                                }
                                controller.setCenter(pinnedLocation)
                            }
                        }
                    }.apply {
                        setTileSource(esriSource)
                        setMultiTouchControls(true)

                        if (isAdmin) {
                            val mapEventsReceiver = object : MapEventsReceiver {
                                override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                                    return false
                                }
                                override fun longPressHelper(p: GeoPoint?): Boolean {
                                    p?.let {
                                        adjustedLocation = it
                                        pinnedLocation = it
                                    }
                                    return true
                                }
                            }
                            overlays.add(MapEventsOverlay(mapEventsReceiver))
                        }

                        val tapOverlay = object : org.osmdroid.views.overlay.Overlay() {
                            override fun onSingleTapConfirmed(e: android.view.MotionEvent, mapView: MapView): Boolean {
                                return true
                            }
                        }
                        overlays.add(tapOverlay)
                        mapContainer = this
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            LaunchedEffect(pinnedLocation) {
                val loc = pinnedLocation
                val map = mapContainer
                if (loc != null && map != null) {
                    var marker = mapMarker
                    if (marker == null) {
                        marker = Marker(map)
                        marker.title = mountain.name
                        
                        val labelText = if (isPending) "${mountain.name} (Proposed)" else mountain.name
                        
                        // Create combined drawable with pin and text
                        val pinResId = R.drawable.ic_map_pin
                        val pinDrawable = ContextCompat.getDrawable(ctx, pinResId)
                        
                        if (pinDrawable != null) {
                            val paddingX = 32f
                            val paddingY = 16f
                            val cornerRadius = 24f
                            
                            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                color = android.graphics.Color.WHITE
                                textSize = 40f
                                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                            }
                            
                            val textBounds = Rect()
                            paint.getTextBounds(labelText, 0, labelText.length, textBounds)
                            
                            val pillWidth = textBounds.width() + paddingX * 2
                            val pillHeight = textBounds.height() + paddingY * 2
                            
                            val pinWidth = pinDrawable.intrinsicWidth
                            val pinHeight = pinDrawable.intrinsicHeight
                            
                            val gap = 12f
                            val totalWidth = Math.max(pillWidth.toInt(), pinWidth)
                            val totalHeight = pinHeight + gap + pillHeight
                            
                            val bitmap = Bitmap.createBitmap(totalWidth, totalHeight.toInt(), Bitmap.Config.ARGB_8888)
                            val canvas = Canvas(bitmap)
                            
                            // Draw Pin
                            val pinLeft = (totalWidth - pinWidth) / 2
                            pinDrawable.setBounds(pinLeft, 0, pinLeft + pinWidth, pinHeight)
                            pinDrawable.draw(canvas)
                            
                            // Draw Pill Background
                            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                color = android.graphics.Color.parseColor("#1C1C1E")
                            }
                            
                            val pillTop = pinHeight + gap
                            val pillLeft = (totalWidth - pillWidth) / 2f
                            canvas.drawRoundRect(
                                pillLeft, pillTop, pillLeft + pillWidth, pillTop + pillHeight,
                                cornerRadius, cornerRadius, bgPaint
                            )
                            
                            // Draw Text
                            val textX = pillLeft + paddingX
                            val textY = pillTop + pillHeight - paddingY - textBounds.bottom
                            canvas.drawText(labelText, textX, textY, paint)
                            
                            marker.icon = BitmapDrawable(ctx.resources, bitmap)
                            
                            // Adjust anchor so the pin tip points to the location
                            val anchorY = pinHeight / totalHeight.toFloat()
                            marker.setAnchor(Marker.ANCHOR_CENTER, anchorY)
                        } else {
                            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        }
                        
                        if (isAdmin) {
                            marker.isDraggable = true
                            marker.setOnMarkerDragListener(object : Marker.OnMarkerDragListener {
                                override fun onMarkerDragStart(marker: Marker) {}
                                override fun onMarkerDrag(marker: Marker) {
                                    pinnedLocation = marker.position
                                }
                                override fun onMarkerDragEnd(marker: Marker) {
                                    adjustedLocation = marker.position
                                    pinnedLocation = marker.position
                                }
                            })
                        }
                        
                        map.overlays.add(marker)
                        mapMarker = marker
                    }
                    marker.position = loc
                    map.invalidate()
                }
            }
            
            // Top UI Elements
            Column(modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter).padding(top = 48.dp)) {
                // Top Pill
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White.copy(alpha = 0.95f),
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Close",
                            color = GliderBlue,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 17.sp,
                            modifier = Modifier.clickable { onDismiss() }
                        )
                        
                        Text(
                            text = if (isPending) "${mountain.name} (Prop..." else mountain.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = Color.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
                        )
                        
                        if (displayLat != null && displayLng != null) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(GliderBlue, CircleShape)
                                    .clickable { 
                                        mapContainer?.controller?.animateTo(initialLocation) 
                                        pinnedLocation = initialLocation
                                        adjustedLocation = null
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Navigation, contentDescription = "Center", tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
                
                // Admin Help Prompt
                if (isAdmin && adjustedLocation == null) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = Color(0xFFF3F2EA),
                        shadowElevation = 4.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.TouchApp,
                                contentDescription = null,
                                tint = OrangeMap,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "Admin Mode: Hold & drag pin or long-press map to adjust",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.Black
                            )
                        }
                    }
                }
                
            }

            // Bottom UI Elements
            Column(modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter)) {
                
                // Admin Action Card
                AnimatedVisibility(
                    visible = isAdmin && adjustedLocation != null,
                    enter = slideInVertically(initialOffsetY = { it }, animationSpec = spring(dampingRatio = 0.8f)),
                    exit = slideOutVertically(targetOffsetY = { it }, animationSpec = spring(dampingRatio = 0.8f))
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        shadowElevation = 8.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            // Top Row: Icon, Title, Reset
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Tune, contentDescription = null, tint = OrangeMap, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Admin Mode: Location Adjusted", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                Spacer(modifier = Modifier.weight(1f))
                                Text(
                                    text = "Reset",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.Red,
                                    modifier = Modifier.clickable {
                                        adjustedLocation = null
                                        pinnedLocation = initialLocation
                                        mapContainer?.controller?.animateTo(initialLocation)
                                    }.padding(4.dp)
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Middle Row: New GPS
                            Text(
                                text = "New GPS: ${String.format("%.5f, %.5f", adjustedLocation?.latitude ?: 0.0, adjustedLocation?.longitude ?: 0.0)}",
                                fontSize = 14.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(start = 28.dp) // align with the title text (20.dp icon + 8.dp spacer)
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Bottom Row: Buttons
                            if (isPending) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Button(
                                        onClick = { adjustedLocation?.let { onUpdateProposal?.invoke(it.latitude, it.longitude) } },
                                        colors = ButtonDefaults.buttonColors(containerColor = OrangeMap, contentColor = Color.White),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth().height(44.dp)
                                    ) {
                                        Text("Update Proposal", fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            } else {
                                Button(
                                    onClick = { adjustedLocation?.let { onApprove?.invoke(it.latitude, it.longitude) } },
                                    colors = ButtonDefaults.buttonColors(containerColor = GliderBlue, contentColor = Color.White),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth().height(44.dp)
                                ) {
                                    Text("Save Adjusted Official GPS", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
                
                // Bottom Info Pill
                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.9f),
                    shadowElevation = 6.dp,
                    modifier = Modifier.padding(bottom = 32.dp).align(Alignment.CenterHorizontally)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Outlined.Terrain, 
                            contentDescription = null, 
                            tint = if (isPending) OrangeMap else GliderBlue,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("${mountain.elevationMASL} MASL", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
                        
                        Text("  •  ", color = Color.Gray, fontSize = 14.sp)
                        
                        if (displayLat != null && displayLng != null) {
                            Text(
                                String.format("%.4f, %.4f", pinnedLocation?.latitude ?: displayLat, pinnedLocation?.longitude ?: displayLng) + if (isPending) " (Proposed)" else "",
                                fontSize = 12.sp,
                                color = if (isPending) OrangeMap else Color.Gray
                            )
                        } else {
                            Text("Coordinates needed", fontSize = 12.sp, color = OrangeMap)
                        }
                    }
                }
            }
        }
    }
}
