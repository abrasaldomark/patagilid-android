package com.devmarkabrasaldo.PataGilid.ui.screens.profile

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.net.URL

// iOS matched colors
private val PageBackground = Color(0xFFF2F2F7)
private val PrimaryText = Color(0xFF1C1C1E)
private val SecondaryText = Color(0xFF8E8E93)
private val GliderBlue = Color(0xFF007AFF) // iOS Default Blue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DonationQRScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var qrImageUrl by remember { mutableStateOf<String?>(null) }
    var caption by remember { mutableStateOf("Metrobank · Mark Abrasaldo") }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var savedToPhotos by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }

    // Fetch config on start
    LaunchedEffect(Unit) {
        try {
            val snapshot = FirebaseFirestore.getInstance()
                .collection("app_config")
                .document("support")
                .get()
                .await()

            if (snapshot.exists()) {
                qrImageUrl = snapshot.getString("qrImageUrl")
                val fetchedCaption = snapshot.getString("caption")
                if (!fetchedCaption.isNullOrBlank()) {
                    caption = fetchedCaption
                }
            } else {
                errorMessage = "Support config not found. Please try again later."
            }
        } catch (e: Exception) {
            errorMessage = "Could not load QR code: ${e.localizedMessage}"
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        containerColor = PageBackground,
        topBar = {
            TopAppBar(
                title = { Text("Support the Developer", fontWeight = FontWeight.Bold, color = PrimaryText) },
                actions = {
                    TextButton(onClick = onNavigateBack) {
                        Text("Done", color = GliderBlue, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PageBackground)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Text("⛰️", fontSize = 56.sp)
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Pang akyat lang",
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                color = PrimaryText
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "PataGilid is completely free.\nIf it helped your climbs, a small treat is deeply appreciated!",
                fontSize = 15.sp,
                color = SecondaryText,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            // QR Code Card container
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .size(260.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.Black.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = GliderBlue)
                }
            } else if (errorMessage != null) {
                Box(
                    modifier = Modifier
                        .size(260.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.Black.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFF9500), modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = errorMessage ?: "",
                            fontSize = 12.sp,
                            color = SecondaryText,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            } else if (qrImageUrl != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(qrImageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Donation QR Code",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(260.dp)
                        .shadow(elevation = 16.dp, shape = RoundedCornerShape(20.dp), spotColor = Color.Black.copy(alpha = 0.12f))
                        .background(Color.White, RoundedCornerShape(20.dp))
                        .padding(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Caption
            Text(
                text = caption,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = PrimaryText
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Scan with your banking app",
                fontSize = 12.sp,
                color = SecondaryText
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Save to Photos Button
            if (qrImageUrl != null && !isLoading) {
                val buttonColor by animateColorAsState(targetValue = if (savedToPhotos) Color(0xFF34C759) else GliderBlue)
                val buttonBgColor = buttonColor.copy(alpha = 0.12f)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(buttonBgColor)
                        .clickable {
                            coroutineScope.launch {
                                saveError = null
                                val success = saveImageToGallery(context, qrImageUrl!!)
                                if (success) {
                                    savedToPhotos = true
                                    delay(3000)
                                    savedToPhotos = false
                                } else {
                                    saveError = "Failed to save QR code."
                                }
                            }
                        }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (savedToPhotos) Icons.Default.CheckCircle else Icons.Default.Download,
                            contentDescription = null,
                            tint = buttonColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (savedToPhotos) "Saved to Gallery!" else "Save QR to Gallery",
                            fontWeight = FontWeight.SemiBold,
                            color = buttonColor,
                            fontSize = 15.sp
                        )
                    }
                }

                if (saveError != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = saveError ?: "",
                        fontSize = 12.sp,
                        color = Color(0xFFFF3B30),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Thank You Note
            Text(
                text = "Thank you for supporting PataGilid! 🏔️\uD83D\uDE4F\nEvery peso helps keep our mountain list growing.",
                fontSize = 13.sp,
                color = SecondaryText,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

private suspend fun saveImageToGallery(context: Context, imageUrl: String): Boolean {
    return withContext(Dispatchers.IO) {
        try {
            val url = URL(imageUrl)
            val connection = url.openConnection()
            connection.connect()
            val inputStream = connection.getInputStream()
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            if (bitmap == null) return@withContext false

            val filename = "PataGilid_QR_${System.currentTimeMillis()}.jpg"
            var fos: OutputStream? = null

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                }
                val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                if (imageUri != null) {
                    fos = resolver.openOutputStream(imageUri)
                }
            } else {
                val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val imageFile = java.io.File(imagesDir, filename)
                fos = java.io.FileOutputStream(imageFile)
            }

            fos?.use {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
                true
            } ?: false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
