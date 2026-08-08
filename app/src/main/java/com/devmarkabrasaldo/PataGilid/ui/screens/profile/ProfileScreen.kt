package com.devmarkabrasaldo.PataGilid.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.devmarkabrasaldo.PataGilid.data.repository.AuthRepository
import com.devmarkabrasaldo.PataGilid.data.repository.MountainRepository
import kotlinx.coroutines.launch

private val SectionHeaderColor = Color(0xFF8E8E93)       // iOS secondary label for section headers
private val PageBackground = Color(0xFFF2F2F7)           // iOS grouped list background
private val CardBackground = Color.White
private val PrimaryText = Color(0xFF1C1C1E)               // iOS primary label
private val SecondaryText = Color(0xFF8E8E93)              // iOS secondary label
private val DestructiveRed = Color(0xFFFF3B30)             // iOS system red
private val AdminOrange = Color(0xFFFF9500)                // Badge & admin accent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    authRepository: AuthRepository,
    mountainRepository: MountainRepository,
    modifier: Modifier = Modifier,
    onNavigateToDonation: () -> Unit,
    onNavigateToAdminQueue: () -> Unit,
    onNavigateToOnboarding: () -> Unit,
    onSignOut: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val isAdmin = remember { authRepository.isAdmin }
    val userEmail = remember { authRepository.currentUser.value?.email ?: "Verified Google Account" }

    // Count pending reviews for admin badge
    var pendingCount by remember { mutableIntStateOf(0) }
    if (isAdmin) {
        val unapproved by mountainRepository.unapprovedMountains.collectAsState(initial = emptyList())
        pendingCount = unapproved.size
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = PageBackground,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PageBackground)
                    .statusBarsPadding()
                    .padding(top = 12.dp, start = 20.dp, end = 20.dp, bottom = 4.dp)
            ) {
                Text(
                    text = "Profile & Settings",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFFF9500)
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            // ─── Section 1: Mountaineer Profile ─────────────────────
            SectionHeader("Mountaineer Profile")

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Profile photo or fallback avatar
                    val photoUrl = authRepository.userPhotoUrl
                    if (photoUrl != null) {
                        AsyncImage(
                            model = photoUrl,
                            contentDescription = "Profile photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFD1D5DB)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = "Default avatar",
                                tint = Color(0xFF9CA3AF),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = authRepository.userDisplayName,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryText
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = userEmail,
                            fontSize = 13.sp,
                            color = SecondaryText
                        )
                    }
                }
            }

            // ─── Section 2: Administrator Control Center ────────────
            if (isAdmin) {
                SectionHeader("Administrator Control Center")

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        // Admin status row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.VerifiedUser,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(26.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Super Admin Mode Active",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryText
                                )
                                Text(
                                    userEmail,
                                    fontSize = 12.sp,
                                    color = SecondaryText
                                )
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(start = 54.dp),
                            thickness = 0.5.dp,
                            color = Color(0xFFE5E5EA)
                        )

                        // Open Moderation Queue
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToAdminQueue() }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Checklist,
                                contentDescription = null,
                                tint = AdminOrange,
                                modifier = Modifier.size(26.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "Open Moderation Queue",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = PrimaryText,
                                modifier = Modifier.weight(1f)
                            )
                            if (pendingCount > 0) {
                                Text(
                                    "$pendingCount",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier
                                        .background(AdminOrange, CircleShape)
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            } else {
                                Text(
                                    "0 Pending",
                                    fontSize = 12.sp,
                                    color = SecondaryText
                                )
                            }
                        }
                    }
                }

                // Admin section footer
                Text(
                    "As a verified PataGilid administrator, you can moderate, approve, or merge crowdsourced local mountains before they appear on the nationwide public list.",
                    fontSize = 13.sp,
                    color = SecondaryText,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
                )
            }

            // ─── Section 3: Support the Developer ───────────────────
            SectionHeader("Support the Developer")

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToDonation() }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Coffee emoji in yellow-tinted box
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                Color(0xFFFFF9C4),
                                RoundedCornerShape(10.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("☕️", fontSize = 22.sp)
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Buy Me a Coffee",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = PrimaryText
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            "Scan my bank QR and fuel the dev's next summit attempt 🥾🏕️⛰️",
                            fontSize = 12.sp,
                            color = SecondaryText,
                            lineHeight = 16.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Icon(
                        Icons.Default.QrCode2,
                        contentDescription = "QR Code",
                        tint = SecondaryText,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Donation section footer
            Text(
                "PataGilid is free to use. If it has helped your mountaineering journeys, a small coffee goes a long way! ☕️",
                fontSize = 13.sp,
                color = SecondaryText,
                lineHeight = 18.sp,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
            )

            // ─── Section 4: Account Settings ────────────────────────
            SectionHeader("Account Settings")

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    // Replay Onboarding Tour
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToOnboarding() }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = AdminOrange,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Replay Onboarding Tour",
                            fontSize = 15.sp,
                            color = PrimaryText
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(start = 46.dp),
                        thickness = 0.5.dp,
                        color = Color(0xFFE5E5EA)
                    )

                    // Sign Out
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                coroutineScope.launch {
                                    authRepository.signOut()
                                    onSignOut()
                                }
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = null,
                            tint = DestructiveRed,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Sign Out of Google Account",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = DestructiveRed
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Spacer(modifier = Modifier.height(24.dp))
    Text(
        text = title,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        color = SectionHeaderColor,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
    )
}
