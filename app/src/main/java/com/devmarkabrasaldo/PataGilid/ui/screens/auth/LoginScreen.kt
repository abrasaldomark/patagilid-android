package com.devmarkabrasaldo.PataGilid.ui.screens.auth

import android.app.Activity
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.devmarkabrasaldo.PataGilid.data.repository.AuthRepository
import com.devmarkabrasaldo.PataGilid.ui.theme.DarkSurface
import com.devmarkabrasaldo.PataGilid.ui.theme.GliderBlue
import com.devmarkabrasaldo.PataGilid.ui.theme.SummitSteel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    authRepository: AuthRepository,
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                if (account != null) {
                    isLoading = true
                    coroutineScope.launch {
                        try {
                            authRepository.signInWithGoogle(account)
                            isLoading = false
                            onLoginSuccess()
                        } catch (e: Exception) {
                            isLoading = false
                            errorMessage = "Firebase sign in failed: ${e.localizedMessage}"
                            Log.e("LoginScreen", "Auth error", e)
                        }
                    }
                }
            } catch (e: ApiException) {
                errorMessage = "Google Sign-In failed (Code ${e.statusCode}): ${e.localizedMessage}"
                Log.e("LoginScreen", "Google Sign in ApiException", e)
            }
        } else {
            errorMessage = "Sign in cancelled or failed."
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Subtle theme decorative accent gradient at the top matching iOS
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(380.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(GliderBlue.copy(alpha = 0.25f), Color.Transparent)
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            // Branding & Icon
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(GliderBlue.copy(alpha = 0.12f))
                        .border(2.dp, GliderBlue.copy(alpha = 0.3f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Terrain,
                        contentDescription = "PataGilid",
                        tint = GliderBlue,
                        modifier = Modifier.size(64.dp)
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "PataGilid",
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        ),
                        letterSpacing = 1.2.sp
                    )

                    Text(
                        text = "Track Your Journey Across 2,688 Philippine Mountains",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = SummitSteel
                        ),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Account Requirement Banner & Action Box
            Column(
                verticalArrangement = Arrangement.spacedBy(22.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                Surface(
                    color = DarkSurface,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, GliderBlue.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = GliderBlue,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Account Required for Climb Logs",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Text(
                            text = "A verified Google account is required to securely back up your summit progress, photos, and hiking achievements across devices.",
                            fontSize = 13.sp,
                            color = SummitSteel,
                            lineHeight = 18.sp
                        )
                    }
                }

                // Google Sign-In Action Button
                Button(
                    onClick = {
                        errorMessage = null
                        val signInClient = authRepository.getGoogleSignInClient(context)
                        googleSignInLauncher.launch(signInClient.signInIntent)
                    },
                    contentPadding = PaddingValues(0.dp),
                    shape = RoundedCornerShape(16.dp),
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                if (!isLoading) {
                                    Brush.horizontalGradient(colors = listOf(GliderBlue, SummitSteel))
                                } else {
                                    Brush.horizontalGradient(colors = listOf(Color.Gray, Color.DarkGray))
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                                Text(
                                    "Connecting to Google...",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.AccountCircle,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    "Continue with Google",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                if (errorMessage != null) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFE57373),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = errorMessage!!,
                            fontSize = 13.sp,
                            color = Color(0xFFE57373)
                        )
                    }
                }
            }
        }
    }
}
