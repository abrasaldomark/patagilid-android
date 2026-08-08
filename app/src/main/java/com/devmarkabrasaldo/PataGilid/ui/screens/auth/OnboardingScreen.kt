package com.devmarkabrasaldo.PataGilid.ui.screens.auth

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Hiking
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devmarkabrasaldo.PataGilid.ui.theme.GliderBlue
import com.devmarkabrasaldo.PataGilid.ui.theme.OrangeAccent
import com.devmarkabrasaldo.PataGilid.ui.theme.SummitSteel
import kotlinx.coroutines.launch

private data class OnboardingPage(
    val icon: ImageVector,
    val iconTint: Color,
    val badgeText: String?,
    val title: String,
    val subtitle: String
)

@Composable
fun OnboardingScreen(onNavigateToLogin: () -> Unit) {
    val pages = remember {
        listOf(
            OnboardingPage(
                icon = Icons.Default.Terrain,
                iconTint = GliderBlue,
                badgeText = "EXPLORE",
                title = "Discover Philippine Mountains",
                subtitle = "Explore over 2,300 official mountains across Luzon, Visayas, and Mindanao with precise elevations and trail difficulty ratings."
            ),
            OnboardingPage(
                icon = Icons.Default.Tune,
                iconTint = SummitSteel,
                badgeText = "CURATE",
                title = "Filter & Plan Ascents",
                subtitle = "Effortlessly sort mountains by elevation or filter by island groups and provinces to curate your personal hiking bucket list."
            ),
            OnboardingPage(
                icon = Icons.Default.Hiking,
                iconTint = OrangeAccent,
                badgeText = "JOURNAL",
                title = "Log Your Climb Legacies",
                subtitle = "Record your summit triumphs with timestamps, duration times, and personal trail notes whether online or deep in the wilderness."
            ),
            OnboardingPage(
                icon = Icons.Default.PhotoCamera,
                iconTint = OrangeAccent,
                badgeText = null,
                title = "Preserve Climb Memories",
                subtitle = "Attach high-definition photos to any climb log and back up your entire climbing legacy securely to your Google Drive — free for every hiker."
            )
        )
    }

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header: Skip button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                if (pagerState.currentPage < pages.size - 1) {
                    TextButton(
                        onClick = onNavigateToLogin,
                        colors = ButtonDefaults.textButtonColors(
                            containerColor = Color.White.copy(alpha = 0.12f),
                            contentColor = Color.LightGray
                        ),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = "Skip",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.height(48.dp))
                }
            }

            // Pages Swiper
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { index ->
                val page = pages[index]
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Concentric circle emblem matching iOS shadow & circles
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(160.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(144.dp)
                                .clip(CircleShape)
                                .background(page.iconTint.copy(alpha = 0.12f))
                        )
                        Box(
                            modifier = Modifier
                                .size(116.dp)
                                .clip(CircleShape)
                                .background(page.iconTint.copy(alpha = 0.25f))
                        )
                        Icon(
                            imageVector = page.icon,
                            contentDescription = page.title,
                            tint = page.iconTint,
                            modifier = Modifier.size(64.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Badge (if present)
                    page.badgeText?.let { badge ->
                        Surface(
                            color = page.iconTint.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.padding(bottom = 16.dp)
                        ) {
                            Text(
                                text = badge,
                                color = page.iconTint,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.2.sp,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Text(
                        text = page.title,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        ),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = page.subtitle,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = SummitSteel,
                            lineHeight = 24.sp
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }

            // Footer Controls: Custom Dots & Buttons
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Dot pagination indicator
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(pages.size) { index ->
                        val isSelected = pagerState.currentPage == index
                        val width = if (isSelected) 28.dp else 8.dp
                        val color = if (isSelected) pages[index].iconTint else Color.LightGray.copy(alpha = 0.25f)
                        Box(
                            modifier = Modifier
                                .height(8.dp)
                                .width(width)
                                .clip(RoundedCornerShape(4.dp))
                                .background(color)
                                .animateContentSize()
                        )
                    }
                }

                // Action buttons
                val currentPage = pagerState.currentPage
                if (currentPage == pages.size - 1) {
                    Button(
                        onClick = onNavigateToLogin,
                        contentPadding = PaddingValues(0.dp),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(GliderBlue, SummitSteel)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    "Start Exploring Mountains",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = Color.White
                                )
                            }
                        }
                    }
                } else {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(currentPage + 1)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = pages[currentPage].iconTint
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "Next",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}
