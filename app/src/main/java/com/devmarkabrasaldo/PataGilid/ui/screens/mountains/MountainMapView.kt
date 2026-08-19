package com.devmarkabrasaldo.PataGilid.ui.screens.mountains

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
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
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Terrain
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.devmarkabrasaldo.PataGilid.R
import com.devmarkabrasaldo.PataGilid.domain.models.Mountain
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

private val LocalOrangeMap = Color(0xFFFF9500)
private val LocalGliderBlue = Color(0xFF007AFF)

@Composable
fun MountainMapView(
    mountain: Mountain,
    submissionLat: Double? = null,
    submissionLng: Double? = null,
    isAdmin: Boolean = false,
    onUpdateProposal: ((lat: Double, lng: Double) -> Unit)? = null,
    onApprove: ((lat: Double, lng: Double) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val ctx = LocalContext.current
    val isPending = mountain.latitude == null && submissionLat != null
    val displayLat = submissionLat ?: mountain.latitude
    val displayLng = submissionLng ?: mountain.longitude
    
    val initialLocation = if (displayLat != null && displayLng != null) LatLng(displayLat, displayLng) else LatLng(12.8797, 121.7740)

    var pinnedLocation by remember { mutableStateOf<LatLng?>(initialLocation) }
    var adjustedLocation by remember { mutableStateOf<LatLng?>(null) }
    val coroutineScope = rememberCoroutineScope()
    
    val cameraPositionState = rememberCameraPositionState {
        this.position = CameraPosition.fromLatLngZoom(initialLocation, if (displayLat != null && displayLng != null) 13f else 6f)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
            
            val mapProperties by remember { mutableStateOf(MapProperties(mapType = MapType.TERRAIN)) }
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = mapProperties,
                onMapLongClick = { latLng ->
                    if (isAdmin) {
                        adjustedLocation = latLng
                        pinnedLocation = latLng
                    }
                }
            ) {
                pinnedLocation?.let { loc ->
                    val bitmapDescriptor = remember(mountain.name, isPending) {
                        createCustomMarkerBitmap(ctx, mountain.name, isPending)?.let {
                            BitmapDescriptorFactory.fromBitmap(it)
                        } ?: BitmapDescriptorFactory.defaultMarker()
                    }
                    
                    Marker(
                        state = rememberMarkerState(position = loc),
                        title = mountain.name,
                        icon = bitmapDescriptor
                    )
                }
            }
            
            // Top UI Elements
            Column(modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter).padding(top = 48.dp)) {
                // Top Elements
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = Color.White.copy(alpha = 0.95f),
                        shadowElevation = 8.dp,
                    ) {
                        Text(
                            text = "Close",
                            color = LocalGliderBlue,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 17.sp,
                            modifier = Modifier.clickable { onDismiss() }.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    
                    Text(
                        text = if (isPending) "${mountain.name} (Prop..." else mountain.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = LocalTextStyle.current.copy(
                            shadow = androidx.compose.ui.graphics.Shadow(
                                color = Color.White,
                                offset = androidx.compose.ui.geometry.Offset(0f, 0f),
                                blurRadius = 8f
                            )
                        ),
                        modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
                    )
                    
                    if (displayLat != null && displayLng != null) {
                        Surface(
                            shape = CircleShape,
                            shadowElevation = 8.dp,
                            color = LocalGliderBlue
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clickable { 
                                        pinnedLocation = initialLocation
                                        adjustedLocation = null
                                        coroutineScope.launch {
                                            cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(initialLocation, 13f))
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Navigation, contentDescription = "Center", tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.width(40.dp))
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
                                tint = LocalOrangeMap,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "Admin Mode: Hold & drag pin or long-press map to adjust",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onBackground
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
                                Icon(Icons.Default.Tune, contentDescription = null, tint = LocalOrangeMap, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Admin Mode: Location Adjusted", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                                Spacer(modifier = Modifier.weight(1f))
                                Text(
                                    text = "Reset",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.Red,
                                    modifier = Modifier.clickable {
                                        adjustedLocation = null
                                        pinnedLocation = initialLocation
                                    }.padding(4.dp)
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Middle Row: New GPS
                            Text(
                                text = "New GPS: ${String.format("%.5f, %.5f", adjustedLocation?.latitude ?: 0.0, adjustedLocation?.longitude ?: 0.0)}",
                                fontSize = 14.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(start = 28.dp)
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Bottom Row: Buttons
                            if (isPending) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Button(
                                        onClick = { adjustedLocation?.let { onUpdateProposal?.invoke(it.latitude, it.longitude) } },
                                        colors = ButtonDefaults.buttonColors(containerColor = LocalOrangeMap, contentColor = Color.White),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth().height(44.dp)
                                    ) {
                                        Text("Update Proposal", fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            } else {
                                Button(
                                    onClick = { adjustedLocation?.let { onApprove?.invoke(it.latitude, it.longitude) } },
                                    colors = ButtonDefaults.buttonColors(containerColor = LocalGliderBlue, contentColor = Color.White),
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
                            tint = if (isPending) LocalOrangeMap else LocalGliderBlue,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("${mountain.elevationMASL} MASL", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
                        
                        Text("  •  ", color = Color.Gray, fontSize = 14.sp)
                        
                        if (displayLat != null && displayLng != null) {
                            Text(
                                String.format("%.4f, %.4f", pinnedLocation?.latitude ?: displayLat, pinnedLocation?.longitude ?: displayLng) + if (isPending) " (Proposed)" else "",
                                fontSize = 12.sp,
                                color = if (isPending) LocalOrangeMap else Color.Gray
                            )
                        } else {
                            Text("Coordinates needed", fontSize = 12.sp, color = LocalOrangeMap)
                        }
                    }
                }
            }
        }
    }
}

private fun createCustomMarkerBitmap(ctx: Context, name: String, isPending: Boolean): Bitmap? {
    val labelText = if (isPending) "$name (Proposed)" else name
    val pinResId = R.drawable.ic_map_pin
    val pinDrawable = ContextCompat.getDrawable(ctx, pinResId) ?: return null
    
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
    
    return bitmap
}
