package com.devmarkabrasaldo.PataGilid.ui.screens.mountains

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Hiking
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import com.devmarkabrasaldo.PataGilid.R
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.devmarkabrasaldo.PataGilid.di.AppContainer
import com.devmarkabrasaldo.PataGilid.ui.screens.climbs.ClimbsListScreen
import com.devmarkabrasaldo.PataGilid.ui.screens.lists.MountainListsScreen
import com.devmarkabrasaldo.PataGilid.ui.screens.lists.MountainListsViewModel
import com.devmarkabrasaldo.PataGilid.ui.screens.climbs.ClimbsListViewModel
import com.devmarkabrasaldo.PataGilid.ui.screens.profile.ProfileScreen

enum class MainTab(val title: String, val iconVector: ImageVector? = null, val iconRes: Int? = null) {
    MOUNTAINS("Mountains", iconVector = Icons.Default.Terrain),
    MY_CLIMBS("Climbs", iconVector = Icons.Default.Hiking),
    MY_LISTS("Lists", iconRes = R.drawable.ic_custom_list),
    PROFILE("Profile", iconVector = Icons.Default.Person)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    container: AppContainer,
    listsViewModel: MountainListsViewModel,
    climbsListViewModel: ClimbsListViewModel,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToAddCustom: () -> Unit,
    onNavigateToHikeLogDetail: (String) -> Unit,
    onNavigateToListDetail: (com.devmarkabrasaldo.PataGilid.domain.models.MountainList) -> Unit,
    onNavigateToDonation: () -> Unit,
    onNavigateToAdminQueue: () -> Unit,
    onNavigateToContributions: () -> Unit,
    onNavigateToSponsors: () -> Unit,
    onNavigateToOnboarding: () -> Unit,
    onSignOut: () -> Unit
) {
    var selectedTab by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(MainTab.MOUNTAINS) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Surface(
                shadowElevation = 8.dp,
                color = Color.White
            ) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = Color(0xFF1A73E8),
                    tonalElevation = 0.dp
                ) {
                    MainTab.entries.forEach { tab ->
                        val selected = selectedTab == tab
                        NavigationBarItem(
                            selected = selected,
                            onClick = { selectedTab = tab },
                            icon = {
                                if (tab.iconVector != null) {
                                    Icon(imageVector = tab.iconVector, contentDescription = tab.title)
                                } else if (tab.iconRes != null) {
                                    Icon(painter = painterResource(id = tab.iconRes), contentDescription = tab.title)
                                }
                            },
                            label = {
                                Text(
                                    text = tab.title,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color(0xFF1A73E8),
                                selectedTextColor = Color(0xFF1A73E8),
                                indicatorColor = Color(0xFFE8F0FE),
                                unselectedIconColor = Color(0xFF5F6368),
                                unselectedTextColor = Color(0xFF5F6368)
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Modifier.padding(innerPadding).let { padding ->
            when (selectedTab) {
                MainTab.MOUNTAINS -> MountainsListScreen(
                    repository = container.mountainRepository,
                    authRepository = container.authRepository,
                    modifier = padding,
                    onNavigateToDetail = onNavigateToDetail,
                    onNavigateToAddCustom = onNavigateToAddCustom,
                    onNavigateToAdminQueue = onNavigateToAdminQueue
                )
                MainTab.MY_CLIMBS -> ClimbsListScreen(
                    repository = container.mountainRepository,
                    modifier = padding,
                    onNavigateToDetail = onNavigateToHikeLogDetail,
                    vm = climbsListViewModel
                )
                MainTab.MY_LISTS -> MountainListsScreen(
                    viewModel = listsViewModel,
                    onNavigateToDetail = { list ->
                        // Navigate to list detail — handled via nav graph
                        onNavigateToListDetail(list)
                    },
                    modifier = padding
                )
                MainTab.PROFILE -> ProfileScreen(
                    authRepository = container.authRepository,
                    mountainRepository = container.mountainRepository,
                    modifier = padding,
                    onNavigateToDonation = onNavigateToDonation,
                    onNavigateToAdminQueue = onNavigateToAdminQueue,
                    onNavigateToContributions = onNavigateToContributions,
                    onNavigateToSponsors = onNavigateToSponsors,
                    onNavigateToOnboarding = onNavigateToOnboarding,
                    onSignOut = onSignOut
                )
            }
        }
    }
}
